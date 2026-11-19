package com.eshopping.profile.service;

import com.eshopping.profile.dto.UserCountsDTO;
import com.eshopping.profile.dto.UserProfileResponseDTO;
import com.eshopping.profile.dto.UserProfileUpdateRequestDTO;
import com.eshopping.profile.entity.User;
import com.eshopping.profile.exception.ResourceNotFoundException;
import com.eshopping.profile.repository.UserRepository;
import com.eshopping.profile.service.impl.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("alice@eshopping.com");
        testUser.setFullName("Alice Customer");
        testUser.setPhoneNumber("+91-9876543210");
        testUser.setCity("Bengaluru");
        testUser.setCountry("India");
        testUser.setRoles(Collections.singleton("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("Get Profile: Returns profile response DTO for existing user")
    void testGetProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserProfileResponseDTO dto = profileService.getProfile(1L);

        assertNotNull(dto);
        assertEquals("alice@eshopping.com", dto.getEmail());
        assertEquals("Alice Customer", dto.getFullName());
        assertEquals("Bengaluru", dto.getCity());
    }

    @Test
    @DisplayName("Get Profile: Throws ResourceNotFoundException when user does not exist")
    void testGetProfile_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> profileService.getProfile(999L));
    }

    @Test
    @DisplayName("Update Profile: Updates permitted fields and saves to database")
    void testUpdateProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileUpdateRequestDTO request = new UserProfileUpdateRequestDTO();
        request.setFullName("Alice R. Customer");
        request.setPhoneNumber("+91-9988776655");
        request.setCity("Mumbai");
        request.setState("Maharashtra");

        UserProfileResponseDTO updated = profileService.updateProfile(1L, request);

        assertNotNull(updated);
        assertEquals("Alice R. Customer", updated.getFullName());
        assertEquals("Mumbai", updated.getCity());
        assertEquals("+91-9988776655", updated.getPhoneNumber());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Update Profile: Throws ResourceNotFoundException when user does not exist")
    void testUpdateProfile_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        UserProfileUpdateRequestDTO request = new UserProfileUpdateRequestDTO();
        request.setFullName("Non Existent");
        assertThrows(ResourceNotFoundException.class, () -> profileService.updateProfile(999L, request));
    }

    @Test
    @DisplayName("Update User Status: Successfully toggles enabled state")
    void testUpdateUserStatus_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        profileService.updateUserStatus(1L, false);

        assertFalse(testUser.isEnabled());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Update User Status: Throws ResourceNotFoundException when user does not exist")
    void testUpdateUserStatus_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> profileService.updateUserStatus(999L, true));
    }

    @Test
    @DisplayName("Get All Merchants: Returns merchant profile list")
    void testGetAllMerchants() {
        User merchant = new User();
        merchant.setId(2L);
        merchant.setEmail("merchant@eshopping.com");
        merchant.setFullName("Merchant Bob");
        merchant.setRoles(Collections.singleton("ROLE_MERCHANT"));
        merchant.setEnabled(true);

        when(userRepository.findAllMerchants()).thenReturn(Collections.singletonList(merchant));

        List<UserProfileResponseDTO> result = profileService.getAllMerchants();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("merchant@eshopping.com", result.get(0).getEmail());
    }

    @Test
    @DisplayName("Get User Counts: Calculates platform user counts correctly")
    void testGetUserCounts() {
        User merchant1 = new User();
        merchant1.setId(2L);
        merchant1.setEnabled(true);

        User merchant2 = new User();
        merchant2.setId(3L);
        merchant2.setEnabled(false); // pending

        when(userRepository.countByRoleName("ROLE_CUSTOMER")).thenReturn(5L);
        when(userRepository.findAllMerchants()).thenReturn(Arrays.asList(merchant1, merchant2));

        UserCountsDTO counts = profileService.getUserCounts();

        assertNotNull(counts);
        assertEquals(7L, counts.getTotalUsers());
        assertEquals(5L, counts.getActiveCustomers());
        assertEquals(2L, counts.getRegisteredMerchants());
        assertEquals(1L, counts.getPendingMerchants());
    }
}

