package com.eshopping.admin.dto;

import java.time.LocalDateTime;

public class AdminAuditLogDTO {

    private Long id;
    private Long adminId;
    private String adminEmail;
    private String actionType;
    private String targetEntityType;
    private Long targetEntityId;
    private String reason;
    private String metadata;
    private String ipAddress;
    private LocalDateTime createdAt;

    public AdminAuditLogDTO() {
    }

    public AdminAuditLogDTO(Long id, Long adminId, String actionType, String targetEntityType,
                           Long targetEntityId, String reason, String metadata,
                           String ipAddress, LocalDateTime createdAt) {
        this(id, adminId, "admin@eshopping.com", actionType, targetEntityType, targetEntityId, reason, metadata, ipAddress, createdAt);
    }

    public AdminAuditLogDTO(Long id, Long adminId, String adminEmail, String actionType, String targetEntityType,
                           Long targetEntityId, String reason, String metadata,
                           String ipAddress, LocalDateTime createdAt) {
        this.id = id;
        this.adminId = adminId;
        this.adminEmail = adminEmail != null ? adminEmail : "admin@eshopping.com";
        this.actionType = actionType;
        this.targetEntityType = targetEntityType;
        this.targetEntityId = targetEntityId;
        this.reason = reason;
        this.metadata = metadata;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTargetEntityType() {
        return targetEntityType;
    }

    public void setTargetEntityType(String targetEntityType) {
        this.targetEntityType = targetEntityType;
    }

    public Long getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(Long targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
