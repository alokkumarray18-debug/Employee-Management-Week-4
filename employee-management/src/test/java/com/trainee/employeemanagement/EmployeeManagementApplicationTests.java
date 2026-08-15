package com.trainee.employeemanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic smoke test that verifies the Spring application context loads
 * correctly with all beans (controller -> service -> repository) wired up.
 *
 * Runs against the in-memory H2 database configured in
 * src/test/resources/application.properties, so it needs no external MySQL
 * instance and is safe to run in CI.
 */
@SpringBootTest
@ActiveProfiles("test")
class EmployeeManagementApplicationTests {

    @Test
    void contextLoads() {
        // If the application context fails to start (e.g. missing bean,
        // broken wiring, bad configuration), this test fails automatically.
    }
}
