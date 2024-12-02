package com.beeja.api.accounts.repository;

import com.beeja.api.accounts.model.featureFlags.FeatureToggle;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
@JaversSpringDataAuditable
public interface FeatureToggleRepository extends MongoRepository<FeatureToggle, String> {
  FeatureToggle findByOrganizationId(String organizationId);
}
