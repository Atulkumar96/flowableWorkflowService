package com.task.flowable.taskapprovalflowable.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * This filter validates the Access-Token by calling the user service's /validate endpoint.
 * It intercepts incoming requests and only passes them along if the token is valid.
 * Its purpose is to secure the workflow endpoints by requiring a valid token.
 * If the token is invalid, the filter responds with 401 Unauthorized.
 * If the token is missing, the filter responds with 401 Unauthorized.
 * If the user service is unreachable, the filter responds with 401 Unauthorized.
 * Config/FilterConfig registers this filter for all /workflow/* endpoints and calls the user service for validation
 */
public class TokenValidationFilter extends OncePerRequestFilter {
    private final RestTemplate restTemplate;
    private final String userServiceValidateUrl;

    public TokenValidationFilter(RestTemplate restTemplate, String userServiceValidateUrl) {
        this.restTemplate = restTemplate;
        this.userServiceValidateUrl = userServiceValidateUrl; // Base URL for the user service
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Retrieve the Access-Token from the request header
        String accessToken = request.getHeader("Access-Token");

        // If no token is provided, respond with 401 Unauthorized
        if (accessToken == null || accessToken.trim().isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Invalid Token: No token provided");
            return;
        }

        // Call the user service's /validate endpoint to validate the token
        try {
            // Set up headers for calling the user-service validation endpoint
            HttpHeaders headers = new HttpHeaders();
            headers.set("Access-Token", accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Call the external /validate endpoint
            ResponseEntity<String> validationResponse = restTemplate.exchange(
                    userServiceValidateUrl + "/validate",
                    HttpMethod.GET,
                    entity,
                    String.class);

            // Check if the response indicates a valid token
            if (validationResponse.getStatusCode() == HttpStatus.OK &&
                    "Valid Token".equalsIgnoreCase(validationResponse.getBody())){
                // If the token is valid, pass the request along the filter chain
                filterChain.doFilter(request, response);
            }
            else{
                // If the token is invalid, respond with 401 Unauthorized
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("Invalid Token: Token validation failed");
            }
        } catch (Exception ex) {
            // In case of any exception (e.g., network issues), reject the request
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Invalid Token");
        }

    }
}
