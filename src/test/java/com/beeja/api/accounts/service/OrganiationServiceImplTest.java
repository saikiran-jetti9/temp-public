package com.beeja.api.accounts.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.beeja.api.accounts.clients.EmployeeFeignClient;
import com.beeja.api.accounts.exceptions.OrganizationExceptions;
import com.beeja.api.accounts.model.Organization.Organization;
import com.beeja.api.accounts.model.User;
import com.beeja.api.accounts.repository.OrganizationRepository;
import com.beeja.api.accounts.repository.UserRepository;
import com.beeja.api.accounts.serviceImpl.OrganiationServiceImpl;
import com.beeja.api.accounts.utils.Constants;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class OrganiationServiceImplTest {

  @Mock private OrganizationRepository organizationRepository;

  @Mock private UserRepository userRepository;

  @Mock private RoleRepository roleRepository;

  @Mock private EmployeeFeignClient employeeFeignClient;

  @InjectMocks private OrganiationServiceImpl organizationService;

  @Test
  public void testGetAllOrganizations() {
    // Arrange
    Organization organization = new Organization();
    organization.setId("1");
    organization.setName("Organization");
    when(organizationRepository.findAll()).thenReturn(Collections.singletonList(organization));

    // Act
    List<Organization> result = organizationService.getAllOrganizations();

    // Assert
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(organization.getName(), result.get(0).getName());
  }

  @Test
  public void testCreateOrganization() throws Exception {
    // Arrange
    Role role1 = new Role("1", "ROLE_SUPER_ADMIN", Set.of("READ_EMPLOYEE"));

    Organization organization = new Organization();
    organization.setEmail("test@gmail.com");
    organization.setEmailDomain("abcd@gmail.com");

    Mockito.when(organizationRepository.findByEmailDomain(Mockito.anyString())).thenReturn(null);
    Mockito.when(organizationRepository.save(Mockito.any(Organization.class)))
        .thenReturn(organization);

    User user = new User();
    user.setRoles(Set.of(role1));
    when(roleRepository.findByName(anyString())).thenReturn(role1);

    Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);

    Mockito.doNothing().when(employeeFeignClient).createEmployee(Mockito.any(User.class));

    // Act
    Organization result = organizationService.createOrganization(organization);

    // Asserts
    assertNotNull(result);
    assertEquals(organization, result);
  }

  @Test
  void testCreate_error() throws Exception {
    // Arrange
    Organization organization = new Organization();
    organization.setId("1");
    organization.setName("tac");
    organization.setEmail("abcd@gmail.com");

    when(organizationRepository.findByEmailDomain(anyString())).thenReturn(organization);
    // Act & Assert
    assertThrows(
        OrganizationExceptions.class, () -> organizationService.createOrganization(organization));
  }

  @Test
  void testCreate_error_role_super_admin() {

    // Arrange
    Organization organization = new Organization();
    organization.setEmail("test@gmail.com");
    organization.setEmailDomain("abcd@gmail.com");

    Mockito.when(organizationRepository.findByEmailDomain(Mockito.anyString())).thenReturn(null);
    Mockito.when(organizationRepository.save(Mockito.any(Organization.class)))
        .thenReturn(organization);

    User user = new User();
    when(roleRepository.findByName(anyString())).thenReturn(null);

    Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);
    // Act & Assert
    assertThrows(
        RuntimeException.class, () -> organizationService.createOrganization(organization));
  }

  @Test
  void testGetAllUsersByOrganizationId() throws Exception {
    // Arrange
    String organizationId = "1";

    Optional<Organization> mockedOrganization = Optional.of(new Organization());
    when(organizationRepository.findById(organizationId)).thenReturn(mockedOrganization);

    List<User> mockedUsers = Arrays.asList(new User(), new User());

    when(userRepository.findByOrganizationsId(organizationId)).thenReturn(mockedUsers);
    when(organizationRepository.findById(organizationId)).thenReturn(Optional.empty());
    when(organizationRepository.findById(organizationId))
        .thenThrow(new RuntimeException("Some unexpected exception"));

    // Act
    List<User> resultUsers = organizationService.getAllUsersByOrganizationId(organizationId);
    // Asserts
    assertNotNull(resultUsers);
    assertEquals(mockedUsers, resultUsers);
    assertThrows(
        Exception.class,
        () -> organizationService.getAllUsersByOrganizationId(organizationId),
        Constants.ERROR_NO_ORGANIZATION_FOUND_WITH_PROVIDED_ID);
    assertThrows(
        Exception.class,
        () -> organizationService.getAllUsersByOrganizationId(organizationId),
        Constants.ERROR_IN_FETCHING_EMPLOYEES_OF_ORG);
  }

  @Test
  public void testDeleteOrganizationById() {
    // Arrange
    String organizationId = "org123";
    Organization organization = new Organization();
    List<User> employeesInOrg = new ArrayList<>();

    when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

    when(userRepository.findByOrganizationsId(organizationId)).thenReturn(employeesInOrg);
    Mockito.doNothing().when(userRepository).deleteById(Mockito.anyString());
    Mockito.doNothing().when(organizationRepository).deleteById(organizationId);

    // Act
    Optional<Organization> result = organizationService.deleteOrganizationById(organizationId);

    // Assert
    assertEquals(Optional.of(organization), result);
  }

  @Test
  public void testDeleteOrganizationById_ExceptionThrown() {
    // Arrange
    String organizationId = "123";
    Mockito.when(organizationRepository.findById(organizationId))
        .thenThrow(new OrganizationExceptions("Test exception"));
    OrganizationExceptions exception =
        assertThrows(
            OrganizationExceptions.class,
            () -> {
              organizationService.deleteOrganizationById(organizationId);
            });
    // Act & Assert
    assertEquals(Constants.ERROR_NO_ORGANIZATION_FOUND_WITH_PROVIDED_ID, exception.getMessage());
  }
}
