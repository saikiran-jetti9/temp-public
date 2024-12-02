package com.beeja.api.accounts.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.beeja.api.accounts.clients.EmployeeFeignClient;
import com.beeja.api.accounts.exceptions.UserNotFoundException;
import com.beeja.api.accounts.model.Organization.Organization;
import com.beeja.api.accounts.model.User;
import com.beeja.api.accounts.model.UserPreferences;
import com.beeja.api.accounts.repository.UserRepository;
import com.beeja.api.accounts.requests.UpdateUserRequest;
import com.beeja.api.accounts.requests.UpdateUserRoleRequest;
import com.beeja.api.accounts.serviceImpl.EmployeeServiceImpl;
import com.beeja.api.accounts.utils.Constants;
import com.beeja.api.accounts.utils.UserContext;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class EmployeeServiceImplTest {

  @InjectMocks EmployeeServiceImpl employeeServiceImpl;

  @Mock private UserRepository userRepository;

  @Mock RoleRepository roleRepository;

  @Mock private EmployeeFeignClient employeeFeignClient;

  @Mock private UserContext userContext;

  @BeforeEach
  public void init() {
    MockitoAnnotations.openMocks(this);
  }

  Organization organization =
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

  Role role1 = new Role("1", "ROLE_EMPLOYEE", Set.of("READ_EMPLOYEE"));
  Role role2 = new Role("2", "ROLE_MANAGER", Set.of("CREATE_EMPLOYEE", "UPDATE_EMPLOYEE"));

  User user1 =
      new User(
          "1",
          "dattu",
          "gundeti",
          "dattu@example.com",
          Set.of(role1),
          "EMP001",
          organization,
          new UserPreferences(),
          true,
          role1.getPermissions(),
          "admin",
          "admin",
          new Date(),
          new Date());

  User user2 =
      new User(
          "2",
          "ravi",
          "ravi",
          "kiran@example.com",
          Set.of(role2),
          "EMP002",
          organization,
          new UserPreferences(),
          true,
          role2.getPermissions(),
          "admin",
          "admin",
          new Date(),
          new Date());

  @Test
  public void toGetAllUser() {
    // Arrange
    List<User> users = new ArrayList<>(Arrays.asList(user1, user2));
    when(userRepository.findAll()).thenReturn(users);

    // Act
    List<User> allusers = employeeServiceImpl.getAllEmployees();

    // Assert
    assertNotNull(allusers);
    assertEquals(2, allusers.size());
    assertTrue(allusers.contains(user1));
    assertTrue(allusers.contains(user2));
  }

  @Test
  public void toGetAllByEmail() {

    // Arrange
    when(userRepository.findByEmail("dattu@example.com")).thenReturn(user1);
    UserContext.setLoggedInUserOrganization(organization);

    // Act
    User ruser = employeeServiceImpl.getEmployeeByEmail("dattu@example.com");

    // Assert
    assertNotNull(ruser);
    assertEquals("dattu@example.com", ruser.getEmail());
    assertEquals("org1", ruser.getOrganizations().getId());
  }

  @Test
  void testGetEmployeeByEmail_UserNotFound() {
    // Arrange
    String email = "nonexistent@example.com";
    Organization loggedInUserOrganization = new Organization("org1");
    UserContext.setLoggedInUserOrganization(loggedInUserOrganization);

    when(userRepository.findByEmail(email)).thenReturn(null);

    // Act
    UserNotFoundException exception =
        assertThrows(
            UserNotFoundException.class, () -> employeeServiceImpl.getEmployeeByEmail(email));
    // Assert
    assertEquals("User Not Found " + email, exception.getMessage());
  }

  @Test
  void testGetEmployeeByEmployeeId_UserNotFound() {
    // Arrange
    String employeeId = "EMP001";
    when(userRepository.findByEmployeeId(employeeId)).thenReturn(null);

    // Act & Assert
    assertThrows(
        UserNotFoundException.class, () -> employeeServiceImpl.getEmployeeByEmployeeId(employeeId));
  }

  @Test
  void testGetEmployeeByEmployeeId_UserBelongsToDifferentOrganization() {
    // Arrange
    String employeeId = "EMP1";
    when(userRepository.findByEmployeeId(employeeId)).thenReturn(null);
    UserContext.setLoggedInUserOrganization(organization);

    // Act &Assert
    assertThrows(
        UserNotFoundException.class, () -> employeeServiceImpl.getEmployeeByEmployeeId(employeeId));
  }

  @Test
  void testGetEmployeeByEmployeeId_Successful() {
    // Arrange
    when(userRepository.findByEmployeeId("EMP001")).thenReturn(user1);
    UserContext.setLoggedInUserOrganization(organization);

    // Act
    User ruser = employeeServiceImpl.getEmployeeByEmployeeId("EMP001");

    // Assert
    assertNotNull(ruser);
    assertEquals("EMP001", ruser.getEmployeeId());
    assertEquals("org1", ruser.getOrganizations().getId());
  }

  @Test
  void testToggleUserActivation_SuccessfulActivationToggle() {
    // Arrange
    String employeeId = "EMP001";
    UserContext.setLoggedInEmployeeId("EMP002");
    UserContext.setLoggedInUserOrganization(organization);
    when(userRepository.findByEmployeeId("EMP001")).thenReturn(user1);
    when(userRepository.findByEmployeeId(UserContext.getLoggedInEmployeeId())).thenReturn(user1);

    // Act
    employeeServiceImpl.changeEmployeeStatus(employeeId);

    // Assert
    assertNotEquals(!user1.isActive(), user1.isActive());
  }

  @Test
  void testUpdateUserRolesAndPermissions() {
    // Arrange
    String employeeId = "EMP001";

    when(userRepository.findByEmployeeId(employeeId)).thenReturn(user1);
    UserContext.setLoggedInUserOrganization(organization);
    Role role = new Role("1", "ROLE_SUPER_ADMIN", Set.of("READ_EMPLOYEE"));
    UpdateUserRoleRequest uprole = new UpdateUserRoleRequest();
    uprole.setRoles(Set.of("ROLE_HR"));

    Set<Role> roleset = new HashSet<>();
    when(roleRepository.findByName(anyString())).thenReturn(role);
    roleset.add(role);
    user1.setRoles(roleset);

    user1.setPermissions(role.getPermissions());
    Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user1);

    // Act
    User updatedUser = employeeServiceImpl.updateEmployeeRolesDyEmployeeId(employeeId, uprole);
    // Assert
    assertNotNull(updatedUser);
  }

  @Test
  void testUpdateEmployeeRoles_UserNotFound() {
    // Arrange
    String empId = "ABCD";
    UpdateUserRoleRequest updateRequest = new UpdateUserRoleRequest();

    when(userRepository.findByEmployeeId(empId)).thenReturn(null);

    // Act & Assert
    assertThrows(
        UserNotFoundException.class,
        () -> employeeServiceImpl.updateEmployeeRolesDyEmployeeId(empId, updateRequest),
        Constants.USER_NOT_FOUND + empId);
  }

  @Test
  void testCreateEmployee_SuccessfulCreation() {
    // Arrange
    String email = "abc@gmail.com";
    String employeeId = "tacdattu";
    User user = new User();
    user.setEmail(email);
    user.setEmployeeId(employeeId);

    Role role = new Role("1", "ROLE_SUPER_ADMIN", Set.of("READ_EMPLOYEE"));
    Set<Role> roleset = new HashSet<>();
    when(roleRepository.findByName(anyString())).thenReturn(role);
    roleset.add(role);
    user.setRoles(roleset);
    when(userRepository.findByEmail(email)).thenReturn(null);
    when(userRepository.findByEmployeeId(employeeId)).thenReturn(null);
    Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);

    user.setPermissions(role.getPermissions());

    // Act
    User createdUser = employeeServiceImpl.createEmployee(user);
    // Assert
    assertNotNull(createdUser);
  }

  @Test
  void testCreateEmployeeEmail_UserAlreadyExists() {
    // Arrange
    User user = new User();
    user.setEmail("abcd@gmail.com");
    when(userRepository.findByEmail(user.getEmail())).thenReturn(user);

    // Act & Assert
    assertThrows(UserNotFoundException.class, () -> employeeServiceImpl.createEmployee(user));
  }

  @Test
  void testCreateEmployeeId_UserAlreadyExists() {
    // Arrange
    User user = new User();
    user.setEmployeeId("ABCD");
    when(userRepository.findByEmployeeId(user.getEmployeeId())).thenReturn(user);

    // Act & Assert
    assertThrows(UserNotFoundException.class, () -> employeeServiceImpl.createEmployee(user));
  }

  @Test
  public void testUpdateEmployeeByEmployeeId() {
    // Arrange
    String employeeId = "ABCD";
    UserContext.setLoggedInUserOrganization(organization);

    UpdateUserRequest updatedUser = new UpdateUserRequest();

    User existingUser = new User();
    existingUser.setEmployeeId(employeeId);
    existingUser.setOrganizations(organization);

    when(userRepository.findByEmployeeId(employeeId)).thenReturn(existingUser);

    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User savedUser = invocation.getArgument(0);
              savedUser.setModifiedAt(new Date());
              return savedUser;
            });

    // Act
    User result = employeeServiceImpl.updateEmployeeByEmployeeId(employeeId, updatedUser);

    // Assert
    verify(userRepository, times(1)).findByEmployeeId(employeeId);
    verify(userRepository, times(1)).save(any(User.class));
    assertNotNull(result);
    assertEquals(employeeId, result.getEmployeeId());
    assertNotNull(result.getModifiedAt());
  }

  @Test
  void testUpdateEmployeeEmployeeId_UserAlreadyExists() {
    // Arrange
    User user = new User();
    UpdateUserRequest updatedUser = new UpdateUserRequest();
    when(userRepository.findByEmployeeId("ABCD")).thenReturn(null);

    // Act & Assert
    assertThrows(
        UserNotFoundException.class,
        () -> employeeServiceImpl.updateEmployeeByEmployeeId("abcd", updatedUser));
  }
}
