package com.case_api;

import com.case_api.repository.SpikeFailedResultRepository;
import com.case_api.repository.SpikeProductLockRepository;
import com.case_api.repository.SpikeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Scaffold smoke test — no live MySQL required.
 * Repositories and EntityManager are mocked while Testcontainers is still open.
 */
@SpringBootTest(properties = {
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
class CaseApiApplicationTests {

	@MockitoBean
	private SpikeRepository spikeRepository;

	@MockitoBean
	private SpikeFailedResultRepository spikeFailedResultRepository;

	@MockitoBean
	private SpikeProductLockRepository spikeProductLockRepository;

	@MockitoBean
	private EntityManager entityManager;

	@Test
	void contextLoads() {
	}

}
