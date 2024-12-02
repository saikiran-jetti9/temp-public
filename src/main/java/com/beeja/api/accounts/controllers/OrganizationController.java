package com.beeja.api.accounts.controllers;

import com.beeja.api.accounts.annotations.HasPermission;
import com.beeja.api.accounts.constants.PermissionConstants;
import com.beeja.api.accounts.exceptions.BadRequestException;
import com.beeja.api.accounts.model.Organization.Organization;
import com.beeja.api.accounts.response.AddressResponse;
import com.beeja.api.accounts.response.OrganizationResponse;
import com.beeja.api.accounts.service.OrganizationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/organizations")
public class OrganizationController {

  @Autowired OrganizationService organizationService;

  @GetMapping
  @HasPermission(PermissionConstants.READ_ORGANIZATIONS)
  public ResponseEntity<List<Organization>> getAllOrganizations() throws Exception {
    return ResponseEntity.ok(organizationService.getAllOrganizations());
  }

  @GetMapping("/{organizationId}/employees")
  @HasPermission((PermissionConstants.READ_ORGANIZATIONS))
  public ResponseEntity<?> getAllEmployeesByOrganizationId(@PathVariable String organizationId)
      throws Exception {
    return ResponseEntity.ok(organizationService.getAllUsersByOrganizationId(organizationId));
  }

  @PostMapping
  @HasPermission(PermissionConstants.CREATE_ORGANIZATIONS)
  public ResponseEntity<Organization> createOrganization(
      @RequestBody @Valid Organization organization, BindingResult bindingResult) throws Exception {
    if (bindingResult.hasErrors()) {
      List<String> errorMessages =
          bindingResult.getAllErrors().stream()
              .map(ObjectError::getDefaultMessage)
              .collect(Collectors.toList());
      throw new BadRequestException(errorMessages.toString());
    }
    return new ResponseEntity<>(
        organizationService.createOrganization(organization), HttpStatus.CREATED);
  }

  @GetMapping("/{organizationId}")
  @HasPermission(PermissionConstants.READ_ORGANIZATIONS)
  public ResponseEntity<OrganizationResponse> getOrganizationById(
      @PathVariable String organizationId) throws Exception {
    return ResponseEntity.status(HttpStatus.OK)
        .body(organizationService.getOrganizationById(organizationId));
  }

  @DeleteMapping("/{organizationId}")
  @HasPermission(PermissionConstants.DELETE_ORGANIZATIONS)
  public ResponseEntity<Organization> deleteOrganizatiobById(@PathVariable String organizationId)
      throws Exception {
    return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .body(organizationService.deleteOrganizationById(organizationId).get());
  }

  @PatchMapping("/{organizationId}")
  @HasPermission(PermissionConstants.UPDATE_ORGANIZATIONS)
  public ResponseEntity<?> updateOrganization(
      @PathVariable String organizationId,
      @RequestParam(name = "organizationFields", required = false) String fields,
      @RequestParam(name = "logo", required = false) MultipartFile file)
      throws Exception {

    Organization updatedOrganization =
        organizationService.updateOrganization(organizationId, fields, file);
    return ResponseEntity.ok(updatedOrganization);
  }

  @GetMapping("/logo")
  @HasPermission({
    PermissionConstants.READ_EMPLOYEE,
  })
  public ResponseEntity<?> downloadFile() throws Exception {
    ByteArrayResource resource = organizationService.downloadOrganizationFile();
    HttpHeaders headers = new HttpHeaders();
    headers.add(
        HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"");
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .headers(headers)
        .body(resource);
  }

  @GetMapping("/address/{pincode}")
  public AddressResponse getAddressByPincode(@PathVariable String pincode)
      throws JsonProcessingException {
    return organizationService.getAddressByPincode(pincode);
  }
}
