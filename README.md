# Kanban Board

Tablero Kanban simplificado (tipo Jira) para preparar una entrevista técnica: **proyectos** y **tareas** con tres columnas (`TODO`, `IN_PROGRESS`, `DONE`), prioridad, orden por columna y arrastrar y soltar.

Stack: **Java 25 · Spring Boot 4.1.1 · PostgreSQL 18 (Docker Compose) · Angular 22**. Usuario único, ejecución local, sin autenticación.

> **Estado actual: MVP completo** — backend (Fase 2) con CRUD de proyectos y tareas, `PATCH …/move` con renumeración de columnas y errores ProblemDetail; frontend (Fase 3) con lista de proyectos, tablero de tres columnas y arrastrar y soltar.

## Requisitos previos

| Herramienta | Versión | Notas |
|---|---|---|
| JDK | 25 (LTS) | En esta máquina: `C:\Program Files\Java\jdk-25.0.4.1` (ver Notas de implementación) |
| Node.js / npm | 24.x ≥ 24.13.1 | En esta máquina: v24.20.0 / npm 11.19.0 |
| Docker + Docker Compose v2 | actuales | Docker en marcha es **necesario también para los tests del backend** (Testcontainers) |
| Git | cualquier versión actual | |

## Arranque en 3 pasos

```bash
# 1. Base de datos (PostgreSQL 18 en Docker)
docker compose up -d

# 2. Backend (puerto 8080; aplica las migraciones Flyway al arrancar)
cd backend
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run

# 3. Frontend (puerto 4200; proxy /api → backend 8080, sin CORS)
cd frontend
npm install                   # solo la primera vez
npm start                     # = ng serve
```

URLs: Frontend en `http://localhost:4200` · API en `http://localhost:8080/api`.

Comprobación rápida: `curl http://localhost:8080/api/projects` debe devolver `200 []`.

## Cómo ejecutar los tests

```bash
# Backend (unitarios Mockito + integración Testcontainers)
cd backend
./mvnw test        # Windows: .\mvnw.cmd test

# Frontend (unitarios + componentes con Vitest)
cd frontend
npm test           # = ng test; añadir --no-watch para ejecución no interactiva
```

- Los tests de integración del backend usan **Testcontainers** (PostgreSQL real): **Docker debe estar en marcha**. No se usa H2.
- Un único contenedor PostgreSQL compartido para toda la ejecución; cada test empieza con la base limpia.
- Los tests de frontend cubren `moveTask` (TEST-018/019), el formulario de tarea (TEST-020), la lista de proyectos (TEST-021), los estados del tablero (TEST-022) y el movimiento optimista con rollback (TEST-023).

## API (endpoints de proyecto y tareas)

Base: `/api`. Fechas en ISO-8601 UTC. Errores en formato `application/problem+json` (RFC 9457).

| Método y ruta | Entrada | Éxito | Errores |
|---|---|---|---|
| `GET /api/projects` | — | 200, lista (más reciente primero; `[]` si no hay) | — |
| `POST /api/projects` | `{name, description?}` | 201 + cabecera `Location: /api/projects/{id}` | 400 |
| `GET /api/projects/{id}` | — | 200 | 404 |
| `PUT /api/projects/{id}` | `{name, description?}` (reemplaza ambos campos) | 200 | 400, 404 |
| `DELETE /api/projects/{id}` | — | 204 (borra también sus tareas en cascada) | 404 |
| `GET /api/projects/{projectId}/tasks` | — | 200, lista ordenada por columna y posición (`[]` si no hay) | 404 (proyecto) |
| `POST /api/projects/{projectId}/tasks` | `{title, description?, priority?}` | 201 + `Location: /api/tasks/{id}` (siempre `TODO`, al final de la columna, prioridad por defecto `MEDIUM`) | 400, 404 |
| `GET /api/tasks/{id}` | — | 200 | 404 |
| `PUT /api/tasks/{id}` | `{title, description?, priority}` (reemplaza esos tres campos; **no** toca `status` ni `position`) | 200 | 400, 404 |
| `PATCH /api/tasks/{id}/move` | `{status, position}` | 200, tarea actualizada | 400, 404 |
| `DELETE /api/tasks/{id}` | — | 204 (renumera la columna de origen a `0..n-1`) | 404 |

`PATCH …/move` se ejecuta en **una sola transacción** (SPEC §7.4): posición fuera de rango → 400 con `detail` `Position P is out of range for column D (max N)` y **cero** cambios persistidos; mover a la misma columna y posición → 200 sin cambios. La invariante por columna es `0, 1, … n-1` sin huecos ni repetidos.

## Estructura del repositorio

```
kanban/
├── compose.yaml         PostgreSQL 18 (servicio db)
├── backend/             Spring Boot 4.1.1 (web → service → repository → domain)
│   └── src/main/java/dev/osuna/kanban/
│       ├── domain/          entidades JPA + enums
│       ├── repository/      Spring Data JPA
│       ├── service/         reglas de negocio, transacciones
│       └── web/             controllers, dto/, error/ (ProblemDetail)
└── frontend/            Angular 22 (standalone + signals + CDK drag-drop)
    └── src/app/
        ├── core/            api/ (único punto con HttpClient), models/, error/
        └── features/
            ├── projects/    lista de proyectos + diálogo (httpResource, Signal Forms)
            └── board/       tablero: board-page, board-column, task-card,
                             task-form-dialog, board.store, board.utils
```

## Troubleshooting

| Síntoma | Causa y solución |
|---|---|
| El backend falla al arrancar con error de conexión (E-16) | PostgreSQL no está disponible: `docker compose up -d` primero y reintenta. |
| Puerto 5432 u 8080 ocupado (E-22) | Cambia el puerto de la BD con `POSTGRES_PORT` (y ajusta `DB_URL` en el entorno), o el del backend con `SERVER_PORT`. Ver `.env.example`. |
| Tests del backend fallan con error de Docker | Docker Desktop no está en marcha; arráncalo y reejecuta `.\mvnw.cmd test`. |
| Error de compilación `release 25 is not supported` | `JAVA_HOME` apunta a un JDK antiguo. Ver Notas de implementación. |
| `ng serve`/`npm start` falla con error de versiones de Node | Angular 22 exige Node `^22.22.0 \|\| ^24.13.1 \|\| ^26.0.0`. Actualiza a Node 24.x ≥ 24.13.1 (`node -v`). |
| El frontend no carga datos (404 en `/api/...`) | El backend no está en marcha en el 8080, o `proxy.conf.json` no está activo. Arranca el backend primero; `ng serve` usa el proxy automáticamente. |
| Puerto 4200 ocupado | `ng serve` avisa y propone otro puerto; también puedes forzarlo con `ng serve --port 4201`. |


## Documentación

- Especificación técnica completa: [`docs/SPEC.md`](docs/SPEC.md) — **prevalece** sobre cualquier suposición de este README.
- Reglas para asistentes agénticos: [`AGENTS.md`](AGENTS.md).
