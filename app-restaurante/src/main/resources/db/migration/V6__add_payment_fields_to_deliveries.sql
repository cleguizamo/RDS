ALTER TABLE deliveries
  ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  ADD COLUMN payment_method VARCHAR(20) NULL,
  ADD COLUMN payment_proof_url VARCHAR(1024) NULL,
  ADD COLUMN verified_by BIGINT NULL,
  ADD COLUMN verified_at DATETIME NULL;

ALTER TABLE deliveries
  ADD CONSTRAINT fk_delivery_verified_by FOREIGN KEY (verified_by) REFERENCES admins (id);

