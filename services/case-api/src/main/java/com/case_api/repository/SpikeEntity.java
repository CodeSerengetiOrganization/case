package com.case_api.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "spike")
public class SpikeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "product_name", nullable = false, length = 128)
	private String productName;

	@Column(name = "product_code", nullable = false, length = 128)
	private String productCode;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "working_orders", nullable = false, columnDefinition = "json")
	private List<String> workingOrders;

	@Column(name = "description", nullable = false, length = 1024)
	private String description;

	@Column(name = "failed_test_items_abnormal", nullable = false)
	private boolean failedTestItemsAbnormal;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

	public List<String> getWorkingOrders() {
		return workingOrders;
	}

	public void setWorkingOrders(List<String> workingOrders) {
		this.workingOrders = workingOrders;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isFailedTestItemsAbnormal() {
		return failedTestItemsAbnormal;
	}

	public void setFailedTestItemsAbnormal(boolean failedTestItemsAbnormal) {
		this.failedTestItemsAbnormal = failedTestItemsAbnormal;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
