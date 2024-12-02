package com.beeja.api.accounts.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.beeja.api.accounts.clients.EmployeeFeignClient;
import com.beeja.api.accounts.exceptions.OrganizationExceptions;
import com.beeja.api.accounts.model.Organization.Organization;
import com.beeja.api.accounts.model.User;
import com.beeja.api.accounts.repository.OrganizationRepository;
import com.beeja.api.accounts.service.OrganizationService;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

class OrganizationControllerTest {

  @Mock private OrganizationService organizationService;
  @Mock private OrganizationRepository organizationRepository;
  @Mock private EmployeeFeignClient employeeFeignClient;

  @InjectMocks private OrganizationController organizationController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  Organization organization1 =
      new Organization(
          "org1",
          "OrganizationName",
          "org@example.com",
          "sub123",
          "Location",
          "example.com",
          "contact@example.com",
          "https://www.example.com");

  Organization organization2 =
      new Organization(
          "org2",
          "OrganizationName",
          "org@example.com",
          "sub123",
          "Location",
          "example.com",
          "contact@example.com",
          "https://www.example.com");

  @Test
  void testGetAllOrganizations() {
    // Arrange
    List<Organization> org = new ArrayList<>(Arrays.asList(organization1, organization2));
    when(organizationService.getAllOrganizations()).thenReturn(org);

    // Act
    ResponseEntity<?> responseEntity = organizationController.getAllOrganizations();

    // Assert
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
  }

  @Test
  public void testGetAllOrganizations_InternalserverError() {
    // Arrange
    when(organizationService.getAllOrganizations())
        .thenThrow(new OrganizationExceptions("Internal server error"));

    // Act
    ResponseEntity<?> responseEntity = organizationController.getAllOrganizations();

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
  }

  @Test
  void testGetAllEmployeesByOrganizationId_Success() throws Exception {
    // Arrange
    String organizationId = "org123";
    List<User> users = Arrays.asList(new User(), new User());
    when(organizationService.getAllUsersByOrganizationId(organizationId)).thenReturn(users);

    // Act
    ResponseEntity<?> responseEntity =
        organizationController.getAllEmployeesByOrganizationId(organizationId);

    // Assert
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
  }

  @Test
  public void testGetAllEmployeesByOrganizationId_Notfound() throws Exception {
    // Arrange
    String organizationId = "org123";
    when(organizationService.getAllUsersByOrganizationId(organizationId))
        .thenThrow(new OrganizationExceptions("Internal server error"));

    // Act
    ResponseEntity<?> responseEntity =
        organizationController.getAllEmployeesByOrganizationId(organizationId);

    // Assert
    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  public void testGetAllEmployeesByOrganizationId_InternalServerError() throws Exception {
    // Arrange
    String organizationId = "org123";
    when(organizationService.getAllUsersByOrganizationId(organizationId))
        .thenThrow(new RuntimeException("Internal server error"));

    // Act
    ResponseEntity<?> responseEntity =
        organizationController.getAllEmployeesByOrganizationId(organizationId);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
  }

  @Test
  void testCreateOrganization_Success() throws Exception {
    // Arrange
    Organization organization = new Organization();
    BindingResult bindingResult = mock(BindingResult.class);

    when(bindingResult.hasErrors()).thenReturn(false);
    when(organizationService.createOrganization(organization)).thenReturn(organization);

    // Act
    ResponseEntity<?> responseEntity =
        organizationController.createOrganization(organization, bindingResult);

    // Assert
    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
  }

  @Test
  void testCreateOrganization_errormessages() throws Exception {
    // Arrange
    Organization organization = new Organization();
    BindingResult bindingResult = mock(BindingResult.class);

    when(bindingResult.hasErrors()).thenReturn(true);

    // Act
    ResponseEntity<?> responseEntity =
        organizationController.createOrganization(organization, bindingResult);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
  }

  @Test
  public void testCreateOrganization_conflict() throws Exception {
    // Arrange
    Organization organization = new Organization();
    BindingResult bindingResult = mock(BindingResult.class);
    when(organizationService.createOrganization(organization))
        .thenThrow(new OrganizationExceptions("Conflict error"));

    // Act
    ResponseEntity<?> responseEntity =
        organizationController.createOrganization(organization, bindingResult);

    // Assert
    assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
  }

  @Test
  public void testCreateOrganization_Badrequest() throws Exception {
    // Arrange
    Organization organization = new Organization();
    BindingResult bindingResult = mock(BindingResult.class);
    when(organizationService.createOrganization(organization))
        .thenThrow(new RuntimeException("Bad request"));

    // Act
    ResponseEntity<?> responseEntity =
        organizationController.createOrganization(organization, bindingResult);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
  }

  @Test
  public void testGetOrganizationById() {
    // Arrange
    String organizationId = "ABCD";
    Organization organization = new Organization();
    Optional<Organization> optionalOrganization = Optional.of(organization);

    Mockito.when(organizationRepository.findById(anyString())).thenReturn(optionalOrganization);

    // Act
    ResponseEntity<Organization> responseEntity =
        organizationController.getOrganizationById(organizationId);

    // Assert
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(optionalOrganization, responseEntity.getBody());
    Mockito.verify(organizationRepository).findById(organizationId);
  }

  @Test
  void testDeleteOrganizationById() {
    // Arrange
    String organizationId = "org123";

    when(employeeFeignClient.deleteAllEmployeesByOrganizationId(organizationId))
        .thenReturn(ResponseEntity.ok().build());
    when(organizationService.deleteOrganizationById(organizationId))
        .thenReturn(Optional.ofNullable(organization1));

    // Act
    ResponseEntity<?> responseEntity =
        organizationController.deleteOrganizatiobById(organizationId);

    // Assert
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());
  }
}
