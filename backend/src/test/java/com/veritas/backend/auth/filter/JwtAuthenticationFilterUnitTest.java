package com.veritas.backend.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.auth.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterUnitTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws IOException {
        SecurityContextHolder.clearContext();
        responseWriter = new StringWriter();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_AuthHeaderNull_ContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService, objectMapper);
    }

    @Test
    void doFilterInternal_AuthHeaderDoesNotStartWithBearer_ContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abcd");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService, objectMapper);
    }

    @Test
    void doFilterInternal_TokenExpired_SendsErrorResponse() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired_token");
        when(jwtService.extractEmail("expired_token")).thenThrow(new ExpiredJwtException(null, null, "Expired"));
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(objectMapper).writeValue(any(PrintWriter.class), eq(Map.of(
                "status", HttpServletResponse.SC_UNAUTHORIZED,
                "message", "Token expired"
        )));
        verifyNoInteractions(filterChain, userDetailsService);
    }

    @Test
    void doFilterInternal_UserEmailNull_ContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractEmail("token")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilterInternal_AlreadyAuthenticated_ContinuesChain() throws Exception {
        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractEmail("token")).thenReturn("user@veritas.com");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
        assertSame(existingAuth, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_UserDisabled_SendsErrorResponse() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractEmail("token")).thenReturn("user@veritas.com");
        
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.isEnabled()).thenReturn(false);
        when(userDetailsService.loadUserByUsername("user@veritas.com")).thenReturn(userDetails);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(objectMapper).writeValue(any(PrintWriter.class), eq(Map.of(
                "status", HttpServletResponse.SC_UNAUTHORIZED,
                "message", "Account disabled"
        )));
        verifyNoInteractions(filterChain);
    }

    @Test
    void doFilterInternal_TokenValid_AuthenticatesUser() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractEmail("token")).thenReturn("user@veritas.com");

        UserDetails userDetails = User.withUsername("user@veritas.com")
                .password("password")
                .authorities(Collections.emptyList())
                .disabled(false)
                .build();
        when(userDetailsService.loadUserByUsername("user@veritas.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("token", userDetails)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("user@veritas.com", auth.getName());
    }

    @Test
    void doFilterInternal_TokenInvalid_DoesNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtService.extractEmail("token")).thenReturn("user@veritas.com");

        UserDetails userDetails = User.withUsername("user@veritas.com")
                .password("password")
                .authorities(Collections.emptyList())
                .disabled(false)
                .build();
        when(userDetailsService.loadUserByUsername("user@veritas.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("token", userDetails)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
