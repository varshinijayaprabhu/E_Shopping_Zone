package com.eshopping.order.dto.client;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartClientDTO {

    private Long cartId;
    private Long customerId;
    private List<CartItemClientDTO> items = new ArrayList<>();
    private int totalItems;
    private BigDecimal cartSubtotal;

    public CartClientDTO() {
        this.cartSubtotal = BigDecimal.ZERO;
    }

    public CartClientDTO(Long cartId, Long customerId, List<CartItemClientDTO> items, int totalItems, BigDecimal cartSubtotal) {
        this.cartId = cartId;
        this.customerId = customerId;
        this.items = items != null ? items : new ArrayList<>();
        this.totalItems = totalItems;
        this.cartSubtotal = cartSubtotal != null ? cartSubtotal : BigDecimal.ZERO;
    }

    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<CartItemClientDTO> getItems() {
        return items;
    }

    public void setItems(List<CartItemClientDTO> items) {
        this.items = items;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public BigDecimal getCartSubtotal() {
        return cartSubtotal;
    }

    public void setCartSubtotal(BigDecimal cartSubtotal) {
        this.cartSubtotal = cartSubtotal;
    }
}

