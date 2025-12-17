-- Agregar campos de pago a la tabla orders
ALTER TABLE orders 
ADD COLUMN payment_status VARCHAR(50),
ADD COLUMN payment_method VARCHAR(50),
ADD COLUMN payment_proof_url VARCHAR(500),
ADD COLUMN verified_by BIGINT,
ADD COLUMN verified_at DATETIME;

-- Agregar foreign key para verified_by
ALTER TABLE orders
ADD CONSTRAINT fk_order_verified_by 
FOREIGN KEY (verified_by) REFERENCES admins(id);

-- Establecer valores por defecto para órdenes existentes
UPDATE orders 
SET payment_status = 'PENDING' 
WHERE payment_status IS NULL;

