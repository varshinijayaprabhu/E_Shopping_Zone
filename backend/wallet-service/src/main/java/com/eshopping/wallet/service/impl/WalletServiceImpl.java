package com.eshopping.wallet.service.impl;

import com.eshopping.wallet.dto.DepositRequestDTO;
import com.eshopping.wallet.dto.PaymentRequestDTO;
import com.eshopping.wallet.dto.PaymentStatusResponseDTO;
import com.eshopping.wallet.dto.RefundRequestDTO;
import com.eshopping.wallet.dto.WalletDTO;
import com.eshopping.wallet.dto.WalletTransactionDTO;
import com.eshopping.wallet.entity.TransactionStatus;
import com.eshopping.wallet.entity.TransactionType;
import com.eshopping.wallet.entity.Wallet;
import com.eshopping.wallet.entity.WalletStatus;
import com.eshopping.wallet.entity.WalletTransaction;
import com.eshopping.wallet.exception.BadRequestException;
import com.eshopping.wallet.exception.InsufficientBalanceException;
import com.eshopping.wallet.exception.ResourceNotFoundException;
import com.eshopping.wallet.repository.WalletRepository;
import com.eshopping.wallet.repository.WalletTransactionRepository;
import com.eshopping.wallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletServiceImpl implements WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletServiceImpl(WalletRepository walletRepository,
                             WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public WalletDTO getWallet(Long customerId) {
        if (customerId == null) {
            throw new BadRequestException("Customer ID cannot be null");
        }

        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    log.info("Auto-provisioning active digital wallet for customer ID: {}", customerId);
                    Wallet newWallet = new Wallet(customerId);
                    return walletRepository.save(newWallet);
                });

        return toWalletDTO(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionDTO> getTransactions(Long customerId, Pageable pageable) {
        if (customerId == null) {
            throw new BadRequestException("Customer ID cannot be null");
        }

        Wallet wallet = walletRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for customer ID: " + customerId));

        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable)
                .map(this::toTransactionDTO);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletTransactionDTO deposit(Long customerId, DepositRequestDTO request) {
        if (customerId == null) {
            throw new BadRequestException("Customer ID cannot be null");
        }
        if (request == null || request.getAmount() == null) {
            throw new BadRequestException("Deposit request and amount cannot be null");
        }

        BigDecimal amount = request.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Deposit amount must be strictly greater than zero");
        }

        // Idempotency check: return existing transaction if already processed
        Optional<WalletTransaction> existingTxn = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingTxn.isPresent()) {
            WalletTransaction existing = existingTxn.get();
            if (existing.getAmount().compareTo(amount) != 0) {
                throw new BadRequestException("Idempotency conflict: A previous deposit exists with this idempotency key but with a different amount");
            }
            log.info("Idempotent deposit request detected with key: {}. Returning existing transaction: {}",
                    request.getIdempotencyKey(), existing.getTransactionReference());
            return toTransactionDTO(existing);
        }

        // Pessimistic write lock for atomic balance modification
        Wallet wallet = walletRepository.findByCustomerIdForUpdate(customerId)
                .orElseGet(() -> {
                    log.info("Creating initial digital wallet for customer ID: {}", customerId);
                    Wallet newWallet = new Wallet(customerId);
                    return walletRepository.save(newWallet);
                });

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet status is " + wallet.getStatus() + "; deposits are not permitted");
        }

        BigDecimal balanceBefore = wallet.getCurrentBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
        wallet.setCurrentBalance(balanceAfter);
        walletRepository.save(wallet);

        String transactionRef = "TXN-DEP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        String description = request.getDescription() != null && !request.getDescription().trim().isEmpty()
                ? request.getDescription().trim()
                : "Wallet deposit via " + (request.getFundingSource() != null ? request.getFundingSource() : "SIMULATED_GATEWAY");

        WalletTransaction transaction = new WalletTransaction(
                wallet,
                transactionRef,
                request.getIdempotencyKey(),
                null,
                TransactionType.DEPOSIT,
                amount,
                balanceBefore,
                balanceAfter,
                TransactionStatus.SUCCESS,
                description
        );

        WalletTransaction savedTxn = transactionRepository.save(transaction);
        log.info("Deposit successful: Customer={}, Amount=₹{}, Balance: ₹{} -> ₹{}, TxnRef={}",
                customerId, amount, balanceBefore, balanceAfter, transactionRef);

        return toTransactionDTO(savedTxn);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletTransactionDTO pay(PaymentRequestDTO request) {
        if (request == null || request.getCustomerId() == null || request.getAmount() == null) {
            throw new BadRequestException("Payment request, customer ID, and amount cannot be null");
        }

        BigDecimal amount = request.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be strictly greater than zero");
        }

        // Idempotency check: return existing transaction if already processed
        Optional<WalletTransaction> existingTxn = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingTxn.isPresent()) {
            WalletTransaction existing = existingTxn.get();
            if (existing.getAmount().compareTo(amount) != 0 ||
                    (request.getOrderReference() != null && !request.getOrderReference().equals(existing.getOrderReference()))) {
                throw new BadRequestException("Idempotency conflict: A previous payment exists with this idempotency key but with different order reference or amount");
            }
            log.info("Idempotent payment request detected with key: {}. Returning existing transaction: {}",
                    request.getIdempotencyKey(), existing.getTransactionReference());
            return toTransactionDTO(existing);
        }

        // Pessimistic write lock for atomic balance deduction
        Wallet wallet = walletRepository.findByCustomerIdForUpdate(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for customer ID: " + request.getCustomerId()));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet status is " + wallet.getStatus() + "; payments are not permitted");
        }

        BigDecimal balanceBefore = wallet.getCurrentBalance();
        if (balanceBefore.compareTo(amount) < 0) {
            log.warn("Payment rejected: Insufficient funds for Customer={}. Required=₹{}, Available=₹{}",
                    request.getCustomerId(), amount, balanceBefore);
            throw new InsufficientBalanceException(
                    String.format("Insufficient wallet balance. Available: ₹%.2f, Required: ₹%.2f", balanceBefore, amount));
        }

        BigDecimal balanceAfter = balanceBefore.subtract(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientBalanceException("Resulting wallet balance cannot be negative");
        }

        wallet.setCurrentBalance(balanceAfter);
        walletRepository.save(wallet);

        String transactionRef = "TXN-PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        String description = request.getDescription() != null && !request.getDescription().trim().isEmpty()
                ? request.getDescription().trim()
                : "Payment for order " + request.getOrderReference();

        WalletTransaction transaction = new WalletTransaction(
                wallet,
                transactionRef,
                request.getIdempotencyKey(),
                request.getOrderReference(),
                TransactionType.PAYMENT,
                amount,
                balanceBefore,
                balanceAfter,
                TransactionStatus.SUCCESS,
                description
        );

        WalletTransaction savedTxn = transactionRepository.save(transaction);
        log.info("Payment deduction successful: Customer={}, Order={}, Amount=₹{}, Balance: ₹{} -> ₹{}, TxnRef={}",
                request.getCustomerId(), request.getOrderReference(), amount, balanceBefore, balanceAfter, transactionRef);

        return toTransactionDTO(savedTxn);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WalletTransactionDTO refund(RefundRequestDTO request) {
        if (request == null || request.getCustomerId() == null || request.getAmount() == null) {
            throw new BadRequestException("Refund request, customer ID, and amount cannot be null");
        }

        BigDecimal amount = request.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Refund amount must be strictly greater than zero");
        }

        // Idempotency check: return existing transaction if already processed
        Optional<WalletTransaction> existingTxn = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingTxn.isPresent()) {
            WalletTransaction existing = existingTxn.get();
            if (existing.getAmount().compareTo(amount) != 0 ||
                    (request.getOrderReference() != null && !request.getOrderReference().equals(existing.getOrderReference()))) {
                throw new BadRequestException("Idempotency conflict: A previous refund exists with this idempotency key but with different order reference or amount");
            }
            log.info("Idempotent refund request detected with key: {}. Returning existing transaction: {}",
                    request.getIdempotencyKey(), existing.getTransactionReference());
            return toTransactionDTO(existing);
        }

        // Refund validation: refund cannot exceed original payment amount
        if (request.getOrderReference() != null && !request.getOrderReference().trim().isEmpty()) {
            Optional<WalletTransaction> originalPayment = transactionRepository
                    .findByOrderReferenceAndTransactionType(request.getOrderReference().trim(), TransactionType.PAYMENT);
            if (originalPayment.isPresent() && amount.compareTo(originalPayment.get().getAmount()) > 0) {
                throw new BadRequestException(String.format(
                        "Refund rejected: Refund amount (₹%.2f) exceeds original payment amount (₹%.2f) for order %s",
                        amount, originalPayment.get().getAmount(), request.getOrderReference()));
            }
        }

        // Pessimistic write lock for atomic refund crediting
        Wallet wallet = walletRepository.findByCustomerIdForUpdate(request.getCustomerId())
                .orElseGet(() -> {
                    log.info("Creating wallet for refund credit to customer ID: {}", request.getCustomerId());
                    Wallet newWallet = new Wallet(request.getCustomerId());
                    return walletRepository.save(newWallet);
                });

        BigDecimal balanceBefore = wallet.getCurrentBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
        wallet.setCurrentBalance(balanceAfter);
        walletRepository.save(wallet);

        String transactionRef = "TXN-REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        String description = "Refund for order " + request.getOrderReference()
                + (request.getReason() != null ? " (" + request.getReason() + ")" : "");

        WalletTransaction transaction = new WalletTransaction(
                wallet,
                transactionRef,
                request.getIdempotencyKey(),
                request.getOrderReference(),
                TransactionType.REFUND,
                amount,
                balanceBefore,
                balanceAfter,
                TransactionStatus.SUCCESS,
                description
        );

        WalletTransaction savedTxn = transactionRepository.save(transaction);
        log.info("Refund compensation successful: Customer={}, Order={}, Amount=₹{}, Balance: ₹{} -> ₹{}, TxnRef={}",
                request.getCustomerId(), request.getOrderReference(), amount, balanceBefore, balanceAfter, transactionRef);

        return toTransactionDTO(savedTxn);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponseDTO getPaymentStatusByOrderReference(String orderReference) {
        if (orderReference == null || orderReference.trim().isEmpty()) {
            throw new BadRequestException("Order reference cannot be empty");
        }

        Optional<WalletTransaction> paymentTxn = transactionRepository
                .findByOrderReferenceAndTransactionType(orderReference.trim(), TransactionType.PAYMENT);

        if (paymentTxn.isPresent()) {
            WalletTransaction txn = paymentTxn.get();
            return new PaymentStatusResponseDTO(
                    true,
                    txn.getTransactionReference(),
                    txn.getOrderReference(),
                    txn.getWallet().getCustomerId(),
                    txn.getAmount(),
                    txn.getStatus().name(),
                    txn.getTransactionType().name(),
                    txn.getCreatedAt()
            );
        }

        return PaymentStatusResponseDTO.notFound(orderReference.trim());
    }

    private WalletDTO toWalletDTO(Wallet wallet) {
        return new WalletDTO(
                wallet.getId(),
                wallet.getCustomerId(),
                wallet.getCurrentBalance(),
                wallet.getCurrency(),
                wallet.getStatus().name(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }

    private WalletTransactionDTO toTransactionDTO(WalletTransaction txn) {
        return new WalletTransactionDTO(
                txn.getId(),
                txn.getTransactionReference(),
                txn.getOrderReference(),
                txn.getTransactionType().name(),
                txn.getAmount(),
                txn.getBalanceBefore(),
                txn.getBalanceAfter(),
                txn.getStatus().name(),
                txn.getDescription(),
                txn.getCreatedAt()
        );
    }
}

