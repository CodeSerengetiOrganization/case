package com.case_api.service;

import java.time.Instant;

/**
 * One failed EOL manufacturing result from the data-retrieving layer.
 * {@code id} is the MES manufacturing-result id.
 * {@code failedTestItems} keeps the JSON type MES sent (object, array, primitive) or null when missing.
 */
public record FailedManufacturingResult(
		long id,
		String serialNumber,
		String productType,
		String moId,
		String opCode,
		String stationId,
		String rackId,
		String fixtureId,
		Integer nestNumber,
		String overallResult,
		Integer cycleTimeSeconds,
		String swVersion,
		String hwRevision,
		Object failedTestItems,
		String errorCode,
		String operatorId,
		String shiftCode,
		Instant createdAt) {
}
