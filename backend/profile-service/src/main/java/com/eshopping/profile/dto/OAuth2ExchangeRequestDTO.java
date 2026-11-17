package com.eshopping.profile.dto;

import javax.validation.constraints.NotBlank;

public class OAuth2ExchangeRequestDTO {

    @NotBlank(message = "Authorization exchange code is required")
    private String code;

    public OAuth2ExchangeRequestDTO() {
    }

    public OAuth2ExchangeRequestDTO(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

