# Variables de Entorno para Railway

## 🔐 Variables Requeridas (Críticas)

Estas variables son **OBLIGATORIAS** para que la aplicación funcione:

```bash
# Perfil de producción
SPRING_PROFILES_ACTIVE=prod

# Base de datos MySQL
# Railway proporciona automáticamente cuando conectas MySQL:
# - MYSQLHOST
# - MYSQLPORT
# - MYSQLDATABASE
# - MYSQLUSER
# - MYSQLPASSWORD
# Construye DB_URL manualmente:
DB_URL=jdbc:mysql://${MYSQLHOST}:${MYSQLPORT}/${MYSQLDATABASE}?useSSL=true&requireSSL=false&serverTimezone=UTC
DB_USERNAME=${MYSQLUSER}
DB_PASSWORD=${MYSQLPASSWORD}

# JWT Secret (genera uno fuerte)
JWT_SECRET=tu_jwt_secret_muy_largo_y_seguro_aqui
JWT_EXPIRATION=86400000

# Hibernate (para crear tablas en el primer deploy)
HIBERNATE_DDL_AUTO=update
# Después del primer deploy exitoso, cámbialo a:
# HIBERNATE_DDL_AUTO=validate
```

## 📧 Variables Opcionales (Email)

El servicio de email ahora es **opcional**. Si no configuras estas variables, la app funcionará pero NO enviará emails:

```bash
# SMTP Configuration (opcional)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=tu_email@gmail.com
SMTP_PASSWORD=tu_contraseña_de_aplicación_gmail
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_STARTTLS_REQUIRED=false
EMAIL_FROM=tu_email@gmail.com
EMAIL_FROM_NAME=Restaurante App
```

**Nota sobre Gmail:**
- Necesitas una "Contraseña de aplicación" de Google
- Genera una en: https://myaccount.google.com/apppasswords
- No uses tu contraseña normal de Gmail

## ☁️ Variables Opcionales (Cloudinary - Imágenes)

Si quieres subir imágenes, configura Cloudinary:

```bash
CLOUDINARY_CLOUD_NAME=tu_cloud_name
CLOUDINARY_API_KEY=tu_api_key
CLOUDINARY_API_SECRET=tu_api_secret
```

## 🌐 Variables Opcionales (CORS)

Si tu frontend está en un dominio diferente:

```bash
CORS_ALLOWED_ORIGINS=https://tu-frontend.railway.app,https://tu-otro-dominio.com
```

## ✅ Checklist de Variables Mínimas

Mínimo necesitas estas variables para que la app inicie:

- [x] `SPRING_PROFILES_ACTIVE=prod`
- [x] `DB_URL` (construida desde variables MySQL de Railway)
- [x] `DB_USERNAME` (desde MYSQLUSER)
- [x] `DB_PASSWORD` (desde MYSQLPASSWORD)
- [x] `JWT_SECRET` (genera uno nuevo)
- [x] `HIBERNATE_DDL_AUTO=update` (para el primer deploy)

## 🚨 Importante sobre HIBERNATE_DDL_AUTO

1. **Primer deploy:** Usa `HIBERNATE_DDL_AUTO=update` para crear las tablas
2. **Después del primer deploy exitoso:** Cambia a `HIBERNATE_DDL_AUTO=validate` para seguridad

## 📝 Cómo Generar JWT_SECRET

En tu terminal local:
```bash
openssl rand -base64 64
```

Copia el resultado y úsalo como `JWT_SECRET`.

