package com.case_api.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpikeRepository extends JpaRepository<SpikeEntity, Long> {

	List<SpikeEntity> findByProductName(String productName);
}

