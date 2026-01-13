# RDS - Restaurant Digital System (Rincón del Sabor)

RDS es una aplicación web para un gastrobar donde el administrador pueda gestionar inventario, productos, pedidos, reservas y finanzas. Los clientes pueden realizar pedidos, reservas y gestionar su perfil.

## 💻 Tecnologías usadas

### 🛠️ Backend:
        
- Java 17        
- Spring Boot
- Spring Data JPA
- Spring Security + JWT
- MySQL
- Caffeine Cache

### 🌄 Frontend

- Angular

### 🏛️ Infraestructura

- Docker
- Railway (backend)
- Netlify (frontend)

### 🧱 Arquitectura

- Backend
    - Config -> Configuración de Spring (CORS, Beans, cache, mail).
    - Controller -> Presenta los endpoints REST y gestiona las solicitudes HTTP.
    - DTO -> Objetos diseñados para la transferencia de datos.
    - Exception -> Manejo centralizado de excepciones y errores personalizados.
    - Model -> Entidades JPA que representan el modelo de datos.
    - Repository -> Manejo de datos mediante Spring Data JPA.
    - Scheduler -> Se diseñó para la programación de pagos.
    - Security -> Configuración necesaria para garantizar la seguridad en el sistema.
    - Service -> Contiene la lógica de negocio de la aplicación.


### 🚀 Funcionalidades principales

- Creación y gestión de pedidos y usuarios.
- Autenticación y verificación con JWT.
- CRUD de gastrobar, productos, categorías y usuarios.
- Envío de correos (SMTP)
- Rate limiting para endpoints esenciales.

## 🔐 Autenticación y Autorización (JWT)

La aplicación utiliza **JSON Web Tokens (JWT)** para la autenticación y autorización de usuarios.

### 🧩 Flujo de autenticación

1. El usuario inicia sesión enviando sus credenciales (email y contraseña).
2. El backend valida las credenciales usando Spring Security.
3. Si la autenticación es exitosa, se genera un **JWT** que contiene:
   - ID del usuario
   - Email
   - Rol (ADMIN / CLIENT)
4. El token es firmado y enviado al cliente.
5. El cliente debe enviar el JWT en cada solicitud protegida usando el header con el token incluido.
6. Un filtro de seguridad intercepta cada petición, valida el token y autoriza el acceso según el rol del usuario.

### 🔑 Seguridad del token

- El token es firmado con una clave secreta configurada mediante variables de entorno.
- Tiene un tiempo de expiración configurable.
- Se valida la firma y la expiración en cada solicitud.
- Los roles incluidos en el token permiten proteger endpoints por permisos.

### 🛡️ Protección de endpoints

- Los endpoints públicos no requieren autenticación.
- Los endpoints protegidos requieren un JWT válido.
- Los endpoints administrativos solo son accesibles por usuarios con rol **ADMIN**.


### 🛜 Donde ver el proyecto?

Click en el siguiente enlace!! 👇👇
https://rincondelsaborgaragoa.netlify.app/