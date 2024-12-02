package com.beeja.api.accounts.repository;

import com.beeja.api.accounts.model.Organization.Role;
import java.util.List;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.mongodb.repository.MongoRepository;

@JaversSpringDataAuditable
public interface RolesRepository extends MongoRepository<Role, String> {
  Role findByName(String name);

  Role findByNameAndOrganizationId(String name, String organizationId);

  Role findByIdAndOrganizationId(String id, String organizationId);

  List<Role> findByOrganizationId(String organizationId);
}
