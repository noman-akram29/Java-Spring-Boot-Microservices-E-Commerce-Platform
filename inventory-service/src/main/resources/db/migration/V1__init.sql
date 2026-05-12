CREATE TABLE t_inventory
(
    id BIGINT AUTO_INCREMENT,
    sku_code VARCHAR(255) NOT NULL UNIQUE,
    quantity INT NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_inventory_sku_code
ON t_inventory(sku_code);