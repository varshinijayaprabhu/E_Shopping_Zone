package com.eshopping.profile.service.impl;

import com.eshopping.profile.dto.UserCountsDTO;
import com.eshopping.profile.dto.UserProfileResponseDTO;
import com.eshopping.profile.dto.UserProfileUpdateRequestDTO;
import com.eshopping.profile.entity.User;
import com.eshopping.profile.exception.ResourceNotFoundException;
import com.eshopping.profile.repository.UserRepository;
import com.eshopping.profile.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private final UserRepository userRepository;

    public ProfileServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDTO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mapToDto(user);
    }

    @Override
    public UserProfileResponseDTO updateProfile(Long userId, UserProfileUpdateRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddressLine1(request.getAddressLine1());
        user.setAddressLine2(request.getAddressLine2());
        user.setCity(request.getCity());
        user.setState(request.getState());
        user.setPostalCode(request.getPostalCode());
        user.setCountry(request.getCountry() != null ? request.getCountry() : "India");
        user.setProfilePictureUrl(request.getProfilePictureUrl());

        User updatedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", updatedUser.getEmail());

        return mapToDto(updatedUser);
    }

    private UserProfileResponseDTO mapToDto(User user) {
        UserProfileResponseDTO dto = new UserProfileResponseDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAddressLine1(user.getAddressLine1());
        dto.setAddressLine2(user.getAddressLine2());
        dto.setCity(user.getCity());
        dto.setState(user.getState());
        dto.setPostalCode(user.getPostalCode());
        dto.setCountry(user.getCountry());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());
        dto.setAuthProvider(user.getAuthProvider());
        dto.setRoles(user.getRoles());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    @Override
    public void updateUserStatus(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setEnabled(enabled);
        userRepository.save(user);
        log.info("Updated status for user {}: enabled={}", user.getEmail(), enabled);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponseDTO> getAllMerchants() {
        return userRepository.findAllMerchants().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserCountsDTO getUserCounts() {
        long customers = userRepository.countByRoleName("ROLE_CUSTOMER");
        long merchants = userRepository.countByRoleName("ROLE_MERCHANT");
        return new UserCountsDTO(customers, merchants);
    }
}

