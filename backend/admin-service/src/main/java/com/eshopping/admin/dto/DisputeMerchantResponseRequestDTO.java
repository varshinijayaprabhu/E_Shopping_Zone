package com.eshopping.admin.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class DisputeMerchantResponseRequestDTO {

    @NotBlank(message = "Action must be ACCEPT or REJECT")
    @Pattern(regexp = "^(?i)(ACCEPT|REJECT)$", message = "Action must be either ACCEPT or REJECT")
    private String action;

    private String responseComment;

    public DisputeMerchantResponseRequestDTO() {
    }

    public DisputeMerchantResponseRequestDTO(String action, String responseComment) {
        this.action = action;
        this.responseComment = responseComment;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResponseComment() {
        return responseComment;
    }

    public void setResponseComment(String responseComment) {
        this.responseComment = responseComment;
    }
}
