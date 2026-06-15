package com.veritas.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class BackendApplicationIntegrationTest extends BaseDBIntegrationTest {

	@Autowired
	private ApplicationContext context;

	@Test
	void ApplicationContext_Load_StartsSuccessfully() {
		assertNotNull(context);
	}
}
