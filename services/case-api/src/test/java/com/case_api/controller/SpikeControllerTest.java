package com.case_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.case_api.service.MockSpikeDataRetriever;
import com.case_api.service.SpikeService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Controller wiring only — one trigger per HTTP branch.
 * Business rules live in SpikeServiceTest.
 */
@WebMvcTest(controllers = SpikeController.class)
class SpikeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SpikeService spikeService;

	@Test
	void reportSpike_whenValidationFails_returns400() throws Exception {
		mockMvc.perform(post("/apis/spikes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());

		verify(spikeService, never()).reportSpike(any());
	}

	@Test
	void reportSpike_whenNoFailedPart_returns200() throws Exception {
		String productName = MockSpikeDataRetriever.PRODUCT_ALL_PASS;
		when(spikeService.reportSpike(any(SpikeRequest.class)))
				.thenReturn(new NoFailedPartResponse(
						productName, "no failed part found under " + productName));

		mockMvc.perform(post("/apis/spikes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "product_name": "%s"
								}
								""".formatted(productName)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void reportSpike_whenNewSpike_returns201() throws Exception {
		String productName = MockSpikeDataRetriever.PRODUCT_HAS_FAILED;
		when(spikeService.reportSpike(any(SpikeRequest.class)))
				.thenReturn(sampleSpikeResponse(1L, productName, false));

		mockMvc.perform(post("/apis/spikes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "product_name": "%s"
								}
								""".formatted(productName)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.repeated").value(false));
	}

	@Test
	void reportSpike_whenRepeatedSpike_returns200() throws Exception {
		String productName = MockSpikeDataRetriever.PRODUCT_HAS_FAILED;
		when(spikeService.reportSpike(any(SpikeRequest.class)))
				.thenReturn(sampleSpikeResponse(42L, productName, true));

		mockMvc.perform(post("/apis/spikes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "product_name": "%s"
								}
								""".formatted(productName)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.repeated").value(true));
	}

	private static SpikeResponse sampleSpikeResponse(long id, String productName, boolean repeated) {
		return new SpikeResponse(
				id,
				productName,
				"Ford-APCU-01",
				List.of("WO-DEMO-001"),
				"1 unit failed on EOL test, failed test items: switch-open.",
				false,
				Instant.parse("2026-11-06T12:05:00Z"),
				repeated,
				List.of(new SpikeResponse.FailedResultResponse(
						id, 1L, "APCU-CD4-PCBA-1", "Ford-APCU", "WO-DEMO-001", "COLD_EOL",
						"AEL-01-EOL-01", "RACK-APCU-G1-01", "FIXTURE-APCU-G2-01", 1,
						"FAIL", 30, "sw-1.0", "hw-2.0",
						Map.of("switch-open", "0010"),
						"switch-error", "Alice", "day-shift",
						Instant.parse("2026-11-06T12:00:00Z"))));
	}
}
