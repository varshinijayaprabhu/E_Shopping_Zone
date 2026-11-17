package com.eshopping.profile.controller;

import com.eshopping.profile.dto.UserProfileResponseDTO;
import com.eshopping.profile.dto.UserProfileUpdateRequestDTO;
import com.eshopping.profile.security.CustomUserDetails;
import com.eshopping.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "Customer and Merchant Profile Management")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Get Current User Profile", description = "Retrieves full profile details for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT Bearer token required")
    })
    public ResponseEntity<UserProfileResponseDTO> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserProfileResponseDTO profile = profileService.getProfile(userDetails.getId());
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    @Operation(summary = "Update User Profile", description = "Updates personal details and shipping address for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserProfileResponseDTO> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserProfileUpdateRequestDTO request) {
        UserProfileResponseDTO updatedProfile = profileService.updateProfile(userDetails.getId(), request);
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping
    @Operation(summary = "Save / Update User Profile (POST)", description = "Updates or saves personal details and shipping address for the authenticated user (POST alias for PUT).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile saved/updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error in request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserProfileResponseDTO> saveProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserProfileUpdateRequestDTO request) {
        return updateProfile(userDetails, request);
    }
}

