package com.eshopping.cart.service;

import com.eshopping.cart.client.ProductServiceClient;
import com.eshopping.cart.dto.AddToCartRequestDTO;
import com.eshopping.cart.dto.CartResponseDTO;
import com.eshopping.cart.dto.ProductClientDTO;
import com.eshopping.cart.dto.UpdateCartItemRequestDTO;
import com.eshopping.cart.entity.Cart;
import com.eshopping.cart.entity.CartItem;
import com.eshopping.cart.exception.InsufficientStockException;
import com.eshopping.cart.exception.ResourceNotFoundException;
import com.eshopping.cart.exception.UnauthorizedCartAccessException;
import com.eshopping.cart.repository.CartItemRepository;
import com.eshopping.cart.repository.CartRepository;
import com.eshopping.cart.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart testCart;
    private ProductClientDTO.ProductData testProduct;

    @BeforeEach
    void setUp() {
        testCart = new Cart(100L);
        testCart.setId(1L);

        testProduct = new ProductClientDTO.ProductData();
        testProduct.setId(10L);
        testProduct.setName("Wireless Headphones");
        testProduct.setSku("ELEC-HP-001");
        testProduct.setPrice(new BigDecimal("9999.00"));
        testProduct.setImageUrl("http://img.com/hp.png");
        testProduct.setAvailableStock(50);
        testProduct.setActive(true);
    }

    @Test
    @DisplayName("Get cart: Retrieves customer cart and calculates live subtotal from Product Service")
    void testGetCart_Success() {
        CartItem item = new CartItem(testCart, 10L, 2);
        item.setId(5L);
        testCart.addItem(item);

        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(testCart));
        when(productServiceClient.getProductDetails(10L)).thenReturn(testProduct);

        CartResponseDTO response = cartService.getCart(100L);

        assertNotNull(response);
        assertEquals(1L, response.getCartId());
        assertEquals(100L, response.getCustomerId());
        assertEquals(1, response.getItems().size());
        assertEquals(2, response.getTotalItems());
        assertEquals(new BigDecimal("19998.00"), response.getCartSubtotal());
    }

    @Test
    @DisplayName("Add item: Successfully adds new product to empty cart")
    void testAddItemToCart_NewItem_Success() {
        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(testCart));
        when(productServiceClient.getProductDetails(10L)).thenReturn(testProduct);
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        AddToCartRequestDTO request = new AddToCartRequestDTO(10L, 1);
        CartResponseDTO response = cartService.addItemToCart(100L, request);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(1, response.getTotalItems());
        assertEquals(new BigDecimal("9999.00"), response.getCartSubtotal());
        verify(cartRepository, times(1)).save(testCart);
    }

    @Test
    @DisplayName("Add item: Existing product in cart aggregates quantity")
    void testAddItemToCart_ExistingItem_AggregatesQuantity() {
        CartItem existingItem = new CartItem(testCart, 10L, 2);
        existingItem.setId(5L);
        testCart.addItem(existingItem);

        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(testCart));
        when(productServiceClient.getProductDetails(10L)).thenReturn(testProduct);
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        AddToCartRequestDTO request = new AddToCartRequestDTO(10L, 3);
        CartResponseDTO response = cartService.addItemToCart(100L, request);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(5, response.getTotalItems()); // 2 + 3 = 5
        assertEquals(new BigDecimal("49995.00"), response.getCartSubtotal());
    }

    @Test
    @DisplayName("Add item: Request exceeds available stock throws InsufficientStockException")
    void testAddItemToCart_InsufficientStock() {
        testProduct.setAvailableStock(3);

        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(testCart));
        when(productServiceClient.getProductDetails(10L)).thenReturn(testProduct);

        AddToCartRequestDTO request = new AddToCartRequestDTO(10L, 5); // requests 5, but stock is 3

        assertThrows(InsufficientStockException.class, () -> cartService.addItemToCart(100L, request));
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Add item: Inactive or non-existent product throws ResourceNotFoundException")
    void testAddItemToCart_ProductNotFound() {
        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(testCart));
        when(productServiceClient.getProductDetails(999L)).thenThrow(new ResourceNotFoundException("Product not found"));

        AddToCartRequestDTO request = new AddToCartRequestDTO(999L, 1);

        assertThrows(ResourceNotFoundException.class, () -> cartService.addItemToCart(100L, request));
    }

    @Test
    @DisplayName("Update item quantity: Owner customer successfully updates quantity")
    void testUpdateItemQuantity_Success() {
        CartItem item = new CartItem(testCart, 10L, 2);
        item.setId(5L);
        testCart.addItem(item);

        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(productServiceClient.getProductDetails(10L)).thenReturn(testProduct);

        UpdateCartItemRequestDTO request = new UpdateCartItemRequestDTO(4);
        CartResponseDTO response = cartService.updateItemQuantity(100L, 5L, request);

        assertNotNull(response);
        assertEquals(4, item.getQuantity());
        verify(cartItemRepository, times(1)).save(item);
    }

    @Test
    @DisplayName("Update item quantity: Cross-customer access attempt throws UnauthorizedCartAccessException")
    void testUpdateItemQuantity_CrossCustomerViolation() {
        Cart foreignCart = new Cart(200L); // owned by customer 200
        foreignCart.setId(2L);
        CartItem foreignItem = new CartItem(foreignCart, 10L, 2);
        foreignItem.setId(8L);

        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(8L)).thenReturn(Optional.of(foreignItem));

        UpdateCartItemRequestDTO request = new UpdateCartItemRequestDTO(4);

        // Customer 100 attempts to modify item belonging to customer 200
        assertThrows(UnauthorizedCartAccessException.class, () ->
                cartService.updateItemQuantity(100L, 8L, request)
        );
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Remove item: Successfully deletes item from cart")
    void testRemoveItem_Success() {
        CartItem item = new CartItem(testCart, 10L, 2);
        item.setId(5L);
        testCart.addItem(item);

        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(item));

        CartResponseDTO response = cartService.removeItem(100L, 5L);

        assertNotNull(response);
        assertEquals(0, testCart.getItems().size());
        verify(cartItemRepository, times(1)).delete(item);
    }

    @Test
    @DisplayName("Clear cart: Clears all items from customer cart")
    void testClearCart_Success() {
        CartItem item1 = new CartItem(testCart, 10L, 2);
        CartItem item2 = new CartItem(testCart, 11L, 1);
        testCart.addItem(item1);
        testCart.addItem(item2);

        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(testCart));

        cartService.clearCart(100L);

        assertEquals(0, testCart.getItems().size());
        verify(cartRepository, times(1)).save(testCart);
    }

    @Test
    @DisplayName("Update item quantity: Item not found throws ResourceNotFoundException")
    void testUpdateItemQuantity_ItemNotFound() {
        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateCartItemRequestDTO request = new UpdateCartItemRequestDTO(2);

        assertThrows(ResourceNotFoundException.class, () ->
                cartService.updateItemQuantity(100L, 999L, request)
        );
    }

    @Test
    @DisplayName("Update item quantity: Exceeding stock throws InsufficientStockException")
    void testUpdateItemQuantity_ExceedsStock() {
        CartItem item = new CartItem(testCart, 10L, 2);
        item.setId(5L);
        testCart.addItem(item);

        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(item));
        testProduct.setAvailableStock(3);
        when(productServiceClient.getProductDetails(10L)).thenReturn(testProduct);

        UpdateCartItemRequestDTO request = new UpdateCartItemRequestDTO(10); // requests 10, stock is 3

        assertThrows(InsufficientStockException.class, () ->
                cartService.updateItemQuantity(100L, 5L, request)
        );
    }

    @Test
    @DisplayName("Remove item: Item not found throws ResourceNotFoundException")
    void testRemoveItem_ItemNotFound() {
        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                cartService.removeItem(100L, 999L)
        );
    }

    @Test
    @DisplayName("Clear cart: When cart does not exist does nothing gracefully")
    void testClearCart_CartNotFound() {
        when(cartRepository.findByCustomerId(200L)).thenReturn(Optional.empty());

        cartService.clearCart(200L);

        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get cart: Product service error triggers fallback product metadata")
    void testGetCart_ProductServiceFallback() {
        CartItem item = new CartItem(testCart, 99L, 1);
        item.setId(8L);
        testCart.addItem(item);

        when(cartRepository.findByCustomerId(100L)).thenReturn(Optional.of(testCart));
        when(productServiceClient.getProductDetails(99L)).thenThrow(new RuntimeException("Product service down"));

        CartResponseDTO response = cartService.getCart(100L);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals("Product #99", response.getItems().get(0).getProductName());
        assertEquals(BigDecimal.ZERO, response.getItems().get(0).getUnitPrice());
    }

    @Test
    @DisplayName("Get cart: Auto-provisions cart if customer does not have one")
    void testGetCart_AutoProvisionsCart() {
        when(cartRepository.findByCustomerId(300L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> {
            Cart c = i.getArgument(0);
            c.setId(30L);
            return c;
        });

        CartResponseDTO response = cartService.getCart(300L);

        assertNotNull(response);
        assertEquals(300L, response.getCustomerId());
        assertEquals(0, response.getItems().size());
        verify(cartRepository).save(any(Cart.class));
    }
}

