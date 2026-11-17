package com.eshopping.admin.client;

import com.eshopping.admin.dto.MerchantProfileResponseDTO;
import com.eshopping.admin.dto.UserCountsDTO;

import java.util.List;

public interface ProfileServiceClient {

    List<MerchantProfileResponseDTO> getAllMerchants();

    void updateMerchantStatus(Long merchantId, boolean enabled);

    UserCountsDTO getUserCounts();
}
