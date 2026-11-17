package com.case_api.repository;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key: CASE spike id + copied MES manufacturing-result id.
 */
public class SpikeFailedResultId implements Serializable {

	private Long spikeId;
	private Long id;

	public SpikeFailedResultId() {
	}

	public SpikeFailedResultId(Long spikeId, Long id) {
		this.spikeId = spikeId;
		this.id = id;
	}

	public Long getSpikeId() {
		return spikeId;
	}

	public void setSpikeId(Long spikeId) {
		this.spikeId = spikeId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SpikeFailedResultId that)) {
			return false;
		}
		return Objects.equals(spikeId, that.spikeId) && Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(spikeId, id);
	}
}
