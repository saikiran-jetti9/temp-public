package com.beeja.api.accounts.repository;

import com.beeja.api.accounts.enums.SubscriptionName;
import com.beeja.api.accounts.model.subscriptions.Permissions;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PermissionRepository extends MongoRepository<Permissions, String> {
  Permissions findByName(SubscriptionName name);
}
