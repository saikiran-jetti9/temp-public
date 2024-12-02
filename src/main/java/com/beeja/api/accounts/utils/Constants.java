package com.beeja.api.accounts.utils;

public class Constants {
  public static final String BEEJA = "BEEJA";
  public static final String USE_BUSINESS_EMAIL = "Please use a business email";
  public static final String ORGANIZATION_ALREADY_EXIST =
      "Organization already registered at Beeja";
  public static final String REGISTER_BEFORE_LOGIN = "Please Register with Beeja to authenticate";
  public static final String LOGIN_SUCCESSFULL = "Login Successfull";
  public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
  public static final String TOKEN_VERIFICATION_FAILED = "Token Validation Failed";
  public static final String HTTP_ERROR = "Http Error";
  public static final String NOT_AUTHORISED = "Not Authorised - ACC";
  public static final String CANT_INACTIVE_SELF = "/ Can't Inactive Yourself";
  public static final String CANT_UPDATE_ROLES_SELF = "/ Can't Update Role Yourself";
  public static final String ACCESS_DENIED = "Access Denied - ACC";
  public static final String COOKIE_ACCESS_TOKEN = "authorization";
  public static final String UNAUTHORISED_ACCESS = "Unauthorized access";
  public static final String ERROR_IN_CHECKING_PERMISSION = "Error in Checking Permission";
  public static final String ERROR_IN_ASSIGNING_ROLE = "Error in Assigning Role - Employee";
  public static final String CANT_DELETE_SELF_ORGANIZATION = "Cannot delete your own organization";
  public static final String ERROR_CREATING_EMPLOYEE_FOR_ORG =
      "Error in Creating Employee for Organization";
  public static final String ERROR_IN_CREATE_ORGANIZATION = "Error in Creating Organization";
  public static final String ERROR_IN_UPDATING_ORGANIZATION = "Error in Updating Organization";
  public static final String ERROR_IN_FETCHING_ORGANIZATION_LOGO =
      "Error in Fetching Organization Logo";
  public static final String ERROR_IN_CREATING_USER = "Error in Creating User";

  //    Errors
  public static final String USER_NOT_FOUND = "User Not Found ";
  public static final String USER_ALREADY_FOUND = "User Already Found ";
  public static final String CANNOT_CREATE_ORGANIZATION_USER =
      "Cannot Create Organization User/Admin as it is already registered, please use different email";
  public static final String EMPLOYEE_WITH_ID_ALREADY_FOUND =
      "Employee With Given Id Is Already Found ";
  public static final String ERROR_RETRIEVING_USER = "Error In Retrieving User ";
  public static final String ERROR_RETRIEVING_USER_FOR_ORGANIZATION =
      "Error In Retrieving User, in order to create new user ";
  public static final String USER_STATUS_UPDATED = "User Status Successfully Updated, ";
  public static final String USER_UPDATE_ERROR = "Error Encountered in Updating User, ";
  public static final String USER_CREATE_ERROR = "Error Encountered in Creating User, ";
  public static final String EMPLOYEE_FEIGN_CLIENT_ERROR = "Error in Employee EmployeeFeignClient";
  public static final String ERROR_IN_FETCHING_EMPLOYEE_COUNT = "Error in Fetching Employee Count";
  public static final String ERROR_IN_UPDATING_ORG_LOGO = "Error in Updating Organization Logo";

  public static final String NO_REQUIRED_PERMISSIONS =
      "You have no required permissions to do this operation";

  //    ORGANIZATIONS
  public static final String ERROR_IN_CREATING_ORGANIZATION_ORG_EXISTS =
      "Organization with the same email domain already exists";
  public static final String ERROR_IN_INSTANTIATING_FEATURE_TOGGLES =
      "Organization Not Created, Feature Toggle Error";
  public static final String ERROR_IN_FETCHING_ORGANIZATIONS =
      "Error Encountered in Fetching Organizations";
  public static final String ERROR_IN_FETCHING_EMPLOYEES_OF_ORG =
      "Error Encountered in fetching employees of organization";

  public static final String ERROR_NO_ORGANIZATION_FOUND_WITH_PROVIDED_ID =
      "No Organization Found with provided Id";

  //    VALIDATION MESSAGES
  //    ORGANIZATION
  public static final String VALIDATION_ORGANIZATION_NAME_IS_REQUIRED =
      "Organization Name is Required";
  public static final String VALIDATION_ORGANIZATION_EMAIL_IS_REQUIRED =
      "Organization Email is Required";
  public static final String VALIDATION_ORGANIZATION_EMAIL_IS_INVALID_FORMAT =
      "Organization Email is not in proper format";
  public static final String ORGANIZATION_DALETE_FEIGN_ERROR =
      "Organization Delete Error Occured From Feign Client - Emp.";

  public static final String VALIDATION_ORGANIZATION_OWNER_CONTACT_EMAIL_IS_REQUIRED =
      "Organization Owner Contact Mail is Required";
  public static final String VALIDATION_ORGANIZATION_OWNER_CONTACT_EMAIL_IS_INVALID_FORMAT =
      "Organization Owner Contact Mail is Invalid Format";
  public static final String VALIDATION_ORGANIZATION_WEBSITE_REQUIRED =
      "Organization Website is Required";
  public static final String VALIDATION_ORGANIZATION_WEBSITE_INVALID_FORMAT =
      "Organization Website is not in proper format";

  public static final String ERROR_IN_ADDING_ROLE_TO_ORGANIZATION =
      "Error Occurred in Adding Role to Organization, ";
  public static final String ERROR_IN_UPDATING_ROLE_TO_ORGANIZATION =
      "Error Occurred in Updating Role to Organization, ";
  public static final String ERROR_IN_DELETING_ROLE_TO_ORGANIZATION =
      "Error Occurred in Deleting Role to Organization, ";
  public static final String ROLE_NOT_FOUND = "Role Not Found ";
  public static final String ROLE_ALREADY_FOUND = "Role Already Found ";
  public static final String ERROR_IN_DELETING_ROLE_AS_IT_IN_USE =
      "Error Occurred in Deleting Role because it is in use and assigned users is/are: ";

  public static final String FIELD_NOT_EXIST_ORGANIZATION_ENTITY =
      "Field does not exist in Organization class: ";
  public static final String ERROR_UPDATING_FIELD = "Error Updating Field: ";
  public static final String ERROR_PARSING_JSON = "Error Parsing JSON: ";
  public static final String ORGANIZATION_LOGO_NOT_FOUND = "Organization Logo Not Found";
  public static final String RESOURCE_NOT_FOUND = "Resource Not Found";
  public static final String ERROR_IN_FETCHING_ROLES = "Error in Fetching Roles";
  public static final String CANT_DELETE_DEFAULT_ROLE = "Cannot delete default role";
  public static final String CANT_UPDATE_DEFAULT_ROLE = "Cannot Update default role ";
  public static final String UNABLE_TO_FETCH_DETAILS_FROM_DATABASE =
      "Unable to fetch details from Database ";
  public static final String RESOURCE_UPDATING_ERROR_FEATURE_TOGGLE =
      "Resource Updating Error - Features, it will happen very rarely";
  public static final String ERROR_IN_CREATING_ROLE_TO_ORGANIZATION =
      "Error in Creating Role to Organization";

  public static final String INVALID_PINCODE =
      "Invalid pincode format: Must be 6 digits starting with a digit from 1 to 9 & without alphabetic & special characters.";
  public static final String PINCODE_NOT_EXISTS = "No Addresses Found with given Pincode";
  public static final String ERROR_FETCHING_ADDRESSES =
      "Error occurred while fetching address data: ";
  //  DOC URLS
  public static final String DOC_URL_RESOURCE_NOT_FOUND = "https://dev.beeja.techatcore.com/";
}
