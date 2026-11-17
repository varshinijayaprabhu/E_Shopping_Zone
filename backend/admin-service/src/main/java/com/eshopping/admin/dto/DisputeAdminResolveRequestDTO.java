package com.eshopping.admin.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class DisputeAdminResolveRequestDTO {

    @NotBlank(message = "Action must be REFUND or NO_REFUND")
    @Pattern(regexp = "^(?i)(REFUND|NO_REFUND)$", message = "Action must be either REFUND or NO_REFUND")
    private String action;

    private String adminNotes;

    public DisputeAdminResolveRequestDTO() {
    }

    public DisputeAdminResolveRequestDTO(String action, String adminNotes) {
        this.action = action;
        this.adminNotes = adminNotes;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
}
