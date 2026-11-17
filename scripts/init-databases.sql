-- ==============================================================================
-- E-ShoppingZone Microservices Platform — Database Initialization Script
-- Capgemini Case Study Architecture (Local Windows Environment)
-- Target: MySQL 8.4 Server
-- ==============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ==============================================================================
-- 1. PROFILE SERVICE DATABASE (profile_db)
-- ==============================================================================
CREATE DATABASE IF NOT EXISTS profile_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE profile_db;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(191) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NULL,
    full_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(25) NULL,
    address_line1 VARCHAR(255) NULL,
    address_line2 VARCHAR(255) NULL,
    city VARCHAR(100) NULL,
    state VARCHAR(100) NULL,
    postal_code VARCHAR(20) NULL,
    country VARCHAR(100) DEFAULT 'India',
    profile_picture_url VARCHAR(500) NULL,
    auth_provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
    github_id VARCHAR(100) NULL UNIQUE,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_github (github_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role_name),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    parent_token_hash VARCHAR(64) NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by_ip VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_rt_user (user_id),
    INDEX idx_rt_hash (token_hash),
    INDEX idx_rt_parent (parent_token_hash)
) ENGINE=InnoDB;

-- Pre-seed Users (Password: Pass1234!)
INSERT INTO users (id, email, password_hash, full_name, phone_number, auth_provider, enabled) VALUES
(1, 'customer@eshopping.com', '$2a$10$wT0l0hF2BvH7gP5T5b4KeeiWb7Y5l89C1aZp1bZ4zFqJ2cK6f0g5K', 'Alice Customer', '9876543210', 'LOCAL', 1),
(2, 'merchant@eshopping.com', '$2a$10$wT0l0hF2BvH7gP5T5b4KeeiWb7Y5l89C1aZp1bZ4zFqJ2cK6f0g5K', 'Bob Merchant', '9876543211', 'LOCAL', 1)
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);

INSERT INTO user_roles (user_id, role_name) VALUES
(1, 'ROLE_CUSTOMER'),
(2, 'ROLE_MERCHANT')
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name);

-- ==============================================================================
-- 2. PRODUCT SERVICE DATABASE (product_db)
-- ==============================================================================
CREATE DATABASE IF NOT EXISTS product_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE product_db;

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255) NULL,
    image_url VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_categories_slug (slug)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NULL,
    price DECIMAL(12,2) NOT NULL,
    image_url VARCHAR(500) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    INDEX idx_products_category (category_id),
    INDEX idx_products_merchant (merchant_id),
    INDEX idx_products_active (active),
    INDEX idx_products_sku (sku)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    available_stock INT NOT NULL DEFAULT 0,
    reserved_stock INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_inventory_product (product_id)
) ENGINE=InnoDB;

-- Pre-seed Categories
INSERT INTO categories (id, name, slug, description, image_url) VALUES
(1, 'Electronics', 'electronics', 'Cutting-edge gadgets, computing, and smart accessories', 'https://images.unsplash.com/photo-1498049794561-7780e7231661?w=500'),
(2, 'Apparel', 'apparel', 'Trendy clothing, luxury footwear, and seasonal fashion', 'https://images.unsplash.com/photo-1445205170230-053b83016050?w=500'),
(3, 'Books', 'books', 'Bestsellers, technical guides, literature, and self-help', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500'),
(4, 'Personal Care', 'personal-care', 'Skincare, grooming kits, and health wellness products', 'https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=500')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Pre-seed Products (Owned by Bob Merchant ID 2)
INSERT INTO products (id, merchant_id, category_id, name, sku, description, price, image_url, active) VALUES
(1, 2, 1, 'Pro Wireless Noise-Cancelling Headphones', 'ELEC-HP-001', 'Active noise cancellation with 40-hour battery life and Hi-Res audio drivers', 14999.00, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500', 1),
(2, 2, 1, 'Ultra Slim 4K IPS Monitor 27"', 'ELEC-MON-002', 'Crisp 3840x2160 resolution with HDR400 and USB-C 65W power delivery', 28999.00, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=500', 1),
(3, 2, 1, 'Smart Fitness Tracker Watch', 'ELEC-WAT-003', 'Continuous heart rate, SpO2 monitoring, and 5ATM water resistance', 4999.00, 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500', 1),
(4, 2, 2, 'Classic Denim Sherpa Jacket', 'APP-JKT-001', 'Heavyweight authentic denim lined with soft thermal faux sherpa fleece', 3499.00, 'https://images.unsplash.com/photo-1576995853123-5a10305d93c0?w=500', 1),
(5, 2, 2, 'Organic Cotton Minimalist T-Shirt', 'APP-TSH-002', '100% GOTS certified combed organic cotton with reinforced crew neckline', 799.00, 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500', 1),
(6, 2, 2, 'Breathable Performance Running Shoes', 'APP-SHO-003', 'Engineered mesh upper with responsive foam cushioning for road running', 4299.00, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500', 1),
(7, 2, 3, 'Designing Data-Intensive Applications', 'BK-TECH-001', 'The definitive guide to distributed systems, storage engines, and reliability', 1850.00, 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500', 1),
(8, 2, 3, 'Clean Architecture: A Craftsman\'s Guide', 'BK-TECH-002', 'Robert C. Martin guide to software structure, component boundaries, and decoupling', 1250.00, 'https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=500', 1),
(9, 2, 3, 'Atomic Habits: An Easy & Proven Way', 'BK-SELF-003', 'James Clear framework for building remarkable lifelong daily habits', 599.00, 'https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=500', 1),
(10, 2, 4, 'Organic Botanical Facial Cleanser 200ml', 'PC-CLN-001', 'Gentle clarifying face wash with green tea extract and tea tree essential oil', 649.00, 'https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500', 1),
(11, 2, 4, 'Hydrating Hyaluronic Acid Serum 50ml', 'PC-SRM-002', 'Multi-molecular weight hyaluronic acid formula for deep barrier hydration', 899.00, 'https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=500', 1),
(12, 2, 4, 'Precision Beard Grooming & Trimming Kit', 'PC-GRM-003', 'Titanium self-sharpening cordless trimmer with 20 lock-in precision length settings', 1999.00, 'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=500', 1),
(13, 2, 1, 'Mechanical RGB Gaming Keyboard with Hot-Swappable Switches', 'ELEC-KB-004', 'Linear red mechanical switches, per-key RGB backlighting, sound-dampening foam, and aircraft-grade aluminum top plate', 6499.00, 'https://images.unsplash.com/photo-1595225476474-87563907a212?w=500', 1),
(14, 2, 1, 'Flagship 5G Smartphone 256GB Midnight Black', 'ELEC-PHN-005', '6.7-inch 120Hz AMOLED display, triple 50MP Sony camera system, 5000mAh battery with 68W fast charging', 54999.00, 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500', 1),
(15, 2, 1, 'Portable Waterproof Bluetooth Speaker 30W Stereo', 'ELEC-SPK-006', 'IPX7 waterproof rating, dual passive radiators for deep bass, 24-hour continuous playtime, and party pairing mode', 3799.00, 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=500', 1),
(16, 2, 1, 'Fast Charging Power Bank 20000mAh with LED Display', 'ELEC-PB-007', 'Dual USB-C 65W PD + QC 3.0 output, digital percentage display, multi-device simultaneous charging, and airline approved', 2199.00, 'https://images.unsplash.com/photo-1609592426868-45e032543958?w=500', 1),
(17, 2, 2, 'Premium Wool Blend Tailored Overcoat Charcoal', 'APP-COT-004', 'Italian-inspired slim cut overcoat crafted with heavy wool-cashmere blend, notched lapels, and silk-lined interior', 6999.00, 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=500', 1),
(18, 2, 2, 'Comfort Stretch Slim-Fit Chinos Navy Blue', 'APP-CHN-005', 'Four-way stretch twill fabric with wrinkle-resistant finish, concealed zippered coin pocket, and flex waistband', 1899.00, 'https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=500', 1),
(19, 2, 2, 'Handcrafted Genuine Leather Crossbody Bag Tan', 'APP-BAG-006', 'Full-grain vegetable-tanned leather with antique brass hardware, padded tablet compartment, and adjustable shoulder strap', 3299.00, 'https://images.unsplash.com/photo-1548036328-c9fa89d128fa?w=500', 1),
(20, 2, 2, 'Cozy Heavyweight Fleece Pullover Hoodie Heather Grey', 'APP-HOD-007', '400 GSM brushed fleece cotton, double-layered hood with metal-tipped drawstrings, and kangaroo hand-warmer pocket', 1599.00, 'https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=500', 1),
(21, 2, 3, 'The Psychology of Money: Timeless Lessons on Wealth', 'BK-FIN-004', 'Morgan Housel bestselling exploration of how our emotions, biases, and ego shape our financial decisions and success', 450.00, 'https://images.unsplash.com/photo-1592496431122-2349e0fbc666?w=500', 1),
(22, 2, 3, 'Sapiens: A Brief History of Humankind by Yuval Noah Harari', 'BK-HIS-005', 'A groundbreaking journey through 70,000 years of human evolution, culture, cognitive revolutions, and civilization', 699.00, 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500', 1),
(23, 2, 3, 'Deep Work: Rules for Focused Success in a Distracted World', 'BK-PROD-006', 'Cal Newport actionable blueprint for cultivating intense concentration and mastering hard things in the modern workplace', 550.00, 'https://images.unsplash.com/photo-1506880018603-83d5b814b5a6?w=500', 1),
(24, 2, 3, 'The Pragmatic Programmer: Your Journey To Mastery (20th Anniv)', 'BK-TECH-007', 'David Thomas & Andrew Hunt classic guide to career longevity, code craftsmanship, pragmatic thinking, and software excellence', 1699.00, 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=500', 1),
(25, 2, 4, 'Broad Spectrum Mineral Sunscreen SPF 50+ PA++++ 100ml', 'PC-SUN-004', 'Non-greasy, zero white-cast zinc oxide and niacinamide formula providing ultra-light UVA/UVB defense for all skin types', 749.00, 'https://images.unsplash.com/photo-1598440947619-2c35fc9aa908?w=500', 1),
(26, 2, 4, 'Pure Moroccan Argan Hair Repair Treatment 100ml', 'PC-OIL-005', 'Cold-pressed organic argan oil enriched with vitamin E, restoring split ends, adding luminous shine, and taming frizz', 999.00, 'https://images.unsplash.com/photo-1608248597359-58a3a93e390c?w=500', 1),
(27, 2, 4, 'Activated Charcoal Detox Body Wash 300ml with Tea Tree Oil', 'PC-BDY-006', 'Deep pore cleansing body wash with micro-exfoliating volcanic pumice, cooling peppermint, and antibacterial tea tree', 499.00, 'https://images.unsplash.com/photo-1585232351009-aa87416fca90?w=500', 1),
(28, 2, 4, 'Natural Botanical Hydrating Lip Balm Trio (Vanilla, Mint, Berry)', 'PC-LIP-007', 'Beeswax, organic shea butter, and sweet almond oil lip therapy trio keeping lips deeply moisturized for 12 hours', 349.00, 'https://images.unsplash.com/photo-1567928805162-f6745f949c12?w=500', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Pre-seed Inventory
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
(12, 12, 45, 0, 0),
(13, 13, 85, 0, 0),
(14, 14, 40, 0, 0),
(15, 15, 120, 0, 0),
(16, 16, 150, 0, 0),
(17, 17, 45, 0, 0),
(18, 18, 110, 0, 0),
(19, 19, 60, 0, 0),
(20, 20, 130, 0, 0),
(21, 21, 140, 0, 0),
(22, 22, 95, 0, 0),
(23, 23, 100, 0, 0),
(24, 24, 70, 0, 0),
(25, 25, 125, 0, 0),
(26, 26, 80, 0, 0),
(27, 27, 115, 0, 0),
(28, 28, 160, 0, 0)
ON DUPLICATE KEY UPDATE available_stock=VALUES(available_stock);

-- ==============================================================================
-- 3. CART SERVICE DATABASE (cart_db)
-- ==============================================================================
CREATE DATABASE IF NOT EXISTS cart_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cart_db;

CREATE TABLE IF NOT EXISTS carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_carts_customer (customer_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    UNIQUE KEY uk_cart_product (cart_id, product_id),
    INDEX idx_cart_items_cart (cart_id),
    INDEX idx_cart_items_product (product_id)
) ENGINE=InnoDB;

-- ==============================================================================
-- 4. WALLET SERVICE DATABASE (wallet_db)
-- ==============================================================================
CREATE DATABASE IF NOT EXISTS wallet_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE wallet_db;

CREATE TABLE IF NOT EXISTS wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL UNIQUE,
    current_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_wallets_customer (customer_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    transaction_reference VARCHAR(64) NOT NULL UNIQUE,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    order_reference VARCHAR(64) NULL,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    balance_before DECIMAL(12,2) NOT NULL,
    balance_after DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    description VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
    INDEX idx_wallet_tx_wallet (wallet_id),
    INDEX idx_wallet_tx_ref (transaction_reference),
    INDEX idx_wallet_tx_idemp (idempotency_key),
    INDEX idx_wallet_tx_order (order_reference)
) ENGINE=InnoDB;

-- Pre-seed Wallet for Alice Customer (Customer ID 1) with ₹15,000.00
INSERT INTO wallets (id, customer_id, current_balance, currency, status, version) VALUES
(1, 1, 15000.00, 'INR', 'ACTIVE', 0)
ON DUPLICATE KEY UPDATE current_balance=VALUES(current_balance);

INSERT INTO wallet_transactions (wallet_id, transaction_reference, idempotency_key, transaction_type, amount, balance_before, balance_after, status, description) VALUES
(1, 'TXN-DEP-SEED-001', 'IDEMP-DEP-SEED-001', 'DEPOSIT', 15000.00, 0.00, 15000.00, 'SUCCESS', 'Initial Seed Wallet Top-up')
ON DUPLICATE KEY UPDATE status=VALUES(status);

-- ==============================================================================
-- 5. ORDER SERVICE DATABASE (order_db)
-- ==============================================================================
CREATE DATABASE IF NOT EXISTS order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE order_db;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(64) NOT NULL UNIQUE,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    order_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(30) NOT NULL,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    saga_state VARCHAR(40) NOT NULL DEFAULT 'STARTED',
    saga_failure_reason VARCHAR(255) NULL,
    subtotal_amount DECIMAL(12,2) NOT NULL,
    shipping_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12,2) NOT NULL,
    shipping_name VARCHAR(100) NOT NULL,
    shipping_phone VARCHAR(25) NOT NULL,
    shipping_address TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_orders_customer (customer_id),
    INDEX idx_orders_status (order_status),
    INDEX idx_orders_saga (saga_state),
    INDEX idx_orders_number (order_number),
    INDEX idx_orders_idemp (idempotency_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    product_sku VARCHAR(100) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order_items_order (order_id),
    INDEX idx_order_items_merchant (merchant_id),
    INDEX idx_order_items_product (product_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payment_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    payment_method VARCHAR(30) NOT NULL,
    payment_status VARCHAR(30) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_reference VARCHAR(100) NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_payment_order (order_id)
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;

