-- ==============================================================================
-- E-ShoppingZone: Additional Product Catalog Seed Script
-- Adds 16 new rich products across 4 categories (IDs 13-28) + inventory records
-- ==============================================================================

USE product_db;

-- 1. Insert Additional Products (IDs 13-28)
INSERT INTO products (id, merchant_id, category_id, name, sku, description, price, image_url, active) VALUES
-- Electronics (Category 1)
(13, 2, 1, 'Mechanical RGB Gaming Keyboard with Hot-Swappable Switches', 'ELEC-KB-004', 'Linear red mechanical switches, per-key RGB backlighting, sound-dampening foam, and aircraft-grade aluminum top plate', 6499.00, 'https://images.unsplash.com/photo-1595225476474-87563907a212?w=500', 1),
(14, 2, 1, 'Flagship 5G Smartphone 256GB Midnight Black', 'ELEC-PHN-005', '6.7-inch 120Hz AMOLED display, triple 50MP Sony camera system, 5000mAh battery with 68W fast charging', 54999.00, 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500', 1),
(15, 2, 1, 'Portable Waterproof Bluetooth Speaker 30W Stereo', 'ELEC-SPK-006', 'IPX7 waterproof rating, dual passive radiators for deep bass, 24-hour continuous playtime, and party pairing mode', 3799.00, 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=500', 1),
(16, 2, 1, 'Fast Charging Power Bank 20000mAh with LED Display', 'ELEC-PB-007', 'Dual USB-C 65W PD + QC 3.0 output, digital percentage display, multi-device simultaneous charging, and airline approved', 2199.00, 'https://images.unsplash.com/photo-1609592426868-45e032543958?w=500', 1),

-- Apparel (Category 2)
(17, 2, 2, 'Premium Wool Blend Tailored Overcoat Charcoal', 'APP-COT-004', 'Italian-inspired slim cut overcoat crafted with heavy wool-cashmere blend, notched lapels, and silk-lined interior', 6999.00, 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=500', 1),
(18, 2, 2, 'Comfort Stretch Slim-Fit Chinos Navy Blue', 'APP-CHN-005', 'Four-way stretch twill fabric with wrinkle-resistant finish, concealed zippered coin pocket, and flex waistband', 1899.00, 'https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=500', 1),
(19, 2, 2, 'Handcrafted Genuine Leather Crossbody Bag Tan', 'APP-BAG-006', 'Full-grain vegetable-tanned leather with antique brass hardware, padded tablet compartment, and adjustable shoulder strap', 3299.00, 'https://images.unsplash.com/photo-1548036328-c9fa89d128fa?w=500', 1),
(20, 2, 2, 'Cozy Heavyweight Fleece Pullover Hoodie Heather Grey', 'APP-HOD-007', '400 GSM brushed fleece cotton, double-layered hood with metal-tipped drawstrings, and kangaroo hand-warmer pocket', 1599.00, 'https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=500', 1),

-- Books (Category 3)
(21, 2, 3, 'The Psychology of Money: Timeless Lessons on Wealth', 'BK-FIN-004', 'Morgan Housel bestselling exploration of how our emotions, biases, and ego shape our financial decisions and success', 450.00, 'https://images.unsplash.com/photo-1592496431122-2349e0fbc666?w=500', 1),
(22, 2, 3, 'Sapiens: A Brief History of Humankind by Yuval Noah Harari', 'BK-HIS-005', 'A groundbreaking journey through 70,000 years of human evolution, culture, cognitive revolutions, and civilization', 699.00, 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500', 1),
(23, 2, 3, 'Deep Work: Rules for Focused Success in a Distracted World', 'BK-PROD-006', 'Cal Newport actionable blueprint for cultivating intense concentration and mastering hard things in the modern workplace', 550.00, 'https://images.unsplash.com/photo-1506880018603-83d5b814b5a6?w=500', 1),
(24, 2, 3, 'The Pragmatic Programmer: Your Journey To Mastery (20th Anniv)', 'BK-TECH-007', 'David Thomas & Andrew Hunt classic guide to career longevity, code craftsmanship, pragmatic thinking, and software excellence', 1699.00, 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=500', 1),

-- Personal Care (Category 4)
(25, 2, 4, 'Broad Spectrum Mineral Sunscreen SPF 50+ PA++++ 100ml', 'PC-SUN-004', 'Non-greasy, zero white-cast zinc oxide and niacinamide formula providing ultra-light UVA/UVB defense for all skin types', 749.00, 'https://images.unsplash.com/photo-1598440947619-2c35fc9aa908?w=500', 1),
(26, 2, 4, 'Pure Moroccan Argan Hair Repair Treatment 100ml', 'PC-OIL-005', 'Cold-pressed organic argan oil enriched with vitamin E, restoring split ends, adding luminous shine, and taming frizz', 999.00, 'https://images.unsplash.com/photo-1608248597359-58a3a93e390c?w=500', 1),
(27, 2, 4, 'Activated Charcoal Detox Body Wash 300ml with Tea Tree Oil', 'PC-BDY-006', 'Deep pore cleansing body wash with micro-exfoliating volcanic pumice, cooling peppermint, and antibacterial tea tree', 499.00, 'https://images.unsplash.com/photo-1585232351009-aa87416fca90?w=500', 1),
(28, 2, 4, 'Natural Botanical Hydrating Lip Balm Trio (Vanilla, Mint, Berry)', 'PC-LIP-007', 'Beeswax, organic shea butter, and sweet almond oil lip therapy trio keeping lips deeply moisturized for 12 hours', 349.00, 'https://images.unsplash.com/photo-1567928805162-f6745f949c12?w=500', 1)
ON DUPLICATE KEY UPDATE 
    name = VALUES(name),
    description = VALUES(description),
    price = VALUES(price),
    image_url = VALUES(image_url),
    active = VALUES(active);

-- 2. Insert Inventory Records for Products 13-28
INSERT INTO inventory (id, product_id, available_stock, reserved_stock, version) VALUES
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
ON DUPLICATE KEY UPDATE 
    available_stock = VALUES(available_stock);

