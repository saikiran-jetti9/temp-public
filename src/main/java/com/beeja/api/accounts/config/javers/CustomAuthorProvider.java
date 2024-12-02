package com.beeja.api.accounts.config.javers;

import com.beeja.api.accounts.utils.UserContext;
import org.javers.spring.auditable.AuthorProvider;

public class CustomAuthorProvider implements AuthorProvider {
  @Override
  public String provide() {
    return UserContext.getLoggedInUserEmail();
  }
}
