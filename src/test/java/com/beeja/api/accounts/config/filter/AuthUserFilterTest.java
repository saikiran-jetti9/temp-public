package com.beeja.api.accounts.config.filter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.beeja.api.accounts.config.filters.AuthUrlProperties;
import com.beeja.api.accounts.config.filters.AuthUserFilter;
import com.beeja.api.accounts.repository.UserRepository;
import com.beeja.api.accounts.utils.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

public class AuthUserFilterTest {

  @Mock private UserRepository userRepository;

  @Mock private AuthUrlProperties authUrlProperties;

  @Mock private RestTemplate restTemplate;

  @InjectMocks private AuthUserFilter authUserFilter;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testDoFilterInternalDenyAccessForProtectedEndpointWithInvalidToken()
      throws ServletException, IOException {
    // Arrange
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    when(request.getRequestURI()).thenReturn("/abc");
    when(request.getHeader(Constants.COOKIE_ACCESS_TOKEN)).thenReturn("invalid token");

    // Act
    ReflectionTestUtils.invokeMethod(
        authUserFilter, "isValidAccessToken", "request", "response", "filterChain");
    ReflectionTestUtils.invokeMethod(
        authUserFilter, "doFilterInternal", request, response, filterChain);

    // Assert
    verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response.getWriter(), times(1)).write(anyString());
  }

  @Test
  public void testDoFilterInternalDenyAccessForProtectedEndpointWithNullToken()
      throws ServletException, IOException {
    // Arrange
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    when(request.getRequestURI()).thenReturn("/accounts/actuator/");
    when(request.getHeader(Constants.COOKIE_ACCESS_TOKEN)).thenReturn(null);
    when(response.getWriter()).thenReturn(mock(PrintWriter.class));

    // Act
    ReflectionTestUtils.invokeMethod(
        authUserFilter, "isValidAccessToken", "request", "response", "filterChain");
    ReflectionTestUtils.invokeMethod(
        authUserFilter, "doFilterInternal", request, response, filterChain);

    // Assert
    verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response.getWriter(), times(1)).write(anyString());
  }
}
