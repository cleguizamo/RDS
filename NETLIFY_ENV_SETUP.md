# Variables de Entorno en Netlify para Angular

## ⚠️ Nota Importante sobre Angular y Variables de Entorno

Angular **NO** puede leer variables de entorno en tiempo de ejecución como Next.js o React. Las variables de entorno en Angular se deben definir **en tiempo de build**.

## ✅ Opción Recomendada (Más Simple)

### Actualizar directamente `environment.prod.ts`

1. **Obtén la URL de tu backend en Railway**:
   - Ve a Railway → Tu servicio backend → Settings → Generate Domain
   - Copia la URL (ej: `https://app-restaurante-production.up.railway.app`)

2. **Actualiza el archivo**:
   ```typescript
   // frontend-angular/src/environments/environment.prod.ts
   export const environment = {
     production: true,
     apiUrl: 'https://TU-BACKEND-URL.railway.app/api'  // ⬅️ Cambia esto
   };
   ```

3. **Commit y push**:
   ```bash
   git add frontend-angular/src/environments/environment.prod.ts
   git commit -m "Update backend API URL for production"
   git push
   ```

4. **Netlify se desplegará automáticamente** con la nueva URL.

---

## 🔧 Opción Alternativa (Usar Variables de Netlify)

Si quieres cambiar la URL sin hacer commit, puedes usar un script de build personalizado:

### 1. Crear script de build

Crea `frontend-angular/build-with-env.sh`:

```bash
#!/bin/bash
# Reemplaza la URL del API en environment.prod.ts con la variable de entorno

if [ -n "$NETLIFY_API_URL" ]; then
  echo "Usando NETLIFY_API_URL: $NETLIFY_API_URL"
  sed -i.bak "s|apiUrl:.*|apiUrl: '$NETLIFY_API_URL'|" src/environments/environment.prod.ts
fi

npm run build
```

### 2. Hacer el script ejecutable y actualizar netlify.toml

Pero esto es más complejo. **La opción recomendada es actualizar directamente el archivo.**

---

## 📝 Variables de Entorno en Netlify (Si las necesitas en el futuro)

Si en el futuro necesitas otras variables (como claves de API públicas), puedes agregarlas en:

**Netlify → Site settings → Environment variables**

Pero recuerda: **solo funcionarán si las usas en tiempo de build**, no en tiempo de ejecución en el navegador.

---

## 🎯 Resumen

**Para este proyecto, simplemente:**
1. Actualiza `environment.prod.ts` con la URL real del backend
2. Haz commit y push
3. Netlify se desplegará automáticamente

**No necesitas configurar variables de entorno en Netlify** para la URL del API si actualizas el archivo directamente.

