package com.case_api.service;

import com.case_api.controller.SpikeResponse;
import com.case_api.controller.SpikeResponse.FailedResultResponse;
import com.case_api.repository.SpikeEntity;
import com.case_api.repository.SpikeFailedResultEntity;
import com.case_api.repository.SpikeFailedResultRepository;
import com.case_api.repository.SpikeProductLockRepository;
import com.case_api.repository.SpikeRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lock, repeat check, and write. Callers must enter this bean so Spring opens the transaction.
 */
@Service
public class SpikeStoreWriter {

	private static final Logger log = LoggerFactory.getLogger(SpikeStoreWriter.class);

	private final SpikeRepository spikeRepository;
	private final SpikeFailedResultRepository spikeFailedResultRepository;
	private final SpikeProductLockRepository spikeProductLockRepository;

	public SpikeStoreWriter(
			SpikeRepository spikeRepository,
			SpikeFailedResultRepository spikeFailedResultRepository,
			SpikeProductLockRepository spikeProductLockRepository) {
		this.spikeRepository = spikeRepository;
		this.spikeFailedResultRepository = spikeFailedResultRepository;
		this.spikeProductLockRepository = spikeProductLockRepository;
	}

	/**
	 * Only proxy entry for the store TX. Helpers below have no {@code @Transactional}.
	 * They run on {@code this} and already sit in this method's transaction.
	 */
	@Transactional
	public SpikeResponse storeOrReturnRepeat(String productName, SpikeSearchResult search) {
		acquireProductNameLock(productName);
		SpikeResponse repeated = findRepeatByIdSet(productName, search.failedManufacturingResults());
		if (repeated != null) {
			return repeated;
		}
		return storeNewSpike(productName, search);
	}

	/**
	 * Insert the lock row if missing ({@code INSERT IGNORE}), then {@code SELECT … FOR UPDATE}.
	 * A primary-key clash inserts nothing and does not throw, so the store transaction stays clean.
	 * No {@code @Transactional}: called via {@code this} from {@link #storeOrReturnRepeat}.
	 */
	void acquireProductNameLock(String productName) {
		spikeProductLockRepository.insertIgnore(productName, Instant.now());
		spikeProductLockRepository
				.findByProductNameForUpdate(productName)
				.orElseThrow(() -> new IllegalStateException(
						"spike_product_lock row missing after insert for product_name=" + productName));
	}

	/**
	 * Id-set only. Different {@code failed_test_items} on the same ids is still a repeat.
	 * No {@code @Transactional}: called via {@code this} from {@link #storeOrReturnRepeat}.
	 */
	SpikeResponse findRepeatByIdSet(
			String productName, List<FailedManufacturingResult> failedManufacturingResults) {
		Set<Long> incomingIds = failedManufacturingResults.stream()
				.map(FailedManufacturingResult::id)
				.collect(Collectors.toCollection(HashSet::new));

		List<SpikeEntity> existingSpikes = spikeRepository.findByProductName(productName);
		for (SpikeEntity existing : existingSpikes) {
			List<SpikeFailedResultEntity> storedRows =
					spikeFailedResultRepository.findBySpikeId(existing.getId());
			Set<Long> storedIds = storedRows.stream()
					.map(SpikeFailedResultEntity::getId)
					.collect(Collectors.toCollection(HashSet::new));
			if (storedIds.equals(incomingIds)) {
				return toSpikeResponse(existing, storedRows, true);
			}
		}
		return null;
	}

	/**
	 * Writes {@code spike} and {@code spike_failed_result} rows. Builds description and
	 * {@code failed_test_items_abnormal}. Returns {@code repeated=false}.
	 * No {@code @Transactional}: called via {@code this} from {@link #storeOrReturnRepeat}.
	 * An annotation here would not join; the call never enters the proxy.
	 */
	SpikeResponse storeNewSpike(String productName, SpikeSearchResult search) {
		List<FailedManufacturingResult> failedManufacturingResults =
				search.failedManufacturingResults();

		boolean anyAbnormal = false;
		Set<String> testItemKeys = new LinkedHashSet<>();
		for (FailedManufacturingResult result : failedManufacturingResults) {
			logMissingEquipment(productName, result);
			if (isAbnormalFailedTestItems(result.failedTestItems())) {
				anyAbnormal = true;
				log.warn(
						"failed_test_items missing or not pairs; product_name={}, manufacturing_result_id={}",
						productName,
						result.id());
			}
			else {
				collectPairKeys(result.failedTestItems(), testItemKeys);
			}
		}

		String description = buildDescription(failedManufacturingResults.size(), testItemKeys);

		SpikeEntity spike = new SpikeEntity();
		spike.setProductName(productName);
		spike.setProductCode(search.productCode());
		spike.setWorkingOrders(search.workingOrders());
		spike.setDescription(description);
		spike.setFailedTestItemsAbnormal(anyAbnormal);
		spike.setCreatedAt(Instant.now());
		SpikeEntity saved = spikeRepository.save(spike);

		List<FailedResultResponse> responseResults = new ArrayList<>(failedManufacturingResults.size());
		for (FailedManufacturingResult result : failedManufacturingResults) {
			SpikeFailedResultEntity row = toSpikeFailedResultEntity(saved.getId(), result);
			spikeFailedResultRepository.save(row);
			responseResults.add(toFailedResultResponse(row));
		}

		return new SpikeResponse(
				saved.getId(),
				saved.getProductName(),
				saved.getProductCode(),
				saved.getWorkingOrders(),
				saved.getDescription(),
				saved.isFailedTestItemsAbnormal(),
				saved.getCreatedAt(),
				false,
				responseResults);
	}

	/** List sentence: failed-row count plus unique pair keys from normal {@code failed_test_items}. */
	static String buildDescription(int failedCount, Set<String> testItemKeys) {
		String keys = String.join(",", testItemKeys);
		return failedCount + " unit failed on EOL test, failed test items: " + keys + ".";
	}

	/**
	 * Abnormal when missing/null, an array, or a primitive. An object of pairs (including empty) is normal.
	 */
	static boolean isAbnormalFailedTestItems(Object failedTestItems) {
		if (failedTestItems == null) {
			return true;
		}
		if (failedTestItems instanceof Map<?, ?> map) {
			return false;
		}
		return true;
	}

	/** Adds test-item keys from a pairs object into {@code keys}. No-op when abnormal. */
	private static void collectPairKeys(Object failedTestItems, Set<String> keys) {
		if (failedTestItems instanceof Map<?, ?> map) {
			for (Object key : map.keySet()) {
				if (key != null) {
					keys.add(String.valueOf(key));
				}
			}
		}
	}

	/**
	 * EOL expected shape: rack_id, fixture_id, nest_number. Logs gaps; does not refuse the store.
	 */
	private void logMissingEquipment(String productName, FailedManufacturingResult result) {
		List<String> missing = new ArrayList<>();
		if (isBlank(result.rackId())) {
			missing.add("rack_id");
		}
		if (isBlank(result.fixtureId())) {
			missing.add("fixture_id");
		}
		if (result.nestNumber() == null) {
			missing.add("nest_number");
		}
		if (missing.isEmpty()) {
			return;
		}
		log.warn(
				"MES equipment gap; product_name={}, manufacturing_result_id={}, station_id={}, missing_fields={}",
				productName,
				result.id(),
				result.stationId(),
				missing);
	}

	/** True when null or whitespace-only. */
	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	/** Maps a stored spike plus its failed-result rows to the API success body. */
	private static SpikeResponse toSpikeResponse(
			SpikeEntity spike, List<SpikeFailedResultEntity> rows, boolean repeated) {
		List<FailedResultResponse> results = new ArrayList<>(rows.size());
		for (SpikeFailedResultEntity row : rows) {
			results.add(toFailedResultResponse(row));
		}
		return new SpikeResponse(
				spike.getId(),
				spike.getProductName(),
				spike.getProductCode(),
				spike.getWorkingOrders(),
				spike.getDescription(),
				spike.isFailedTestItemsAbnormal(),
				spike.getCreatedAt(),
				repeated,
				results);
	}

	/** Copies one MES failed result into a {@code spike_failed_result} row under {@code spikeId}. */
	private static SpikeFailedResultEntity toSpikeFailedResultEntity(
			Long spikeId, FailedManufacturingResult result) {
		SpikeFailedResultEntity row = new SpikeFailedResultEntity();
		row.setSpikeId(spikeId);
		row.setId(result.id());
		row.setSerialNumber(result.serialNumber());
		row.setProductType(result.productType());
		row.setMoId(result.moId());
		row.setOpCode(result.opCode());
		row.setStationId(result.stationId());
		row.setRackId(result.rackId());
		row.setFixtureId(result.fixtureId());
		row.setNestNumber(result.nestNumber());
		row.setOverallResult(result.overallResult());
		row.setCycleTimeSeconds(result.cycleTimeSeconds());
		row.setSwVersion(result.swVersion());
		row.setHwRevision(result.hwRevision());
		row.setFailedTestItems(result.failedTestItems());
		row.setErrorCode(result.errorCode());
		row.setOperatorId(result.operatorId());
		row.setShiftCode(result.shiftCode());
		row.setCreatedAt(result.createdAt());
		return row;
	}

	/** Maps one stored failed-result row to the response element. */
	private static FailedResultResponse toFailedResultResponse(SpikeFailedResultEntity row) {
		return new FailedResultResponse(
				row.getSpikeId(),
				row.getId(),
				row.getSerialNumber(),
				row.getProductType(),
				row.getMoId(),
				row.getOpCode(),
				row.getStationId(),
				row.getRackId(),
				row.getFixtureId(),
				row.getNestNumber(),
				row.getOverallResult(),
				row.getCycleTimeSeconds(),
				row.getSwVersion(),
				row.getHwRevision(),
				row.getFailedTestItems(),
				row.getErrorCode(),
				row.getOperatorId(),
				row.getShiftCode(),
				row.getCreatedAt());
	}
}
