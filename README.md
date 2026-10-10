# Kanban Board

Tablero Kanban simplificado (tipo Jira) para preparar una entrevista técnica: **proyectos** y **tareas** con tres columnas (`TODO`, `IN_PROGRESS`, `DONE`), prioridad, orden por columna y arrastrar y soltar.

Stack: **Java 25 · Spring Boot 4.1.1 · PostgreSQL 18 (Docker Compose) · Angular 22** (el frontend llega en la Fase 3 del plan). Usuario único, ejecución local, sin autenticación.

> **Estado actual: Fase 1 completa** — backend con CRUD de proyectos y errores ProblemDetail. Tareas, tablero y frontend: fases posteriores.

## Requisitos previos

| Herramienta | Versión | Notas |
|---|---|---|
| JDK | 25 (LTS) | En esta máquina: `C:\Program Files\Java\jdk-25.0.4.1` (ver Notas de implementación) |
| Node.js / npm | 24.x ≥ 24.13.1 | Solo necesario para el frontend (**Fase 3**) |
| Docker + Docker Compose v2 | actuales | Docker en marcha es **necesario también para los tests del backend** (Testcontainers) |
| Git | cualquier versión actual | |

## Arranque en 3 pasos

```bash
# 1. Base de datos (PostgreSQL 18 en Docker)
docker compose up -d

# 2. Backend ( puerto 8080; aplica las migraciones Flyway al arrancar )
cd backend
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run

# 3. Frontend — pendiente (Fase 3)
# cd frontend && ng serve     → http://localhost:4200
```

URLs: API en `http://localhost:8080/api` · Frontend (Fase 3) en `http://localhost:4200`.

Comprobación rápida: `curl http://localhost:8080/api/projects` debe devolver `200 []`.

## Cómo ejecutar los tests

```bash
cd backend
./mvnw test        # Windows: .\mvnw.cmd test
```

- Los tests de integración usan **Testcontainers** (PostgreSQL real): **Docker debe estar en marcha**. No se usa H2.
- Un único contenedor PostgreSQL compartido para toda la ejecución; cada test empieza con la base limpia.
- Tests de frontend (`npm test`): **pendiente (Fase 3)**.

## API (endpoints de proyecto)

Base: `/api`. Fechas en ISO-8601 UTC. Errores en formato `application/problem+json` (RFC 9457).

| Método y ruta | Entrada | Éxito | Errores |
|---|---|---|---|
| `GET /api/projects` | — | 200, lista (más reciente primero; `[]` si no hay) | — |
| `POST /api/projects` | `{name, description?}` | 201 + cabecera `Location: /api/projects/{id}` | 400 |
| `GET /api/projects/{id}` | — | 200 | 404 |
| `PUT /api/projects/{id}` | `{name, description?}` (reemplaza ambos campos) | 200 | 400, 404 |
| `DELETE /api/projects/{id}` | — | 204 (borra también sus tareas en cascada) | 404 |

Los endpoints de tareas (`/api/projects/{id}/tasks`, `/api/tasks/{id}`, `PATCH …/move`) pertenecen a la **Fase 2**.

## Estructura del repositorio

```
kanban/
├── AGENTS.md            fuente de verdad de las reglas del proyecto
├── CLAUDE.md            → @AGENTS.md
├── docs/SPEC.md         especificación técnica completa
├── compose.yaml         PostgreSQL 18 (servicio db)
├── backend/             Spring Boot 4.1.1 (web → service → repository → domain)
│   └── src/main/java/dev/osuna/kanban/
│       ├── domain/          entidades JPA + enums
│       ├── repository/      Spring Data JPA
│       ├── service/         reglas de negocio, transacciones
│       └── web/             controllers, dto/, error/ (ProblemDetail)
└── frontend/            Angular 22 (pendiente, Fase 3)
```

## Troubleshooting

| Síntoma | Causa y solución |
|---|---|
| El backend falla al arrancar con error de conexión (E-16) | PostgreSQL no está disponible: `docker compose up -d` primero y reintenta. |
| Puerto 5432 u 8080 ocupado (E-22) | Cambia el puerto de la BD con `POSTGRES_PORT` (y ajusta `DB_URL` en el entorno), o el del backend con `SERVER_PORT`. Ver `.env.example`. |
| Tests del backend fallan con error de Docker | Docker Desktop no está en marcha; arráncalo y reejecuta `.\mvnw.cmd test`. |
| Error de compilación `release 25 is not supported` | `JAVA_HOME` apunta a un JDK antiguo. Ver Notas de implementación. |

## Notas de implementación

1. **`JAVA_HOME` en esta máquina apunta a JDK 17.** Antes de ejecutar Maven define el JDK 25:
   ```powershell
   $env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4.1'
   ```
2. **Node.js instalado: v24.11.1** — por debajo del mínimo de Angular 22 (`^22.22.0 || ^24.13.1 || ^26.0.0`). Actualizar a ≥ 24.13.1 antes de la Fase 3.
3. **TEST-004 adaptado (Fase 1):** la verificación de cascada siembra 2 tareas con `JdbcTemplate` a nivel de BD y comprueba `count(task) = 0` tras `DELETE`. La aserción `GET /api/projects/{id}/tasks` → 404 queda **diferida a la Fase 2** (cuando exista el endpoint).
4. **`domain/Task`, `TaskStatus`, `TaskPriority` y `TaskRepository` ya existen** aunque el endpoint de tareas es Fase 2: así `ddl-auto=validate` valida todo el esquema `V1__init.sql` desde el primer arranque.
5. **Módulo Testcontainers 2.x:** el artifactId del módulo PostgreSQL es `org.testcontainers:testcontainers-postgresql` (renombrado en Testcontainers 2.x; el BOM de Spring Boot 4.1.1 lo gestiona).
6. Sin dependencias fuera de la lista del SPEC §9. No hay secretos reales en el repo; usa `.env` (ignorado por git) para overrides locales.

## Documentación

- Especificación técnica completa: [`docs/SPEC.md`](docs/SPEC.md) — **prevalece** sobre cualquier suposición de este README.
- Reglas para asistentes agénticos: [`AGENTS.md`](AGENTS.md).
