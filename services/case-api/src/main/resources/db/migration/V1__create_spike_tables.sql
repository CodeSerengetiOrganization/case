-- Issue 02: store the spike.
-- Tables follow the contract. working_orders and failed_test_items are JSON.
-- failed_test_items_abnormal is the spike-level boolean.
-- Equipment fields on spike_failed_result stay nullable. CASE does not refuse a gap.
-- Delete of a spike row deletes its failed-result rows.
--
-- Identity (do not invent a second id scheme):
--   spike.id                  — CASE auto-increment. The database assigns it.
--   spike_failed_result.id    — copied manufacturing-result id from MES. NOT auto-increment.
-- A re-test of the same serial is a new MES result. MES assigns a new manufacturing-result id.
-- That new id is how CASE tells two tests apart when serial_number matches.
-- The repeat check compares the set of those copied ids. See tech note 03.

CREATE TABLE spike (
  id                          BIGINT        NOT NULL AUTO_INCREMENT,
  product_name                VARCHAR(128)  NOT NULL,
  product_code                VARCHAR(128)  NOT NULL,
  working_orders              JSON          NOT NULL,
  description                 VARCHAR(1024) NOT NULL,
  failed_test_items_abnormal  TINYINT(1)    NOT NULL,
  created_at                  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  INDEX idx_spike_product_name (product_name)
);

CREATE TABLE spike_failed_result (
  spike_id             BIGINT        NOT NULL,
  -- Copied MES manufacturing-result id. Not AUTO_INCREMENT. Not a live MES FK.
  id                   BIGINT        NOT NULL,
  serial_number        VARCHAR(128)  NULL,
  product_type         VARCHAR(128)  NULL,
  mo_id                VARCHAR(128)  NULL,
  op_code              VARCHAR(64)   NULL,
  station_id           VARCHAR(128)  NULL,
  rack_id              VARCHAR(128)  NULL,
  fixture_id           VARCHAR(128)  NULL,
  nest_number          INT           NULL,
  overall_result       VARCHAR(32)   NULL,
  cycle_time_seconds   INT           NULL,
  sw_version           VARCHAR(64)   NULL,
  hw_revision          VARCHAR(64)   NULL,
  failed_test_items    JSON          NULL,
  error_code           VARCHAR(128)  NULL,
  operator_id          VARCHAR(128)  NULL,
  shift_code           VARCHAR(64)   NULL,
  created_at           DATETIME(3)   NULL,
  PRIMARY KEY (spike_id, id),
  CONSTRAINT fk_spike_failed_result_spike
    FOREIGN KEY (spike_id) REFERENCES spike (id)
    ON DELETE CASCADE
);

CREATE TABLE spike_product_lock (
  product_name  VARCHAR(128)  NOT NULL,
  created_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (product_name)
);
