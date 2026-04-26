package com.veritas.backend.config;

import com.veritas.backend.auth.filter.JwtAuthenticationFilter;
import java.util.List;

import com.veritas.backend.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthenticationFilter jwtAuthFilter;

  private static final String[] WHITELIST_URLS = {
          "/api/v1/auth/**", "/login", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
          "/api/v1/api.json", "/api/v1/api.json/**",
          // UI Entry Points
          "/",
          "/index.html",
          "/favicon.ico",
          "/*.js",
          "/*.css",
          "/assets/**",
          // Infrastructure for Kubernetes
          "/api/v1/health" };

  @Bean
  public RoleHierarchy roleHierarchy() {
    String hierarchy = String.format(
        "ROLE_%s > ROLE_%s \n ROLE_%s > ROLE_%s \n ROLE_%s > ROLE_%s",
        UserRole.ADMINISTRATOR.name(), UserRole.FINANCE_OFFICER.name(),
        UserRole.FINANCE_OFFICER.name(), UserRole.PROCUREMENT_OFFICER.name(),
        UserRole.PROCUREMENT_OFFICER.name(), UserRole.REQUESTER.name());

    return RoleHierarchyImpl.fromHierarchy(hierarchy);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable).cors(cors -> cors.configurationSource(request -> {
      CorsConfiguration config = new CorsConfiguration();
      config.setAllowedOrigins(List.of("http://localhost:4200", "https://veritas.apps.student.inso-w.at"));
      config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
      config.setAllowedHeaders(List.of("*"));
      config.setAllowCredentials(true);
      return config;
    })).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorizeRequests -> authorizeRequests.requestMatchers(WHITELIST_URLS).permitAll()
                .anyRequest().authenticated()

        ).addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

}
