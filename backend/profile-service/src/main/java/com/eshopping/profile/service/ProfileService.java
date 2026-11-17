package com.eshopping.profile.service;

import com.eshopping.profile.dto.UserProfileResponseDTO;
import com.eshopping.profile.dto.UserProfileUpdateRequestDTO;

public interface ProfileService {

    UserProfileResponseDTO getProfile(Long userId);

    UserProfileResponseDTO updateProfile(Long userId, UserProfileUpdateRequestDTO request);
}

