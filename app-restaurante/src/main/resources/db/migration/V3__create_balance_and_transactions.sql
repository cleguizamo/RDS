-- Migración para crear las tablas de balance, transacciones y alertas
-- Esta migración crea el sistema de gestión de balance y caja

-- Tabla de Balance
CREATE TABLE IF NOT EXISTS balance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    current_balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    low_balance_threshold DECIMAL(15, 2) NOT NULL DEFAULT 100000.00,
    last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_last_updated (last_updated)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de Transacciones
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    balance_before DECIMAL(15, 2) NOT NULL,
    balance_after DECIMAL(15, 2) NOT NULL,
    description VARCHAR(500) NOT NULL,
    reference_id BIGINT NULL,
    reference_type VARCHAR(50) NULL,
    notes VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_transaction_type (transaction_type),
    INDEX idx_created_at (created_at),
    INDEX idx_reference (reference_type, reference_id),
    INDEX idx_created_at_desc (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de Alertas
CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    message VARCHAR(1000) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    INDEX idx_status (status),
    INDEX idx_alert_type (alert_type),
    INDEX idx_created_at (created_at DESC),
    INDEX idx_status_type (status, alert_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Actualizar tabla salary_payments para agregar campos de estado
-- Nota: Si las columnas ya existen, esta migración puede fallar.
-- En ese caso, ejecuta manualmente solo las líneas que necesites.

-- Verificar si las columnas existen antes de agregarlas (requiere procedimiento almacenado o verificación manual)
-- Por simplicidad, asumimos que las columnas no existen aún

-- Para producción, se recomienda verificar primero si las columnas existen:
-- SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'salary_payments' AND COLUMN_NAME = 'status';

-- Agregar columna status si no existe
SET @dbname = DATABASE();
SET @tablename = 'salary_payments';
SET @columnname = 'status';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(20) NOT NULL DEFAULT ''PENDING'' AFTER payment_frequency')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Agregar columna processed_at si no existe
SET @columnname = 'processed_at';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' DATETIME NULL AFTER status')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Agregar columna failure_reason si no existe
SET @columnname = 'failure_reason';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(500) NULL AFTER processed_at')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Agregar índices si no existen
-- MySQL 8.0.16+ soporta IF NOT EXISTS para índices
-- Para versiones anteriores, estos comandos pueden fallar si los índices ya existen, lo cual es aceptable
-- Verificar existencia de índice status
SET @indexname = 'idx_status';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (INDEX_NAME = @indexname)
  ) > 0,
  'SELECT 1',
  CONCAT('CREATE INDEX ', @indexname, ' ON ', @tablename, '(status)')
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

-- Verificar existencia de índice payment_date_status
SET @indexname = 'idx_payment_date_status';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (INDEX_NAME = @indexname)
  ) > 0,
  'SELECT 1',
  CONCAT('CREATE INDEX ', @indexname, ' ON ', @tablename, '(payment_date, status)')
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;

-- Nota: Los valores permitidos para los enums se manejan a nivel de aplicación
-- transaction_type: INCOME, EXPENSE, SALARY_PAYMENT, ADJUSTMENT, REFUND
-- alert_type: LOW_BALANCE, BALANCE_THRESHOLD, PENDING_PAYMENTS
-- alert_status: ACTIVE, RESOLVED, DISMISSED
-- payment_status: PENDING, PAID, FAILED, CANCELLED

