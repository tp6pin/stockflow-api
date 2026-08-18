-- Order and Inventory Management System
-- MySQL 8.0+
-- This script initializes an empty database. It does not drop existing data.


-- =========================================================
-- Security
-- =========================================================

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    description VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE = InnoDB;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, role_id),
    KEY idx_user_roles_role_id (role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE = InnoDB;

-- =========================================================
-- Master Data
-- =========================================================

CREATE TABLE customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_code VARCHAR(30) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    tax_id VARCHAR(20),
    contact_name VARCHAR(50),
    phone VARCHAR(30),
    email VARCHAR(100),
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_customers_code UNIQUE (customer_code),
    CONSTRAINT uk_customers_tax_id UNIQUE (tax_id),
    KEY idx_customers_company_name (company_name),
    KEY idx_customers_active (active)
) ENGINE = InnoDB;

CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_categories_name UNIQUE (name),
    KEY idx_categories_active (active)
) ENGINE = InnoDB;

CREATE TABLE suppliers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_code VARCHAR(30) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    tax_id VARCHAR(20),
    contact_name VARCHAR(50),
    phone VARCHAR(30),
    email VARCHAR(100),
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_suppliers_code UNIQUE (supplier_code),
    CONSTRAINT uk_suppliers_tax_id UNIQUE (tax_id),
    KEY idx_suppliers_company_name (company_name),
    KEY idx_suppliers_active (active)
) ENGINE = InnoDB;

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    category_id BIGINT NOT NULL,
    unit VARCHAR(20) NOT NULL,
    cost DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    safety_stock INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_products_sku UNIQUE (sku),
    KEY idx_products_name (name),
    KEY idx_products_category_id (category_id),
    KEY idx_products_active (active),

    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT chk_products_cost CHECK (cost >= 0),
    CONSTRAINT chk_products_price CHECK (price >= 0),
    CONSTRAINT chk_products_safety_stock CHECK (safety_stock >= 0)
) ENGINE = InnoDB;

-- =========================================================
-- Inventory
-- =========================================================

CREATE TABLE inventory_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    batch_number VARCHAR(50) NOT NULL,
    quantity_on_hand INT NOT NULL DEFAULT 0,
    quantity_reserved INT NOT NULL DEFAULT 0,
    received_date DATE NOT NULL,
    manufacture_date DATE,
    expiration_date DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_inventory_batches_product_batch
        UNIQUE (product_id, batch_number),
    KEY idx_batches_product_expiration (product_id, expiration_date),
    KEY idx_batches_supplier_id (supplier_id),
    KEY idx_batches_expiration_date (expiration_date),

    CONSTRAINT fk_inventory_batches_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT fk_inventory_batches_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT chk_inventory_batches_on_hand
        CHECK (quantity_on_hand >= 0),

    CONSTRAINT chk_inventory_batches_reserved
        CHECK (quantity_reserved >= 0),

    CONSTRAINT chk_inventory_batches_available
        CHECK (quantity_reserved <= quantity_on_hand),

    CONSTRAINT chk_inventory_batches_dates
        CHECK (
            manufacture_date IS NULL
            OR manufacture_date <= expiration_date
        )
) ENGINE = InnoDB;

CREATE TABLE inventory_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    on_hand_change INT NOT NULL DEFAULT 0,
    reserved_change INT NOT NULL DEFAULT 0,
    on_hand_after INT NOT NULL,
    reserved_after INT NOT NULL,
    reference_type VARCHAR(30),
    reference_id BIGINT,
    note VARCHAR(255),
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    KEY idx_inventory_transactions_product_id (product_id),
    KEY idx_inventory_transactions_batch_id (batch_id),
    KEY idx_inventory_transactions_type (transaction_type),
    KEY idx_inventory_transactions_created_at (created_at),
    KEY idx_inventory_transactions_reference (reference_type, reference_id),
    KEY idx_inventory_transactions_created_by (created_by),

    CONSTRAINT fk_inventory_transactions_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT fk_inventory_transactions_batch
        FOREIGN KEY (batch_id) REFERENCES inventory_batches (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT fk_inventory_transactions_user
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE SET NULL,

    CONSTRAINT chk_inventory_transactions_type
        CHECK (
            transaction_type IN (
                'INBOUND',
                'RESERVE',
                'RELEASE',
                'SHIPMENT',
                'ADJUSTMENT',
                'RETURN',
                'EXPIRED'
            )
        ),

    CONSTRAINT chk_inventory_transactions_after
        CHECK (
            on_hand_after >= 0
            AND reserved_after >= 0
            AND reserved_after <= on_hand_after
        )
) ENGINE = InnoDB;

-- =========================================================
-- Sales Orders
-- =========================================================

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(30) NOT NULL,
    customer_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    order_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expected_delivery_date DATE,
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    note VARCHAR(500),
    confirmed_at DATETIME,
    processing_at DATETIME,
    shipped_at DATETIME,
    completed_at DATETIME,
    cancelled_at DATETIME,
    created_by BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_orders_order_number UNIQUE (order_number),
    KEY idx_orders_customer_id (customer_id),
    KEY idx_orders_status (status),
    KEY idx_orders_order_date (order_date),
    KEY idx_orders_created_by (created_by),

    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT fk_orders_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT chk_orders_status
        CHECK (
            status IN (
                'DRAFT',
                'CONFIRMED',
                'PROCESSING',
                'SHIPPED',
                'COMPLETED',
                'CANCELLED'
            )
        ),

    CONSTRAINT chk_orders_amounts
        CHECK (
            subtotal >= 0
            AND tax_amount >= 0
            AND total_amount >= 0
        )
) ENGINE = InnoDB;

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    line_amount DECIMAL(12, 2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_order_items_order_product UNIQUE (order_id, product_id),
    KEY idx_order_items_product_id (product_id),

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_order_items_line_amount CHECK (line_amount >= 0)
) ENGINE = InnoDB;

CREATE TABLE order_item_allocations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    allocated_quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    allocated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at DATETIME,
    shipped_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_allocations_order_item_batch
        UNIQUE (order_item_id, batch_id),
    KEY idx_allocations_batch_id (batch_id),
    KEY idx_allocations_status (status),

    CONSTRAINT fk_allocations_order_item
        FOREIGN KEY (order_item_id) REFERENCES order_items (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT fk_allocations_batch
        FOREIGN KEY (batch_id) REFERENCES inventory_batches (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT chk_allocations_quantity CHECK (allocated_quantity > 0),

    CONSTRAINT chk_allocations_status
        CHECK (status IN ('ACTIVE', 'RELEASED', 'SHIPPED'))
) ENGINE = InnoDB;

-- =========================================================
-- Shipments
-- MVP rule: one order has at most one shipment.
-- =========================================================

CREATE TABLE shipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_number VARCHAR(30) NOT NULL,
    order_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PREPARING',
    carrier VARCHAR(50),
    tracking_number VARCHAR(100),
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(30),
    shipping_address VARCHAR(255) NOT NULL,
    shipped_at DATETIME,
    delivered_at DATETIME,
    note VARCHAR(500),
    created_by BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_shipments_number UNIQUE (shipment_number),
    CONSTRAINT uk_shipments_order UNIQUE (order_id),
    CONSTRAINT uk_shipments_tracking UNIQUE (tracking_number),
    KEY idx_shipments_status (status),
    KEY idx_shipments_created_at (created_at),
    KEY idx_shipments_shipped_at (shipped_at),
    KEY idx_shipments_created_by (created_by),

    CONSTRAINT fk_shipments_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT fk_shipments_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT chk_shipments_status
        CHECK (
            status IN (
                'PREPARING',
                'SHIPPED',
                'DELIVERED',
                'FAILED',
                'CANCELLED'
            )
        )
) ENGINE = InnoDB;

CREATE TABLE shipment_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    allocation_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_shipment_items_allocation UNIQUE (allocation_id),
    KEY idx_shipment_items_shipment_id (shipment_id),

    CONSTRAINT fk_shipment_items_shipment
        FOREIGN KEY (shipment_id) REFERENCES shipments (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT fk_shipment_items_allocation
        FOREIGN KEY (allocation_id) REFERENCES order_item_allocations (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT chk_shipment_items_quantity CHECK (quantity > 0)
) ENGINE = InnoDB;

-- =========================================================
-- Initial roles
-- =========================================================

INSERT IGNORE INTO roles (name, description)
VALUES
    ('ADMIN', 'System and account management'),
    ('MANAGER', 'Product management, dashboards and exception handling'),
    ('SALES', 'Customer and order management'),
    ('WAREHOUSE', 'Inbound, inventory and shipment management');
