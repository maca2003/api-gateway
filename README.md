# api-gateway

**API Gateway** de la solución de microservicios. Es el punto de entrada único (puerto `8080`) y se encarga de:

- **Routing**: redirige cada petición al microservicio correspondiente.
- **JWT**: valida la firma y expiración del token emitido por MS-AUTH.
- **Roles**: permite o rechaza la operación según el rol del usuario.

Si el token es válido, agrega los headers `X-Usuario` y `X-Rol` antes de enviar la petición al microservicio.

## Tecnologías

- Java 21
- Spring Boot 3.5
- Spring Cloud Gateway (2025.0)
- JJWT 0.12
- Maven
- JUnit 5

## Estructura

```
src/main/java/cl/duoc/apigateway
├── config     -> Definición de rutas
└── security   -> Filtro JWT, validación de token y reglas de acceso por rol
```

## Rutas

| Ruta | Microservicio |
|---|---|
| `/api/auth/**`, `/api/usuarios/**`, `/api/roles/**` | ms-auth (`localhost:8081`) |
| `/api/productos/**`, `/api/categorias/**` | ms-productos (`localhost:8082`) |
| `/api/ventas/**` | ms-ventas (`localhost:8083`) |
| `/api/inventario/**` | ms-inventario (`localhost:8084`) |

## Reglas de acceso

| Operación | ADMIN | VENDEDOR |
|---|---|---|
| `POST /api/auth/login` y `/api/auth/registro` | Público | Público |
| Usuarios y roles | Sí | No |
| Consultar productos, categorías e inventario (GET) | Sí | Sí |
| Crear, actualizar o eliminar productos, categorías e inventario | Sí | No |
| Consultar y registrar ventas (GET, POST) | Sí | Sí |
| Anular y eliminar ventas | Sí | No |

## Requisitos

- Java 21
- Maven 3.9+
- Los microservicios ms-auth, ms-productos, ms-ventas y ms-inventario en ejecución

## Cómo ejecutar

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/maca2003/api-gateway.git
   cd api-gateway
   ```
2. Compilar, ejecutar pruebas y empaquetar:
   ```bash
   mvn clean install
   ```
3. Ejecutar:
   ```bash
   java -jar target/api-gateway-1.0.0.jar
   ```
   o bien `mvn spring-boot:run`.

La clave JWT (`jwt.secret`) en `application.properties` debe ser la misma que la de ms-auth.

## Docker

El `Dockerfile` usa dos etapas:

1. **Compilación** (`maven:3.9.9-eclipse-temurin-21`): descarga las dependencias (en una capa separada para aprovechar la caché) y genera el `.jar`.
2. **Ejecución** (`eclipse-temurin:21-jre-alpine`): imagen liviana solo con el JRE, usuario sin privilegios (uid 1001) y la JVM limitada al 75% de la memoria del contenedor.

```bash
docker build -t api-gateway:1.0.0 .
docker run -p 8080:8080 --name api-gateway api-gateway:1.0.0
```

Para levantar el sistema completo (todos los servicios + MySQL) usa el `docker-compose.yml` del repositorio [infraestructura-swarm](https://github.com/maca2003/infraestructura-swarm). En el clúster Docker Swarm este es el **único servicio con puerto publicado** (8080); los microservicios solo son accesibles a través de él. Delante se ubica **Amazon API Gateway** (HTTP API con límite de peticiones), definido en `infraestructura-swarm/cloudformation/cluster.yaml`.

## CI/CD (GitHub Actions)

Archivo `.github/workflows/ci-cd.yml`. Se ejecuta en cada push o pull request a `main` y `develop`:

| Etapa | Ramas | Qué hace |
|---|---|---|
| Build y test | todas | `mvn clean verify` (compila, ejecuta pruebas y genera el `.jar`) |
| Provisionar Amazon ECR | `main` | Crea el repositorio de imágenes `api-gateway` en ECR si no existe |
| Construir y publicar imagen | `main` | `docker build` y `docker push` a ECR con etiquetas `<commit>` y `latest` |
| Desplegar en Docker Swarm | `main` | Por SSH al manager: `docker service update --image ... sistema_api-gateway` y verifica que `/api/productos` responda 200 |

Secrets requeridos (Settings → Secrets and variables → Actions):

| Secret | Valor |
|---|---|
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | Learner Lab → AWS Details → AWS CLI (cambian en cada sesión) |
| `EC2_SSH_KEY` | Contenido de `labsuser.pem` |
| `EC2_HOST` | IP pública del manager (salida `IpPublicaManager` del stack `cluster-swarm`) |

## Prueba rápida con Postman

1. `POST http://localhost:8080/api/auth/login` con
   ```json
   { "username": "admin", "password": "admin123" }
   ```
2. Copiar el `token` de la respuesta.
3. En las siguientes peticiones, agregar el header `Authorization: Bearer <token>`.
4. Ejemplo: `GET http://localhost:8080/api/productos`.

## Respuestas de error

| Código | Caso |
|---|---|
| 401 | Falta el token, o es inválido o expiró |
| 403 | El rol no tiene permiso para la operación |
