package com.platform.user.repository;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Checks whether a user exists in Auth Service.
 *
 * This service deliberately does not import the Auth Service's
 * JPA entity or access its database directly.
 */
@Repository
public class UserRepository {

    private final RestClient restClient;

    public UserRepository(
            RestClient.Builder restClientBuilder,
            @Value("${services.auth.base-url:http://localhost:8081}")
            String authBaseUrl) {

        this.restClient = restClientBuilder
                .baseUrl(authBaseUrl)
                .build();
    }

    /**
     * Returns true if Auth Service confirms that the user exists.
     *
     * IMPORTANT:
     * The Auth Service must expose GET /admin/users/{id},
     * returning HTTP 200 for an existing user and HTTP 404
     * for a user that does not exist.
     */
    public boolean existsById(UUID id) {
        if (id == null) {
            return false;
        }

        try {
            restClient.get()
                    .uri("/admin/users/{id}", id)
                    .retrieve()
                    .toBodilessEntity();

            return true;

        } catch (HttpClientErrorException.NotFound ex) {
            return false;

        } catch (HttpClientErrorException ex) {
            throw new IllegalStateException(
                    "Auth Service rejected the user-existence request. "
                            + "HTTP status: "
                            + ex.getStatusCode(),
                    ex
            );
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to verify user existence with Auth Service.",
                    ex
            );
        }
    }
}