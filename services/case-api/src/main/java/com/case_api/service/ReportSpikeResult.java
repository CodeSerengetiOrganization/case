package com.case_api.service;

import com.case_api.controller.NoFailedPartResponse;
import com.case_api.controller.SpikeResponse;

/**
 * Outcome of {@link SpikeService#reportSpike}. Controller maps each variant to HTTP.
 */
public sealed interface ReportSpikeResult
		permits ReportSpikeResult.NoFailedPart, ReportSpikeResult.Stored {

	record NoFailedPart(NoFailedPartResponse body) implements ReportSpikeResult {
	}

	record Stored(SpikeResponse body) implements ReportSpikeResult {
	}
}
