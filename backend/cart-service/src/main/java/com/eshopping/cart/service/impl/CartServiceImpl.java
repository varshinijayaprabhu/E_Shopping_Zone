package com.eshopping.cart.service.impl;

import com.eshopping.cart.client.ProductServiceClient;
import com.eshopping.cart.dto.*;
import com.eshopping.cart.entity.Cart;
import com.eshopping.cart.entity.CartItem;
import com.eshopping.cart.exception.InsufficientStockException;
import com.eshopping.cart.exception.ResourceNotFoundException;
import com.eshopping.cart.exception.UnauthorizedCartAccessException;
import com.eshopping.cart.repository.CartItemRepository;
import com.eshopping.cart.repository.CartRepository;
import com.eshopping.cart.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           ProductServiceClient productServiceClient) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productServiceClient = productServiceClient;
    }

    @Override
    public CartResponseDTO getCart(Long customerId) {
        Cart cart = getOrCreateCart(customerId);
        return buildCartResponse(cart);
    }

    @Override
    public CartResponseDTO addItemToCart(Long customerId, AddToCartRequestDTO request) {
        Cart cart = getOrCreateCart(customerId);

        // Fetch live product details from Product Service (Zero Trust on frontend prices/stock)
        ProductClientDTO.ProductData product = productServiceClient.getProductDetails(request.getProductId());

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            if (newQuantity > product.getAvailableStock()) {
                throw new InsufficientStockException("Cannot add " + request.getQuantity() + " more units of '" +
                        product.getName() + "'. Product only has " + product.getAvailableStock() +
                        " units in stock, and you already have " + existingItem.getQuantity() + " in your cart.");
            }

            existingItem.setQuantity(newQuantity);
            log.info("Aggregated quantity for product ID {} in customer ID {}'s cart. New quantity: {}",
                    product.getId(), customerId, newQuantity);
        } else {
            if (request.getQuantity() > product.getAvailableStock()) {
                throw new InsufficientStockException("Requested quantity " + request.getQuantity() +
                        " for '" + product.getName() + "' exceeds available stock of " + product.getAvailableStock());
            }

            CartItem newItem = new CartItem(cart, request.getProductId(), request.getQuantity());
            cart.addItem(newItem);
            log.info("Added product ID {} (qty: {}) to customer ID {}'s cart",
                    product.getId(), request.getQuantity(), customerId);
        }

        cartRepository.save(cart);
        return buildCartResponse(cart);
    }

    @Override
    public CartResponseDTO updateItemQuantity(Long customerId, Long itemId, UpdateCartItemRequestDTO request) {
        Cart cart = getOrCreateCart(customerId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item with ID " + itemId + " not found"));

        // Validate Ownership: Cross-Customer Access Prevention
        if (!item.getCart().getCustomerId().equals(customerId)) {
            log.error("Cross-customer security violation: Customer ID {} attempted to update item ID {} owned by customer ID {}",
                    customerId, itemId, item.getCart().getCustomerId());
            throw new UnauthorizedCartAccessException("Access denied: You do not have permission to modify cart item " + itemId);
        }

        // Live stock check
        ProductClientDTO.ProductData product = productServiceClient.getProductDetails(item.getProductId());
        if (request.getQuantity() > product.getAvailableStock()) {
            throw new InsufficientStockException("Requested quantity " + request.getQuantity() +
                    " exceeds available stock of " + product.getAvailableStock() + " for '" + product.getName() + "'");
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        log.info("Updated quantity of item ID {} to {} in customer ID {}'s cart",
                itemId, request.getQuantity(), customerId);

        return buildCartResponse(cart);
    }

    @Override
    public CartResponseDTO removeItem(Long customerId, Long itemId) {
        Cart cart = getOrCreateCart(customerId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item with ID " + itemId + " not found"));

        // Validate Ownership
        if (!item.getCart().getCustomerId().equals(customerId)) {
            log.error("Cross-customer security violation: Customer ID {} attempted to delete item ID {} owned by customer ID {}",
                    customerId, itemId, item.getCart().getCustomerId());
            throw new UnauthorizedCartAccessException("Access denied: You do not have permission to delete cart item " + itemId);
        }

        cart.removeItem(item);
        cartItemRepository.delete(item);
        log.info("Removed item ID {} from customer ID {}'s cart", itemId, customerId);

        return buildCartResponse(cart);
    }

    @Override
    public void clearCart(Long customerId) {
        Optional<Cart> cartOpt = cartRepository.findByCustomerId(customerId);
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            cart.clearItems();
            cartRepository.save(cart);
            log.info("Cleared all items from customer ID {}'s cart", customerId);
        }
    }

    private Cart getOrCreateCart(Long customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    Cart newCart = new Cart(customerId);
                    Cart saved = cartRepository.save(newCart);
                    log.info("Created new shopping cart ID {} for customer ID {}", saved.getId(), customerId);
                    return saved;
                });
    }

    private CartResponseDTO buildCartResponse(Cart cart) {
        List<CartItemDTO> itemDTOs = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalItemsCount = 0;

        for (CartItem item : cart.getItems()) {
            ProductClientDTO.ProductData product;
            try {
                product = productServiceClient.getProductDetails(item.getProductId());
            } catch (Exception ex) {
                log.warn("Could not retrieve real-time details for product ID {}: {}", item.getProductId(), ex.getMessage());
                // Fallback default representation when service is unreachable
                product = new ProductClientDTO.ProductData();
                product.setId(item.getProductId());
                product.setName("Product #" + item.getProductId());
                product.setPrice(BigDecimal.ZERO);
                product.setAvailableStock(0);
                product.setActive(false);
            }

            BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            boolean inStock = product.getAvailableStock() >= item.getQuantity();

            CartItemDTO itemDTO = new CartItemDTO(
                    item.getId(),
                    item.getProductId(),
                    product.getName(),
                    product.getSku(),
                    unitPrice,
                    product.getImageUrl(),
                    item.getQuantity(),
                    itemTotal,
                    inStock,
                    product.getAvailableStock()
            );

            itemDTOs.add(itemDTO);
            subtotal = subtotal.add(itemTotal);
            totalItemsCount += item.getQuantity();
        }

        return new CartResponseDTO(
                cart.getId(),
                cart.getCustomerId(),
                itemDTOs,
                totalItemsCount,
                subtotal
        );
    }
}

