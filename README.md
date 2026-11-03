# IceCream
Proyecto de Aula - SpringBoot

<h3>Integrantes:</h3>
Juan Guzman<br>
Oscar Taborda<br>
Dilan Osorio<br>
Jean Benavides

<h2>Informacion:</h2>
Aplicacion web para la gestion de una heladeria (inventario, ventas, reportes y usuarios).
Toda la informacion se almacena en **MongoDB Atlas**.

## Requisitos

- Docker y Docker Compose
- Una base de datos en MongoDB Atlas
- Opcional para desarrollo local: Java 17 y Maven

## Configuracion de MongoDB Atlas

1. Copia las variables de entorno de ejemplo:

   ```bash
   cp .env.example .env
   ```

2. En `.env`, reemplaza `SPRING_DATA_MONGODB_URI` por la cadena de conexion de tu cluster de Atlas:

   ```properties
   SPRING_DATA_MONGODB_URI=mongodb+srv://usuario:password@cluster.mongodb.net/heladeria_db?retryWrites=true&w=majority
   ```

3. En MongoDB Atlas, entra a **Network Access** y permite la IP desde la que se conecta la aplicacion. Para pruebas puedes usar `0.0.0.0/0`.

4. Genera un secreto para firmar los JWT y agregalo a `.env`:

   ```bash
   openssl rand -base64 48
   ```

   ```properties
   JWT_SECRET=el-secreto-generado
   JWT_COOKIE_SECURE=false
   ```

   En produccion con HTTPS cambia `JWT_COOKIE_SECURE` a `true`.

5. Completa en `.env` las credenciales de correo y la API key de Groq si vas a usar esas funciones.

## Ejecucion con Docker

Docker solo levanta la aplicacion; MongoDB se ejecuta en Atlas.

```bash
docker compose up --build -d
```

Abre:

http://localhost:8080/login

Si el puerto `8080` esta ocupado, puedes usar otro puerto sin modificar el archivo:

```bash
APP_PORT=8081 docker compose up -d --build
```

En ese caso la aplicacion queda disponible en `http://localhost:8081/login`.

Para ver los logs:

```bash
docker compose logs -f app
```

Para detener la aplicacion:

```bash
docker compose down
```

Si vienes de una version anterior que creaba un contenedor local de MongoDB, elimina los contenedores antiguos con:

```bash
docker compose down --remove-orphans
```

El volumen local `icecream_mongo_data` de esa version anterior ya no se utiliza y puedes eliminarlo manualmente si lo deseas.

### Usuario inicial

| Usuario | Contrasena | Rol |
|---------|------------|-----|
| admin   | admin123   | Administrador |

El usuario `admin` se crea automaticamente al arrancar si no existe en la base de datos. Los demas usuarios se administran desde el panel de usuarios internos.

## Ejecucion local sin Docker

1. Configura `SPRING_DATA_MONGODB_URI` en `.env` con la URI de Atlas o con una instancia local de MongoDB.
2. Ejecuta:

   ```bash
   ./mvnw spring-boot:run
   ```

## Autenticacion con JWT

La aplicacion no usa formulario de login de Spring Security ni sesiones HTTP. La autenticacion es stateless:

1. La pagina `/login` envia las credenciales a `POST /api/auth/login`.
2. El servidor valida el usuario con BCrypt y devuelve un JWT firmado con HS256 (8 horas de vigencia).
3. El token se guarda en una cookie `HttpOnly` (`icecream_token`, `SameSite=Lax`) para que las paginas Thymeleaf puedan autenticarse al navegar.
4. Cada peticion se valida con un filtro de Spring Security. Tambien se acepta el token en la cabecera `Authorization: Bearer <token>` para clientes API.

Endpoints:

| Metodo | Ruta | Descripcion |
|--------|------|-------------|
| `POST` | `/api/auth/login` | Recibe `{"username","password"}` y devuelve `{"username","roles","redirect"}` |
| `POST` | `/api/auth/logout` | Elimina la cookie del token |
| `GET` | `/api/auth/me` | Devuelve el usuario y roles del token actual |

Ejemplo con cabecera:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

El logout solo elimina la cookie: al ser stateless no hay lista de revocacion, por lo que un token robado sigue siendo valido hasta que expira.

La aplicacion mantiene CSRF desactivado y usa `SameSite=Lax` en la cookie. Si el sistema se publica junto a otros subdominios o aplicaciones del mismo sitio, conviene habilitar proteccion CSRF para las peticiones que modifican datos.

## Estructura de persistencia

- `users`: usuarios y roles (autenticacion con JWT y BCrypt).
- `helados`: inventario disponible.
- `heladosEliminados`: historial de productos eliminados.
- `ventas`: ventas realizadas con sus clientes e items.
