package com.case_api.service;

import com.case_api.controller.NoFailedPartResponse;
import com.case_api.controller.SpikeRequest;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Store orchestration. Search stays outside the store transaction.
 * {@link SpikeStoreWriter} owns lock, repeat check, and write.
 */
@Service
public class SpikeService {

	static final int MAX_FAILED_RESULTS = 100;

	private final SpikeDataRetriever spikeDataRetriever;
	private final SpikeStoreWriter spikeStoreWriter;

	public SpikeService(SpikeDataRetriever spikeDataRetriever, SpikeStoreWriter spikeStoreWriter) {
		this.spikeDataRetriever = spikeDataRetriever;
		this.spikeStoreWriter = spikeStoreWriter;
	}

	/**
	 * Returns {@link ReportSpikeResult.NoFailedPart} when the search finds no failed part.
	 * Returns {@link ReportSpikeResult.Stored} for a new or repeated spike.
	 */
	public ReportSpikeResult reportSpike(SpikeRequest request) {
		String productName = request.getProductName();
		SpikeSearchResult search = spikeDataRetriever.search(productName);
		trimToMostRecent(search.failedManufacturingResults(), MAX_FAILED_RESULTS);

		if (search.failedManufacturingResults().isEmpty()) {
			return new ReportSpikeResult.NoFailedPart(new NoFailedPartResponse(
					productName, "no failed part found under " + productName));
		}

		return new ReportSpikeResult.Stored(
				spikeStoreWriter.storeOrReturnRepeat(productName, search));
	}

	/**
	 * Keeps the most recent {@code limit} results by {@code created_at}. Mutates the list in place.
	 */
	static void trimToMostRecent(List<FailedManufacturingResult> failedResults, int limit) {
		if (failedResults == null || failedResults.size() <= limit) {
			return;
		}
		failedResults.sort(Comparator.comparing(FailedManufacturingResult::createdAt).reversed());
		failedResults.subList(limit, failedResults.size()).clear();
	}
}
