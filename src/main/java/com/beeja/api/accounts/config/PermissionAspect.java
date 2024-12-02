package com.beeja.api.accounts.config;

import com.beeja.api.accounts.annotations.HasPermission;
import com.beeja.api.accounts.annotations.RequireAllPermissions;
import com.beeja.api.accounts.enums.ErrorCode;
import com.beeja.api.accounts.enums.ErrorType;
import com.beeja.api.accounts.exceptions.CustomAccessDenied;
import com.beeja.api.accounts.utils.BuildErrorMessage;
import com.beeja.api.accounts.utils.Constants;
import com.beeja.api.accounts.utils.UserContext;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

  @Before("@annotation(hasPermission)")
  public void checkPermission(HasPermission hasPermission) throws AccessDeniedException {
    String[] requiredPermissions = hasPermission.value();
    List<String> userPermissions = getUserPermissions();

    boolean hasRequiredPermission =
        Arrays.stream(requiredPermissions).anyMatch(userPermissions::contains);

    if (!hasRequiredPermission) {
      throw new CustomAccessDenied(
          BuildErrorMessage.buildErrorMessage(
              ErrorType.AUTHORIZATION_ERROR,
              ErrorCode.PERMISSION_MISSING,
              Constants.NO_REQUIRED_PERMISSIONS));
    }
  }

  @Before("@annotation(requireAllPermissions)")
  public void checkAllPermissions(RequireAllPermissions requireAllPermissions)
      throws AccessDeniedException {
    HasPermission[] requiredPermissions = requireAllPermissions.value();
    List<String> userPermissions = getUserPermissions();

    for (HasPermission permissionAnnotation : requiredPermissions) {
      String[] permissions = permissionAnnotation.value();

      boolean hasRequiredPermissions =
          Arrays.stream(permissions).allMatch(userPermissions::contains);

      if (!hasRequiredPermissions) {
        throw new CustomAccessDenied(
            BuildErrorMessage.buildErrorMessage(
                ErrorType.AUTHORIZATION_ERROR,
                ErrorCode.PERMISSION_MISSING,
                Constants.NO_REQUIRED_PERMISSIONS));
      }
    }
  }

  private List<String> getUserPermissions() {
    Set<String> loggedInUserPermissions = UserContext.getLoggedInUserPermissions();
    return new ArrayList<>(loggedInUserPermissions);
  }
}
