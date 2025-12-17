# 🚀 Guía de Despliegue del Frontend Angular

Esta guía te ayudará a desplegar tu frontend Angular con tu propio dominio.

## 📋 Opciones de Despliegue

### 1. **Vercel** (⭐ Recomendado)

Vercel es la mejor opción para Angular con SSR y dominio personalizado.

#### Ventajas:
- ✅ Configuración automática para Angular
- ✅ SSR (Server-Side Rendering) incluido
- ✅ Dominio personalizado gratuito
- ✅ SSL automático
- ✅ Despliegue automático desde GitHub
- ✅ CDN global
- ✅ Gratis para proyectos personales

#### Pasos para desplegar en Vercel:

1. **Instalar Vercel CLI (opcional)**:
   ```bash
   npm i -g vercel
   ```

2. **Ir a [vercel.com](https://vercel.com)**:
   - Click en "Sign Up" o "Log In"
   - Conecta tu cuenta de GitHub

3. **Importar proyecto**:
   - Click en "Add New" → "Project"
   - Selecciona tu repositorio `cleguizamo/RDS`
   - **Root Directory**: Selecciona `frontend-angular`
   - Framework Preset: **Angular** (debería detectarlo automáticamente)
   - Build Command: `npm run build` (ya está configurado en `vercel.json`)
   - Output Directory: `dist/frontend-angular/browser`

4. **Configurar variables de entorno**:
   - En la sección "Environment Variables", agrega:
     ```bash
     # Si tu backend está en Railway, usa la URL de Railway
     NEXT_PUBLIC_API_URL=https://tu-backend.railway.app/api
     ```
   - **Nota**: Para Angular, puedes usar variables de entorno en tiempo de build modificando `environment.prod.ts`

5. **Desplegar**:
   - Click en "Deploy"
   - Vercel construirá y desplegará tu aplicación
   - Obtendrás una URL como: `https://tu-proyecto.vercel.app`

6. **Configurar dominio personalizado**:
   - Ve a tu proyecto → Settings → Domains
   - Click en "Add Domain"
   - Ingresa tu dominio (ej: `app.tudominio.com`)
   - Vercel te dará instrucciones para configurar DNS:
     - Agrega un registro CNAME apuntando a `cname.vercel-dns.com`
     - O configura registros A según las instrucciones
   - SSL se configurará automáticamente

#### Actualizar la URL del API:

Antes de hacer build, actualiza `environment.prod.ts` con la URL de tu backend:

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://tu-backend.railway.app/api'
};
```

---

### 2. **Netlify**

Similar a Vercel, también excelente para Angular.

#### Pasos:

1. Ir a [netlify.com](https://netlify.com)
2. Click en "Add new site" → "Import an existing project"
3. Conecta GitHub y selecciona tu repositorio
4. Configuración:
   - **Base directory**: `frontend-angular`
   - **Build command**: `npm run build`
   - **Publish directory**: `dist/frontend-angular/browser`
5. Configurar dominio: Site settings → Domain management → Add custom domain

---

### 3. **Railway** (Mantener todo junto)

Si quieres tener backend y frontend en Railway:

#### Pasos:

1. En Railway, agrega un nuevo servicio "GitHub Repo"
2. Selecciona tu repositorio y establece **Root Directory**: `frontend-angular`
3. Railway detectará automáticamente que es Node.js/Angular
4. Variables de entorno:
   - `NODE_ENV=production`
   - Puedes configurar otras variables si las necesitas
5. Build y deploy se harán automáticamente
6. Genera un dominio en Railway o conecta tu dominio personalizado

**Nota**: Railway puede ser más costoso para frontends estáticos. Vercel/Netlify son mejores opciones.

---

### 4. **Cloudflare Pages** (Gratis)

Otra excelente opción gratuita con buen rendimiento.

#### Pasos:

1. Ir a [dash.cloudflare.com](https://dash.cloudflare.com)
2. Ve a "Pages" → "Create a project"
3. Conecta GitHub y selecciona tu repositorio
4. Configuración:
   - **Framework preset**: Angular
   - **Build command**: `npm run build`
   - **Build output directory**: `dist/frontend-angular/browser`
5. Click en "Save and Deploy"
6. Para dominio personalizado: Custom domains → Add custom domain

---

## 🔧 Configuración Pre-Despliegue

### Actualizar URL del Backend

**IMPORTANTE**: Antes de desplegar, actualiza la URL de tu backend en `environment.prod.ts`:

```typescript
// frontend-angular/src/environments/environment.prod.ts
export const environment = {
  production: true,
  apiUrl: 'https://tu-backend.railway.app/api'  // ⬅️ Cambia esto
};
```

### Actualizar CORS en Backend

Asegúrate de que tu backend permita el origen de tu frontend. En Railway, agrega esta variable:

```bash
CORS_ALLOWED_ORIGINS=https://tu-dominio.com,https://www.tu-dominio.com
```

---

## 🌐 Configurar Dominio Personalizado

### Opción 1: Subdominio (ej: `app.tudominio.com`)

1. Ve al panel de tu proveedor de DNS (donde compraste el dominio)
2. Agrega un registro CNAME:
   - **Nombre/Host**: `app` (o el subdominio que quieras)
   - **Valor/Destino**: La URL que te dio Vercel/Netlify/etc
3. Espera a que se propague (5 minutos a 48 horas)

### Opción 2: Dominio raíz (ej: `tudominio.com`)

Dependiendo de la plataforma:
- **Vercel**: Te dará registros A específicos para configurar
- **Netlify**: Similar, configura registros A
- **Cloudflare Pages**: Configuración automática si el dominio está en Cloudflare

---

## ✅ Checklist de Despliegue

- [ ] Actualizar `environment.prod.ts` con URL del backend
- [ ] Configurar CORS en backend para permitir el dominio del frontend
- [ ] Desplegar frontend en la plataforma elegida
- [ ] Verificar que el frontend carga correctamente
- [ ] Configurar dominio personalizado
- [ ] Verificar SSL/HTTPS (debería ser automático)
- [ ] Probar login y funcionalidades que requieren API
- [ ] Verificar que las rutas funcionan correctamente (especialmente al refrescar)

---

## 🐛 Troubleshooting

### Error: Cannot GET /ruta

**Solución**: Configura redirects/rewrites para Angular routing:
- Vercel: Ya está en `vercel.json`
- Netlify: Ya está en `netlify.toml`
- Cloudflare Pages: Agrega un `_redirects` file o configúralo en el dashboard

### Error CORS

**Solución**: Asegúrate de que `CORS_ALLOWED_ORIGINS` en el backend incluya tu dominio del frontend.

### Imágenes o assets no cargan

**Solución**: Verifica que el `base href` esté correcto en `index.html` (debería ser `/`).

---

## 📚 Recursos

- [Vercel Angular Guide](https://vercel.com/docs/frameworks/angular)
- [Netlify Angular Guide](https://docs.netlify.com/integrations/frameworks/angular/)
- [Cloudflare Pages Angular Guide](https://developers.cloudflare.com/pages/framework-guides/angular/)

---

## 💡 Recomendación Final

**Vercel** es la mejor opción para Angular porque:
1. Maneja SSR automáticamente
2. Configuración mínima necesaria
3. Despliegue rápido y fácil
4. Dominio personalizado gratuito
5. Excelente CDN y rendimiento

¡Buena suerte con el despliegue! 🚀

