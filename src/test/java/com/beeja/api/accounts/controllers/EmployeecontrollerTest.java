package com.beeja.api.accounts.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.beeja.api.accounts.constants.PermissionConstants;
import com.beeja.api.accounts.exceptions.UserNotFoundException;
import com.beeja.api.accounts.model.Organization.Organization;
import com.beeja.api.accounts.model.User;
import com.beeja.api.accounts.model.UserPreferences;
import com.beeja.api.accounts.repository.UserRepository;
import com.beeja.api.accounts.requests.UpdateUserRequest;
import com.beeja.api.accounts.requests.UpdateUserRoleRequest;
import com.beeja.api.accounts.service.EmployeeService;
import com.beeja.api.accounts.utils.Constants;
import com.beeja.api.accounts.utils.UserContext;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;

public class EmployeecontrollerTest {

  @InjectMocks EmployeeController employeeController;

  @Autowired MockMvc mockMvc;

  @Mock private EmployeeService employeeService;
  @Mock private BindingResult bindingResult;
  @Mock UserRepository userRepository;

  private String basePath = "/v1/users";

  User user1 =
      new User(
          "1",
          "dattu",
          "gundeti",
          "dattu@example.com",
          new HashSet<>(),
          "EMP001",
          new Organization("Org1"),
          new UserPreferences(),
          true,
          new HashSet<>(),
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
          new HashSet<>(),
          "EMP002",
          new Organization("Org2"),
          new UserPreferences(),
          true,
          new HashSet<>(),
          "admin",
          "admin",
          new Date(),
          new Date());

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(employeeController).build();
  }

  @Test
  void toGetAllUsers() {
    // Arrange
    UserContext.setLoggedInUserPermissions(
        Collections.singleton(PermissionConstants.CREATE_EMPLOYEE));
    UserContext.setLoggedInUserOrganization(new Organization());

    List<User> users = Arrays.asList(user1, user2);
    when(userRepository.findByOrganizationsId(UserContext.getLoggedInUserOrganization().getId()))
        .thenReturn(users);

    // Act
    ResponseEntity<?> responseEntity = employeeController.getAllEmployees();

    // Assert
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(users, responseEntity.getBody());
  }

  @Test
  void toGetUsersByEmail() throws Exception {
    // Arrange
    when(employeeService.getEmployeeByEmail(
            "dattu@example.com", UserContext.getLoggedInUserOrganization()))
        .thenReturn(user1);

    // Act & Assert
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v1/users/{email}", "dattu@example.com"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andDo(print());
  }

  @Test
  void testGetUserByEmail_UserNotFound() throws Exception {
    // Arrange
    String email = "abc@gmail.com";

    // Act & Assert
    when(employeeService.getEmployeeByEmail(email, UserContext.getLoggedInUserOrganization()))
        .thenThrow(new UserNotFoundException("User not found: " + email));
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v1/users/{email}", email))
        .andExpect(status().isNotFound())
        .andDo(print());
  }

  @Test
  void testGetUserByEmail_InternalServerError() throws Exception {
    // Arrange
    String email = "abc@gmail.com";

    // Act & Assert
    when(employeeService.getEmployeeByEmail(email, UserContext.getLoggedInUserOrganization()))
        .thenThrow(new RuntimeException("Internal server error"));
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v1/users/{email}", email))
        .andExpect(status().isInternalServerError())
        .andDo(print());
  }

  @Test
  public void testIsUserPresentWithMail_UserPresent() {
    // Arrange
    String userEmail = "abc@gmail.com";
    User mockUser = new User();
    mockUser.setEmail(userEmail);
    when(employeeService.getEmployeeByEmail(userEmail, UserContext.getLoggedInUserOrganization()))
        .thenReturn(mockUser);

    // Act
    Boolean result = employeeController.isUserPresentWithMail(userEmail);

    // Assert
    assertTrue(result);
  }

  @Test
  public void testIsUserPresentWithMail_UserNotPresent() {

    // Arrange
    String userEmail = "abc@gmail.com";
    when(employeeService.getEmployeeByEmail(userEmail, UserContext.getLoggedInUserOrganization()))
        .thenThrow(new UserNotFoundException("User not found"));

    // Act
    Boolean result = employeeController.isUserPresentWithMail(userEmail);

    // Assert
    assertFalse(result);
  }

  @Test
  void testGetUserByEmployeeId() throws Exception {
    // Arrange
    String employeeId = "ABC";

    when(employeeService.getEmployeeByEmployeeId(
            anyString(), UserContext.getLoggedInUserOrganization()))
        .thenReturn(user1);

    // Act & Assert
    mockMvc.perform(
        MockMvcRequestBuilders.get("/v1/users/empid/{employeeId}", employeeId)
            .contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  void testGetUserByEmployeeId_UserNotFound() throws Exception {
    // Arrange
    String empId = "abc@gmail.com";
    when(employeeController.getUserByEmployeeId(empId))
        .thenThrow(new UserNotFoundException("User not found: " + empId));

    // Act & Assert
    mockMvc
        .perform(MockMvcRequestBuilders.get(basePath + "/empid/{empId}", empId))
        .andExpect(status().isNotFound())
        .andDo(print());
  }

  @Test
  void testGetUserByEmployeeId_InternalServerError() throws Exception {
    // Arrange
    String empId = "ABCD";
    when(employeeController.getUserByEmployeeId(empId))
        .thenThrow(new RuntimeException("Internal server error"));

    // Act & Assert
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v1/users/empid/{empId}", empId))
        .andExpect(status().isInternalServerError())
        .andDo(print());
  }

  @Test
  void testgetLoggedinuser() throws Exception {
    // Arrange
    UserContext.setLoggedInUserEmail("dattu@gmail.com");
    when(employeeService.getEmployeeByEmail(
            "dattu@gmail.com", UserContext.getLoggedInUserOrganization()))
        .thenReturn(user1);

    // Act & Assert
    mockMvc.perform(
        MockMvcRequestBuilders.get(basePath + "/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content(user1.toString()));
  }

  @Test
  void testGetLoggedInUserNotFound() throws Exception {
    // Arrange
    UserContext.setLoggedInUserEmail("nonexistent@example.com");
    when(employeeService.getEmployeeByEmail(
            "nonexistent@example.com", UserContext.getLoggedInUserOrganization()))
        .thenReturn(null);

    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(basePath + "/me").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  public void testCreateEmployee_Success() {
    // Arrange
    User user = new User();
    when(employeeService.createEmployee(user)).thenReturn(user);

    // Act
    ResponseEntity<?> responseEntity = employeeController.createEmployee(user, bindingResult);

    // Assert
    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    assertEquals(user, responseEntity.getBody());
  }

  @Test
  void testCreateEmployee_ValidationError() throws Exception {
    // Arrange
    User userWithValidationErrors = new User();
    userWithValidationErrors.setEmail("invalidEmail");

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(employeeController).build();

    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(basePath)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"invalidEmail\"}")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testCreateEmployee_UserNotFoundException() {
    // Arrange
    User user = new User();
    when(employeeService.createEmployee(user))
        .thenThrow(new UserNotFoundException("User not found"));

    // Act
    ResponseEntity<?> responseEntity = employeeController.createEmployee(user, bindingResult);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    assertEquals("User not found", responseEntity.getBody());
  }

  @Test
  public void testCreateEmployee_InternalserverError() {
    // Arrange
    User user = new User();
    when(employeeService.createEmployee(user))
        .thenThrow(new RuntimeException("Internal server error"));

    // Act
    ResponseEntity<?> responseEntity = employeeController.createEmployee(user, bindingResult);

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
  }

  @Test
  public void testUpdateUser_Success() throws UserNotFoundException {
    // Arrange
    String employeeId = "ABC";
    UpdateUserRequest updateUserRequest = new UpdateUserRequest();
    User updatedUser = new User();

    when(employeeService.updateEmployeeByEmployeeId(employeeId, updateUserRequest))
        .thenReturn(updatedUser);

    // Act
    ResponseEntity<?> responseEntity = employeeController.updateUser(employeeId, updateUserRequest);

    // Assert
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(updatedUser, responseEntity.getBody());
  }

  @Test
  public void testUpdateUser_UserNotFoundException() throws UserNotFoundException {
    // Arrange
    String employeeId = "ABC";
    UpdateUserRequest updateUserRequest = new UpdateUserRequest();
    when(employeeService.updateEmployeeByEmployeeId(employeeId, updateUserRequest))
        .thenThrow(new UserNotFoundException("User not found"));

    // Act
    ResponseEntity<?> responseEntity = employeeController.updateUser(employeeId, updateUserRequest);

    // Assert
    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    assertEquals("User not found", responseEntity.getBody());
  }

  @Test
  public void testChangeEmployeeStatus_Success() throws Exception {
    // Arrange
    String employeeId = "ABC";

    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(basePath + "/{employeeId}/change-status", employeeId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().string(Constants.USER_STATUS_UPDATED));
  }

  @Test
  public void testChangeEmployeeStatus_UserNotFoundException() throws Exception {
    // Arrange
    String employeeId = "ABC";

    doThrow(new UserNotFoundException("User not found"))
        .when(employeeService)
        .changeEmployeeStatus(employeeId);

    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(basePath + "/{employeeId}/change-status", employeeId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testChangeEmployeeStatus_InternalServerError() throws Exception {
    // Arrange
    String employeeId = "ABC";

    doThrow(new RuntimeException("Some internal error"))
        .when(employeeService)
        .changeEmployeeStatus(employeeId);

    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(basePath + "/{employeeId}/change-status", employeeId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string(Constants.USER_UPDATE_ERROR + "Some internal error"));
  }

  @Test
  public void testUpdateUserRoles_Success() {
    // Arrange
    String employeeId = "ABC";
    UpdateUserRoleRequest newRoles = new UpdateUserRoleRequest();
    User updatedUser = new User();

    when(employeeService.updateEmployeeRolesDyEmployeeId(employeeId, newRoles))
        .thenReturn(updatedUser);

    // Act
    ResponseEntity<?> responseEntity = employeeController.updateUserRoles(employeeId, newRoles);

    // Assert
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(updatedUser, responseEntity.getBody());
  }

  @Test
  public void testUpdateUserRoles_UserNotFoundException() {
    // Assert
    String employeeId = "ABC";
    UpdateUserRoleRequest newRoles = new UpdateUserRoleRequest();

    when(employeeService.updateEmployeeRolesDyEmployeeId(employeeId, newRoles))
        .thenThrow(new UserNotFoundException("User not found"));

    // Act
    ResponseEntity<?> responseEntity = employeeController.updateUserRoles(employeeId, newRoles);

    // Assert
    assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    assertEquals("User not found", responseEntity.getBody());
  }
}
