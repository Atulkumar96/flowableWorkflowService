package com.task.flowable.taskapprovalflowable.config;

import com.task.flowable.taskapprovalflowable.security.TokenValidationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * This configuration registers the TokenValidationFilter for all /workflow/* endpoints.
 */
@Configuration
public class FilterConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public FilterRegistrationBean<TokenValidationFilter> tokenValidationFilter(RestTemplate restTemplate) {
        FilterRegistrationBean<TokenValidationFilter> registrationBean = new FilterRegistrationBean<>();

        // Adjust the user-service base URL as needed (e.g., http://user-service)
        registrationBean.setFilter(new TokenValidationFilter(restTemplate, "https://sp3crmtest.claritysystemsinc.com"));
        registrationBean.addUrlPatterns("/workflow/*"); // Apply filter to workflow endpoints
        registrationBean.setOrder(1); // Ensure this filter runs before other filters if needed
        return registrationBean;
    }
}
