package com.case_api.controller;

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
		Object result = spikeService.reportSpike(request);
		if (result instanceof NoFailedPartResponse body) {
			return ResponseEntity.ok(body);
		}
		if (result instanceof SpikeResponse body) {
			if (body.repeated()) {
				return ResponseEntity.ok(body);
			}
			return ResponseEntity.status(HttpStatus.CREATED).body(body);
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	}
}
