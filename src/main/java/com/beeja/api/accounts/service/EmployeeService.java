package com.beeja.api.accounts.service;

import com.beeja.api.accounts.model.Organization.Organization;
import com.beeja.api.accounts.model.User;
import com.beeja.api.accounts.requests.UpdateUserRequest;
import com.beeja.api.accounts.requests.UpdateUserRoleRequest;
import com.beeja.api.accounts.response.EmployeeCount;
import java.util.List;

public interface EmployeeService {
  User createEmployee(User user) throws Exception;

  void changeEmployeeStatus(String userId) throws Exception;

  List<User> getAllEmployees() throws Exception;

  User getEmployeeByEmail(String email, Organization organization) throws Exception;

  User getEmployeeByEmployeeId(String employeeId, Organization organization) throws Exception;

  User updateEmployeeRolesDyEmployeeId(String empId, UpdateUserRoleRequest updateRequest)
      throws Exception;

  User updateEmployeeByEmployeeId(String employeeId, UpdateUserRequest updatedUser);

  List<User> getUsersByPermissionAndOrganization(String permission) throws Exception;

  EmployeeCount getEmployeeCountByOrganization() throws Exception;

  boolean isEmployeeHasPermission(String employeeId, String permission) throws Exception;
}
