# Backend-TOTUM

Backend de **TOTUM**: **4 microservicios Spring Boot** con MySQL. La app Android los usa para manejar usuarios, roles, turnos y los registros de aforo (conteo de clientes y transeúntes por acceso).

📱 **Repositorio de la app Android:** https://github.com/Jemerym/AppConteo

📋 **Tablero del proyecto (Trello):** https://trello.com/b/9NETUH65/app-conteo

---

## Microservicios

```
App Android (Retrofit)
   ├──► roles-ms      :8081  ── MySQL roles_db
   ├──► usuarios-ms   :8082  ── MySQL usuarios_db   ──(WebClient)──► roles-ms
   ├──► turnos-ms     :8083  ── MySQL turnos_db     ──(WebClient)──► usuarios-ms
   └──► registros-ms  :8084  ── MySQL registros_db
```

| Microservicio | Puerto | Base de datos | Responsabilidad |
|---|---|---|---|
| **roles-ms** | 8081 | `roles_db` | Catálogo de roles (Administrador, Observador). |
| **usuarios-ms** | 8082 | `usuarios_db` | Registro, login (BCrypt), perfil, foto de perfil, recuperar clave, anular y restaurar usuarios. |
| **turnos-ms** | 8083 | `turnos_db` | Programar turnos, validar choques de horario, turno en curso (hora de Chile), anular y restaurar turnos. |
| **registros-ms** | 8084 | `registros_db` | Registros de aforo, boleta única, conteo del día, anular y restaurar registros. La fecha y la hora las pone el servidor. |

> **Anular en vez de eliminar:** usuarios, turnos y registros no se borran. Se anulan: dejan de contar o de funcionar, pero se conservan y se pueden restaurar.

### Tecnologías
Java 17, Spring Boot 4, Spring Data JPA, MySQL, Lombok, WebClient (comunicación entre microservicios), BCrypt (contraseñas) y springdoc-openapi (Swagger).

---

## Cómo ejecutarlo

### 1. Requisitos
- Java 17 o superior.
- MySQL (o XAMPP/MariaDB) corriendo en `localhost:3306`, con usuario `root` sin contraseña. Si es distinto, cambia `spring.datasource.*` en cada `application.properties`.

### 2. Descargar el proyecto
Tienes dos opciones:

**Opción A — Descargar ZIP (sin git):**
1. Entra a https://github.com/Jemerym/Back-appconteo
2. Botón verde **Code** → **Download ZIP**.
3. Descomprime el archivo y abre la carpeta `Back-appconteo`.

**Opción B — Clonar con git:**

```bash
git clone https://github.com/Jemerym/Back-appconteo.git
```

```bash
cd Back-appconteo
```

### 3. Levantar los microservicios
Las bases de datos se crean solas (`createDatabaseIfNotExist=true`) y las tablas también (`ddl-auto=update`).

Levanta los 4 microservicios **en este orden**, porque usuarios-ms necesita a roles-ms y turnos-ms necesita a usuarios-ms. En cada carpeta, ejecuta su comando (en Windows usa `mvnw.cmd` en vez de `./mvnw`):

**roles-ms:**
```bash
./mvnw spring-boot:run
```

**usuarios-ms:**
```bash
./mvnw spring-boot:run
```

**turnos-ms:**
```bash
./mvnw spring-boot:run
```

**registros-ms:**
```bash
./mvnw spring-boot:run
```

También puedes ejecutar la clase `*MsApplication` de cada microservicio desde IntelliJ.

**Swagger** (documentación y pruebas de cada microservicio): `http://localhost:<puerto>/swagger-ui.html`

### 4. Conectar la app
- En la app, `data/remote/RetrofitClient.kt` debe apuntar a la **IP del PC** donde corren los microservicios, por ejemplo `http://192.168.1.5:8082/`. Desde el emulador se usa `10.0.2.2`.
- En el **Firewall de Windows**, permite los puertos **8082, 8083 y 8084** para que el celular pueda conectarse.

### 5. Usuarios de prueba
usuarios-ms los crea solo la primera vez:

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@demo.cl` | `1234` |
| Observador | `camila@duocuc.cl` | `123456` |
| Observador | `diego@duocuc.cl` | `123456` |

La palabra secreta de los tres es `duoc`. Los usuarios nuevos que se registren desde la app quedan siempre como **Observador**.

---

## Endpoints

### roles-ms (`/api/roles`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/roles` | Lista los roles |
| GET | `/api/roles/{id}` | Obtiene un rol |

### usuarios-ms (`/api/usuarios`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/usuarios?buscar=&incluirAnulados=` | Lista o busca usuarios |
| GET | `/api/usuarios/{id}` | Obtiene un usuario |
| POST | `/api/usuarios` | Registra un usuario (siempre como Observador) |
| POST | `/api/usuarios/login` | Inicia sesión (401 si la clave es incorrecta o el usuario está anulado) |
| PUT | `/api/usuarios/{id}` | Actualiza nombre y correo |
| PUT | `/api/usuarios/{id}/contrasena` | Cambia la contraseña |
| POST | `/api/usuarios/recuperar-clave` | Recupera la clave con la palabra secreta |
| POST / GET | `/api/usuarios/{id}/avatar` | Sube o descarga la foto de perfil |
| PUT | `/api/usuarios/{id}/anular` | Anula un usuario (409 si es el último administrador) |
| PUT | `/api/usuarios/{id}/restaurar` | Restaura un usuario |

### turnos-ms (`/api/turnos`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/turnos?usuarioId=` | Lista turnos (todos o los de un observador) |
| GET | `/api/turnos/actual?usuarioId=` | Turno en curso (204 si no tiene) |
| GET | `/api/turnos/{id}` | Obtiene un turno |
| POST | `/api/turnos` | Programa un turno (409 si choca; 400 si es para un admin o un usuario anulado) |
| PUT | `/api/turnos/{id}` | Modifica un turno |
| PUT | `/api/turnos/{id}/anular` | Anula un turno (libera el horario) |
| PUT | `/api/turnos/{id}/restaurar` | Restaura un turno (409 si el horario ya está ocupado) |
| PUT | `/api/turnos/usuario/{usuarioId}/anular` | Anula los turnos de un usuario |

### registros-ms (`/api/registros`)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/registros?usuarioId=&turnoId=&fecha=&incluirAnulados=` | Lista registros con filtros |
| GET | `/api/registros/{id}` | Obtiene un registro |
| POST | `/api/registros` | Registra una persona (409 si la boleta ya se usó) |
| PUT | `/api/registros/{id}` | Modifica acceso, grupo y rango etario |
| PUT | `/api/registros/{id}/anular` | Anula un registro (deja de contar) |
| PUT | `/api/registros/{id}/restaurar` | Restaura un registro (409 si su boleta ya está en otro vigente) |
| GET | `/api/registros/boleta/existe?codigo=` | ¿La boleta ya está registrada? |
| GET | `/api/registros/conteo?fecha=&usuarioId=` | Totales del día (clientes, tránsito, total) |

---

## Estructura de cada microservicio

```
<nombre>-ms/src/main/java/cl/duocuc/<nombre>_ms/
├── controller/   Endpoints REST
├── service/      Reglas de negocio y validaciones
├── repository/   Acceso a MySQL (Spring Data JPA)
├── model/        Entidades JPA
├── dto/          Lo que se recibe y se devuelve (Request / Response)
├── client/       Llamadas a otros microservicios (WebClient)   [usuarios-ms, turnos-ms]
└── config/       Datos iniciales y configuración               [roles-ms, usuarios-ms]
```
