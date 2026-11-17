package com.case_api.service;

import java.util.List;

/**
 * Product facts from the data-retrieving layer for one report.
 */
public record SpikeSearchResult(
		String productCode,
		List<String> workingOrders,
		List<FailedManufacturingResult> failedManufacturingResults) {
}
