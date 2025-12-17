# 🔍 Diagnóstico Completo: Frontend-Backend sin conexión

## Paso 1: Verificar que el frontend use la URL correcta

### A. En el navegador:
1. Abre: https://rincondelsaborgaragoa.netlify.app
2. Presiona **F12** → Pestaña **"Network"**
3. Limpia las peticiones (icono 🚫)
4. Intenta hacer login o ver el menú
5. Busca peticiones que fallen (aparecen en rojo)

**¿Qué URL aparece en las peticiones?**
- ✅ Correcto: `https://rds-production.up.railway.app/api/...`
- ❌ Incorrecto: `http://localhost:8080/api/...` → El build no está usando producción

### B. Verificar en la consola:
Presiona **F12** → Pestaña **"Console"** y ejecuta:

```javascript
// Verificar qué URL está usando
console.log('API URL:', window.location.origin);

// Probar conexión directa
fetch('https://rds-production.up.railway.app/api/public/categories')
  .then(r => {
    console.log('Status:', r.status);
    return r.json();
  })
  .then(data => console.log('✅ Backend responde:', data))
  .catch(error => console.error('❌ Error:', error));
```

**Resultados posibles:**
- Si ves datos: ✅ Backend funciona, el problema es en el frontend
- Si ves 404: El endpoint no existe
- Si ves CORS error: Problema de CORS
- Si ves "Failed to fetch": Backend no responde o hay problema de red

---

## Paso 2: Verificar CORS en Railway

1. Ve a **Railway** → Tu servicio backend → **Variables**
2. Verifica que exista: `CORS_ALLOWED_ORIGINS`
3. Debe tener exactamente:
   ```
   https://rincondelsaborgaragoa.netlify.app
   ```
4. **IMPORTANTE**: Sin espacios, sin barra final `/`, exactamente así

Si no existe o está mal, agrégalo/corrígelo y **guarda**. Railway reiniciará automáticamente.

---

## Paso 3: Verificar que Railway esté corriendo

1. Ve a **Railway** → Tu servicio backend → **Logs**
2. Busca mensajes como:
   - ✅ `Started AppRestauranteApplication`
   - ✅ `Tomcat started on port(s): 8080`
3. Si ves errores, cópialos para diagnosticar

### Probar el backend directamente:
Abre en tu navegador:
```
https://rds-production.up.railway.app/actuator/health
```

**Deberías ver:**
```json
{"status":"UP"}
```

Si no ves esto, el backend no está corriendo o no está accesible.

---

## Paso 4: Verificar el build de Netlify

1. Ve a **Netlify** → Tu sitio → **Deploys**
2. Verifica que el último deploy tenga el commit: `"Fix: Add /api suffix..."`
3. Si no, haz clic en **"Trigger deploy"** → **"Deploy site"**
4. Verifica los logs del build para ver si hay errores

### Verificar que use producción:
En los logs del build de Netlify, deberías ver:
```
> ng build --configuration production
```

Si no ves esto, el problema está en `netlify.toml`.

---

## Paso 5: Limpiar caché

### A. En el navegador:
1. Presiona **Ctrl+Shift+Delete** (o Cmd+Shift+Delete en Mac)
2. Selecciona "Cached images and files"
3. Limpia la caché
4. Cierra y vuelve a abrir el navegador

### B. En Netlify:
1. Ve a **Site settings** → **Build & deploy**
2. Busca **"Clear cache and retry deploy"**
3. O simplemente haz un nuevo deploy

---

## Paso 6: Verificar errores específicos

### Si ves error de CORS:
```
Access to fetch at '...' from origin '...' has been blocked by CORS policy
```

**Solución:**
1. Verifica `CORS_ALLOWED_ORIGINS` en Railway
2. Asegúrate de que el backend haya reiniciado después de cambiar la variable
3. Verifica que la URL en Railway coincida EXACTAMENTE con la de Netlify

### Si ves 404:
```
GET https://rds-production.up.railway.app/api/... 404
```

**Posibles causas:**
- La ruta del endpoint no existe
- El backend no tiene ese endpoint mapeado
- Falta el `/api` en la URL (pero ya lo corregimos)

### Si ves "Failed to fetch":
**Posibles causas:**
- El backend no está corriendo
- Problema de red
- La URL está mal

---

## Checklist rápido:

- [ ] Frontend hace peticiones a `rds-production.up.railway.app/api`
- [ ] NO hace peticiones a `localhost:8080`
- [ ] CORS_ALLOWED_ORIGINS configurado en Railway con la URL exacta de Netlify
- [ ] Backend está corriendo (health check responde)
- [ ] Netlify hizo deploy con el código más reciente
- [ ] Limpiaste la caché del navegador
- [ ] No hay errores en la consola del navegador

---

## Prueba rápida final:

Ejecuta esto en la consola del navegador (F12):

```javascript
// 1. Verificar backend
fetch('https://rds-production.up.railway.app/actuator/health')
  .then(r => r.json())
  .then(d => console.log('Backend health:', d))
  .catch(e => console.error('Backend no responde:', e));

// 2. Probar endpoint público
fetch('https://rds-production.up.railway.app/api/public/categories')
  .then(r => {
    console.log('Status:', r.status);
    if (r.status === 200) return r.json();
    return r.text().then(t => Promise.reject(new Error(t)));
  })
  .then(d => console.log('✅ Backend funciona:', d))
  .catch(e => console.error('❌ Error:', e));
```

Copia los resultados y compártelos para diagnóstico más preciso.

