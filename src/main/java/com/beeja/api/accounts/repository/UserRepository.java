package com.beeja.api.accounts.repository;

import com.beeja.api.accounts.model.Organization.Organization;
import com.beeja.api.accounts.model.Organization.Role;
import com.beeja.api.accounts.model.User;
import java.util.List;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
@JaversSpringDataAuditable
public interface UserRepository extends MongoRepository<User, String> {
  User findByEmail(String email);

  User findByEmailAndOrganizations(String email, Organization organizations);

  User findByEmployeeIdAndOrganizations(String employeeId, Organization organizations);

  List<User> findByOrganizationsAndIsActive(Organization organizations, boolean isActive);

  List<User> findByOrganizationsId(String id);

  List<User> findByRoles(Role role);

  Long countByOrganizations(Organization organizations);

  Long countByOrganizationsAndIsActive(Organization organizations, boolean isActive);
}
