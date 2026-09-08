-- StockPulse · Seed Data
-- Adapt table/column names to your schema

INSERT INTO products (id, sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status, version, cost_price, supplier_id, lead_time_days, safety_stock, incoming_stock, minimum_order_quantity) VALUES
  ('PRD-001', 'SKU-ELEC-001', 'Wireless Earbuds Pro',     'ELECTRONICS', 79.99,  45,  20, 3,  'ACTIVE',               0, 45.00, 'SUPPLIER-ELEC-A', 7, 10, 0, 25),
  ('PRD-002', 'SKU-ELEC-002', 'USB-C Hub 7-Port',           'ELECTRONICS', 34.99,  120, 30, 1,  'ACTIVE',               0, 18.00, 'SUPPLIER-ELEC-B', 7, 10, 0, 25),
  ('PRD-003', 'SKU-APP-001',  'Organic Cotton T-Shirt',     'APPAREL',     24.99,  8,   15, 12, 'PRICE_REVIEW_PENDING', 0, 10.00, 'SUPPLIER-APP-A',  5, 10, 0, 25),
  ('PRD-004', 'SKU-APP-002',  'Running Shorts — Navy',      'APPAREL',     39.99,  55,  20, 2,  'ACTIVE',               0, 20.00, 'SUPPLIER-APP-B',  5, 10, 0, 25),
  ('PRD-005', 'SKU-HOME-001', 'Ceramic Pour-Over Set',      'HOME',        49.99,  22,  10, 4,  'ACTIVE',               0, 25.00, 'SUPPLIER-HOME-A', 10, 10, 0, 25),
  ('PRD-006', 'SKU-HOME-002', 'LED Desk Lamp — Dimmable',   'HOME',        59.99,  0,   15, 0,  'OUT_OF_STOCK',         0, 30.00, 'SUPPLIER-HOME-B', 10, 10, 0, 25),
  ('PRD-007', 'SKU-ELEC-003', 'Portable Charger 20K',       'ELECTRONICS', 44.99,  18,  25, 8,  'ACTIVE',               0, 22.00, 'SUPPLIER-ELEC-A', 7, 10, 0, 25),
  ('PRD-008', 'SKU-APP-003',  'Hoodie — Heather Grey',      'APPAREL',     54.99,  11,  12, 15, 'ACTIVE',               0, 28.00, 'SUPPLIER-APP-A',  5, 10, 0, 25);