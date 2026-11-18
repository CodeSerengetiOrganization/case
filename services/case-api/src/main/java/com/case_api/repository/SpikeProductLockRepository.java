package com.case_api.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpikeProductLockRepository extends JpaRepository<SpikeProductLockEntity, String> {

	/**
	 * Insert the product-name lock row when missing. A duplicate primary key inserts nothing.
	 * Does not throw on that clash, so the store transaction is not marked rollback-only.
	 *
	 * @return 1 when a row was inserted, 0 when the name already existed
	 */
	@Modifying(clearAutomatically = true)
	@Query(
			value = "INSERT IGNORE INTO spike_product_lock (product_name, created_at) VALUES (:productName, :createdAt)",
			nativeQuery = true)
	int insertIgnore(
			@Param("productName") String productName, @Param("createdAt") Instant createdAt);

	/**
	 * {@code SELECT … FOR UPDATE} on the product-name lock row.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT l FROM SpikeProductLockEntity l WHERE l.productName = :productName")
	Optional<SpikeProductLockEntity> findByProductNameForUpdate(
			@Param("productName") String productName);
}
