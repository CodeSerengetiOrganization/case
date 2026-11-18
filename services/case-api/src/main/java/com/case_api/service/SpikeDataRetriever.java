package com.case_api.service;

/**
 * Data-retrieving layer. This issue uses a mock. Real MES stays parked.
 */
public interface SpikeDataRetriever {

	SpikeSearchResult search(String productName);
}
