package com.beeja.api.accounts.config.javers;

import org.javers.spring.auditable.AuthorProvider;
import org.javers.spring.boot.mongo.JaversMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration(exclude = JaversMongoAutoConfiguration.class)
public class CustomJaversConfiguration extends JaversMongoAutoConfiguration {

  @Bean(name = "customJaversAuthorProvider")
  public AuthorProvider customJaversAuthorProvider() {
    return new CustomAuthorProvider();
  }
}
