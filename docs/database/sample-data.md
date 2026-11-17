# Sample Data & Database Seed Reference

## 1. Overview

To facilitate end-to-end testing, demo walkthroughs, and frontend interface rendering immediately upon launch, seed scripts populate default users, merchants, product categories, realistic items across apparel, electronics, books, and initial customer wallet balances.

---

## 2. Default Seed Accounts

All default accounts are configured with the pre-hashed BCrypt password: `Password@123`  
BCrypt Hash: `$2a$10$wT0l0hF2BvH7gP5T5b4KeeiWb7Y5l89C1aZp1bZ4zFqJ2cK6f0g5K` (example standard test hash).

| Account Type | Email | Default Password | Role | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Customer** | `customer@eshopping.com` | `Password@123` | `ROLE_CUSTOMER` | Standard shopping user with pre-funded wallet (₹10,000) |
| **Merchant** | `merchant@eshopping.com` | `Password@123` | `ROLE_MERCHANT` | Active merchant owning initial product inventory |
| **Customer 2** | `john.doe@eshopping.com` | `Password@123` | `ROLE_CUSTOMER` | Second customer for multi-user order isolation testing |

---

## 3. Seed Product Categories

1. **Electronics**: Smartphones, laptops, headphones, smartwatches.
2. **Apparel**: Men's and women's fashion, jackets, footwear.
3. **Books**: Software engineering textbooks, literature, non-fiction.
4. **Personal Care**: Grooming kits, skincare, wellness essentials.

---

## 4. Master SQL Seed Script

A complete SQL seed file is located at `database/seed/seed-data.sql`. It populates:
1. Two customers and one merchant in `profile_db`.
2. Initial categories and 12 realistic catalog products with active inventory in `product_db`.
3. Pre-funded digital wallet with ₹10,000.00 initial deposit transaction in `wallet_db`.

