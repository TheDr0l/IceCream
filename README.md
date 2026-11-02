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

4. Completa en `.env` las credenciales de correo y la API key de Groq si vas a usar esas funciones.

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

## Estructura de persistencia

- `users`: usuarios y roles (autenticacion con Spring Security y BCrypt).
- `helados`: inventario disponible.
- `heladosEliminados`: historial de productos eliminados.
- `ventas`: ventas realizadas con sus clientes e items.
