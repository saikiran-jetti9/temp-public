package com.beeja.api.accounts.serviceImpl;

import com.beeja.api.accounts.clients.EmployeeFeignClient;
import com.beeja.api.accounts.clients.NotificationClient;
import com.beeja.api.accounts.constants.PermissionConstants;
import com.beeja.api.accounts.constants.RoleConstants;
import com.beeja.api.accounts.enums.ErrorCode;
import com.beeja.api.accounts.enums.ErrorType;
import com.beeja.api.accounts.exceptions.BadRequestException;
import com.beeja.api.accounts.exceptions.ResourceAlreadyFoundException;
import com.beeja.api.accounts.exceptions.ResourceNotFoundException;
import com.beeja.api.accounts.exceptions.UserNotFoundException;
import com.beeja.api.accounts.model.Organization.Organization;
import com.beeja.api.accounts.model.Organization.Role;
import com.beeja.api.accounts.model.User;
import com.beeja.api.accounts.repository.RolesRepository;
import com.beeja.api.accounts.repository.UserRepository;
import com.beeja.api.accounts.requests.NewUserEmailRequest;
import com.beeja.api.accounts.requests.UpdateUserRequest;
import com.beeja.api.accounts.requests.UpdateUserRoleRequest;
import com.beeja.api.accounts.response.EmployeeCount;
import com.beeja.api.accounts.service.EmployeeService;
import com.beeja.api.accounts.utils.BuildErrorMessage;
import com.beeja.api.accounts.utils.Constants;
import com.beeja.api.accounts.utils.UserContext;
import com.beeja.api.accounts.utils.methods.ServiceMethods;
import feign.FeignException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl implements EmployeeService {

  @Autowired UserRepository userRepository;

  @Autowired private EmployeeFeignClient employeeFeignClient;

  @Autowired RolesRepository rolesRepository;

  @Autowired MongoTemplate mongoTemplate;

  @Autowired NotificationClient notificationClient;

  @Override
  public User createEmployee(User user) throws Exception {
    String userEmail = user.getEmail();
    String employeeId = user.getEmployeeId();

    if (userRepository.findByEmailAndOrganizations(
            userEmail, UserContext.getLoggedInUserOrganization())
        != null) {
      throw new ResourceAlreadyFoundException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.RESOURCE_EXISTS_ERROR,
              ErrorCode.EMPLOYEE_ALREADY_FOUND,
              Constants.USER_ALREADY_FOUND + userEmail));
    }

    if (userRepository.findByEmployeeIdAndOrganizations(
            employeeId, UserContext.getLoggedInUserOrganization())
        != null) {
      throw new ResourceAlreadyFoundException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.RESOURCE_EXISTS_ERROR,
              ErrorCode.EMPLOYEE_ALREADY_FOUND,
              Constants.EMPLOYEE_WITH_ID_ALREADY_FOUND + employeeId));
    }

    Set<Role> userRoles = new HashSet<>();
    /*
    TODO:  Update the Default Role
    */
    try {
      Role defaultRole =
          rolesRepository.findByNameAndOrganizationId(
              RoleConstants.ROLE_EMPLOYEE, UserContext.getLoggedInUserOrganization().getId());
      userRoles.add(defaultRole);
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.API_ERROR,
              ErrorCode.ERROR_ASSIGNING_ROLE,
              Constants.ERROR_IN_ASSIGNING_ROLE));
    }
    user.setRoles(userRoles);
    user.setCreatedBy(UserContext.getLoggedInUserEmail());
    user.setOrganizations(UserContext.getLoggedInUserOrganization());

    UUID uuid = UUID.randomUUID();
    String uuidString = uuid.toString().substring(0, 7);
    String hashedUuid = hashWithBcrypt(uuidString);

    user.setPassword(hashedUuid);

    User createdUser;
    try {
      createdUser = userRepository.save(user);
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.API_ERROR, ErrorCode.RESOURCE_CREATING_ERROR, Constants.USER_CREATE_ERROR));
    }

    try {
      employeeFeignClient.createEmployee(createdUser);
    } catch (FeignException.FeignClientException e) {
      userRepository.delete(createdUser);
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.API_ERROR,
              ErrorCode.RESOURCE_CREATING_ERROR,
              Constants.EMPLOYEE_FEIGN_CLIENT_ERROR));
    }

    CompletableFuture.runAsync(
        () -> {
          NewUserEmailRequest newUserEmailRequest = new NewUserEmailRequest();
          newUserEmailRequest.setEmployeeId(createdUser.getEmployeeId());
          newUserEmailRequest.setEmployeeName(createdUser.getFirstName());
          newUserEmailRequest.setToMail(createdUser.getEmail());
          newUserEmailRequest.setOrganizationId(createdUser.getOrganizations().getId());
          newUserEmailRequest.setOrganizationName(createdUser.getOrganizations().getName());
          newUserEmailRequest.setPassword(uuidString);
          notificationClient.sendEmail(
              newUserEmailRequest, "Bearer " + UserContext.getAccessToken());
        });
    return createdUser;
  }

  @Override
  public void changeEmployeeStatus(String employeeId) throws Exception {
    employeeId = employeeId.toUpperCase();
    User optionalUser =
        userRepository.findByEmployeeIdAndOrganizations(
            employeeId, UserContext.getLoggedInUserOrganization());
    if (optionalUser == null) {
      throw new ResourceAlreadyFoundException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.RESOURCE_NOT_FOUND_ERROR,
              ErrorCode.USER_NOT_FOUND,
              Constants.USER_NOT_FOUND + employeeId));
    }

    if (Objects.equals(optionalUser.getEmployeeId(), UserContext.getLoggedInEmployeeId())) {
      throw new BadRequestException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.AUTHORIZATION_ERROR,
              ErrorCode.CANNOT_CHANGE_SELF_STATUS,
              Constants.CANT_INACTIVE_SELF));
    }

    optionalUser.setActive(!optionalUser.isActive());
    try {
      userRepository.save(optionalUser);
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR, ErrorCode.CANNOT_SAVE_CHANGES, Constants.USER_UPDATE_ERROR));
    }
  }

  @Override
  public List<User> getAllEmployees() throws Exception {
    try {
      if (UserContext.getLoggedInUserPermissions()
          .contains(PermissionConstants.GET_ALL_EMPLOYEES)) {
        return userRepository.findByOrganizationsId(
            UserContext.getLoggedInUserOrganization().getId());
      } else {
        return userRepository.findByOrganizationsAndIsActive(
            UserContext.getLoggedInUserOrganization(), true);
      }
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR,
              ErrorCode.UNABLE_TO_FETCH_DETAILS,
              Constants.ERROR_RETRIEVING_USER));
    }
  }

  @Override
  public User getEmployeeByEmail(String email, Organization organization) throws Exception {
    User user;
    try {
      user = userRepository.findByEmailAndOrganizations(email, organization);
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.API_ERROR,
              ErrorCode.UNABLE_TO_FETCH_DETAILS,
              Constants.UNABLE_TO_FETCH_DETAILS_FROM_DATABASE));
    }
    if (user == null) {
      throw new ResourceNotFoundException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.RESOURCE_NOT_FOUND_ERROR,
              ErrorCode.USER_NOT_FOUND,
              Constants.USER_NOT_FOUND + email));
    }
    return user;
  }

  @Override
  public User getEmployeeByEmployeeId(String employeeId, Organization organization)
      throws Exception {
    User user;
    try {
      user = userRepository.findByEmployeeIdAndOrganizations(employeeId, organization);
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR,
              ErrorCode.UNABLE_TO_FETCH_DETAILS,
              Constants.ERROR_RETRIEVING_USER + e.getMessage()));
    }

    if (user == null) {
      throw new ResourceNotFoundException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.RESOURCE_NOT_FOUND_ERROR,
              ErrorCode.USER_NOT_FOUND,
              Constants.USER_NOT_FOUND));
    }
    return user;
  }

  @Override
  public User updateEmployeeRolesDyEmployeeId(String empId, UpdateUserRoleRequest updateRequest)
      throws Exception {
    User user;
    try {
      user =
          userRepository.findByEmployeeIdAndOrganizations(
              empId, UserContext.getLoggedInUserOrganization());
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR,
              ErrorCode.UNABLE_TO_FETCH_DETAILS,
              Constants.ERROR_RETRIEVING_USER));
    }

    if (user == null) {
      throw new ResourceNotFoundException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.RESOURCE_NOT_FOUND_ERROR,
              ErrorCode.USER_NOT_FOUND,
              Constants.USER_NOT_FOUND + empId));
    }

    if (Objects.equals(user.getEmployeeId(), UserContext.getLoggedInEmployeeId())) {
      throw new BadRequestException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.AUTHORIZATION_ERROR,
              ErrorCode.CANNOT_CHANGE_SELF_ROLES,
              Constants.CANT_UPDATE_ROLES_SELF));
    }
    Set<Role> updatedRoles = new HashSet<>();
    for (String role : updateRequest.getRoles()) {
      Role roleToBeAddedToEmployee =
          rolesRepository.findByNameAndOrganizationId(
              role, UserContext.getLoggedInUserOrganization().getId());
      if (roleToBeAddedToEmployee != null) {
        Role roleFromDB = rolesRepository.findByName(role);
        if (roleFromDB != null) {
          updatedRoles.add(roleFromDB);
        }
      } else {
        throw new ResourceNotFoundException(
            BuildErrorMessage.buildErrorMessage(
                ErrorType.RESOURCE_NOT_FOUND_ERROR,
                ErrorCode.ROLE_NOT_FOUND,
                Constants.ROLE_NOT_FOUND + role));
      }
    }
    user.setRoles(updatedRoles);
    try {
      return userRepository.save(user);
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR,
              ErrorCode.CANNOT_SAVE_CHANGES,
              Constants.ERROR_IN_ASSIGNING_ROLE));
    }
  }

  @Override
  public User updateEmployeeByEmployeeId(String employeeId, UpdateUserRequest updatedUser) {
    User existingUser =
        userRepository.findByEmployeeIdAndOrganizations(
            employeeId, UserContext.getLoggedInUserOrganization());

    if (existingUser == null) {
      throw new ResourceNotFoundException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.RESOURCE_NOT_FOUND_ERROR,
              ErrorCode.USER_NOT_FOUND,
              Constants.USER_NOT_FOUND + employeeId));
    }

    String[] nullProperties = ServiceMethods.getNullPropertyNames(updatedUser);
    BeanUtils.copyProperties(updatedUser, existingUser, nullProperties);
    existingUser.setModifiedAt(new Date());
    existingUser.setModifiedBy(UserContext.getLoggedInUserEmail());
    return userRepository.save(existingUser);
  }

  @Override
  public List<User> getUsersByPermissionAndOrganization(String permission) throws Exception {
    try {
      Query roleQuery = new Query();
      roleQuery.addCriteria(
          Criteria.where("permissions")
              .in(permission.toUpperCase())
              .and("organizationId")
              .is(UserContext.getLoggedInUserOrganization().getId()));
      roleQuery.fields().include("permissions");
      List<Role> roles = mongoTemplate.find(roleQuery, Role.class);
      Set<User> userSet = new HashSet<>();

      for (Role role : roles) {
        Query userQuery = new Query();
        userQuery.addCriteria(Criteria.where("roles").in(role.getId()).and("isActive").is(true));
        userQuery
            .fields()
            .include("firstName")
            .include("lastName")
            .include("employeeId")
            .include("email");
        userSet.addAll(mongoTemplate.find(userQuery, User.class));
      }
      List<User> users = new ArrayList<>(userSet);
      return users;
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR,
              ErrorCode.UNABLE_TO_FETCH_DETAILS,
              Constants.ERROR_RETRIEVING_USER));
    }
  }

  @Override
  public EmployeeCount getEmployeeCountByOrganization() throws Exception {
    try {
      Long totalCount = 0L;
      if (UserContext.getLoggedInUserPermissions()
          .contains(PermissionConstants.GET_ALL_EMPLOYEES)) {
        totalCount = userRepository.countByOrganizations(UserContext.getLoggedInUserOrganization());
      }
      Long activeEmployeeCount =
          userRepository.countByOrganizationsAndIsActive(
              UserContext.getLoggedInUserOrganization(), true);
      EmployeeCount employeeCount = new EmployeeCount();
      employeeCount.setTotalCount(totalCount);
      employeeCount.setActiveCount(activeEmployeeCount);
      if (UserContext.getLoggedInUserPermissions()
          .contains(PermissionConstants.GET_ALL_EMPLOYEES)) {
        employeeCount.setInactiveCount(totalCount - activeEmployeeCount);
      }
      return employeeCount;
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR,
              ErrorCode.UNABLE_TO_FETCH_DETAILS,
              Constants.ERROR_IN_FETCHING_EMPLOYEE_COUNT));
    }
  }

  @Override
  public boolean isEmployeeHasPermission(String employeeId, String permission) throws Exception {
    try {
      User user =
          userRepository.findByEmployeeIdAndOrganizations(
              employeeId, UserContext.getLoggedInUserOrganization());
      if (user == null) {
        throw new UserNotFoundException(Constants.USER_NOT_FOUND + employeeId);
      }
      for (Role role : user.getRoles()) {
        if (role.getPermissions().contains(permission)) {
          return true;
        }
      }
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR,
              ErrorCode.UNABLE_TO_FETCH_DETAILS,
              Constants.ERROR_IN_CHECKING_PERMISSION));
    }
    return false;
  }

  private static String hashWithBcrypt(String uuidString) {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    return encoder.encode(uuidString);
  }
}
