package com.case_api.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpikeFailedResultRepository extends JpaRepository<SpikeFailedResultEntity, SpikeFailedResultId> {

	List<SpikeFailedResultEntity> findBySpikeId(Long spikeId);
}

