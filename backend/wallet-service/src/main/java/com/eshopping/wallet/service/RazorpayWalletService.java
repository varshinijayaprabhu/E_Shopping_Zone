package com.eshopping.wallet.service;

import com.eshopping.wallet.dto.RazorpayTopupCreateOrderRequestDTO;
import com.eshopping.wallet.dto.RazorpayTopupCreateOrderResponseDTO;
import com.eshopping.wallet.dto.RazorpayTopupVerifyRequestDTO;
import com.eshopping.wallet.dto.WalletTransactionDTO;

public interface RazorpayWalletService {

    RazorpayTopupCreateOrderResponseDTO createTopupOrder(Long customerId, RazorpayTopupCreateOrderRequestDTO request);

    WalletTransactionDTO verifyAndCreditWallet(Long customerId, RazorpayTopupVerifyRequestDTO request);
}
