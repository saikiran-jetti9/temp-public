package com.beeja.api.accounts.config.filters;

import com.beeja.api.accounts.model.Organization.Organization;
import com.beeja.api.accounts.model.Organization.Role;
import com.beeja.api.accounts.model.User;
import com.beeja.api.accounts.repository.UserRepository;
import com.beeja.api.accounts.utils.Constants;
import com.beeja.api.accounts.utils.JwtUtils;
import com.beeja.api.accounts.utils.UserContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthUserFilter extends OncePerRequestFilter {

  @Autowired UserRepository userRepository;

  @Autowired AuthUrlProperties authUrlProperties;

  @Autowired JwtProperties jwtProperties;

  private static String responseMessage;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (request.getRequestURI().startsWith("/accounts/actuator/")
        || request.getRequestURI().equals("/accounts/api-docs/swagger-config")
        || request.getRequestURI().startsWith("/accounts/swagger-ui/")
        || request.getRequestURI().startsWith("/accounts/openApi.yaml")) {
      filterChain.doFilter(request, response);
      return;
    }

    String accessToken = request.getHeader(Constants.COOKIE_ACCESS_TOKEN);

    if (accessToken == null) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.getWriter().write(Constants.NOT_AUTHORISED);
      return;
    }

    accessToken = accessToken.substring(7);
    if (isValidAccessToken(
        authUrlProperties.getTokenUri(), accessToken, authUrlProperties.getClientId())) {
      filterChain.doFilter(request, response);
    } else {
      Cookie cookie = new Cookie("SESSION", "");
      cookie.setPath("/");
      cookie.setMaxAge(0);
      response.addCookie(cookie);
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.getWriter().write(Constants.ACCESS_DENIED + ", " + responseMessage);
    }
  }

  private boolean isValidAccessToken(
      String tokenInterceptionURI, String accessToken, String clientId) {
    try {
      if (accessToken.startsWith("eyJhb")) {
        Claims claims = JwtUtils.decodeJWT(accessToken, jwtProperties.getSecret());
        String email = claims.get("sub").toString();
        User user = userRepository.findByEmail(email);

        if (user != null && user.isActive()) {
          Set<String> permissions = new HashSet<>();
          for (Role role : user.getRoles()) {
            permissions.addAll(role.getPermissions());
          }

          Organization organization = user.getOrganizations();
          UserContext.setLoggedInUser(
              email,
              user.getFirstName(),
              organization,
              user.getEmployeeId(),
              user.getRoles(),
              permissions,
              accessToken);
          return true;
        } else {
          responseMessage = "Invalid User";
          return false;
        }
      } else {
        // If token does not start with "eyJhb", validate using token interception URI
        String finalUrl = tokenInterceptionURI + "?access_token=" + accessToken;
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(finalUrl, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
          ObjectMapper objectMapper = new ObjectMapper();
          JsonNode accessTokenAttributes = objectMapper.readTree(response.getBody());
          String email = accessTokenAttributes.get("email").asText();
          String issuedClientId = accessTokenAttributes.get("azp").asText();

          if (!clientId.equals(issuedClientId)) {
            return false;
          }

          User user = userRepository.findByEmail(email);
          if (user != null && user.isActive()) {
            // Retrieve user permissions
            Set<String> permissions = new HashSet<>();
            for (Role role : user.getRoles()) {
              permissions.addAll(role.getPermissions());
            }

            Organization organization = user.getOrganizations();
            UserContext.setLoggedInUser(
                email,
                user.getFirstName(),
                organization,
                user.getEmployeeId(),
                user.getRoles(),
                permissions,
                accessToken);
            return true;
          } else {
            return false;
          }
        } else {
          return false;
        }
      }
    } catch (Exception e) {
      responseMessage = e.getMessage();
      return false;
    }
  }
}
