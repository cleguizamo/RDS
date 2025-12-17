# ⚡ Pasos Inmediatos para Conectar Frontend-Backend

## ✅ Confirmación: El Backend funciona correctamente
- Health check: ✅ Funcionando
- API pública: ✅ Responde 200

## 🔧 Pasos para solucionar:

### 1. VERIFICAR CORS EN RAILWAY (MUY IMPORTANTE)

1. Ve a **Railway Dashboard**
2. Selecciona tu servicio backend (`rds-production`)
3. Click en **"Variables"**
4. Busca o crea: `CORS_ALLOWED_ORIGINS`
5. Configúrala con este valor EXACTO (sin espacios, sin barra final):
   ```
   https://rincondelsaborgaragoa.netlify.app
   ```
6. **Guarda** (Railway reiniciará automáticamente)
7. Espera 1-2 minutos para que reinicie

---

### 2. VERIFICAR QUE NETLIFY USE EL CÓDIGO ACTUALIZADO

1. Ve a **Netlify Dashboard** → Tu sitio
2. Ve a **"Deploys"**
3. Verifica que el último deploy tenga el commit más reciente
4. Si no, haz clic en **"Trigger deploy"** → **"Deploy site"**
5. Espera a que termine el build

---

### 3. VERIFICAR EN EL NAVEGADOR

1. Abre: https://rincondelsaborgaragoa.netlify.app
2. Presiona **Ctrl+Shift+R** (o Cmd+Shift+R en Mac) para limpiar caché
3. Presiona **F12** → Pestaña **"Network"**
4. Intenta hacer login o ver el menú
5. **Copia la URL completa** de cualquier petición que veas

**¿Qué URL aparece?**
- ✅ `https://rds-production.up.railway.app/api/...` → Correcto
- ❌ `http://localhost:8080/api/...` → Problema: build antiguo en caché

---

### 4. PRUEBA DIRECTA EN LA CONSOLA

Abre la consola del navegador (F12) y ejecuta:

```javascript
// Test 1: Backend responde
fetch('https://rds-production.up.railway.app/actuator/health')
  .then(r => r.json())
  .then(d => console.log('✅ Backend OK:', d))
  .catch(e => console.error('❌ Backend error:', e));

// Test 2: CORS funciona
fetch('https://rds-production.up.railway.app/api/public/categories', {
  headers: {
    'Origin': 'https://rincondelsaborgaragoa.netlify.app'
  }
})
  .then(r => {
    console.log('Status:', r.status);
    console.log('CORS headers:', r.headers.get('access-control-allow-origin'));
    return r.json();
  })
  .then(d => console.log('✅ API funciona:', d))
  .catch(e => console.error('❌ CORS o API error:', e));
```

**Comparte los resultados** para diagnosticar mejor.

---

### 5. VERIFICAR LOGS

**En Railway:**
- Ve a **Logs** del servicio backend
- Busca errores relacionados con CORS
- Si ves errores, cópialos

**En Netlify:**
- Ve a **Deploys** → Último deploy → **"View build log"**
- Verifica que no haya errores en el build

---

## 🎯 Lo más probable:

El problema es **CORS**. Asegúrate de:

1. ✅ `CORS_ALLOWED_ORIGINS=https://rincondelsaborgaragoa.netlify.app` en Railway
2. ✅ Railway reinició después de cambiar la variable
3. ✅ La URL coincide EXACTAMENTE (sin espacios, sin `/` al final)

---

## 📞 Si sigue sin funcionar:

Comparte:
1. ¿Qué error ves en la consola del navegador?
2. ¿Qué URL aparece en las peticiones (Network tab)?
3. ¿Configuraste `CORS_ALLOWED_ORIGINS` en Railway?
4. ¿El backend reinició después de configurar CORS?

