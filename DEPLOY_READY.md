# ✅ Todo listo para desplegar en Railway

## 📦 Cambios realizados y confirmados

- ✅ **Email Service arreglado**: No bloqueará el inicio de la aplicación
- ✅ **Configuración SMTP opcional**: La app funcionará con o sin email
- ✅ **Variables de entorno preparadas**: Todas las variables necesarias documentadas
- ✅ **Dockerfile configurado**: Listo para Railway
- ✅ **railway.json configurado**: Build y deploy configurados
- ✅ **Código subido a GitHub**: Todos los cambios están en el repositorio

## 🚀 Pasos para desplegar en Railway

### 1. Crear proyecto y base de datos en Railway

1. Ve a [Railway Dashboard](https://railway.app/dashboard)
2. Click en **"New Project"**
3. Selecciona **"Deploy from GitHub repo"**
4. Conecta tu repositorio `cleguizamo/RDS`
5. Agrega un servicio **MySQL** (New → Database → MySQL)
6. Agrega un servicio **GitHub Repo** → Selecciona el repositorio → **Root Directory: `app-restaurante`**

### 2. Configurar variables de entorno

Ve a tu servicio backend → **Variables** y agrega estas variables:

#### Variables OBLIGATORIAS:

```bash
SPRING_PROFILES_ACTIVE=prod

# Base de datos (Railway proporciona MYSQLHOST, MYSQLPORT, etc.)
# Haz click en "Reference" en la variable MYSQLHOST para ver cómo referenciarla
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=true&requireSSL=false&serverTimezone=UTC
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}

# JWT Secret (genera uno: openssl rand -base64 64)
JWT_SECRET=TU_JWT_SECRET_AQUI

# Hibernate - IMPORTANTE: usa 'update' para el primer deploy
HIBERNATE_DDL_AUTO=update
```

#### Variables OPCIONALES (para emails):

```bash
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=tu_email@gmail.com
SMTP_PASSWORD=tu_contraseña_de_aplicación
EMAIL_FROM=tu_email@gmail.com
EMAIL_FROM_NAME=Restaurante App
```

#### Variables OPCIONALES (para imágenes Cloudinary):

```bash
CLOUDINARY_CLOUD_NAME=tu_cloud_name
CLOUDINARY_API_KEY=tu_api_key
CLOUDINARY_API_SECRET=tu_api_secret
```

#### Variables OPCIONALES (CORS - configurar después del deploy del frontend):

```bash
CORS_ALLOWED_ORIGINS=https://tu-frontend.railway.app
```

### 3. Desplegar

1. Railway detectará automáticamente el Dockerfile
2. El build comenzará automáticamente
3. Monitorea los logs en Railway para ver el progreso

### 4. Verificar el despliegue

1. Revisa los logs en Railway
2. Busca mensajes como:
   - `Started AppRestauranteApplication`
   - `Tomcat started on port(s): 8080`
3. Si ves errores de base de datos, asegúrate de que `HIBERNATE_DDL_AUTO=update`
4. Una vez que las tablas estén creadas, cambia `HIBERNATE_DDL_AUTO=validate`

### 5. Obtener la URL del backend

1. Ve a tu servicio → **Settings** → **Generate Domain**
2. Railway te dará una URL como: `https://app-restaurante-production.up.railway.app`
3. Copia esta URL para configurar el frontend

### 6. Verificar que funciona

Prueba estos endpoints:

- Health check: `https://tu-backend.railway.app/actuator/health`
- API docs: `https://tu-backend.railway.app/swagger-ui.html`

## 🔍 Troubleshooting

### Si la app no inicia:

1. **Error de base de datos**: Verifica que `DB_URL` esté correctamente construida
2. **Error de tablas faltantes**: Asegúrate de que `HIBERNATE_DDL_AUTO=update` para el primer deploy
3. **Error de JWT_SECRET**: Asegúrate de que esté configurado
4. **Error de email**: Esto ya no debería bloquear el inicio, pero verifica los logs

### Después del primer deploy exitoso:

1. Cambia `HIBERNATE_DDL_AUTO` de `update` a `validate` para seguridad
2. Reinicia el servicio

## 📚 Documentación adicional

- `RAILWAY_DEPLOY.md` - Guía detallada de despliegue
- `RAILWAY_VARIABLES.md` - Referencia completa de variables de entorno
- `RAILWAY_TROUBLESHOOTING.md` - Solución de problemas comunes

## ✨ Listo para desplegar

Todo el código está en GitHub y listo. Solo necesitas:
1. Crear el proyecto en Railway
2. Configurar las variables de entorno
3. Desplegar

¡Buena suerte! 🚀

