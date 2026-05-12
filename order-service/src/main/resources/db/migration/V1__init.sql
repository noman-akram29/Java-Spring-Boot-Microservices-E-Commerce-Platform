CREATE TABLE t_orders
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(255) NOT NULL UNIQUE,
    sku_code VARCHAR(255) NOT NULL,
    price DECIMAL(19,2) NOT NULL,
    quantity INT NOT NULL
);

CREATE INDEX idx_orders_order_number
ON t_orders(order_number);

CREATE INDEX idx_orders_sku_code
ON t_orders(sku_code);