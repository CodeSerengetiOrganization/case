package com.case_api.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * API check 5 — production mock returns at most 100 failed results.
 */
class MockSpikeDataRetrieverTest {

	private final MockSpikeDataRetriever retriever = new MockSpikeDataRetriever();

	@Test
	void search_whenManyFailedProduct_returnsAtMost100MostRecent() {
		SpikeSearchResult result = retriever.search(MockSpikeDataRetriever.PRODUCT_MANY_FAILED);

		assertThat(result.failedManufacturingResults()).hasSize(100);
		assertThat(result.failedManufacturingResults().getFirst().id()).isEqualTo(150L);
		assertThat(result.failedManufacturingResults().getLast().id()).isEqualTo(51L);
	}

	@Test
	void search_whenAllPass_returnsNoFailedResults() {
		SpikeSearchResult result = retriever.search(MockSpikeDataRetriever.PRODUCT_ALL_PASS);

		assertThat(result.failedManufacturingResults()).isEmpty();
	}
}
