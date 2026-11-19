-- =============================================================================
-- E-ShoppingZone Database Seed Data Script
-- Populates demo users, merchant, categories, products, inventory, and wallet.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. SEED PROFILE SERVICE (profile_db)
-- Default Password for all demo accounts: Password@123
-- BCrypt Hash: $2a$10$wT0l0hF2BvH7gP5T5b4KeeiWb7Y5l89C1aZp1bZ4zFqJ2cK6f0g5K
-- -----------------------------------------------------------------------------
USE profile_db;

INSERT INTO users (id, email, password_hash, full_name, phone_number, address_line1, city, state, postal_code, country, auth_provider, enabled)
VALUES 
(1, 'customer@eshopping.com', '$2a$10$wT0l0hF2BvH7gP5T5b4KeeiWb7Y5l89C1aZp1bZ4zFqJ2cK6f0g5K', 'Alice Customer', '+91-9876543210', 'Flat 402, Green Glen Layout, Bellandur', 'Bengaluru', 'Karnataka', '560103', 'India', 'LOCAL', TRUE),
(2, 'merchant@eshopping.com', '$2a$10$wT0l0hF2BvH7gP5T5b4KeeiWb7Y5l89C1aZp1bZ4zFqJ2cK6f0g5K', 'Bob Merchant', '+91-9876543211', 'Shop 12, Commercial Complex, MG Road', 'Bengaluru', 'Karnataka', '560001', 'India', 'LOCAL', TRUE),
(3, 'john.doe@eshopping.com', '$2a$10$wT0l0hF2BvH7gP5T5b4KeeiWb7Y5l89C1aZp1bZ4zFqJ2cK6f0g5K', 'John Doe', '+91-9876543212', '104 High Street', 'Mumbai', 'Maharashtra', '400001', 'India', 'LOCAL', TRUE),
(4, 'admin@eshopping.com', '$2a$10$wT0l0hF2BvH7gP5T5b4KeeiWb7Y5l89C1aZp1bZ4zFqJ2cK6f0g5K', 'System Administrator', '+91-9876543213', 'Executive Suite 1, Head Office', 'Bengaluru', 'Karnataka', '560001', 'India', 'LOCAL', TRUE)
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);

INSERT INTO user_roles (user_id, role_name) VALUES
(1, 'ROLE_CUSTOMER'),
(2, 'ROLE_MERCHANT'),
(3, 'ROLE_CUSTOMER'),
(4, 'ROLE_ADMIN')
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name);

-- -----------------------------------------------------------------------------
-- 2. SEED PRODUCT SERVICE (product_db)
-- Categories: Electronics, Books, Apparel, Personal Care
-- -----------------------------------------------------------------------------
USE product_db;

INSERT INTO categories (id, name, slug, description, image_url) VALUES
(1, 'Electronics', 'electronics', 'Cutting-edge gadgets, computing, and smart accessories', 'https://images.unsplash.com/photo-1498049794561-7780e7231661?w=500'),
(2, 'Apparel', 'apparel', 'Trendy clothing, luxury footwear, and seasonal fashion', 'https://images.unsplash.com/photo-1445205170230-053b83016050?w=500'),
(3, 'Books', 'books', 'Bestsellers, technical guides, literature, and self-help', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500'),
(4, 'Personal Care', 'personal-care', 'Skincare, grooming kits, and health wellness products', 'https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO products (id, merchant_id, category_id, name, sku, description, price, image_url, active) VALUES
(1, 2, 1, 'Pro Wireless Noise-Cancelling Headphones', 'ELEC-HP-001', 'Over-ear headphones with 40-hour battery life and spatial audio.', 14999.00, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500', TRUE),
(2, 2, 1, 'Ultra Slim 4K IPS Monitor 27"', 'ELEC-MON-002', 'Crisp 4K UHD display with 99% sRGB color gamut and USB-C power delivery.', 28999.00, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=500', TRUE),
(3, 2, 1, 'Smart Fitness Tracker Watch', 'ELEC-WAT-003', 'Continuous heart rate tracking, sleep stage analysis, and 5 ATM water resistance.', 4999.00, 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500', TRUE),
(4, 2, 2, 'Classic Denim Sherpa Jacket', 'APP-JKT-001', 'Heavyweight vintage washed denim jacket with warm sherpa fleece lining.', 3499.00, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=500', TRUE),
(5, 2, 2, 'Organic Cotton Minimalist T-Shirt', 'APP-TSH-002', '100% certified organic breathable combed cotton regular fit crew neck.', 799.00, 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500', TRUE),
(6, 2, 2, 'Breathable Performance Running Shoes', 'APP-SHO-003', 'Lightweight responsive foam cushioning with engineered mesh upper.', 4299.00, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500', TRUE),
(7, 2, 3, 'Designing Data-Intensive Applications', 'BK-TECH-001', 'The definitive guide to the architecture, storage, and reliability of large-scale systems by Martin Kleppmann.', 1850.00, 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500', TRUE),
(8, 2, 3, 'Clean Architecture: A Craftsman''s Guide', 'BK-TECH-002', 'Master software structure, design patterns, and boundaries by Robert C. Martin.', 1250.00, 'https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500', TRUE),
(9, 2, 3, 'Atomic Habits: An Easy & Proven Way', 'BK-SELF-003', 'Tiny changes, remarkable results: behavioral framework by James Clear.', 599.00, 'https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=500', TRUE),
(10, 2, 4, 'Organic Botanical Facial Cleanser 200ml', 'PC-CLN-001', 'Sulfate-free soothing tea tree and aloe vera gentle foaming cleanser.', 649.00, 'https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500', TRUE),
(11, 2, 4, 'Hydrating Hyaluronic Acid Serum 50ml', 'PC-SRM-002', 'Deep multi-depth cellular hydration with vitamin B5 antioxidants.', 899.00, 'https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=500', TRUE),
(12, 2, 4, 'Precision Beard Grooming & Trimming Kit', 'PC-GRM-003', 'Titanium coated self-sharpening blades with 20 length settings.', 1999.00, 'https://images.unsplash.com/photo-1621607512214-68297480165e?w=500', TRUE)
ON DUPLICATE KEY UPDATE price=VALUES(price);

INSERT INTO inventory (id, product_id, available_stock, reserved_stock, version) VALUES
(1, 1, 50, 0, 0),
(2, 2, 25, 0, 0),
(3, 3, 100, 0, 0),
(4, 4, 40, 0, 0),
(5, 5, 200, 0, 0),
(6, 6, 60, 0, 0),
(7, 7, 75, 0, 0),
(8, 8, 80, 0, 0),
(9, 9, 150, 0, 0),
(10, 10, 90, 0, 0),
(11, 11, 110, 0, 0),
(12, 12, 45, 0, 0)
ON DUPLICATE KEY UPDATE available_stock=VALUES(available_stock);

-- -----------------------------------------------------------------------------
-- 3. SEED WALLET SERVICE (wallet_db)
-- Pre-funds Customer Alice (id=1) with ₹10,000.00
-- -----------------------------------------------------------------------------
USE wallet_db;

INSERT INTO wallets (id, customer_id, current_balance, currency, status, version) VALUES
(1, 1, 10000.00, 'INR', 'ACTIVE', 0),
(2, 3, 5000.00, 'INR', 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE current_balance=VALUES(current_balance);

INSERT INTO wallet_transactions (id, wallet_id, transaction_reference, idempotency_key, order_reference, transaction_type, amount, balance_before, balance_after, status, description) VALUES
(1, 1, 'TXN-INIT-001', 'IDEMP-INIT-001', NULL, 'DEPOSIT', 10000.00, 0.00, 10000.00, 'SUCCESS', 'Initial demo promotional deposit'),
(2, 2, 'TXN-INIT-002', 'IDEMP-INIT-002', NULL, 'DEPOSIT', 5000.00, 0.00, 5000.00, 'SUCCESS', 'Initial demo promotional deposit')
ON DUPLICATE KEY UPDATE amount=VALUES(amount);

-- -----------------------------------------------------------------------------
-- 4. SEED ORDER SERVICE (order_db)
-- Demo historical order for Customer Alice (id=1)
-- -----------------------------------------------------------------------------
USE order_db;

INSERT INTO orders (id, order_number, idempotency_key, customer_id, order_status, payment_method, payment_status, saga_state, subtotal_amount, shipping_fee, total_amount, shipping_name, shipping_phone, shipping_address) VALUES
(1, 'ORD-INIT-001', 'IDEMP-ORD-INIT-001', 1, 'DELIVERED', 'WALLET', 'PAID', 'COMPLETED', 14999.00, 0.00, 14999.00, 'Alice Customer', '+91-9876543210', '123 Tech Park, Bangalore 560001')
ON DUPLICATE KEY UPDATE total_amount=VALUES(total_amount);

INSERT INTO order_items (id, order_id, product_id, merchant_id, product_name_snapshot, unit_price_snapshot, quantity, subtotal) VALUES
(1, 1, 1, 2, 'Pro Wireless Noise-Cancelling Headphones', 14999.00, 1, 14999.00)
ON DUPLICATE KEY UPDATE subtotal=VALUES(subtotal);

INSERT INTO payment_records (id, order_id, payment_reference, payment_method, amount, status, gateway_response) VALUES
(1, 1, 'PAY-INIT-001', 'WALLET', 14999.00, 'SUCCESS', 'TXN-INIT-DEMO')
ON DUPLICATE KEY UPDATE amount=VALUES(amount);

