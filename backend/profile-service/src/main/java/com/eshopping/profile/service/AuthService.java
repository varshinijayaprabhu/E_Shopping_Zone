package com.eshopping.profile.service;

import com.eshopping.profile.dto.*;
import com.eshopping.profile.entity.User;

import javax.servlet.http.HttpServletResponse;

public interface AuthService {

    AuthResponseDTO registerCustomer(CustomerRegisterRequestDTO request, String clientIp, String userAgent, HttpServletResponse response);

    AuthResponseDTO registerMerchant(MerchantRegisterRequestDTO request, String clientIp, String userAgent, HttpServletResponse response);

    AuthResponseDTO login(LoginRequestDTO request, String clientIp, String userAgent, HttpServletResponse response);

    RefreshTokenResponseDTO refreshToken(String rawRefreshToken, String clientIp, String userAgent, HttpServletResponse response);

    AuthResponseDTO exchangeOAuth2Code(OAuth2ExchangeRequestDTO request, HttpServletResponse response);

    void logout(String rawRefreshToken, HttpServletResponse response);

    void logoutAll(User user, HttpServletResponse response);
}

