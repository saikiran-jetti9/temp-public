package com.beeja.api.accounts.controllers;

import com.beeja.api.accounts.annotations.HasPermission;
import com.beeja.api.accounts.constants.PermissionConstants;
import com.beeja.api.accounts.exceptions.BadRequestException;
import com.beeja.api.accounts.model.User;
import com.beeja.api.accounts.repository.UserRepository;
import com.beeja.api.accounts.requests.UpdateUserRequest;
import com.beeja.api.accounts.requests.UpdateUserRoleRequest;
import com.beeja.api.accounts.response.EmployeeCount;
import com.beeja.api.accounts.service.EmployeeService;
import com.beeja.api.accounts.utils.Constants;
import com.beeja.api.accounts.utils.UserContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
public class EmployeeController {

  @Autowired private EmployeeService employeeService;

  @Autowired UserRepository userRepository;

  @GetMapping("/me")
  @HasPermission(PermissionConstants.READ_EMPLOYEE)
  public ResponseEntity<User> getLoggedInUser() throws Exception {
    User loggedInUser =
        employeeService.getEmployeeByEmail(
            UserContext.getLoggedInUserEmail(), UserContext.getLoggedInUserOrganization());
    if (loggedInUser != null) {
      return ResponseEntity.ok(loggedInUser);
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/empid/{employeeId}")
  @HasPermission(PermissionConstants.READ_EMPLOYEE)
  public ResponseEntity<User> getUserByEmployeeId(@PathVariable String employeeId)
      throws Exception {
    return new ResponseEntity<>(
        employeeService.getEmployeeByEmployeeId(
            employeeId, UserContext.getLoggedInUserOrganization()),
        HttpStatus.OK);
  }

  /*
   * Below End Point is used in other services while authenticating User (Used in Auth Filters)
   * */

  @GetMapping("/{email}")
  @HasPermission(PermissionConstants.READ_EMPLOYEE)
  public ResponseEntity<User> getUserByEmail(@PathVariable String email) throws Exception {
    return new ResponseEntity<>(
        employeeService.getEmployeeByEmail(email, UserContext.getLoggedInUserOrganization()),
        HttpStatus.OK);
  }

  /*
   * Below End Point is used to check whether email is already registered or  not while updating from Employee Service
   * */
  @GetMapping("/ispresent/{email}")
  @HasPermission(PermissionConstants.CREATE_EMPLOYEE)
  public Boolean isUserPresentWithMail(@PathVariable String email) {
    try {
      User user =
          employeeService.getEmployeeByEmail(email, UserContext.getLoggedInUserOrganization());
      return user != null;
    } catch (Exception e) {
      return false;
    }
  }

  // NOTE: Currently Employee Service is responsible to send all employees
  @GetMapping
  @HasPermission(PermissionConstants.READ_EMPLOYEE)
  public ResponseEntity<List<User>> getAllEmployees() throws Exception {
    return new ResponseEntity<>(employeeService.getAllEmployees(), HttpStatus.OK);
  }

  @PostMapping
  @HasPermission(PermissionConstants.CREATE_EMPLOYEE)
  public ResponseEntity<User> createEmployee(
      @RequestBody @Valid User user, BindingResult bindingResult) throws Exception {
    if (bindingResult.hasErrors()) {
      List<String> errorMessages =
          bindingResult.getAllErrors().stream()
              .map(ObjectError::getDefaultMessage)
              .collect(Collectors.toList());
      throw new BadRequestException(errorMessages.toString());
    }
    User createdUser = employeeService.createEmployee(user);
    return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
  }

  @PutMapping("/{employeeId}")
  @HasPermission(PermissionConstants.UPDATE_EMPLOYEE)
  public ResponseEntity<User> updateUser(
      @PathVariable String employeeId, @RequestBody UpdateUserRequest updatedUser) {
    return ResponseEntity.ok(employeeService.updateEmployeeByEmployeeId(employeeId, updatedUser));
  }

  @PutMapping("/{employeeId}/change-status")
  @HasPermission(PermissionConstants.INACTIVE_EMPLOYEE)
  public ResponseEntity<String> changeEmployeeStatus(@PathVariable String employeeId)
      throws Exception {
    employeeService.changeEmployeeStatus(employeeId);
    return new ResponseEntity<>(Constants.USER_STATUS_UPDATED, HttpStatus.OK);
  }

  @PatchMapping("/{employeeId}/change-roles")
  @HasPermission(PermissionConstants.UPDATE_ROLES_AND_PERMISSIONS)
  public ResponseEntity<?> updateUserRoles(
      @PathVariable String employeeId, @RequestBody UpdateUserRoleRequest newRoles)
      throws Exception {
    User updatedUser = employeeService.updateEmployeeRolesDyEmployeeId(employeeId, newRoles);
    return new ResponseEntity<>(updatedUser, HttpStatus.OK);
  }

  @GetMapping("/permissions/{permission}")
  public ResponseEntity<List<User>> getUsersByPermissionAndOrganization(
      @PathVariable String permission) throws Exception {
    List<User> users = employeeService.getUsersByPermissionAndOrganization(permission);
    return ResponseEntity.ok(users);
  }

  @GetMapping("/permissions/{employeeId}/{permission}")
  @HasPermission({
    PermissionConstants.CREATE_EMPLOYEE,
    PermissionConstants.UPDATE_EMPLOYEE,
    PermissionConstants.GET_ALL_EMPLOYEES
  })
  public ResponseEntity<Boolean> isEmployeeHasPermission(
      @PathVariable String employeeId, @PathVariable String permission) throws Exception {
    return ResponseEntity.ok(employeeService.isEmployeeHasPermission(employeeId, permission));
  }

  @GetMapping("/count")
  @HasPermission((PermissionConstants.READ_EMPLOYEE))
  public ResponseEntity<EmployeeCount> getEmployeeCountByOrganizationId() throws Exception {
    EmployeeCount uniqueEmployeeCount = employeeService.getEmployeeCountByOrganization();
    return ResponseEntity.ok(uniqueEmployeeCount);
  }
}
