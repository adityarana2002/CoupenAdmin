package com.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Value("${admin.username}")
        private String adminUsername;

        @Value("${admin.password}")
        private String adminPassword;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/api/**"))
                                .authorizeHttpRequests(auth -> auth
                                                // API endpoints — accessible by Android app (no session)
                                                .requestMatchers("/api/**").permitAll()
                                                // User login page — only the specific paths needed by the app
                                                .requestMatchers("/user/login").permitAll()
                                                // Static resources
                                                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                                                // Admin login page
                                                .requestMatchers("/login").permitAll()
                                                // Everything else requires authentication
                                                .requestMatchers("/admin/**").authenticated()
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/admin/dashboard", true)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                // ── Security Headers ──
                                .headers(headers -> headers
                                                // Prevent clickjacking — no framing allowed
                                                .frameOptions(frame -> frame.deny())
                                                // Prevent MIME type sniffing
                                                .contentTypeOptions(contentType -> {})
                                                // Enforce HTTPS via HSTS (1 year, include subdomains)
                                                .httpStrictTransportSecurity(hsts -> hsts
                                                                .includeSubDomains(true)
                                                                .maxAgeInSeconds(31536000))
                                                // Control referrer information
                                                .referrerPolicy(referrer -> referrer
                                                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                                                // Prevent XSS attacks
                                                .xssProtection(xss -> {})
                                                // Content Security Policy
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;"))
                                );
                return http.build();
        }

        @Bean
        public UserDetailsService userDetailsService() {
                var user = User.builder()
                                .username(adminUsername)
                                .password(passwordEncoder().encode(adminPassword))
                                .roles("ADMIN")
                                .build();
                return new InMemoryUserDetailsManager(user);
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
