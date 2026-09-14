CREATE TABLE t_outbox
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_type  VARCHAR(100)  NOT NULL,
    aggregate_id    VARCHAR(255)  NOT NULL,
    event_type      VARCHAR(100)  NOT NULL,
    payload         JSON          NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed       BOOLEAN       NOT NULL DEFAULT FALSE,
    processed_at    TIMESTAMP     NULL
);

CREATE INDEX idx_outbox_unprocessed ON t_outbox (processed, created_at);