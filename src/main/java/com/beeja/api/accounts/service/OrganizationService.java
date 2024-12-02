package com.beeja.api.accounts.service;

import com.beeja.api.accounts.model.Organization.Organization;
import com.beeja.api.accounts.model.User;
import com.beeja.api.accounts.response.AddressResponse;
import com.beeja.api.accounts.response.OrganizationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

public interface OrganizationService {
  Organization createOrganization(Organization organization) throws Exception;

  List<Organization> getAllOrganizations() throws Exception;

  List<User> getAllUsersByOrganizationId(String organizationId) throws Exception;

  Optional<Organization> deleteOrganizationById(String organizationId) throws Exception;

  OrganizationResponse getOrganizationById(String id) throws Exception;

  Organization updateOrganization(
      String organizationId, String organizationProfileUpdate, MultipartFile file) throws Exception;

  ByteArrayResource downloadOrganizationFile() throws Exception;

  AddressResponse getAddressByPincode(String pincode) throws JsonProcessingException;
}
