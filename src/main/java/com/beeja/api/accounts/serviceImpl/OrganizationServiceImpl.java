package com.beeja.api.accounts.serviceImpl;

import static com.beeja.api.accounts.utils.Constants.INVALID_PINCODE;
import static com.beeja.api.accounts.utils.Constants.PINCODE_NOT_EXISTS;

import com.beeja.api.accounts.clients.EmployeeFeignClient;
import com.beeja.api.accounts.clients.FileClient;
import com.beeja.api.accounts.clients.PostalClient;
import com.beeja.api.accounts.constants.PermissionConstants;
import com.beeja.api.accounts.enums.CurrencyType;
import com.beeja.api.accounts.enums.DateFormats;
import com.beeja.api.accounts.enums.ErrorCode;
import com.beeja.api.accounts.enums.ErrorType;
import com.beeja.api.accounts.enums.FeatureToggles;
import com.beeja.api.accounts.enums.FontNames;
import com.beeja.api.accounts.enums.SubscriptionName;
import com.beeja.api.accounts.enums.Theme;
import com.beeja.api.accounts.enums.TimeZones;
import com.beeja.api.accounts.exceptions.BadRequestException;
import com.beeja.api.accounts.exceptions.ResourceAlreadyFoundException;
import com.beeja.api.accounts.exceptions.ResourceNotFoundException;
import com.beeja.api.accounts.model.Organization.Accounts;
import com.beeja.api.accounts.model.Organization.Address;
import com.beeja.api.accounts.model.Organization.LoanLimit;
import com.beeja.api.accounts.model.Organization.Organization;
import com.beeja.api.accounts.model.Organization.Preferences;
import com.beeja.api.accounts.model.Organization.Role;
import com.beeja.api.accounts.model.User;
import com.beeja.api.accounts.model.featureFlags.FeatureToggle;
import com.beeja.api.accounts.model.subscriptions.Permissions;
import com.beeja.api.accounts.repository.FeatureToggleRepository;
import com.beeja.api.accounts.repository.OrganizationRepository;
import com.beeja.api.accounts.repository.PermissionRepository;
import com.beeja.api.accounts.repository.RolesRepository;
import com.beeja.api.accounts.repository.UserRepository;
import com.beeja.api.accounts.requests.FileRequest;
import com.beeja.api.accounts.response.AddressResponse;
import com.beeja.api.accounts.response.FileDownloadResultMetaData;
import com.beeja.api.accounts.response.FileResponse;
import com.beeja.api.accounts.response.OrganizationResponse;
import com.beeja.api.accounts.service.OrganizationService;
import com.beeja.api.accounts.utils.BuildErrorMessage;
import com.beeja.api.accounts.utils.Constants;
import com.beeja.api.accounts.utils.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OrganizationServiceImpl implements OrganizationService {

  @Autowired OrganizationRepository organizationRepository;

  @Autowired UserRepository userRepository;

  @Autowired RolesRepository rolesRepository;

  @Autowired EmployeeFeignClient employeeFeignClient;

  @Autowired PermissionRepository permissionRepository;

  @Autowired FileClient fileClient;

  @Autowired FeatureToggleRepository featureToggleRepository;

  private PostalClient postalClient;
  private ObjectMapper objectMapper;

  @Autowired
  public OrganizationServiceImpl(PostalClient postalClient, ObjectMapper objectMapper) {
    this.postalClient = postalClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public Organization createOrganization(Organization organization) throws Exception {
    Organization savedOrganization = null;
    FeatureToggle featureToggleSaved = null;
    try {
      String domain = extractDomain(organization.getEmail());
      OrganizationResponse existingOrganization = organizationRepository.findByEmailDomain(domain);
      if (existingOrganization != null) {
        throw new ResourceAlreadyFoundException(
            BuildErrorMessage.buildErrorMessage(
                ErrorType.RESOURCE_EXISTS_ERROR,
                ErrorCode.ORGANIZATION_ALREADY_FOUND,
                Constants.ERROR_IN_CREATING_ORGANIZATION_ORG_EXISTS));
      }
      savedOrganization = organizationRepository.save(organization);
      FeatureToggle featureToggle =
          FeatureToggle.builder()
              .organizationId(savedOrganization.getId())
              .featureToggles(
                  EnumSet.of(
                      FeatureToggles.EMPLOYEE_MANAGEMENT, FeatureToggles.DOCUMENT_MANAGEMENT))
              .build();
      try {
        featureToggleSaved = featureToggleRepository.save(featureToggle);
      } catch (Exception e) {
        throw new Exception(
            BuildErrorMessage.buildErrorMessage(
                ErrorType.DB_ERROR,
                ErrorCode.CANNOT_SAVE_CHANGES,
                Constants.ERROR_IN_INSTANTIATING_FEATURE_TOGGLES));
      }
      createNewRole(savedOrganization);
      User user = createUserForOrganization(organization);
      user.setOrganizations(savedOrganization);
      user.setCreatedBy(UserContext.getLoggedInUserEmail());
      User finalUser = userRepository.save(user);
      finalUser.setOrganizations(savedOrganization);
      try {
        employeeFeignClient.createEmployee(finalUser);
      } catch (Exception e) {
        throw new Exception(
            BuildErrorMessage.buildErrorMessage(
                ErrorType.API_ERROR,
                ErrorCode.RESOURCE_CREATING_ERROR,
                Constants.ERROR_CREATING_EMPLOYEE_FOR_ORG));
      }
      return savedOrganization;
    } catch (Exception e) {
      if (savedOrganization != null) {
        organizationRepository.delete(savedOrganization);
      }
      if (featureToggleSaved != null) {
        featureToggleRepository.delete(featureToggleSaved);
      }
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.API_ERROR,
              ErrorCode.RESOURCE_CREATING_ERROR,
              Constants.ERROR_IN_CREATE_ORGANIZATION));
    }
  }

  private String extractDomain(String email) {
    int atIndex = email.lastIndexOf('@');
    return atIndex != -1 ? email.substring(atIndex + 1) : "";
  }

  private void createNewRole(Organization organization) throws Exception {
    Permissions permissions = permissionRepository.findByName(SubscriptionName.FREE_TIER);
    Role role = new Role();
    role.setName("Super Admin - " + organization.getName());
    role.setDescription("Default Role");
    role.setPermissions(permissions.getPermissions());
    role.setOrganizationId(organization.getId());
    try {
      rolesRepository.save(role);
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR,
              ErrorCode.CANNOT_SAVE_CHANGES,
              Constants.ERROR_IN_CREATING_ROLE_TO_ORGANIZATION));
    }
  }

  private User createUserForOrganization(Organization organization) throws Exception {
    User existingUserByOrganizationContactMail;
    User existingUserByOrganizationMail;
    try {
      existingUserByOrganizationMail = userRepository.findByEmail(organization.getEmail());
      existingUserByOrganizationContactMail =
          userRepository.findByEmail(organization.getContactMail());
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR,
              ErrorCode.UNABLE_TO_FETCH_DETAILS,
              Constants.ERROR_RETRIEVING_USER_FOR_ORGANIZATION));
    }
    if (existingUserByOrganizationContactMail != null || existingUserByOrganizationMail != null) {
      throw new BadRequestException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.RESOURCE_EXISTS_ERROR,
              ErrorCode.EMPLOYEE_ALREADY_FOUND,
              Constants.CANNOT_CREATE_ORGANIZATION_USER));
    }
    User user = new User();
    user.setEmail(organization.getContactMail());
    user.setFirstName(organization.getName());

    Role superAdminRole = rolesRepository.findByName("Super Admin - " + organization.getName());
    if (superAdminRole != null) {
      Set<Role> userRoles = new HashSet<>();
      userRoles.add(superAdminRole);
      user.setRoles(userRoles);
    } else {
      throw new ResourceNotFoundException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.RESOURCE_NOT_FOUND_ERROR,
              ErrorCode.ROLE_NOT_FOUND,
              Constants.ERROR_IN_ASSIGNING_ROLE));
    }

    user.setEmployeeId(organization.getEmailDomain().toUpperCase());
    try {
      return userRepository.save(user);
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR, ErrorCode.CANNOT_SAVE_CHANGES, Constants.ERROR_IN_CREATING_USER));
    }
  }

  @Override
  public List<Organization> getAllOrganizations() throws Exception {
    try {
      return organizationRepository.findAll();
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR,
              ErrorCode.UNABLE_TO_FETCH_DETAILS,
              Constants.ERROR_IN_FETCHING_ORGANIZATIONS));
    }
  }

  @Override
  public List<User> getAllUsersByOrganizationId(String organizationId) throws Exception {
    Optional<Organization> organization;
    try {
      organization = organizationRepository.findById(organizationId);
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR,
              ErrorCode.UNABLE_TO_FETCH_DETAILS,
              Constants.ERROR_NO_ORGANIZATION_FOUND_WITH_PROVIDED_ID));
    }
    if (organization.isPresent()) {
      try {
        return userRepository.findByOrganizationsId(organizationId);
      } catch (Exception e) {
        throw new Exception(
            BuildErrorMessage.buildErrorMessage(
                ErrorType.DB_ERROR,
                ErrorCode.UNABLE_TO_FETCH_DETAILS,
                Constants.ERROR_NO_ORGANIZATION_FOUND_WITH_PROVIDED_ID));
      }
    } else {
      throw new ResourceNotFoundException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.RESOURCE_NOT_FOUND_ERROR,
              ErrorCode.ORGANIZATION_NOT_FOUND,
              Constants.ERROR_NO_ORGANIZATION_FOUND_WITH_PROVIDED_ID));
    }
  }

  @Override
  public Optional<Organization> deleteOrganizationById(String organizationId) throws Exception {
    if (Objects.equals(UserContext.getLoggedInUserOrganization().getId(), organizationId)) {
      throw new BadRequestException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.AUTHORIZATION_ERROR,
              ErrorCode.CANNOT_DELETE_SELF_ORGANIZATION,
              Constants.CANT_DELETE_SELF_ORGANIZATION));
    }

    try {
      Optional<Organization> organization = organizationRepository.findById(organizationId);
      if (organization.isPresent()) {
        List<User> employeesInOrg = userRepository.findByOrganizationsId(organizationId);
        //                employeesInOrg.stream().map(emp->userRepository.deleteById(emp.getId()));
        employeesInOrg.forEach(emp -> userRepository.deleteById(emp.getId()));
        organizationRepository.deleteById(organizationId);
        return organization;
      }
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR,
              ErrorCode.UNABLE_TO_FETCH_DETAILS,
              Constants.ERROR_NO_ORGANIZATION_FOUND_WITH_PROVIDED_ID));
    }
    try {
      employeeFeignClient.deleteAllEmployeesByOrganizationId(organizationId);
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.API_ERROR,
              ErrorCode.RESOURCE_DELETING_ERROR,
              Constants.ORGANIZATION_DALETE_FEIGN_ERROR));
    }
    return Optional.empty();
  }

  @Override
  public OrganizationResponse getOrganizationById(String id) throws Exception {
    try {
      if (UserContext.getLoggedInUserPermissions()
          .contains(PermissionConstants.READ_ALL_ORGANIZATIONS)) {
        return organizationRepository.findByOrganizationId(id);
      }
      return organizationRepository.findByOrganizationId(
          UserContext.getLoggedInUserOrganization().getId());
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR,
              ErrorCode.UNABLE_TO_FETCH_DETAILS,
              Constants.ERROR_NO_ORGANIZATION_FOUND_WITH_PROVIDED_ID));
    }
  }

  @Override
  public Organization updateOrganization(String organizationId, String fields, MultipartFile file)
      throws Exception {
    Optional<Organization> orgOptional;
    if (UserContext.getLoggedInUserPermissions()
        .contains(PermissionConstants.UPDATE_ALL_ORGANIZATIONS)) {
      orgOptional = organizationRepository.findById(organizationId);
    } else {
      orgOptional =
          organizationRepository.findById(UserContext.getLoggedInUserOrganization().getId());
    }

    if (orgOptional.isEmpty()) {
      throw new ResourceNotFoundException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.RESOURCE_NOT_FOUND_ERROR,
              ErrorCode.ORGANIZATION_NOT_FOUND,
              Constants.ERROR_NO_ORGANIZATION_FOUND_WITH_PROVIDED_ID));
    }

    Organization organization = orgOptional.get();
    if (fields != null) {
      ObjectMapper objectMapper = new ObjectMapper();

      try {
        Map<String, Object> fieldsMap =
            objectMapper.readValue(
                fields,
                new TypeReference<Map<String, Object>>() {
                  // Added to pass checkstyle
                });

        for (Map.Entry<String, Object> entry : fieldsMap.entrySet()) {
          String key = entry.getKey();
          Object value = entry.getValue();

          if (value == null) {
            continue;
          }

          try {
            Field field = Organization.class.getDeclaredField(key);
            field.setAccessible(true);
            if (key.equals("preferences")) {
              Map<String, Object> preferencesMap = (Map<String, Object>) value;
              Preferences preferences;
              if (organization.getPreferences() == null) {
                preferences = new Preferences();
              } else {
                preferences = organization.getPreferences();
              }
              for (Map.Entry<String, Object> prefEntry : preferencesMap.entrySet()) {
                String prefKey = prefEntry.getKey();
                Object prefValue = prefEntry.getValue();
                if (prefValue != null) {
                  if (prefKey.equals("theme")) {
                    Theme theme = Theme.valueOf((String) prefValue);
                    preferences.setTheme(theme);
                  } else if (prefKey.equals("dateFormat")) {
                    DateFormats dateFormats = DateFormats.valueOf((String) prefValue);
                    preferences.setDateFormat(dateFormats);
                  } else if (prefKey.equals("timeZone")) {
                    TimeZones timeZone = TimeZones.valueOf((String) prefValue);
                    preferences.setTimeZone(timeZone);
                  } else if (prefKey.equals("fontName")) {
                    FontNames fontName = FontNames.valueOf((String) prefValue);
                    preferences.setFontName(fontName);
                  } else if (prefKey.equals("currencyType")) {
                    CurrencyType currencyType = CurrencyType.valueOf((String) prefValue);
                    preferences.setCurrencyType(currencyType);
                  } else {
                    Field prefField = Preferences.class.getDeclaredField(prefKey);
                    prefField.setAccessible(true);
                    prefField.set(preferences, prefValue);
                  }
                }
              }
              field.set(organization, preferences);
            } else if (key.equals("address")) {
              Map<String, Object> addressMap = (Map<String, Object>) value;
              Address address;
              if (organization.getAddress() == null) {
                address = new Address();
              } else {
                address = organization.getAddress();
              }
              for (Map.Entry<String, Object> addressEntry : addressMap.entrySet()) {
                String addressKey = addressEntry.getKey();
                Object addressValue = addressEntry.getValue();
                if (addressValue != null) {
                  Field addressField = Address.class.getDeclaredField(addressKey);
                  addressField.setAccessible(true);
                  addressField.set(address, addressValue);
                }
              }
              field.set(organization, address);
            } else if (key.equals("accounts")) {
              Map<String, Object> accountsMap = (Map<String, Object>) value;
              Accounts accounts;
              if (organization.getAccounts() == null) {
                accounts = new Accounts();
              } else {
                accounts = organization.getAccounts();
              }
              for (Map.Entry<String, Object> accountsEntry : accountsMap.entrySet()) {
                String accountsKey = accountsEntry.getKey();
                Object accountsValue = accountsEntry.getValue();
                if (accountsValue != null) {
                  Field accountsField = Accounts.class.getDeclaredField(accountsKey);
                  accountsField.setAccessible(true);
                  accountsField.set(accounts, accountsValue);
                }
              }
              field.set(organization, accounts);
            } else if (key.equals("loanLimit")) {
              Map<String, Object> loanLimitMap = (Map<String, Object>) value;
              LoanLimit loanLimit;
              if (organization.getLoanLimit() == null) {
                loanLimit = new LoanLimit();
              } else {
                loanLimit = organization.getLoanLimit();
              }
              for (Map.Entry<String, Object> loanLimitEntry : loanLimitMap.entrySet()) {
                String loanLimitKey = loanLimitEntry.getKey();
                Object loanLimitValue = loanLimitEntry.getValue();
                if (loanLimitValue != null) {
                  Field loanLimitField = LoanLimit.class.getDeclaredField(loanLimitKey);
                  loanLimitField.setAccessible(true);
                  loanLimitField.set(loanLimit, loanLimitValue);
                }
              }
              field.set(organization, loanLimit);
            } else {
              field.set(organization, value);
            }
          } catch (NoSuchFieldException e) {
            throw new BadRequestException(
                BuildErrorMessage.buildErrorMessage(
                    ErrorType.VALIDATION_ERROR,
                    ErrorCode.FIELD_VALIDATION_MISSING,
                    Constants.FIELD_NOT_EXIST_ORGANIZATION_ENTITY + key));
          } catch (IllegalAccessException e) {
            throw new BadRequestException(
                BuildErrorMessage.buildErrorMessage(
                    ErrorType.VALIDATION_ERROR,
                    ErrorCode.FIELD_VALIDATION_MISSING,
                    Constants.ERROR_UPDATING_FIELD + key));
          }
        }
      } catch (IOException e) {
        throw new Exception(
            BuildErrorMessage.buildErrorMessage(
                ErrorType.VALIDATION_ERROR,
                ErrorCode.FIELD_VALIDATION_MISSING,
                Constants.ERROR_PARSING_JSON));
      }
    }

    if (file != null) {
      if (organization.getLogoFileId() == null) {
        try {
          FileRequest fileRequest = new FileRequest();
          fileRequest.setFile(file);
          fileRequest.setEntityType("orgLogo");
          fileRequest.setEntityId(UserContext.getLoggedInUserOrganization().getId());
          ResponseEntity<?> fileResponse = fileClient.uploadFile(fileRequest);
          LinkedHashMap<String, Object> responseBody =
              (LinkedHashMap<String, Object>) fileResponse.getBody();

          ObjectMapper objectMapper = new ObjectMapper();
          FileResponse savedFile = objectMapper.convertValue(responseBody, FileResponse.class);
          organization.setLogoFileId(savedFile.getId());
        } catch (Exception e) {
          throw new Exception(
              BuildErrorMessage.buildErrorMessage(
                  ErrorType.API_ERROR,
                  ErrorCode.RESOURCE_CREATING_ERROR,
                  Constants.ERROR_IN_UPDATING_ORG_LOGO));
        }
      } else {
        try {
          FileRequest fileRequest = new FileRequest();
          fileRequest.setFile(file);
          fileClient.updateFile(organization.getLogoFileId(), fileRequest);
        } catch (Exception e) {
          throw new Exception(
              BuildErrorMessage.buildErrorMessage(
                  ErrorType.API_ERROR,
                  ErrorCode.RESOURCE_CREATING_ERROR,
                  Constants.ERROR_IN_UPDATING_ORG_LOGO));
        }
      }
    }

    try {
      return organizationRepository.save(organization);
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.DB_ERROR,
              ErrorCode.CANNOT_SAVE_CHANGES,
              Constants.ERROR_IN_UPDATING_ORGANIZATION));
    }
  }

  @Override
  public ByteArrayResource downloadOrganizationFile() throws Exception {
    try {
      if (UserContext.getLoggedInUserOrganization().getLogoFileId() == null) {
        throw new ResourceNotFoundException(
            BuildErrorMessage.buildErrorMessage(
                ErrorType.RESOURCE_NOT_FOUND_ERROR,
                ErrorCode.FILE_NOT_FOUND,
                Constants.ORGANIZATION_LOGO_NOT_FOUND));
      }
      ResponseEntity<byte[]> fileResponse =
          fileClient.downloadFile(UserContext.getLoggedInUserOrganization().getLogoFileId());
      byte[] fileData = fileResponse.getBody();
      FileDownloadResultMetaData finalMetaData = getMetaData(fileResponse);

      return new ByteArrayResource(Objects.requireNonNull(fileData)) {
        @Override
        public String getFilename() {
          return finalMetaData.getFileName() != null ? finalMetaData.getFileName() : "beeja_file";
        }
      };
    } catch (Exception e) {
      throw new Exception(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.API_ERROR,
              ErrorCode.SERVER_ERROR,
              Constants.ERROR_IN_FETCHING_ORGANIZATION_LOGO));
    }
  }

  private static FileDownloadResultMetaData getMetaData(ResponseEntity<byte[]> fileResponse) {
    HttpHeaders headers = fileResponse.getHeaders();
    String contentDisposition = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
    String createdBy = headers.getFirst("createdby");
    String organizationId = headers.getFirst("organizationid");
    String entityId = headers.getFirst("entityId");
    String filename = null;

    if (contentDisposition != null && !contentDisposition.isEmpty()) {
      int startIndex = contentDisposition.indexOf("filename=\"") + 10;
      int endIndex = contentDisposition.lastIndexOf("\"");
      if (endIndex != -1) {
        filename = contentDisposition.substring(startIndex, endIndex);
      }
    }

    return new FileDownloadResultMetaData(filename, createdBy, entityId, organizationId);
  }

  public AddressResponse getAddressByPincode(String pincode) throws JsonProcessingException {
    if (!pincode.matches("^[1-9]\\d{5}$") || !pincode.matches("^[0-9]*$")) {
      throw new BadRequestException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.VALIDATION_ERROR, ErrorCode.FIELD_VALIDATION_MISSING, INVALID_PINCODE));
    }
    String response = postalClient.getPostalResponseByPincode(pincode);
    JsonNode rootNode = objectMapper.readTree(response);
    JsonNode firstNode = rootNode.get(0);
    String status = firstNode.path("Status").asText();
    if ("Success".equalsIgnoreCase(status)) {
      JsonNode postOffices = firstNode.path("PostOffice");
      JsonNode firstPostOffice = postOffices.get(0);
      AddressResponse addressResponse = new AddressResponse();
      addressResponse.setPincode(pincode);
      addressResponse.setState(firstPostOffice.path("State").asText());
      addressResponse.setCountry(firstPostOffice.path("Country").asText());
      addressResponse.setDistrict(firstPostOffice.path("District").asText());
      return addressResponse;
    } else {
      throw new ResourceNotFoundException(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.API_ERROR, ErrorCode.RESOURCE_NOT_FOUND_ERROR, PINCODE_NOT_EXISTS));
    }
  }
}
