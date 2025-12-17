# Variables de Entorno Requeridas

Esta es una lista completa de todas las variables de entorno necesarias para el deploy en Railway.

## 🔑 Variables Críticas (Obligatorias)

```bash
# Perfil de Spring
SPRING_PROFILES_ACTIVE=prod

# Base de Datos
DB_URL=jdbc:mysql://host:port/database?useSSL=true&requireSSL=false&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=tu_password_aqui

# JWT Secret (Generar con: openssl rand -base64 64)
JWT_SECRET=VF1UclIlthxW35+eCePBzvwELjv03oRP5bZ//jr582Irjcm0SHsK4qD1T5xIhR/16J/ksPBfdWy5ctL7P0cH5w==

# CORS - URL de tu frontend
CORS_ALLOWED_ORIGINS=https://tu-frontend.vercel.app

# Cloudinary
CLOUDINARY_CLOUD_NAME=drp8os7tp
CLOUDINARY_API_KEY=446264682988843
CLOUDINARY_API_SECRET=eA6l0j6rJAofqFPBVevQLDrjwMY
```

## 📧 Variables de Email (Obligatorias si usas notificaciones por email)

```bash
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=cristianleguizamo31@gmail.com
SMTP_PASSWORD=kenglyappspckzep


```

**Nota**: Para Gmail, necesitas generar una "Contraseña de aplicación" en:
https://myaccount.google.com/apppasswords

## 🔧 Variables Opcionales (Con valores por defecto)

```bash
# Puerto (Railway lo establece automáticamente)
PORT=8080

# Hibernate DDL (validate en producción)
HIBERNATE_DDL_AUTO=validate

# Admin email
ADMIN_EMAIL=cristianleguizamo31@gmail.com

# Cloudinary folder
CLOUDINARY_FOLDER=restaurante

# Logging
LOG_LEVEL=INFO
APP_LOG_LEVEL=INFO
```

## 🚨 Importante

1. **NUNCA** subas estas variables con valores reales al repositorio
2. Configura todas estas variables en Railway Dashboard → Variables
3. El JWT_SECRET generado arriba es solo un ejemplo, genera uno nuevo y único para tu aplicación
4. Guarda todas las credenciales en un lugar seguro (gestor de contraseñas)

## 📋 Para Railway con MySQL

Si usas MySQL de Railway, Railway proporciona automáticamente:
- `MYSQLHOST`
- `MYSQLPORT`
- `MYSQLDATABASE`
- `MYSQLUSER`
- `MYSQLPASSWORD`

Puedes construir `DB_URL` así:
```
DB_URL=jdbc:mysql://${MYSQLHOST}:${MYSQLPORT}/${MYSQLDATABASE}?useSSL=true&requireSSL=false&serverTimezone=UTC
DB_USERNAME=${MYSQLUSER}
DB_PASSWORD=${MYSQLPASSWORD}
```

