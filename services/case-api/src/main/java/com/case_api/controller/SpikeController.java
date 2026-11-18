package com.case_api.controller;

import com.case_api.service.ReportSpikeResult;
import com.case_api.service.SpikeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Report spike. The product name is the only key.
 */
@RestController
@RequestMapping("/apis/spikes")
public class SpikeController {

	private final SpikeService spikeService;

	public SpikeController(SpikeService spikeService) {
		this.spikeService = spikeService;
	}

	@PostMapping
	public ResponseEntity<?> reportSpike(@Valid @RequestBody SpikeRequest request) {
		return switch (spikeService.reportSpike(request)) {
			case ReportSpikeResult.NoFailedPart(var body) -> ResponseEntity.ok(body);
			case ReportSpikeResult.Stored(var body) -> body.repeated()
					? ResponseEntity.ok(body)
					: ResponseEntity.status(HttpStatus.CREATED).body(body);
		};
	}
}
