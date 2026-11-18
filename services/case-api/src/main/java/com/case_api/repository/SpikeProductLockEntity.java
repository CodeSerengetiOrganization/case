package com.case_api.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Concurrency pin for one product name. Not a business row for the operator.
 */
@Entity
@Table(name = "spike_product_lock")
public class SpikeProductLockEntity {

	@Id
	@Column(name = "product_name", nullable = false, length = 128)
	private String productName;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
