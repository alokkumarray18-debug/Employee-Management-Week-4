package com.trainee.employeemanagement.service;

import com.trainee.employeemanagement.dto.ExternalProfileDTO;
import com.trainee.employeemanagement.exception.ExternalApiException;
import com.trainee.employeemanagement.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Demonstrates external REST API integration (assignment section 10) using
 * Spring's RestClient. It calls the public mock API
 * https://jsonplaceholder.typicode.com/users/{id} - "external id" here is a
 * stand-in for a real integration (e.g. an HRMS or SSO profile service). The
 * response is mapped into ExternalProfileDTO rather than exposed raw, and
 * every failure mode (4xx, 5xx, timeout/connection failure) is translated
 * into a clean, typed exception instead of leaking RestClient internals.
 */
@Service
public class EmployeeExternalIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeExternalIntegrationService.class);

    private final RestClient restClient;

    public EmployeeExternalIntegrationService(RestClient externalApiRestClient) {
        this.restClient = externalApiRestClient;
    }

    public ExternalProfileDTO fetchExternalProfile(Long externalId) {
        log.info("Calling external profile API for externalId={}", externalId);
        try {
            ExternalProfileDTO profile = restClient.get()
                    .uri("/users/{id}", externalId)
                    .header("X-Client", "employee-management-service")
                    .retrieve()
                    .body(ExternalProfileDTO.class);

            if (profile == null) {
                throw new ResourceNotFoundException("No external profile found for id: " + externalId);
            }
            log.debug("External profile fetched successfully for externalId={}", externalId);
            return profile;

        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("External profile not found for externalId={}", externalId);
            throw new ResourceNotFoundException("No external profile found for id: " + externalId);
        } catch (HttpClientErrorException ex) {
            log.error("External API rejected request for externalId={}: {}", externalId, ex.getStatusCode());
            throw new ExternalApiException("External API returned client error: " + ex.getStatusCode());
        } catch (HttpServerErrorException ex) {
            log.error("External API server error for externalId={}: {}", externalId, ex.getStatusCode());
            throw new ExternalApiException("External API returned server error: " + ex.getStatusCode());
        } catch (ResourceAccessException ex) {
            log.error("External API unreachable/timed out for externalId={}", externalId, ex);
            throw new ExternalApiException("External API is unreachable or timed out", ex);
        }
    }
}
