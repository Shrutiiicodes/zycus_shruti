-- StockPulse Initial Schema Migration (Flyway V1)
-- Compatible with H2 and PostgreSQL

CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(64) PRIMARY KEY,
    sku VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    current_price DECIMAL(19, 2) NOT NULL,
    stock_level INT NOT NULL,
    reorder_threshold INT NOT NULL,
    demand_velocity INT NOT NULL,
    status VARCHAR(64) NOT NULL,
    version BIGINT DEFAULT 0,
    cost_price DECIMAL(19, 2),
    supplier_id VARCHAR(64),
    lead_time_days INT DEFAULT 7,
    safety_stock INT DEFAULT 10,
    incoming_stock INT DEFAULT 0,
    minimum_order_quantity INT DEFAULT 25,
    last_price_change_timestamp TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pricing_suggestions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(64) NOT NULL,
    current_price DECIMAL(19, 2) NOT NULL,
    product_version BIGINT,
    recommended_price DECIMAL(19, 2) NOT NULL,
    direction VARCHAR(32) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    reasoning VARCHAR(2000),
    status VARCHAR(32) NOT NULL,
    trigger_reason VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_pricing_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS reorder_suggestions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(64) NOT NULL,
    current_stock INT NOT NULL,
    incoming_stock INT DEFAULT 0,
    product_version BIGINT,
    recommended_quantity INT NOT NULL,
    suggested_lead_time_days INT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    reasoning VARCHAR(2000),
    status VARCHAR(32) NOT NULL,
    trigger_reason VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_reorder_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS purchase_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(64) NOT NULL,
    quantity_ordered INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    supplier_id VARCHAR(64),
    suggestion_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    expected_arrival_date TIMESTAMP,
    received_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_json VARCHAR(2000),
    status VARCHAR(32) NOT NULL,
    retry_count INT DEFAULT 0,
    locked_by VARCHAR(64),
    locked_at TIMESTAMP,
    lease_expiry TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS price_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(64) NOT NULL,
    old_price DECIMAL(19, 2) NOT NULL,
    new_price DECIMAL(19, 2) NOT NULL,
    changed_by VARCHAR(64) NOT NULL,
    reason VARCHAR(255),
    suggestion_id BIGINT,
    timestamp TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS inventory_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(64) NOT NULL,
    quantity_delta INT NOT NULL,
    new_stock_level INT NOT NULL,
    transaction_type VARCHAR(64) NOT NULL,
    reference_id VARCHAR(64),
    timestamp TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS recommendation_audits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(64) NOT NULL,
    suggestion_type VARCHAR(32) NOT NULL,
    strategy_used VARCHAR(64) NOT NULL,
    trigger_reason VARCHAR(64) NOT NULL,
    calculated_price DECIMAL(19, 2),
    calculated_quantity INT,
    system_confidence DOUBLE PRECISION NOT NULL,
    guardrails_applied_json VARCHAR(2000),
    ai_reasoning VARCHAR(2000),
    status VARCHAR(32) NOT NULL,
    decided_by VARCHAR(64),
    timestamp TIMESTAMP NOT NULL
);
