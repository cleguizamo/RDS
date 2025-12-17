# Solución de Problemas de Deploy en Railway

## Error: "Error creating build plan with Railpack"

Este error ocurre cuando Railway no puede detectar automáticamente cómo construir tu proyecto.

### Solución Implementada

He creado `app-restaurante/railway.json` que le dice explícitamente a Railway:
- Usar el Dockerfile para construir
- Comando de inicio correcto
- Health check path

### Pasos Adicionales en Railway Dashboard

Si el error persiste, verifica en Railway:

1. **Settings → Build & Deploy**
   - ✅ **Builder**: Debe estar en "Dockerfile" (no "Nixpacks" o "Railpack")
   - ✅ **Dockerfile Path**: `Dockerfile`
   - ✅ **Build Command**: (dejar vacío - el Dockerfile maneja el build)
   - ✅ **Start Command**: (dejar vacío - el Dockerfile tiene ENTRYPOINT)

2. **Settings → Root Directory**
   - Si tu servicio está en la raíz del repo: dejar vacío
   - Si creaste el servicio desde la carpeta `app-restaurante`: debe estar vacío o ser `app-restaurante`

3. **Settings → Environment**
   - Verifica que todas las variables de entorno estén configuradas
   - Ver `ENV_VARIABLES.md` para la lista completa

### Alternativa: Usar Nixpacks (si Dockerfile no funciona)

Si el Dockerfile causa problemas, puedes configurar Railway para usar Nixpacks:

1. En Railway Dashboard → Settings → Build & Deploy
2. Cambiar **Builder** a "Nixpacks"
3. Configurar:
   - **Build Command**: `./gradlew clean build -x test`
   - **Start Command**: `java -jar build/libs/app-restaurante-0.0.1-SNAPSHOT.jar`
   - **Root Directory**: `app-restaurante` (si el servicio está en la raíz)

## Error: "Cannot find Dockerfile"

**Causa**: Railway está buscando el Dockerfile en el directorio incorrecto.

**Solución**:
- Verifica que el Dockerfile esté en `app-restaurante/Dockerfile`
- Verifica que el Root Directory en Railway esté configurado correctamente
- Si el servicio está en la raíz, Railway buscará el Dockerfile en la raíz, no en `app-restaurante`

## Error: "Port already in use" o "Port binding failed"

**Causa**: Railway espera que la aplicación escuche en el puerto definido por la variable `PORT`.

**Solución**:
- Tu `application.yml` ya está configurado para usar `${PORT:8080}`
- Railway establece automáticamente la variable `PORT`
- Verifica que no tengas `SERVER_PORT` configurado (usa solo `PORT`)

## Error: "Database connection failed"

**Causa**: Variables de base de datos incorrectas o base de datos no conectada.

**Solución**:
1. Si usas MySQL de Railway:
   - Asegúrate de que el servicio de MySQL esté creado
   - Railway proporciona variables automáticamente: `MYSQLHOST`, `MYSQLPORT`, etc.
   - Construye `DB_URL` manualmente: `jdbc:mysql://${MYSQLHOST}:${MYSQLPORT}/${MYSQLDATABASE}?useSSL=true&requireSSL=false&serverTimezone=UTC`

2. Si usas MySQL externa:
   - Verifica `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
   - Asegúrate de que la base de datos permita conexiones desde Railway

## Verificación Rápida

Después del deploy, verifica:

1. **Health Check**:
   ```bash
   curl https://tu-backend.railway.app/actuator/health
   ```
   Debe responder: `{"status":"UP"}`

2. **Logs**:
   - Railway Dashboard → Tu Servicio → View Logs
   - Busca errores en rojo
   - Verifica que veas "Started AppRestauranteApplication"

3. **Variables de Entorno**:
   - Railway Dashboard → Variables
   - Verifica que todas estén configuradas
   - Especialmente `SPRING_PROFILES_ACTIVE=prod`

## Contacto y Recursos

- Railway Docs: https://docs.railway.app
- Railway Community: https://discord.gg/railway

