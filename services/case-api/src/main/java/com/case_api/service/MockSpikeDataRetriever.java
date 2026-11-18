package com.case_api.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Production mock for this issue. Returns at most 100 failed results,
 * the most recent by result {@code created_at}.
 * Lists are fresh and mutable so SpikeService can trim in place.
 */
@Component
public class MockSpikeDataRetriever implements SpikeDataRetriever {

	public static final String PRODUCT_ALL_PASS = "Ford-APCU-AllPass-001";
	public static final String PRODUCT_HAS_FAILED = "Ford-APCU-HasFailedRecord-001";
	public static final String PRODUCT_MANY_FAILED = "Ford-APCU-ManyFailed-001";
	public static final String PRODUCT_MISSING_NEST = "Ford-APCU-MissingNest-001";
	public static final String PRODUCT_ABNORMAL_TEST_ITEMS = "Ford-APCU-AbnormalTestItems-001";

	private static final int MAX_FAILED_RESULTS = 100;

	@Override
	public SpikeSearchResult search(String productName) {
		if (PRODUCT_ALL_PASS.equals(productName)) {
			return new SpikeSearchResult(
					"Ford-APCU-ALL-PASS",
					List.of("WO-ALL-PASS-001"),
					new ArrayList<>());
		}
		if (PRODUCT_HAS_FAILED.equals(productName)) {
			List<FailedManufacturingResult> failed = new ArrayList<>();
			failed.add(sampleFailedResult(1L, Instant.parse("2026-11-06T12:00:00Z"), 1,
					Map.of("switch-open", "0010", "switch-close", "0001")));
			return new SpikeSearchResult(
					"Ford-APCU-01",
					List.of("WO-DEMO-001", "WO-DEMO-002"),
					failed);
		}
		if (PRODUCT_MANY_FAILED.equals(productName)) {
			List<FailedManufacturingResult> failed =
					new ArrayList<>(mostRecent(buildFailedPool(150), MAX_FAILED_RESULTS));
			return new SpikeSearchResult(
					"Ford-APCU-MANY",
					List.of("WO-MANY-001"),
					failed);
		}
		if (PRODUCT_MISSING_NEST.equals(productName)) {
			List<FailedManufacturingResult> failed = new ArrayList<>();
			failed.add(sampleFailedResult(10L, Instant.parse("2026-11-06T12:00:00Z"), null,
					Map.of("switch-open", "0010")));
			return new SpikeSearchResult(
					"Ford-APCU-MISSING-NEST",
					List.of("WO-DEMO-001"),
					failed);
		}
		if (PRODUCT_ABNORMAL_TEST_ITEMS.equals(productName)) {
			List<FailedManufacturingResult> failed = new ArrayList<>();
			failed.add(sampleFailedResult(11L, Instant.parse("2026-11-06T12:00:00Z"), 1,
					List.of("switch-open", "switch-close")));
			return new SpikeSearchResult(
					"Ford-APCU-ABNORMAL",
					List.of("WO-DEMO-001"),
					failed);
		}
		return new SpikeSearchResult(
				"UNKNOWN-" + productName,
				List.of(),
				new ArrayList<>());
	}

	static List<FailedManufacturingResult> mostRecent(
			List<FailedManufacturingResult> source, int limit) {
		return source.stream()
				.sorted(Comparator.comparing(FailedManufacturingResult::createdAt).reversed())
				.limit(limit)
				.toList();
	}

	static List<FailedManufacturingResult> buildFailedPool(int size) {
		List<FailedManufacturingResult> pool = new ArrayList<>(size);
		Instant base = Instant.parse("2026-01-01T00:00:00Z");
		for (int i = 0; i < size; i++) {
			pool.add(sampleFailedResult(i + 1L, base.plusSeconds(i), 1,
					Map.of("switch-open", "0010", "switch-close", "0001")));
		}
		return pool;
	}

	private static FailedManufacturingResult sampleFailedResult(
			long id, Instant createdAt, Integer nestNumber, Object failedTestItems) {
		return new FailedManufacturingResult(
				id,
				"APCU-CD4-PCBA-" + id,
				"Ford-APCU",
				"WO-DEMO-001",
				"COLD_EOL",
				"AEL-01-EOL-01",
				"RACK-APCU-G1-01",
				"FIXTURE-APCU-G2-01",
				nestNumber,
				"FAIL",
				30,
				"sw-1.0",
				"hw-2.0",
				failedTestItems,
				"switch-error",
				"Alice",
				"day-shift",
				createdAt);
	}
}
