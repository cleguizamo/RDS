# 🔍 Verificar que el Backend esté Funcionando

## Paso 1: Probar el Health Check

Abre en tu navegador:
```
https://rds-production.up.railway.app/actuator/health
```

**Deberías ver:**
- Si funciona: `{"status":"UP"}` o similar (JSON)
- Si no funciona: Error 404 o conexión rechazada

## Paso 2: Probar endpoint de login directamente

Abre la consola del navegador (F12) en cualquier página y ejecuta:

```javascript
fetch('https://rds-production.up.railway.app/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    email: 'test@test.com',
    password: 'test123'
  })
})
.then(r => r.json())
.then(data => console.log('Respuesta:', data))
.catch(error => console.error('Error:', error))
```

**Resultados esperados:**
- ✅ Si ves una respuesta JSON (aunque sea un error de autenticación): El endpoint existe y funciona
- ❌ Si ves 404: El endpoint no existe o la ruta está mal
- ❌ Si ves CORS error: Necesitas configurar CORS_ALLOWED_ORIGINS

## Paso 3: Verificar que el backend esté corriendo

En Railway:
1. Ve a tu servicio backend
2. Revisa los logs
3. Deberías ver mensajes como "Started AppRestauranteApplication"
4. Si ves errores, cópialos para diagnosticar

## Posibles Problemas

### 1. Backend no está corriendo
**Síntoma**: No puedes acceder a `/actuator/health`
**Solución**: Verifica los logs en Railway y asegúrate de que el servicio esté "Running"

### 2. Endpoint no existe
**Síntoma**: 404 en `/api/auth/login` pero el health check funciona
**Solución**: Verifica que el controlador esté correctamente mapeado

### 3. Problema de CORS
**Síntoma**: Error de CORS en la consola
**Solución**: Configura `CORS_ALLOWED_ORIGINS=https://rincondelsaborgaragoa.netlify.app` en Railway

