package com.case_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.case_api.controller.NoFailedPartResponse;
import com.case_api.controller.SpikeRequest;
import com.case_api.controller.SpikeResponse;
import com.case_api.repository.SpikeEntity;
import com.case_api.repository.SpikeFailedResultEntity;
import com.case_api.repository.SpikeFailedResultRepository;
import com.case_api.repository.SpikeProductLockEntity;
import com.case_api.repository.SpikeProductLockRepository;
import com.case_api.repository.SpikeRepository;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/**
 * API checks 1, 2, 6, 7, 8, 9 (lock unit path), 10, 11, 12 (repositories mocked).
 * Check 9 concurrent wait still needs Testcontainers + real MySQL.
 */
@ExtendWith(MockitoExtension.class)
class SpikeServiceTest {

	@Mock
	private SpikeDataRetriever spikeDataRetriever;

	@Mock
	private SpikeRepository spikeRepository;

	@Mock
	private SpikeFailedResultRepository spikeFailedResultRepository;

	@Mock
	private SpikeProductLockRepository spikeProductLockRepository;

	private SpikeService spikeService;

	@BeforeEach
	void defaults() {
		SpikeStoreWriter spikeStoreWriter = new SpikeStoreWriter(
				spikeRepository,
				spikeFailedResultRepository,
				spikeProductLockRepository);
		spikeService = new SpikeService(spikeDataRetriever, spikeStoreWriter);
		lenient().when(spikeRepository.findByProductName(anyString())).thenReturn(List.of());
		lenient().when(spikeProductLockRepository.insertIgnore(anyString(), any(Instant.class)))
				.thenReturn(1);
		lenient().when(spikeProductLockRepository.findByProductNameForUpdate(anyString()))
				.thenAnswer(invocation -> {
					SpikeProductLockEntity lock = new SpikeProductLockEntity();
					lock.setProductName(invocation.getArgument(0));
					lock.setCreatedAt(Instant.now());
					return Optional.of(lock);
				});
	}

	@Test
	void reportSpike_whenNoFailedPart_returnsMessageAndDoesNotStore() {
		String productName = MockSpikeDataRetriever.PRODUCT_ALL_PASS;
		when(spikeDataRetriever.search(productName))
				.thenReturn(new SpikeSearchResult("Ford-APCU-ALL-PASS", List.of("WO-1"), new ArrayList<>()));

		SpikeRequest request = new SpikeRequest();
		request.setProductName(productName);

		Object result = spikeService.reportSpike(request);

		assertThat(result).isInstanceOf(NoFailedPartResponse.class);
		NoFailedPartResponse body = (NoFailedPartResponse) result;
		assertThat(body.productName()).isEqualTo(productName);
		assertThat(body.message()).isEqualTo("no failed part found under " + productName);
		verify(spikeRepository, never()).save(any());
		verify(spikeFailedResultRepository, never()).save(any());
		verify(spikeProductLockRepository, never()).insertIgnore(anyString(), any(Instant.class));
		verify(spikeProductLockRepository, never()).findByProductNameForUpdate(anyString());
	}

	@Test
	void reportSpike_whenHasFailed_storesSpikeAndReturns201ShapeWithRepeatedFalse() {
		String productName = MockSpikeDataRetriever.PRODUCT_HAS_FAILED;
		List<FailedManufacturingResult> failed = new ArrayList<>();
		failed.add(sampleResult(1L, Map.of("switch-open", "0010", "switch-close", "0001")));
		when(spikeDataRetriever.search(productName))
				.thenReturn(new SpikeSearchResult("Ford-APCU-01", List.of("WO-DEMO-001", "WO-DEMO-002"), failed));

		SpikeEntity savedSpike = existingSpike(42L, productName, "Ford-APCU-01");
		when(spikeRepository.save(any(SpikeEntity.class))).thenReturn(savedSpike);
		when(spikeFailedResultRepository.save(any(SpikeFailedResultEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		SpikeRequest request = new SpikeRequest();
		request.setProductName(productName);

		Object result = spikeService.reportSpike(request);

		assertThat(result).isInstanceOf(SpikeResponse.class);
		SpikeResponse body = (SpikeResponse) result;
		assertThat(body.repeated()).isFalse();
		assertThat(body.id()).isEqualTo(42L);
		assertThat(body.failedTestItemsAbnormal()).isFalse();
		assertThat(body.description())
				.isEqualTo("1 unit failed on EOL test, failed test items: switch-open,switch-close.");
		assertThat(body.failedManufacturingResults()).hasSize(1);
		assertThat(body.failedManufacturingResults().getFirst().spikeId()).isEqualTo(42L);
		assertThat(body.failedManufacturingResults().getFirst().id()).isEqualTo(1L);

		ArgumentCaptor<SpikeEntity> spikeCaptor = ArgumentCaptor.forClass(SpikeEntity.class);
		verify(spikeRepository).save(spikeCaptor.capture());
		assertThat(spikeCaptor.getValue().isFailedTestItemsAbnormal()).isFalse();

		ArgumentCaptor<SpikeFailedResultEntity> rowCaptor =
				ArgumentCaptor.forClass(SpikeFailedResultEntity.class);
		verify(spikeFailedResultRepository).save(rowCaptor.capture());
		assertThat(rowCaptor.getValue().getSpikeId()).isEqualTo(42L);
		assertThat(rowCaptor.getValue().getId()).isEqualTo(1L);
		assertThat(rowCaptor.getValue().getNestNumber()).isEqualTo(1);
	}

	@Test
	void reportSpike_whenRetrieverReturnsMoreThan100_trimsTo100MostRecentBeforeStore() {
		List<FailedManufacturingResult> oversized = MockSpikeDataRetriever.buildFailedPool(150);
		when(spikeDataRetriever.search("trim-product"))
				.thenReturn(new SpikeSearchResult("CODE", List.of("WO-1"), oversized));

		SpikeEntity savedSpike = existingSpike(7L, "trim-product", "CODE");
		when(spikeRepository.save(any(SpikeEntity.class))).thenReturn(savedSpike);
		when(spikeFailedResultRepository.save(any(SpikeFailedResultEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		SpikeRequest request = new SpikeRequest();
		request.setProductName("trim-product");

		SpikeResponse body = (SpikeResponse) spikeService.reportSpike(request);

		assertThat(body.failedManufacturingResults()).hasSize(100);
		assertThat(body.failedManufacturingResults().getFirst().id()).isEqualTo(150L);
		assertThat(body.failedManufacturingResults().getLast().id()).isEqualTo(51L);
		assertThat(oversized).hasSize(100);
	}

	@Test
	void reportSpike_whenNestMissing_storesNullAndDoesNotRefuse() {
		String productName = MockSpikeDataRetriever.PRODUCT_MISSING_NEST;
		List<FailedManufacturingResult> failed = new ArrayList<>();
		failed.add(new FailedManufacturingResult(
				10L, "APCU-CD4-PCBA-10", "Ford-APCU", "WO-DEMO-001", "COLD_EOL",
				"AEL-01-EOL-01", "RACK-APCU-G1-01", "FIXTURE-APCU-G2-01", null,
				"FAIL", 30, "sw-1.0", "hw-2.0",
				Map.of("switch-open", "0010"),
				"switch-error", "Alice", "day-shift",
				Instant.parse("2026-11-06T12:00:00Z")));
		when(spikeDataRetriever.search(productName))
				.thenReturn(new SpikeSearchResult("Ford-APCU-MISSING-NEST", List.of("WO-DEMO-001"), failed));

		SpikeEntity savedSpike = existingSpike(8L, productName, "Ford-APCU-MISSING-NEST");
		when(spikeRepository.save(any(SpikeEntity.class))).thenReturn(savedSpike);
		when(spikeFailedResultRepository.save(any(SpikeFailedResultEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		ListAppender<ILoggingEvent> logAppender = attachSpikeServiceLogAppender();
		try {
			SpikeRequest request = new SpikeRequest();
			request.setProductName(productName);

			SpikeResponse body = (SpikeResponse) spikeService.reportSpike(request);

			assertThat(body.repeated()).isFalse();
			ArgumentCaptor<SpikeFailedResultEntity> rowCaptor =
					ArgumentCaptor.forClass(SpikeFailedResultEntity.class);
			verify(spikeFailedResultRepository).save(rowCaptor.capture());
			assertThat(rowCaptor.getValue().getNestNumber()).isNull();

			assertThat(logAppender.list)
					.anySatisfy(event -> {
						assertThat(event.getLevel()).isEqualTo(Level.WARN);
						assertThat(event.getFormattedMessage())
								.contains("MES equipment gap")
								.contains("product_name=" + productName)
								.contains("manufacturing_result_id=10")
								.contains("station_id=AEL-01-EOL-01")
								.contains("nest_number");
					});
		}
		finally {
			detachSpikeServiceLogAppender(logAppender);
		}
	}

	@Test
	void reportSpike_whenFailedTestItemsAbnormal_setsFlagAndSkipsKeysInDescription() {
		String productName = MockSpikeDataRetriever.PRODUCT_ABNORMAL_TEST_ITEMS;
		List<FailedManufacturingResult> failed = new ArrayList<>();
		failed.add(new FailedManufacturingResult(
				11L, "APCU-CD4-PCBA-11", "Ford-APCU", "WO-DEMO-001", "COLD_EOL",
				"AEL-01-EOL-01", "RACK-APCU-G1-01", "FIXTURE-APCU-G2-01", 1,
				"FAIL", 30, "sw-1.0", "hw-2.0",
				List.of("switch-open", "switch-close"),
				"switch-error", "Alice", "day-shift",
				Instant.parse("2026-11-06T12:00:00Z")));
		when(spikeDataRetriever.search(productName))
				.thenReturn(new SpikeSearchResult("Ford-APCU-ABNORMAL", List.of("WO-DEMO-001"), failed));

		when(spikeRepository.save(any(SpikeEntity.class))).thenAnswer(invocation -> {
			SpikeEntity spike = invocation.getArgument(0);
			spike.setId(9L);
			return spike;
		});
		when(spikeFailedResultRepository.save(any(SpikeFailedResultEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		ListAppender<ILoggingEvent> logAppender = attachSpikeServiceLogAppender();
		try {
			SpikeRequest request = new SpikeRequest();
			request.setProductName(productName);

			SpikeResponse body = (SpikeResponse) spikeService.reportSpike(request);

			assertThat(body.repeated()).isFalse();
			assertThat(body.failedTestItemsAbnormal()).isTrue();
			assertThat(body.description()).isEqualTo("1 unit failed on EOL test, failed test items: .");
			ArgumentCaptor<SpikeFailedResultEntity> rowCaptor =
					ArgumentCaptor.forClass(SpikeFailedResultEntity.class);
			verify(spikeFailedResultRepository).save(rowCaptor.capture());
			assertThat(rowCaptor.getValue().getFailedTestItems())
					.isEqualTo(List.of("switch-open", "switch-close"));

			assertThat(logAppender.list)
					.anySatisfy(event -> {
						assertThat(event.getLevel()).isEqualTo(Level.WARN);
						assertThat(event.getFormattedMessage())
								.contains("failed_test_items missing or not pairs")
								.contains("product_name=" + productName)
								.contains("manufacturing_result_id=11");
					});
		}
		finally {
			detachSpikeServiceLogAppender(logAppender);
		}
	}

	@Test
	void reportSpike_whenIdSetMatchesExisting_returnsRepeatedAndStoresNothing() {
		String productName = MockSpikeDataRetriever.PRODUCT_HAS_FAILED;
		List<FailedManufacturingResult> incoming = new ArrayList<>();
		incoming.add(sampleResult(1L, Map.of("switch-open", "0010", "switch-close", "0001")));
		when(spikeDataRetriever.search(productName))
				.thenReturn(new SpikeSearchResult("Ford-APCU-01", List.of("WO-DEMO-001"), incoming));

		SpikeEntity existing = existingSpike(42L, productName, "Ford-APCU-01");
		SpikeFailedResultEntity storedRow = storedRow(42L, 1L, Map.of("switch-open", "0010", "switch-close", "0001"));
		when(spikeRepository.findByProductName(productName)).thenReturn(List.of(existing));
		when(spikeFailedResultRepository.findBySpikeId(42L)).thenReturn(List.of(storedRow));

		SpikeRequest request = new SpikeRequest();
		request.setProductName(productName);

		SpikeResponse body = (SpikeResponse) spikeService.reportSpike(request);

		assertThat(body.repeated()).isTrue();
		assertThat(body.id()).isEqualTo(42L);
		assertThat(body.failedManufacturingResults()).hasSize(1);
		assertThat(body.failedManufacturingResults().getFirst().id()).isEqualTo(1L);
		verify(spikeRepository, never()).save(any());
		verify(spikeFailedResultRepository, never()).save(any());
	}

	@Test
	void reportSpike_whenNewManufacturingResultId_storesNewSpike() {
		String productName = MockSpikeDataRetriever.PRODUCT_HAS_FAILED;
		List<FailedManufacturingResult> incoming = new ArrayList<>();
		incoming.add(sampleResult(1L, Map.of("switch-open", "0010")));
		incoming.add(sampleResult(2L, Map.of("switch-close", "0001")));
		when(spikeDataRetriever.search(productName))
				.thenReturn(new SpikeSearchResult("Ford-APCU-01", List.of("WO-DEMO-001"), incoming));

		SpikeEntity existing = existingSpike(42L, productName, "Ford-APCU-01");
		SpikeFailedResultEntity storedRow = storedRow(42L, 1L, Map.of("switch-open", "0010"));
		when(spikeRepository.findByProductName(productName)).thenReturn(List.of(existing));
		when(spikeFailedResultRepository.findBySpikeId(42L)).thenReturn(List.of(storedRow));

		SpikeEntity savedSpike = existingSpike(99L, productName, "Ford-APCU-01");
		when(spikeRepository.save(any(SpikeEntity.class))).thenReturn(savedSpike);
		when(spikeFailedResultRepository.save(any(SpikeFailedResultEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		SpikeRequest request = new SpikeRequest();
		request.setProductName(productName);

		SpikeResponse body = (SpikeResponse) spikeService.reportSpike(request);

		assertThat(body.repeated()).isFalse();
		assertThat(body.id()).isEqualTo(99L);
		assertThat(body.failedManufacturingResults()).hasSize(2);
		verify(spikeRepository).save(any(SpikeEntity.class));
	}

	@Test
	void reportSpike_acquiresProductNameLockBeforeRepeatCheck() {
		String productName = MockSpikeDataRetriever.PRODUCT_HAS_FAILED;
		List<FailedManufacturingResult> incoming = new ArrayList<>();
		incoming.add(sampleResult(1L, Map.of("switch-open", "0010")));
		when(spikeDataRetriever.search(productName))
				.thenReturn(new SpikeSearchResult("Ford-APCU-01", List.of("WO-DEMO-001"), incoming));

		SpikeEntity existing = existingSpike(42L, productName, "Ford-APCU-01");
		when(spikeRepository.findByProductName(productName)).thenReturn(List.of(existing));
		when(spikeFailedResultRepository.findBySpikeId(42L))
				.thenReturn(List.of(storedRow(42L, 1L, Map.of("switch-open", "0010"))));

		SpikeRequest request = new SpikeRequest();
		request.setProductName(productName);

		spikeService.reportSpike(request);

		InOrder order = inOrder(spikeProductLockRepository, spikeRepository);
		order.verify(spikeProductLockRepository).insertIgnore(eq(productName), any(Instant.class));
		order.verify(spikeProductLockRepository).findByProductNameForUpdate(productName);
		order.verify(spikeRepository).findByProductName(productName);
	}

	@Test
	void reportSpike_whenLockInsertIgnoresExistingRow_continuesWithForUpdate() {
		String productName = MockSpikeDataRetriever.PRODUCT_HAS_FAILED;
		List<FailedManufacturingResult> incoming = new ArrayList<>();
		incoming.add(sampleResult(1L, Map.of("switch-open", "0010")));
		when(spikeDataRetriever.search(productName))
				.thenReturn(new SpikeSearchResult("Ford-APCU-01", List.of("WO-DEMO-001"), incoming));

		when(spikeProductLockRepository.insertIgnore(eq(productName), any(Instant.class))).thenReturn(0);

		SpikeEntity existing = existingSpike(42L, productName, "Ford-APCU-01");
		when(spikeRepository.findByProductName(productName)).thenReturn(List.of(existing));
		when(spikeFailedResultRepository.findBySpikeId(42L))
				.thenReturn(List.of(storedRow(42L, 1L, Map.of("switch-open", "0010"))));

		SpikeRequest request = new SpikeRequest();
		request.setProductName(productName);

		SpikeResponse body = (SpikeResponse) spikeService.reportSpike(request);

		assertThat(body.repeated()).isTrue();
		verify(spikeProductLockRepository).findByProductNameForUpdate(productName);
		verify(spikeRepository, never()).save(any());
	}

	@Test
	void reportSpike_whenSameIdsButDifferentFailedTestItems_stillRepeated() {
		String productName = MockSpikeDataRetriever.PRODUCT_HAS_FAILED;
		List<FailedManufacturingResult> incoming = new ArrayList<>();
		incoming.add(sampleResult(1L, Map.of("switch-open", "CHANGED")));
		when(spikeDataRetriever.search(productName))
				.thenReturn(new SpikeSearchResult("Ford-APCU-01", List.of("WO-DEMO-001"), incoming));

		SpikeEntity existing = existingSpike(42L, productName, "Ford-APCU-01");
		SpikeFailedResultEntity storedRow =
				storedRow(42L, 1L, Map.of("switch-open", "0010", "switch-close", "0001"));
		when(spikeRepository.findByProductName(productName)).thenReturn(List.of(existing));
		when(spikeFailedResultRepository.findBySpikeId(42L)).thenReturn(List.of(storedRow));

		SpikeRequest request = new SpikeRequest();
		request.setProductName(productName);

		SpikeResponse body = (SpikeResponse) spikeService.reportSpike(request);

		assertThat(body.repeated()).isTrue();
		assertThat(body.id()).isEqualTo(42L);
		assertThat(body.failedManufacturingResults().getFirst().failedTestItems())
				.isEqualTo(Map.of("switch-open", "0010", "switch-close", "0001"));
		verify(spikeRepository, never()).save(any());
		verify(spikeFailedResultRepository, never()).save(any());
	}

	private static FailedManufacturingResult sampleResult(long id, Object failedTestItems) {
		return new FailedManufacturingResult(
				id, "APCU-CD4-PCBA-" + id, "Ford-APCU", "WO-DEMO-001", "COLD_EOL",
				"AEL-01-EOL-01", "RACK-APCU-G1-01", "FIXTURE-APCU-G2-01", 1,
				"FAIL", 30, "sw-1.0", "hw-2.0",
				failedTestItems,
				"switch-error", "Alice", "day-shift",
				Instant.parse("2026-11-06T12:00:00Z"));
	}

	private static SpikeEntity existingSpike(long id, String productName, String productCode) {
		SpikeEntity spike = new SpikeEntity();
		spike.setId(id);
		spike.setProductName(productName);
		spike.setProductCode(productCode);
		spike.setWorkingOrders(List.of("WO-DEMO-001"));
		spike.setDescription("1 unit failed on EOL test, failed test items: switch-open,switch-close.");
		spike.setFailedTestItemsAbnormal(false);
		spike.setCreatedAt(Instant.parse("2026-11-06T12:05:00Z"));
		return spike;
	}

	private static SpikeFailedResultEntity storedRow(long spikeId, long id, Object failedTestItems) {
		SpikeFailedResultEntity row = new SpikeFailedResultEntity();
		row.setSpikeId(spikeId);
		row.setId(id);
		row.setSerialNumber("APCU-CD4-PCBA-" + id);
		row.setFailedTestItems(failedTestItems);
		row.setCreatedAt(Instant.parse("2026-11-06T12:00:00Z"));
		return row;
	}

	private static ListAppender<ILoggingEvent> attachSpikeServiceLogAppender() {
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		Logger logger = (Logger) LoggerFactory.getLogger(SpikeStoreWriter.class);
		logger.addAppender(appender);
		return appender;
	}

	private static void detachSpikeServiceLogAppender(ListAppender<ILoggingEvent> appender) {
		Logger logger = (Logger) LoggerFactory.getLogger(SpikeStoreWriter.class);
		logger.detachAppender(appender);
		appender.stop();
	}
}
