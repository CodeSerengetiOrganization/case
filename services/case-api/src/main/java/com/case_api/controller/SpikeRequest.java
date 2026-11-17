package com.case_api.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * POST /apis/spikes — request body. The product name is the only key.
 */
public class SpikeRequest {

	@NotBlank(message = "product_name is required")
	@JsonProperty("product_name")
	private String productName;

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}
}
