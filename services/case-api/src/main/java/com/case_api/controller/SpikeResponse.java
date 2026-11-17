package com.case_api.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Success body for a stored (or later: repeated) spike.
 */
public record SpikeResponse(
		@JsonProperty("id") Long id,
		@JsonProperty("product_name") String productName,
		@JsonProperty("product_code") String productCode,
		@JsonProperty("working_orders") List<String> workingOrders,
		@JsonProperty("description") String description,
		@JsonProperty("failed_test_items_abnormal") boolean failedTestItemsAbnormal,
		@JsonProperty("created_at") Instant createdAt,
		@JsonProperty("repeated") boolean repeated,
		@JsonProperty("failed_manufacturing_results") List<FailedResultResponse> failedManufacturingResults) {

	public record FailedResultResponse(
			@JsonProperty("spike_id") Long spikeId,
			@JsonProperty("id") Long id,
			@JsonProperty("serial_number") String serialNumber,
			@JsonProperty("product_type") String productType,
			@JsonProperty("mo_id") String moId,
			@JsonProperty("op_code") String opCode,
			@JsonProperty("station_id") String stationId,
			@JsonProperty("rack_id") String rackId,
			@JsonProperty("fixture_id") String fixtureId,
			@JsonProperty("nest_number") Integer nestNumber,
			@JsonProperty("overall_result") String overallResult,
			@JsonProperty("cycle_time_seconds") Integer cycleTimeSeconds,
			@JsonProperty("sw_version") String swVersion,
			@JsonProperty("hw_revision") String hwRevision,
			@JsonProperty("failed_test_items") Object failedTestItems,
			@JsonProperty("error_code") String errorCode,
			@JsonProperty("operator_id") String operatorId,
			@JsonProperty("shift_code") String shiftCode,
			@JsonProperty("created_at") Instant createdAt) {
	}
}
