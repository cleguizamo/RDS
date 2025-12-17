# 🔧 Solución: Frontend no conectado al Backend en Netlify

## 🔍 Diagnóstico

Si tu frontend desplegado en Netlify no se conecta al backend, el problema más común es **CORS** (Cross-Origin Resource Sharing).

## ✅ Solución: Configurar CORS en Railway

### Paso 1: Obtener la URL de tu sitio en Netlify

1. Ve a tu dashboard de Netlify
2. Selecciona tu sitio
3. Copia la URL que aparece (ejemplo: `https://tu-sitio-123.netlify.app`)

### Paso 2: Agregar la URL a CORS en Railway

1. Ve a **Railway Dashboard** → Tu servicio backend → **Variables**
2. Busca o crea la variable `CORS_ALLOWED_ORIGINS`
3. Agrega la URL de Netlify (con comas si hay múltiples orígenes):

```bash
CORS_ALLOWED_ORIGINS=https://tu-sitio.netlify.app
```

Si ya tienes otros orígenes, sepáralos con comas:
```bash
CORS_ALLOWED_ORIGINS=https://tu-sitio.netlify.app,https://otro-dominio.com
```

4. **Guarda** los cambios
5. Railway reiniciará automáticamente el servicio con la nueva configuración

### Paso 3: Si tienes dominio personalizado

Si configuraste un dominio personalizado en Netlify (ej: `app.tudominio.com`), también agrégalo:

```bash
CORS_ALLOWED_ORIGINS=https://tu-sitio.netlify.app,https://app.tudominio.com,https://www.app.tudominio.com
```

### Paso 4: Verificar que funciona

1. Abre tu sitio en Netlify
2. Abre la consola del navegador (F12 → Console)
3. Intenta hacer login o cualquier acción que requiera el API
4. Si ves errores de CORS, verifica que la URL en `CORS_ALLOWED_ORIGINS` coincida exactamente (incluyendo `https://`)

## 🐛 Errores comunes

### Error: "Access to fetch at '...' from origin '...' has been blocked by CORS policy"

**Solución**: La URL del frontend no está en `CORS_ALLOWED_ORIGINS`. Asegúrate de:
- Incluir `https://` al inicio
- No incluir la barra final `/` al final
- Coincidir exactamente con la URL que aparece en el error

### Error: "Network Error" o "Failed to fetch"

**Posibles causas**:
1. La URL del backend es incorrecta → Verifica `environment.prod.ts`
2. El backend no está corriendo → Verifica los logs de Railway
3. Problemas de red → Espera unos minutos y reintenta

### El frontend carga pero no hace peticiones

**Verificar**:
1. Abre la consola del navegador (F12)
2. Ve a la pestaña "Network"
3. Intenta hacer una acción (login, etc.)
4. Verifica que las peticiones se hagan a: `https://rds-production.up.railway.app/api/...`

## 🔍 Verificar configuración

### 1. Verificar URL del backend en el código

Abre `environment.prod.ts` y verifica que tenga:
```typescript
apiUrl: 'https://rds-production.up.railway.app/api'
```

### 2. Verificar en el navegador

1. Abre tu sitio en Netlify
2. Abre DevTools (F12)
3. Ve a la pestaña "Network"
4. Intenta hacer login
5. Deberías ver peticiones a `https://rds-production.up.railway.app/api/auth/login`

### 3. Verificar CORS en el backend

En Railway, verifica que la variable esté configurada:
- Variable: `CORS_ALLOWED_ORIGINS`
- Valor: `https://tu-url-de-nlify.netlify.app`

## 📝 Checklist

- [ ] URL del backend correcta en `environment.prod.ts`: `https://rds-production.up.railway.app/api`
- [ ] Variable `CORS_ALLOWED_ORIGINS` configurada en Railway con la URL de Netlify
- [ ] Railway reinició después de cambiar las variables
- [ ] Probado en el navegador con la consola abierta para ver errores

## 💡 Nota

Después de cambiar `CORS_ALLOWED_ORIGINS` en Railway, el servicio se reinicia automáticamente. Espera 1-2 minutos y prueba de nuevo.

