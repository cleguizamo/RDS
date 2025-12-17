# ✅ Verificar Conexión Frontend-Backend

## 🔧 Paso 1: Configurar CORS en Railway

1. Ve a **Railway Dashboard** → Tu servicio backend (`rds-production`) → **Variables**
2. Busca o crea la variable: `CORS_ALLOWED_ORIGINS`
3. Configúrala con esta URL exacta:

```bash
CORS_ALLOWED_ORIGINS=https://rincondelsaborgaragoa.netlify.app
```

4. **Guarda** los cambios
5. Espera 1-2 minutos para que Railway reinicie el servicio

---

## 🔍 Paso 2: Verificar en el Navegador

### A. Abre tu sitio y la consola

1. Ve a: https://rincondelsaborgaragoa.netlify.app
2. Presiona **F12** (o clic derecho → "Inspeccionar")
3. Ve a la pestaña **"Console"**

### B. Intenta hacer login

1. Haz clic en "Iniciar Sesión"
2. Intenta iniciar sesión (aunque falle por credenciales incorrectas)
3. Observa la consola del navegador

### C. Verificar peticiones al backend

1. En DevTools, ve a la pestaña **"Network"**
2. Intenta hacer cualquier acción (login, ver menú, etc.)
3. Deberías ver peticiones a: `https://rds-production.up.railway.app/api/...`

---

## ✅ Señales de que está bien conectado

### ✅ **Funciona correctamente si:**
- ✅ Las peticiones aparecen en la pestaña "Network"
- ✅ Las peticiones van a `rds-production.up.railway.app/api`
- ✅ No ves errores de CORS en la consola
- ✅ El login funciona (si tienes credenciales válidas)
- ✅ Puedes ver el menú, productos, etc.

### ❌ **NO funciona si ves:**
- ❌ Error: "Access to fetch at '...' has been blocked by CORS policy"
- ❌ Error: "Failed to fetch" o "Network Error"
- ❌ Las peticiones no aparecen en la pestaña Network
- ❌ Todo aparece en blanco o carga infinitamente

---

## 🐛 Solución de Problemas

### Error de CORS

Si ves: `Access to fetch at '...' from origin 'https://rincondelsaborgaragoa.netlify.app' has been blocked by CORS policy`

**Solución:**
1. Verifica que `CORS_ALLOWED_ORIGINS` en Railway tenga exactamente:
   ```
   https://rincondelsaborgaragoa.netlify.app
   ```
2. Asegúrate de que NO tenga barra final `/` al final
3. Verifica que Railway haya reiniciado (espera 1-2 minutos)
4. Prueba de nuevo

### Error "Failed to fetch"

**Posibles causas:**
1. El backend no está corriendo → Verifica logs en Railway
2. URL incorrecta → Verifica `environment.prod.ts`
3. Problema de red → Espera y reintenta

### No aparecen peticiones en Network

**Solución:**
1. Asegúrate de que la pestaña "Network" esté abierta ANTES de hacer la acción
2. Recarga la página y vuelve a intentar
3. Verifica que no haya un bloqueador de anuncios bloqueando las peticiones

---

## 📝 Checklist Rápido

- [ ] Variable `CORS_ALLOWED_ORIGINS` configurada en Railway con: `https://rincondelsaborgaragoa.netlify.app`
- [ ] Railway reinició (esperaste 1-2 minutos)
- [ ] Abriste DevTools (F12) → Console
- [ ] Intentaste hacer login o ver el menú
- [ ] Revisaste la pestaña Network para ver peticiones
- [ ] No hay errores de CORS en la consola

---

## 🧪 Prueba Rápida

1. Abre: https://rincondelsaborgaragoa.netlify.app
2. F12 → Console
3. Escribe en la consola:
   ```javascript
   fetch('https://rds-production.up.railway.app/api/public/categories')
     .then(r => r.json())
     .then(console.log)
     .catch(console.error)
   ```
4. Si ves datos (categorías), **está conectado correctamente** ✅
5. Si ves error de CORS, necesitas verificar la configuración en Railway

