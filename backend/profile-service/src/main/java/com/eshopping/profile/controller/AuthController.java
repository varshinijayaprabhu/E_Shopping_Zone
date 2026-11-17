package com.eshopping.profile.controller;

import com.eshopping.profile.dto.*;
import com.eshopping.profile.exception.InvalidTokenException;
import com.eshopping.profile.security.CustomUserDetails;
import com.eshopping.profile.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Customer & Merchant onboarding, authentication, and session lifecycle")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Customer Registration", description = "Registers a new customer account with ROLE_CUSTOMER only. Public self-assignment of merchant privileges is disallowed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer account created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload"),
            @ApiResponse(responseCode = "409", description = "Email is already registered")
    })
    public ResponseEntity<AuthResponseDTO> registerCustomer(
            @Valid @RequestBody CustomerRegisterRequestDTO request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthResponseDTO response = authService.registerCustomer(
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"),
                httpResponse
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register-merchant")
    @Operation(summary = "Merchant Registration", description = "Registers a new merchant account with ROLE_MERCHANT via a controlled administrative invite code.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Merchant account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid merchant invite code or validation error"),
            @ApiResponse(responseCode = "409", description = "Email is already registered")
    })
    public ResponseEntity<AuthResponseDTO> registerMerchant(
            @Valid @RequestBody MerchantRegisterRequestDTO request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthResponseDTO response = authService.registerMerchant(
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"),
                httpResponse
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates credentials and returns a short-lived access JWT while setting an HttpOnly refresh cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthResponseDTO response = authService.login(
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"),
                httpResponse
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token", description = "Rotates refresh token and issues new access token. Requires valid HttpOnly cookie and X-Requested-With header.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Refresh token expired or replay attack detected"),
            @ApiResponse(responseCode = "403", description = "CSRF protection header missing")
    })
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(
            @CookieValue(name = "eshopping_refresh_token", required = false) String rawRefreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (rawRefreshToken == null || rawRefreshToken.trim().isEmpty()) {
            throw new InvalidTokenException("Missing refresh token cookie");
        }
        RefreshTokenResponseDTO response = authService.refreshToken(
                rawRefreshToken,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"),
                httpResponse
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/oauth2/exchange")
    @Operation(summary = "Exchange OAuth2 Code", description = "Exchanges a secure, one-time OAuth2 authorization code for an access token and user profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token exchanged successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired exchange code")
    })
    public ResponseEntity<AuthResponseDTO> exchangeOAuth2Code(
            @Valid @RequestBody OAuth2ExchangeRequestDTO request,
            HttpServletResponse httpResponse) {
        AuthResponseDTO response = authService.exchangeOAuth2Code(request, httpResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout Current Session", description = "Revokes the active refresh token and clears the HttpOnly cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out successfully")
    })
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(name = "eshopping_refresh_token", required = false) String rawRefreshToken,
            HttpServletResponse httpResponse) {
        authService.logout(rawRefreshToken, httpResponse);
        return ResponseEntity.ok(Collections.singletonMap("message", "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout All Sessions", description = "Revokes all refresh tokens belonging to the authenticated user across all devices.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All active sessions revoked successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, String>> logoutAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse httpResponse) {
        if (userDetails != null) {
            com.eshopping.profile.entity.User user = new com.eshopping.profile.entity.User();
            user.setId(userDetails.getId());
            user.setEmail(userDetails.getEmail());
            authService.logoutAll(user, httpResponse);
        }
        return ResponseEntity.ok(Collections.singletonMap("message", "All user sessions invalidated successfully"));
    }
}

