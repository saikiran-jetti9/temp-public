package com.beeja.api.accounts.response;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileResponse {
  private String id;
  private String name;
  private String fileType;
  private String fileFormat;
  private String fileSize;
  private String entityId;
  private String entityType;

  private String description;

  private String organizationId;

  private String createdBy;
  private String createdByName;
  private String modifiedBy;

  @Field("created_at")
  @CreatedDate
  private Date createdAt;

  @Field("modified_at")
  @LastModifiedDate
  private Date modifiedAt;
}
