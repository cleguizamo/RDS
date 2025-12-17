# ✅ Checklist de Deploy en Railway

Usa esta lista para asegurarte de que todo esté configurado correctamente antes y después del deploy.

## 📋 Antes del Deploy

### Seguridad
- [x] Removidas todas las credenciales de `application.yml`
- [x] Creado `application-prod.yml` para producción
- [x] Actualizado `SecurityConfig` para CORS en producción
- [x] Generado JWT secret fuerte
- [x] Verificado `.gitignore` para excluir archivos sensibles
- [ ] **TU TAREA**: Cambiar el JWT_SECRET generado por uno nuevo y único
- [ ] **TU TAREA**: Verificar que ninguna credencial esté en el código antes de hacer push

### Configuración
- [x] `application.yml` usa solo variables de entorno
- [x] `HIBERNATE_DDL_AUTO` configurado como `validate` en producción
- [x] Puerto configurado para usar `PORT` (Railway)
- [x] Frontend `environment.prod.ts` preparado (actualizar URL después)

### Documentación
- [x] Creado `RAILWAY_DEPLOY.md` con guía completa
- [x] Creado `ENV_VARIABLES.md` con lista de variables
- [x] Creado este checklist

## 🚀 Durante el Deploy

### Backend en Railway
- [ ] Crear nuevo proyecto en Railway
- [ ] Conectar repositorio de GitHub (o subir código)
- [ ] Configurar root directory como `app-restaurante` (si aplica)
- [ ] Agregar todas las variables de entorno de `ENV_VARIABLES.md`
- [ ] Configurar base de datos MySQL (si usas Railway DB)
- [ ] Verificar que Railway detecte el proyecto como Java/Gradle
- [ ] Iniciar el deploy
- [ ] Esperar a que el build complete exitosamente
- [ ] Copiar la URL pública del backend

### Frontend
- [ ] Actualizar `environment.prod.ts` con la URL del backend
- [ ] Hacer build local para verificar: `npm run build`
- [ ] Elegir plataforma de deploy (Vercel recomendado)
- [ ] Configurar variables de entorno si es necesario
- [ ] Hacer deploy
- [ ] Copiar la URL pública del frontend

### Actualizar CORS
- [ ] Agregar URL del frontend a `CORS_ALLOWED_ORIGINS` en Railway
- [ ] Reiniciar el servicio backend en Railway

## ✅ Después del Deploy

### Verificación Backend
- [ ] Health check funciona: `GET https://tu-backend.railway.app/actuator/health`
- [ ] Respuesta: `{"status":"UP"}`
- [ ] Logs no muestran errores críticos
- [ ] Base de datos conectada correctamente
- [ ] Migraciones ejecutadas (verificar en logs)

### Verificación Frontend
- [ ] Frontend carga correctamente
- [ ] Puede conectarse al backend (verificar en DevTools → Network)
- [ ] No hay errores de CORS en la consola
- [ ] Login funciona correctamente
- [ ] Navegación funciona

### Verificación Funcional
- [ ] Crear un usuario de prueba
- [ ] Iniciar sesión
- [ ] Verificar que las peticiones al API funcionen
- [ ] Probar funcionalidades principales
- [ ] Verificar que los emails se envíen (si aplica)

### Seguridad Final
- [ ] Verificar que ninguna credencial esté expuesta en logs públicos
- [ ] Confirmar que HTTPS está activo (Railway lo hace automáticamente)
- [ ] Verificar que CORS solo permita tu dominio
- [ ] Revisar que actuator endpoints estén protegidos

## 🔧 Si algo sale mal

### Backend no inicia
1. Revisar logs en Railway Dashboard
2. Verificar variables de entorno
3. Verificar conexión a base de datos
4. Verificar que el JAR se haya construido correctamente

### Frontend no se conecta al backend
1. Verificar URL en `environment.prod.ts`
2. Verificar CORS en Railway
3. Revisar consola del navegador para errores específicos
4. Verificar que el backend esté corriendo

### Base de datos no conecta
1. Verificar variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
2. Si usas Railway DB, verificar que el servicio esté activo
3. Verificar que la URL de conexión sea correcta
4. Revisar logs para mensajes de error específicos

## 📝 Notas Finales

- **JWT_SECRET**: Debe ser único y fuerte. Si lo cambias después del deploy, todos los usuarios deberán iniciar sesión de nuevo.
- **Base de datos**: Las migraciones se ejecutan automáticamente al iniciar la aplicación.
- **CORS**: Asegúrate de incluir tanto `http://` como `https://` si es necesario, pero en producción deberías usar solo `https://`.
- **Logs**: Revisa regularmente los logs en Railway para detectar problemas temprano.

---

✅ Una vez completado todo, tu aplicación estará lista para producción!

