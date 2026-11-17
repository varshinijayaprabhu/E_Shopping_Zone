package com.eshopping.order.client;

import com.eshopping.order.dto.client.WalletPayClientRequestDTO;
import com.eshopping.order.dto.client.WalletPaymentStatusClientResponseDTO;
import com.eshopping.order.dto.client.WalletRefundClientRequestDTO;
import com.eshopping.order.dto.client.WalletTransactionClientResponseDTO;

public interface WalletServiceClient {

    WalletTransactionClientResponseDTO pay(WalletPayClientRequestDTO request);

    WalletTransactionClientResponseDTO refund(WalletRefundClientRequestDTO request);

    WalletPaymentStatusClientResponseDTO getPaymentStatusByOrder(String orderReference);
}

