package com.eshopping.profile.service;

import com.eshopping.profile.dto.UserCountsDTO;
import com.eshopping.profile.dto.UserProfileResponseDTO;
import com.eshopping.profile.dto.UserProfileUpdateRequestDTO;

import java.util.List;

public interface ProfileService {

    UserProfileResponseDTO getProfile(Long userId);

    UserProfileResponseDTO updateProfile(Long userId, UserProfileUpdateRequestDTO request);

    void updateUserStatus(Long userId, boolean enabled);

    List<UserProfileResponseDTO> getAllMerchants();

    UserCountsDTO getUserCounts();
}

