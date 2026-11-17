package com.case_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.case_api.controller.SpikeRequest;
import com.case_api.controller.SpikeResponse;
import com.case_api.repository.SpikeFailedResultRepository;
import com.case_api.repository.SpikeProductLockRepository;
import com.case_api.repository.SpikeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Live MySQL IT via Testcontainers — happy store, concurrent lock (check 9), store TX rollback (check 3),
 * null nest (check 10), abnormal failed_test_items array (check 11).
 * Check 3 uses a hand-built null product_code knife; not a MockSpikeDataRetriever category.
 * No class-level {@code @Transactional}: each call must commit so {@code FOR UPDATE} can serialize.
 */
@SpringBootTest
@Testcontainers
class SpikeServiceIT {

	@Container
	@SuppressWarnings("resource")
	static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.21")
			.withDatabaseName("case")
			.withUsername("case-user")
			.withPassword("case");

	@DynamicPropertySource
	static void datasourceProps(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
	}

	@Autowired
	private SpikeService spikeService;

	@Autowired
	private SpikeStoreWriter spikeStoreWriter;

	@Autowired
	private SpikeRepository spikeRepository;

	@Autowired
	private SpikeFailedResultRepository spikeFailedResultRepository;

	@Autowired
	private SpikeProductLockRepository spikeProductLockRepository;

	@AfterEach
	void cleanup() {
		spikeFailedResultRepository.deleteAll();
		spikeRepository.deleteAll();
		spikeProductLockRepository.deleteAll();
	}

	@Test
	void reportSpike_happyPath_storesSpikeAndFailedResult() {
		SpikeRequest request = new SpikeRequest();
		request.setProductName(MockSpikeDataRetriever.PRODUCT_HAS_FAILED);

		SpikeResponse body = storedBody(spikeService.reportSpike(request));
		assertThat(body.repeated()).isFalse();
		assertThat(body.failedManufacturingResults()).hasSize(1);
		assertThat(body.failedManufacturingResults().getFirst().id()).isEqualTo(1L);

		assertThat(spikeRepository.count()).isEqualTo(1);
		assertThat(spikeFailedResultRepository.count()).isEqualTo(1);
		assertThat(spikeProductLockRepository.count()).isEqualTo(1);
	}

	@Test
	void storeOrReturnRepeat_whenProductCodeNull_rollsBackWholeStore() {
		String productName = "Ford-APCU-NullProductCode-001";
		FailedManufacturingResult failed = new FailedManufacturingResult(
				9001L,
				"APCU-CD4-PCBA-9001",
				"Ford-APCU",
				"WO-NULL-PC-001",
				"COLD_EOL",
				"AEL-01-EOL-01",
				"RACK-APCU-G1-01",
				"FIXTURE-APCU-G2-01",
				1,
				"FAIL",
				30,
				"sw-1.0",
				"hw-2.0",
				Map.of("switch-open", "0010"),
				"switch-error",
				"Alice",
				"day-shift",
				Instant.parse("2026-11-06T12:00:00Z"));
		// Test knife only: null product_code violates spike.product_code NOT NULL.
		SpikeSearchResult search =
				new SpikeSearchResult(null, List.of("WO-NULL-PC-001"), List.of(failed));

		assertThatThrownBy(() -> spikeStoreWriter.storeOrReturnRepeat(productName, search))
				.isInstanceOf(DataIntegrityViolationException.class);

		assertThat(spikeRepository.count()).isZero();
		assertThat(spikeFailedResultRepository.count()).isZero();
		assertThat(spikeProductLockRepository.count()).isZero();
	}

	@Test
	void reportSpike_whenNestMissing_persistsNullNestNumber() {
		SpikeRequest request = new SpikeRequest();
		request.setProductName(MockSpikeDataRetriever.PRODUCT_MISSING_NEST);

		SpikeResponse body = storedBody(spikeService.reportSpike(request));

		assertThat(body.repeated()).isFalse();
		assertThat(spikeRepository.count()).isEqualTo(1);
		assertThat(spikeFailedResultRepository.count()).isEqualTo(1);

		var rows = spikeFailedResultRepository.findBySpikeId(body.id());
		assertThat(rows).hasSize(1);
		assertThat(rows.getFirst().getNestNumber()).isNull();
	}

	@Test
	void reportSpike_whenFailedTestItemsIsArray_persistsAndSetsAbnormal() {
		SpikeRequest request = new SpikeRequest();
		request.setProductName(MockSpikeDataRetriever.PRODUCT_ABNORMAL_TEST_ITEMS);

		SpikeResponse body = storedBody(spikeService.reportSpike(request));

		assertThat(body.repeated()).isFalse();
		assertThat(body.failedTestItemsAbnormal()).isTrue();

		var spikes = spikeRepository.findByProductName(MockSpikeDataRetriever.PRODUCT_ABNORMAL_TEST_ITEMS);
		assertThat(spikes).hasSize(1);
		assertThat(spikes.getFirst().isFailedTestItemsAbnormal()).isTrue();

		var rows = spikeFailedResultRepository.findBySpikeId(spikes.getFirst().getId());
		assertThat(rows).hasSize(1);
		assertThat(rows.getFirst().getFailedTestItems())
				.isEqualTo(List.of("switch-open", "switch-close"));
	}

	@Test
	void reportSpike_twoConcurrentSameIdSet_oneNewOneRepeated() throws Exception {
		String productName = MockSpikeDataRetriever.PRODUCT_HAS_FAILED;
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Future<ReportSpikeResult> first =
					pool.submit(() -> reportAfterBarrier(productName, ready, start));
			Future<ReportSpikeResult> second =
					pool.submit(() -> reportAfterBarrier(productName, ready, start));

			assertTrue(ready.await(15, TimeUnit.SECONDS), "workers did not reach the barrier");
			start.countDown();

			SpikeResponse a = storedBody(first.get(60, TimeUnit.SECONDS));
			SpikeResponse b = storedBody(second.get(60, TimeUnit.SECONDS));

			long newStores = List.of(a, b).stream().filter(r -> !r.repeated()).count();
			long repeats = List.of(a, b).stream().filter(SpikeResponse::repeated).count();
			assertThat(newStores).as("one new store (201 shape)").isEqualTo(1);
			assertThat(repeats).as("one repeat (200 shape)").isEqualTo(1);
			assertThat(a.id()).isEqualTo(b.id());

			assertThat(spikeRepository.findByProductName(productName)).hasSize(1);
			assertThat(spikeFailedResultRepository.count()).isEqualTo(1);
		}
		finally {
			pool.shutdownNow();
		}
	}

	private ReportSpikeResult reportAfterBarrier(
			String productName, CountDownLatch ready, CountDownLatch start) throws Exception {
		ready.countDown();
		assertTrue(start.await(30, TimeUnit.SECONDS), "start signal timed out");
		SpikeRequest request = new SpikeRequest();
		request.setProductName(productName);
		return spikeService.reportSpike(request);
	}

	private static SpikeResponse storedBody(ReportSpikeResult result) {
		assertThat(result).isInstanceOf(ReportSpikeResult.Stored.class);
		return ((ReportSpikeResult.Stored) result).body();
	}
}
