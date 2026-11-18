package com.case_api.controller;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * HTTP 200 when the search finds no failed part. CASE stores nothing.
 */
public record NoFailedPartResponse(
		@JsonProperty("product_name") String productName,
		@JsonProperty("message") String message) {
}
