-- Agregar campos para código de recuperación de contraseña de 8 dígitos
ALTER TABLE users 
  ADD COLUMN reset_password_code VARCHAR(8) NULL,
  ADD COLUMN reset_password_code_expiry DATETIME NULL;

-- Si existían los campos antiguos de token, eliminarlos (opcional)
-- ALTER TABLE users 
--   DROP COLUMN IF EXISTS reset_password_token,
--   DROP COLUMN IF EXISTS reset_password_token_expiry;

