package com.case_api.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "spike_failed_result")
@IdClass(SpikeFailedResultId.class)
public class SpikeFailedResultEntity {

	@Id
	@Column(name = "spike_id", nullable = false)
	private Long spikeId;

	@Id
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "serial_number", length = 128)
	private String serialNumber;

	@Column(name = "product_type", length = 128)
	private String productType;

	@Column(name = "mo_id", length = 128)
	private String moId;

	@Column(name = "op_code", length = 64)
	private String opCode;

	@Column(name = "station_id", length = 128)
	private String stationId;

	@Column(name = "rack_id", length = 128)
	private String rackId;

	@Column(name = "fixture_id", length = 128)
	private String fixtureId;

	@Column(name = "nest_number")
	private Integer nestNumber;

	@Column(name = "overall_result", length = 32)
	private String overallResult;

	@Column(name = "cycle_time_seconds")
	private Integer cycleTimeSeconds;

	@Column(name = "sw_version", length = 64)
	private String swVersion;

	@Column(name = "hw_revision", length = 64)
	private String hwRevision;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "failed_test_items", columnDefinition = "json")
	private Object failedTestItems;

	@Column(name = "error_code", length = 128)
	private String errorCode;

	@Column(name = "operator_id", length = 128)
	private String operatorId;

	@Column(name = "shift_code", length = 64)
	private String shiftCode;

	@Column(name = "created_at")
	private Instant createdAt;

	public Long getSpikeId() {
		return spikeId;
	}

	public void setSpikeId(Long spikeId) {
		this.spikeId = spikeId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public String getProductType() {
		return productType;
	}

	public void setProductType(String productType) {
		this.productType = productType;
	}

	public String getMoId() {
		return moId;
	}

	public void setMoId(String moId) {
		this.moId = moId;
	}

	public String getOpCode() {
		return opCode;
	}

	public void setOpCode(String opCode) {
		this.opCode = opCode;
	}

	public String getStationId() {
		return stationId;
	}

	public void setStationId(String stationId) {
		this.stationId = stationId;
	}

	public String getRackId() {
		return rackId;
	}

	public void setRackId(String rackId) {
		this.rackId = rackId;
	}

	public String getFixtureId() {
		return fixtureId;
	}

	public void setFixtureId(String fixtureId) {
		this.fixtureId = fixtureId;
	}

	public Integer getNestNumber() {
		return nestNumber;
	}

	public void setNestNumber(Integer nestNumber) {
		this.nestNumber = nestNumber;
	}

	public String getOverallResult() {
		return overallResult;
	}

	public void setOverallResult(String overallResult) {
		this.overallResult = overallResult;
	}

	public Integer getCycleTimeSeconds() {
		return cycleTimeSeconds;
	}

	public void setCycleTimeSeconds(Integer cycleTimeSeconds) {
		this.cycleTimeSeconds = cycleTimeSeconds;
	}

	public String getSwVersion() {
		return swVersion;
	}

	public void setSwVersion(String swVersion) {
		this.swVersion = swVersion;
	}

	public String getHwRevision() {
		return hwRevision;
	}

	public void setHwRevision(String hwRevision) {
		this.hwRevision = hwRevision;
	}

	public Object getFailedTestItems() {
		return failedTestItems;
	}

	public void setFailedTestItems(Object failedTestItems) {
		this.failedTestItems = failedTestItems;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getOperatorId() {
		return operatorId;
	}

	public void setOperatorId(String operatorId) {
		this.operatorId = operatorId;
	}

	public String getShiftCode() {
		return shiftCode;
	}

	public void setShiftCode(String shiftCode) {
		this.shiftCode = shiftCode;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
