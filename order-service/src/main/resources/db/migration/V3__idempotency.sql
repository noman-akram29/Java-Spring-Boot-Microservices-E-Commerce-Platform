CREATE TABLE t_idempotency
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    idempotency_key  VARCHAR(128) NOT NULL,
    order_number     VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_idempotency_order_number ON t_idempotency (order_number);