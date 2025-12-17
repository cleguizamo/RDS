# Guía de Deploy en Railway

Esta guía te ayudará a hacer deploy de la aplicación en Railway.

## 📋 Prerequisitos

1. Cuenta en [Railway](https://railway.app)
2. Git configurado
3. Base de datos MySQL (puede ser la misma de Railway o externa)

## 🚀 Paso 1: Preparar el Backend

### 1.1. Crear el proyecto en Railway

1. Ve a [Railway Dashboard](https://railway.app/dashboard)
2. Click en "New Project"
3. Selecciona "Deploy from GitHub repo" (o "Empty Project" si prefieres usar Railway CLI)

### 1.2. Conectar el repositorio

Si usas GitHub:
- Conecta tu repositorio
- Selecciona la carpeta `app-restaurante` como root del servicio
- Railway detectará automáticamente que es un proyecto Java/Gradle

### 1.3. Configurar variables de entorno

En Railway, ve a tu servicio backend → Variables y agrega:

```bash
# Perfil de producción
SPRING_PROFILES_ACTIVE=prod

# Base de datos (si usas MySQL de Railway)
# Railway proporciona estas variables automáticamente cuando conectas una base de datos
# Variables proporcionadas por Railway:
# - MYSQLHOST
# - MYSQLPORT
# - MYSQLDATABASE
# - MYSQLUSER
# - MYSQLPASSWORD
# Construye DB_URL manualmente:
DB_URL=jdbc:mysql://${MYSQLHOST}:${MYSQLPORT}/${MYSQLDATABASE}?useSSL=true&requireSSL=false&serverTimezone=UTC

# O si tienes las variables ya separadas:
DB_USERNAME=${MYSQLUSER}
DB_PASSWORD=${MYSQLPASSWORD}

# JWT Secret - Genera uno con: openssl rand -base64 64
JWT_SECRET=tu_jwt_secret_fuerte_aqui

# SMTP (Gmail)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=tu-email@gmail.com
SMTP_PASSWORD=tu_app_password_de_gmail

# CORS - URL de tu frontend (actualiza después del deploy del frontend)
CORS_ALLOWED_ORIGINS=https://tu-frontend.vercel.app

# Cloudinary
CLOUDINARY_CLOUD_NAME=tu_cloud_name
CLOUDINARY_API_KEY=tu_api_key
CLOUDINARY_API_SECRET=tu_api_secret
CLOUDINARY_FOLDER=restaurante

# Admin
ADMIN_EMAIL=admin@restaurante.com

# Hibernate (IMPORTANTE: validate en producción)
HIBERNATE_DDL_AUTO=validate
```

### 1.4. Configurar el puerto

Railway usa la variable `PORT` automáticamente. El código ya está configurado para usar `PORT` en lugar de `SERVER_PORT`.

### 1.5. Deploy

Railway detectará automáticamente:
- Tipo de proyecto: Java/Gradle
- Comando de build: `./gradlew build`
- Comando de start: `java -jar build/libs/app-restaurante-0.0.1-SNAPSHOT.jar`

Si necesitas configurarlo manualmente:
- **Build Command**: `./gradlew clean build -x test`
- **Start Command**: `java -jar build/libs/app-restaurante-0.0.1-SNAPSHOT.jar`

### 1.6. Obtener la URL del backend

Una vez desplegado, Railway te dará una URL como:
`https://app-restaurante-production.up.railway.app`

Copia esta URL, la necesitarás para el frontend.

## 🎨 Paso 2: Preparar el Frontend

### 2.1. Actualizar environment.prod.ts

Antes de hacer build, actualiza la URL del API:

```typescript
// frontend-angular/src/environments/environment.prod.ts
export const environment = {
  production: true,
  apiUrl: 'https://tu-backend-url.railway.app/api'
};
```

### 2.2. Build del frontend

```bash
cd frontend-angular
npm install
npm run build
```

### 2.3. Opciones de deploy del frontend

#### Opción A: Vercel (Recomendado)

1. Ve a [Vercel](https://vercel.com)
2. Conecta tu repositorio
3. Configura:
   - **Framework Preset**: Angular
   - **Root Directory**: `frontend-angular`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist/frontend-angular/browser`
   - **Install Command**: `npm install`

4. Agrega variable de entorno:
   - `NG_APP_API_URL` o edita `environment.prod.ts` antes del build

#### Opción B: Netlify

1. Ve a [Netlify](https://netlify.com)
2. Conecta tu repositorio
3. Configura:
   - **Base directory**: `frontend-angular`
   - **Build command**: `npm run build`
   - **Publish directory**: `dist/frontend-angular/browser`

#### Opción C: Railway (Mismo servicio)

Puedes crear un segundo servicio en Railway para el frontend usando el Dockerfile existente.

### 2.4. Actualizar CORS en el backend

Después de obtener la URL del frontend, actualiza la variable `CORS_ALLOWED_ORIGINS` en Railway:

```
CORS_ALLOWED_ORIGINS=https://tu-frontend.vercel.app
```

## 🔒 Paso 3: Configuración de Base de Datos

### Si usas MySQL de Railway:

1. En Railway Dashboard → New → Database → Add MySQL
2. Railway creará automáticamente las variables:
   - `MYSQLHOST`
   - `MYSQLPORT`
   - `MYSQLDATABASE`
   - `MYSQLUSER`
   - `MYSQLPASSWORD`

3. Construye `DB_URL` manualmente:
   ```
   DB_URL=jdbc:mysql://${MYSQLHOST}:${MYSQLPORT}/${MYSQLDATABASE}?useSSL=true&requireSSL=false&serverTimezone=UTC
   ```

### Si usas MySQL externa:

Usa las variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` directamente.

### Ejecutar migraciones

Las migraciones de Flyway se ejecutarán automáticamente al iniciar la aplicación.

## ✅ Paso 4: Verificación

1. **Backend Health Check**:
   ```
   GET https://tu-backend.railway.app/actuator/health
   ```
   Debe responder: `{"status":"UP"}`

2. **Frontend**:
   - Abre tu frontend desplegado
   - Verifica que las peticiones al API funcionen
   - Revisa la consola del navegador para errores de CORS

## 🔧 Solución de Problemas

### Error: "Cannot connect to database"

- Verifica que las variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` estén correctas
- Asegúrate de que la base de datos esté accesible desde Railway
- Si usas MySQL de Railway, verifica que el servicio de BD esté corriendo

### Error: CORS

- Verifica que `CORS_ALLOWED_ORIGINS` incluya la URL exacta del frontend (con https://)
- No incluyas trailing slash en las URLs
- Reinicia el servicio backend después de cambiar variables de entorno

### Error: JWT Invalid

- Verifica que `JWT_SECRET` esté configurado y sea el mismo entre reinicios
- Si cambias el secret, todos los usuarios deberán iniciar sesión de nuevo

### Error: Email no se envía

- Verifica que `SMTP_USERNAME` y `SMTP_PASSWORD` estén correctos
- Para Gmail, asegúrate de usar una "Contraseña de aplicación", no tu contraseña normal
- Genera una nueva contraseña de aplicación en: https://myaccount.google.com/apppasswords

## 📝 Notas Importantes

1. **Nunca subas credenciales al repositorio**: Todas las credenciales deben estar en variables de entorno
2. **Hibernate DDL**: Siempre usa `validate` en producción, nunca `update`
3. **JWT Secret**: Debe ser fuerte y aleatorio, guárdalo en un lugar seguro
4. **CORS**: Limita solo a tus dominios de producción
5. **Logs**: Revisa los logs en Railway para diagnosticar problemas

## 🔄 Actualizaciones Futuras

Para actualizar la aplicación:

1. Haz push a tu repositorio
2. Railway detectará los cambios y desplegará automáticamente
3. El frontend se actualizará según tu plataforma (Vercel/Netlify también tienen auto-deploy)

---

¿Necesitas ayuda? Revisa los logs en Railway Dashboard → Tu Servicio → Deployments → View Logs

