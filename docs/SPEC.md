# Kanban Board — Especificación técnica

Versión del documento: 1.0 (borrador para aprobación) · Fecha: 2026-10-10
Destino: OpenCode (desarrollo) y Raúl Osuna (preparación de entrevista del martes 13 de octubre de 2026).

## 0. Cómo leer este documento

Cada afirmación lleva una etiqueta implícita o explícita:

| Etiqueta | Significado |
|---|---|
| **[REQ]** | Requisito confirmado por el usuario. Vinculante. |
| **[DEC]** | Decisión de diseño. Su estado es *Vinculante* (decidida por el usuario) o *Propuesta* (pendiente de aprobación). |
| **[SUP]** | Suposición por falta de información. Se cambia si el usuario la corrige. |
| **[FUERA]** | Fuera de alcance. OpenCode NO debe implementarlo. |

Regla de oro para OpenCode: si algo no está en este documento, **no se inventa**; se elige la opción más simple que no contradiga el documento y se anota en el README, sección "Notas de implementación".

---

## 1. Resumen, objetivo y alcance

### 1.1 Resumen

Aplicación web de tablero Kanban simplificado (tipo Jira). Hay **proyectos**; cada proyecto contiene **tareas**; cada tarea tiene un estado (`TODO`, `IN_PROGRESS`, `DONE`), una prioridad y una posición dentro de su columna. Las tareas se mueven entre columnas y se reordenan arrastrando y soltando.

### 1.2 Objetivo

Proyecto personal para preparar una entrevista técnica. Debe demostrar, en 4 días de trabajo, dominio práctico del stack del puesto: Java 25, Spring Boot 4, Angular 22, PostgreSQL, Linux, Git y trabajo diario con asistentes de código agénticos. Usuario único (el desarrollador), ejecución local.

### 1.3 Requisitos confirmados [REQ]

| ID | Requisito |
|---|---|
| REQ-01 | Backend en Java 25 y Spring Boot 4.x (última versión estable de la rama 4.x). |
| REQ-02 | Frontend en Angular 22, componentes standalone y signals. Se evalúan Signal Forms (resultado en DECISIÓN-012). |
| REQ-03 | Base de datos PostgreSQL ejecutada con Docker Compose. |
| REQ-04 | Control de versiones con Git. |
| REQ-05 | Incluir `AGENTS.md` (o `CLAUDE.md`) con las reglas para asistentes agénticos. |
| REQ-06 | Todo se levanta en local con instrucciones claras en el README. |
| REQ-07 | Máximo 4 días de trabajo; MVP estricto. |
| REQ-08 | Arquitectura por capas simple: sin microservicios, colas, CQRS formal ni capas artificiales. |
| REQ-09 | Sin dependencias que la biblioteca estándar o Spring ya resuelvan. |
| REQ-10 | Sin autenticación (JWT / Spring Security) en el MVP. |
| REQ-11 | Mover tareas entre columnas con arrastrar y soltar y orden dentro de la columna (P1 = B). |
| REQ-12 | Campos de tarea: `title`, `description`, `status`, `priority`, `position`, `createdAt`, `updatedAt` (P2). |
| REQ-13 | Maven (P4). Flyway con SQL plano y `ddl-auto=validate` (P5). |
| REQ-14 | Borrar un proyecto borra en cascada sus tareas, con confirmación en el frontend (P6). |
| REQ-15 | Código, API y mensajes de error en inglés; interfaz de usuario en español (P7). |
| REQ-16 | Anexo de entrevista con preguntas y respuestas de referencia (verificadas en web). |

### 1.4 Suposiciones [SUP]

- SUP-01: Un solo usuario implícito. No existe entidad `User`.
- SUP-02: Todo corre en la máquina local; no hay despliegue ni entorno de producción.
- SUP-03: Estilos con CSS propio. Sin Angular Material, Tailwind ni librerías de UI.
- SUP-04: Sin paginación (volumen pequeño).
- SUP-05: Sin control de concurrencia optimista; se documenta como limitación.
- SUP-06: La entrevista puede ser en español; por eso la UI va en español (PREGUNTA ABIERTA solo si la entrevista fuese en inglés: cambiar textos de UI no afecta a la arquitectura).

### 1.5 Alcance

**MVP (todo lo siguiente y nada más):**

1. CRUD de proyectos (crear, listar, ver, editar, borrar con cascada).
2. CRUD de tareas dentro de un proyecto (crear siempre en `TODO`, editar título/descripción/prioridad, borrar).
3. Mover una tarea a otra columna y/o a otra posición (endpoint dedicado + arrastrar y soltar).
4. Tablero con tres columnas, contadores por columna y estados de carga, vacío y error.
5. Migraciones Flyway, Docker Compose para PostgreSQL, README, `AGENTS.md`/`CLAUDE.md`.
6. Tests unitarios, de integración (Testcontainers) y de frontend según la sección 12.

**Después del MVP [FUERA de esta versión]:**

- Autenticación y usuarios (Spring Security + JWT), asignación de tareas.
- Etiquetas, fecha límite, comentarios, filtros y búsqueda.
- Todo en Docker Compose (backend + frontend con nginx) y Dockerfiles.
- Documentación OpenAPI (springdoc) y colección de ejemplos.
- Bloqueo optimista (`@Version`) y manejo de conflictos de edición.
- Accesibilidad por teclado del arrastrar y soltar.
- Tests end-to-end (Playwright).

**Fuera de alcance [FUERA]:** sprints, workflows configurables, permisos, notificaciones, WebSockets, i18n multiidioma, modo oscuro, internacionalización, caché, paginación, soft-delete, auditoría, CI/CD.

---

## 2. Versiones verificadas en la web (2026-10-10)

OpenCode debe usar estas versiones y **confirmar** la última patch con el comando indicado antes de fijar nada.

| Componente | Versión a usar | Fuente de verificación | Cómo confirmar |
|---|---|---|---|
| JDK | **25** (LTS, GA 16-sep-2025). Distribución recomendada: Temurin 25. | Notas de Java 25 | `java -version` |
| Spring Boot | **4.1.1** (última estable 4.1.x encontrada, publicada el 20-ago-2026). Si start.spring.io ofrece una patch posterior de 4.1.x (4.1.2…), usar esa. No usar milestones (4.2.0-Mx) ni RC. | spring.io/blog | start.spring.io o `./mvnw -v` + `pom.xml` |
| Spring Framework / Jakarta | Spring Framework 7, Jakarta EE 11 (los trae Boot 4). | Guías de migración Boot 4 | — |
| Angular | **22.0.x** (última patch estable de 22.0 al crear el proyecto). | blog.angular.dev, angular.dev | `ng version` |
| Node.js | **24.x ≥ 24.13.1** (Angular 22.0.x exige `^22.22.0 \|\| ^24.13.1 \|\| ^26.0.0`). | angular.dev (tabla de versiones) | `node -v` |
| TypeScript | **6.0.x** (Angular 22.0.x exige `>=6.0.0 <6.1.0`). Lo instala el CLI. | angular.dev | `npx tsc -v` |
| RxJS | La que instale el CLI (rango `^6.5.3 \|\| ^7.4.0`). | angular.dev | `package.json` |
| PostgreSQL | **18.x** (última estable: 18.6 en ago-2026). Imagen Docker: `postgres:18`. **No usar PostgreSQL 19** (en beta a la fecha). | neon.com/docs, PostgreSQL versioning policy | `docker compose exec db postgres --version` |
| Maven | Maven Wrapper (`./mvnw`) generado por start.spring.io. | — | `./mvnw -v` |

Nota: las versiones de Flyway, Hibernate, Jackson, Testcontainers, JUnit y Mockito **las gestiona el BOM de Spring Boot**. Prohibido fijarlas a mano salvo que un problema real lo exija (y se documenta).

---

## 3. Decisiones de diseño

Estados: **V** = vinculante (decidida por el usuario); **P** = propuesta, pendiente de aprobación (se implementa tal cual salvo que el usuario la cambie).

```text
DECISIÓN-001 [V]
Problema: ¿Estructura del repositorio?
Opciones: A) Un repo con backend/ y frontend/ (monorepo). B) Dos repos.
Decisión: A.
Motivo: Un solo git log, un solo README, un solo AGENTS.md; más fácil de enseñar en entrevista.

DECISIÓN-002 [P]
Problema: ¿Rama de Spring Boot?
Opciones: A) 4.1.x (última). B) 4.0.x (más madura; 4.0.8 en ago-2026).
Decisión: A (4.1.1 o patch posterior).
Motivo: REQ-01 pide la última estable de la rama 4.x. Cambia poco para este proyecto.

DECISIÓN-003 [P]
Problema: ¿Organización de paquetes del backend?
Opciones: A) Por capa (web, service, repository, domain). B) Por funcionalidad (project/, task/).
Decisión: A.
Motivo: REQ-08 pide arquitectura por capas explícita; con dos entidades no hace falta más.

DECISIÓN-004 [P]
Problema: ¿Acceso a datos?
Opciones: A) Spring Data JPA (Hibernate). B) JdbcClient + SQL a mano.
Decisión: A.
Motivo: Es lo que se espera del puesto (Spring + PostgreSQL) y es tema habitual de entrevista (LAZY, N+1, open-in-view).

DECISIÓN-005 [P]
Problema: ¿Cómo se guarda el orden de las tareas?
Opciones: A) Enteros contiguos 0..n-1 por columna (project_id, status), renumerando al mover/borrar. B) Posiciones con huecos. C) Fractional indexing.
Decisión: A.
Motivo: Es lo más simple y correcto para decenas de tareas. Coste de renumerar irrelevante a esta escala. Los límites de A (carrera entre dos pestañas, O(n)) se explican en el Anexo A.

DECISIÓN-006 [P]
Problema: ¿Cómo se expresa mover una tarea en la API?
Opciones: A) Endpoint dedicado PATCH /api/tasks/{id}/move con {status, position}. B) Meter status y position en un PUT/PATCH genérico.
Decisión: A.
Motivo: Mover tiene reglas propias (renumerar dos columnas en una transacción); un endpoint con nombre propio las aísla.

DECISIÓN-007 [P]
Problema: ¿Formato de errores?
Opciones: A) ProblemDetail (RFC 9457) nativo de Spring. B) JSON propio.
Decisión: A, con una extensión `errors` en validaciones.
Motivo: Estándar, ya soportado por Spring 7, y buen tema de entrevista.

DECISIÓN-008 [P]
Problema: ¿Tipo de identificadores?
Opciones: A) BIGINT autogenerado (identity). B) UUID.
Decisión: A (Long en Java).
Motivo: Simplicidad y URLs legibles. No hay requisito de IDs generados en cliente.

DECISIÓN-009 [V]
Problema: ¿Migraciones?
Decisión: Flyway con SQL plano (`V1__init.sql`) y `spring.jpa.hibernate.ddl-auto=validate`. Hibernate nunca modifica el esquema.

DECISIÓN-010 [V]
Problema: ¿Qué se ejecuta en Docker?
Decisión: Solo PostgreSQL en Docker Compose. Backend con `./mvnw spring-boot:run`; frontend con `ng serve` (proxy a /api). Todo en Compose queda como "después del MVP".

DECISIÓN-011 [P]
Problema: ¿Gestión de estado en Angular?
Opciones: A) Signals + servicios (un store ligero para el tablero). B) NgRx.
Decisión: A.
Motivo: REQ-09 y REQ-02 (signals). NgRx es una dependencia que no aporta con este tamaño.

DECISIÓN-012 [P]
Problema: ¿Signal Forms?
Opciones: A) Signal Forms para todos los formularios. B) Reactive Forms. C) Template-driven.
Decisión: A.
Motivo: Son estables en Angular 22 (confirmado en el anuncio oficial). Aprovecha la novedad del puesto y evita mezclar APIs.

DECISIÓN-013 [P]
Problema: ¿Arrastrar y soltar?
Opciones: A) @angular/cdk/drag-drop. B) API HTML5 nativa. C) Librería de terceros.
Decisión: A.
Motivo: Es el paquete oficial; maneja listas conectadas y reordenación. Es la única dependencia de UI añadida.

DECISIÓN-014 [P]
Problema: ¿Cómo se cargan los datos en Angular?
Decisión: `httpResource` (estable en Angular 22) solo para la lista de proyectos (lectura pura). El tablero usa `HttpClient` dentro de `BoardStore` porque necesita actualización optimista y rollback.
Motivo: Muestra ambas APIs con un criterio claro de uso.

DECISIÓN-015 [P]
Problema: ¿Tests de integración contra qué base de datos?
Opciones: A) Testcontainers con PostgreSQL real. B) H2 en memoria.
Decisión: A.
Motivo: REQ explícito de Testcontainers como ejemplo; H2 oculta diferencias con PostgreSQL (checks, tipos, identity).

DECISIÓN-016 [P]
Problema: ¿AGENTS.md o CLAUDE.md?
Decisión: `AGENTS.md` es la fuente de verdad (OpenCode lo lee). `CLAUDE.md` existe y contiene únicamente la línea `@AGENTS.md` (import) más, como máximo, 3 líneas específicas de Claude Code.
Motivo: Una sola fuente de reglas sirve a ambos asistentes y evita duplicación.

DECISIÓN-017 [V]
Problema: ¿Idiomas?
Decisión: Código, nombres de API, mensajes de error de la API y commits en inglés. Textos visibles de la interfaz en español, escritos directamente en las plantillas (sin librería i18n).

DECISIÓN-018 [P]
Problema: ¿Diálogos de formulario y confirmación?
Decisión: Elemento HTML nativo `<dialog>`. Sin Angular Material.

DECISIÓN-019 [V]
Problema: ¿Borrar un proyecto con tareas?
Decisión: Borrado en cascada (FK `ON DELETE CASCADE`). El frontend pide confirmación explícita.

DECISIÓN-020 [P]
Problema: ¿Concurrencia?
Decisión: Sin `@Version` ni bloqueos. Última escritura gana. Documentado como limitación.

DECISIÓN-021 [P]
Problema: ¿CORS?
Decisión: No se configura CORS. En desarrollo el navegador habla con `ng serve` (4200) y este reenvía `/api` al backend (8080) mediante proxy.
Motivo: Evita código y superficie de seguridad innecesarios.

DECISIÓN-022 [P]
Problema: ¿Mapeo entidad ↔ DTO?
Decisión: DTOs como `record` con método estático `from(Entity)`; sin MapStruct ni ModelMapper. Las entidades JPA nunca salen del paquete `service`.
```

---

## 4. Arquitectura

### 4.1 Vista general

```text
┌──────────────────────┐   HTTP /api (proxy en dev)   ┌─────────────────────────────┐   JDBC   ┌──────────────────┐
│ Navegador            │ ───────────────────────────► │ Backend Spring Boot (8080)  │ ───────► │ PostgreSQL 18    │
│ Angular 22 (4200)    │ ◄─────────────────────────── │ web → service → repository │ ◄─────── │ (Docker, 5432)   │
└──────────────────────┘          JSON                └─────────────────────────────┘          └──────────────────┘
```

### 4.2 Backend (capas)

```text
HTTP request
   ↓
web/            Controller  (valida entrada con Bean Validation, llama al servicio, devuelve DTO)
   ↓
service/        Service     (reglas de negocio, transacciones, mover/renumerar, lanza excepciones de dominio)
   ↓
repository/     Spring Data JPA (consultas)
   ↓
domain/         Entidades JPA + enums
   ↓
PostgreSQL (esquema creado por Flyway)

Errores: excepción → web/error/GlobalExceptionHandler → ProblemDetail (JSON)
```

**Reglas de dependencia (obligatorias):**

| Paquete | Puede importar | NO puede importar |
|---|---|---|
| `domain` | solo JDK y anotaciones JPA | `service`, `web`, `repository` |
| `repository` | `domain` | `service`, `web` |
| `service` | `domain`, `repository` | `web` |
| `web` | `service`, DTOs propios | `repository`, entidades (nunca devuelve una entidad) |

### 4.3 Frontend

```text
Ruta (app.routes.ts)
   ↓
Página (project-list-page / board-page)
   ↓
Store o recurso (BoardStore con signals · httpResource en la lista)
   ↓
API (core/api: wrappers finos de HttpClient)
   ↓
Backend /api
```

Reglas: los componentes no llaman a `HttpClient` directamente; solo `core/api` lo hace. La lógica pura de mover tareas vive en `board.utils.ts` (sin Angular) para poder testearla sin TestBed.

---

## 5. Estructura de carpetas y archivos

```text
kanban/
├── AGENTS.md
├── CLAUDE.md
├── README.md
├── .gitignore
├── .env.example
├── compose.yaml
├── docs/
│   └── SPEC.md                      (este documento)
├── backend/
│   ├── pom.xml
│   ├── mvnw, mvnw.cmd, .mvn/
│   └── src/
│       ├── main/java/dev/osuna/kanban/
│       │   ├── KanbanApplication.java
│       │   ├── domain/    Project.java, Task.java, TaskStatus.java, TaskPriority.java
│       │   ├── repository/ ProjectRepository.java, TaskRepository.java
│       │   ├── service/   ProjectService.java, TaskService.java,
│       │   │              ResourceNotFoundException.java, InvalidPositionException.java
│       │   └── web/       ProjectController.java, TaskController.java,
│       │       ├── dto/   ProjectRequest, ProjectResponse, TaskCreateRequest,
│       │       │          TaskUpdateRequest, TaskMoveRequest, TaskResponse  (records)
│       │       └── error/ GlobalExceptionHandler.java
│       ├── main/resources/
│       │   ├── application.yaml
│       │   └── db/migration/V1__init.sql
│       └── test/java/dev/osuna/kanban/
│           ├── service/   TaskServiceTest.java, ProjectServiceTest.java   (unitarios, Mockito)
│           ├── support/   PostgresContainerConfig.java                    (Testcontainers)
│           └── web/       ProjectApiIntegrationTest.java, TaskApiIntegrationTest.java
└── frontend/
    ├── angular.json, package.json, tsconfig*.json, proxy.conf.json
    └── src/app/
        ├── app.ts, app.config.ts, app.routes.ts
        ├── core/
        │   ├── api/     project-api.ts, task-api.ts
        │   ├── models/  project.model.ts, task.model.ts, problem-detail.model.ts
        │   └── error/   error-message.ts
        └── features/
            ├── projects/ project-list-page.ts, project-form-dialog.ts
            └── board/    board-page.ts, board.store.ts, board.utils.ts,
                          board-column.ts, task-card.ts, task-form-dialog.ts
```

Los nombres de archivo del frontend siguen los valores por defecto de `ng generate` de Angular 22. No añadir sufijos manualmente.

### 5.1 Responsabilidad de cada pieza (backend)

| Archivo | Responsabilidad | No debe encargarse de |
|---|---|---|
| `domain/Project` | Entidad JPA de `project`. | Lógica de negocio, DTOs, relación inversa a tareas (no hay `@OneToMany`). |
| `domain/Task` | Entidad JPA de `task`; `@ManyToOne(fetch = LAZY)` a `Project`. | Reglas de reordenación. |
| `domain/TaskStatus` | Enum `TODO, IN_PROGRESS, DONE` (en ese orden; el orden de declaración define el orden de columnas). | Texto de interfaz. |
| `domain/TaskPriority` | Enum `LOW, MEDIUM, HIGH`. | Texto de interfaz. |
| `repository/*` | Interfaces Spring Data. Consultas listadas en la sección 6.3. | Lógica. |
| `service/ProjectService` | CRUD de proyectos, transaccional; lanza `ResourceNotFoundException`. | HTTP, DTOs de entrada. |
| `service/TaskService` | CRUD de tareas, creación al final de `TODO`, **mover**, renumerar al borrar. | HTTP, serialización. |
| `web/*Controller` | Mapeo de rutas, `@Valid`, códigos HTTP, conversión entidad→DTO. | Reglas de negocio, acceso a repositorios. |
| `web/error/GlobalExceptionHandler` | Convertir excepciones a `ProblemDetail`. | Lógica de negocio. |
| `application.yaml` | Configuración (sección 10). | Secretos reales. |

### 5.2 Responsabilidad de cada pieza (frontend)

| Archivo | Responsabilidad | No debe encargarse de |
|---|---|---|
| `core/api/*` | Una función por endpoint; devuelve `Observable`. | Estado, mensajes de UI. |
| `core/error/error-message.ts` | Convertir `HttpErrorResponse` en un texto en español para el usuario. | Mostrarlo. |
| `board.utils.ts` | Función pura que, dada una lista de tareas, un id, un estado destino y un índice, devuelve la nueva lista con posiciones renumeradas. | Angular, HTTP. |
| `board.store.ts` | Signals del tablero (`tasks`, `loading`, `error`), `columns` calculado, acciones (cargar, crear, editar, borrar, mover con rollback). | Plantillas, drag-drop de CDK. |
| `board-page.ts` | Leer el `:id` de la ruta, cargar el proyecto y el store, componer columnas y diálogos. | Lógica de mover. |
| `board-column.ts` | Una columna: título, contador, lista `cdkDropList`; emite el evento de soltar. | Llamadas HTTP. |
| `task-card.ts` | Mostrar una tarea y emitir editar/borrar. | Estado. |
| `task-form-dialog.ts` | Formulario (Signal Forms) en `<dialog>` para crear/editar tarea. | Llamadas HTTP (emite el valor al padre). |
| `project-list-page.ts` | Lista de proyectos con `httpResource`; crear/editar/borrar. | Tablero. |
| `project-form-dialog.ts` | Formulario (Signal Forms) en `<dialog>` para crear/editar proyecto. | HTTP. |

---

## 6. Modelo de datos

### 6.1 Entidades

```text
Project
- id: Long (BIGINT, identity, PK)
- name: String (obligatorio, 1–100 tras trim)
- description: String (opcional, máx. 1000)
- createdAt: Instant (obligatorio, lo fija el sistema)
- updatedAt: Instant (obligatorio, lo fija el sistema)

Task
- id: Long (PK)
- project: Project (obligatorio, FK, ON DELETE CASCADE)
- title: String (obligatorio, 1–200 tras trim)
- description: String (opcional, máx. 2000)
- status: TaskStatus (obligatorio, defecto TODO)
- priority: TaskPriority (obligatorio, defecto MEDIUM)
- position: int (obligatorio, >= 0; índice dentro de (project, status))
- createdAt: Instant
- updatedAt: Instant
```

Relación: `Project 1 ── * Task`. Solo existe el lado `Task → Project` en JPA.

**Invariante de posición:** para cada par `(project_id, status)`, las posiciones forman exactamente la secuencia `0, 1, …, n-1` sin huecos ni repetidos. Toda operación que cree, mueva o borre tareas debe dejar esa invariante cumplida al terminar la transacción. No hay restricción UNIQUE en BD (se evita para no complicar la renumeración); la invariante la garantiza `TaskService` y la comprueban los tests.

### 6.2 Migración V1 (`db/migration/V1__init.sql`)

```sql
CREATE TABLE project (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name        VARCHAR(100)  NOT NULL,
  description VARCHAR(1000),
  created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE task (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  project_id  BIGINT        NOT NULL REFERENCES project (id) ON DELETE CASCADE,
  title       VARCHAR(200)  NOT NULL,
  description VARCHAR(2000),
  status      VARCHAR(20)   NOT NULL DEFAULT 'TODO',
  priority    VARCHAR(10)   NOT NULL DEFAULT 'MEDIUM',
  position    INTEGER       NOT NULL,
  created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CONSTRAINT ck_task_status   CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
  CONSTRAINT ck_task_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
  CONSTRAINT ck_task_position CHECK (position >= 0)
);

CREATE INDEX ix_task_board ON task (project_id, status, position);
```

Mapeo JPA obligatorio: enums con `@Enumerated(EnumType.STRING)`; `Instant` para `created_at`/`updated_at` (`@CreationTimestamp` y `@UpdateTimestamp` de Hibernate); `GenerationType.IDENTITY`.

### 6.3 Estrategia de migraciones

- Flyway ejecuta las migraciones al arrancar el backend (y en los tests con Testcontainers).
- Las migraciones aplicadas **nunca se editan**; un cambio de esquema = nuevo `V2__…sql`.
- `ddl-auto=validate`: si las entidades no coinciden con el esquema, el arranque falla (comportamiento deseado).
- No hay datos semilla (seed) en el MVP.

Consultas obligatorias en `TaskRepository` (nombres orientativos; el comportamiento es vinculante):

| Método | Devuelve |
|---|---|
| `findByProjectIdAndStatusOrderByPositionAsc(projectId, status)` | Tareas de una columna ordenadas. |
| `findByProjectId(projectId)` | Todas las tareas del proyecto (el servicio las ordena por `status` según el orden del enum y luego por `position`). |
| `countByProjectIdAndStatus(projectId, status)` | Tamaño de una columna. |

`ProjectRepository`: `findAllByOrderByCreatedAtDesc()` además de lo heredado de `JpaRepository`.

---

## 7. Contrato de la API REST

Base: `/api`. Formato: JSON UTF-8. Fechas en ISO-8601 UTC (ej. `2026-10-10T09:15:30.123Z`). Sin autenticación.

### 7.1 Tabla de endpoints

| Método y ruta | Entrada | Éxito | Errores |
|---|---|---|---|
| `GET /api/projects` | — | 200, lista de `Project` (más reciente primero; `[]` si no hay) | — |
| `POST /api/projects` | `{name, description?}` | 201 + cabecera `Location: /api/projects/{id}` + `Project` | 400 |
| `GET /api/projects/{id}` | — | 200 `Project` | 404 |
| `PUT /api/projects/{id}` | `{name, description?}` (reemplaza ambos campos) | 200 `Project` | 400, 404 |
| `DELETE /api/projects/{id}` | — | 204 (borra también sus tareas) | 404 |
| `GET /api/projects/{projectId}/tasks` | — | 200, lista de `Task` ordenada por columna (TODO, IN_PROGRESS, DONE) y luego por `position`; `[]` si no hay | 404 (proyecto) |
| `POST /api/projects/{projectId}/tasks` | `{title, description?, priority?}` | 201 + `Location: /api/tasks/{id}` + `Task` (status `TODO`, position = tamaño de la columna TODO antes de insertar, priority por defecto `MEDIUM`) | 400, 404 (proyecto) |
| `GET /api/tasks/{id}` | — | 200 `Task` | 404 |
| `PUT /api/tasks/{id}` | `{title, description?, priority}` (reemplaza esos tres campos; **no** toca status ni position) | 200 `Task` | 400, 404 |
| `PATCH /api/tasks/{id}/move` | `{status, position}` | 200 `Task` actualizada | 400, 404 |
| `DELETE /api/tasks/{id}` | — | 204; renumera la columna de origen | 404 |

Cualquier otra ruta o método no listado: respuesta estándar de Spring (404/405) en formato ProblemDetail.

### 7.2 Representaciones

```json
// Project
{ "id": 1, "name": "Entrevista", "description": null,
  "createdAt": "2026-10-10T09:15:30.123Z", "updatedAt": "2026-10-10T09:15:30.123Z" }

// Task
{ "id": 12, "projectId": 1, "title": "Escribir README", "description": null,
  "status": "TODO", "priority": "MEDIUM", "position": 0,
  "createdAt": "2026-10-10T09:20:00.000Z", "updatedAt": "2026-10-10T09:20:00.000Z" }
```

### 7.3 Reglas de validación

| Campo | Regla | Mensaje (inglés) |
|---|---|---|
| `Project.name` | no nulo, tras `trim` longitud 1–100 | `must not be blank` / `size must be between 1 and 100` |
| `Project.description` | máx. 1000 | `size must be between 0 and 1000` |
| `Task.title` | no nulo, tras `trim` longitud 1–200 | idem |
| `Task.description` | máx. 2000 | idem |
| `Task.priority` (PUT) | obligatorio; uno de `LOW`, `MEDIUM`, `HIGH` | `must not be null` |
| `move.status` | obligatorio; uno de `TODO`, `IN_PROGRESS`, `DONE` | `must not be null` |
| `move.position` | obligatorio, entero >= 0 | `must be greater than or equal to 0` |

Los textos de `title`/`name` se guardan con `trim()` aplicado. Propiedades JSON desconocidas se ignoran. Un valor de enum no reconocido produce 400.

### 7.4 Semántica de `PATCH /api/tasks/{id}/move`

Sea `T` la tarea, `S` su estado actual, `D` el estado destino y `P` la posición destino.

1. Si `T` no existe → 404.
2. Se construye la lista de la columna destino **sin `T`** (si `D == S`, es la columna actual sin `T`; si no, la columna `D` tal cual).
3. Si `P > tamaño de esa lista` → 400 con `detail` = `Position P is out of range for column D (max N)`.
4. Se inserta `T` en el índice `P` de esa lista y `T.status = D`.
5. Se renumeran `0..n-1` la columna destino y, si `D != S`, también la columna origen (sin `T`).
6. Todo ocurre en **una sola transacción**. Si falla algo, no se persiste ningún cambio.
7. Si `D == S` y `P` es la posición actual de `T` → no hay cambios; responde 200 con la tarea tal cual (sin error).
8. Respuesta: 200 con la `Task` actualizada.

Ejemplo: columna `TODO = [A, B, C]`, `IN_PROGRESS = []`. `PATCH` de `A` con `{status:"IN_PROGRESS", position:0}` → `TODO = [B(0), C(1)]`, `IN_PROGRESS = [A(0)]`.

### 7.5 Formato de errores (ProblemDetail, RFC 9457)

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/projects",
  "errors": [ { "field": "name", "message": "must not be blank" } ]
}
```

| Situación | Código | `title` | `detail` |
|---|---|---|---|
| Validación de cuerpo (`@Valid`) | 400 | `Bad Request` | `Validation failed` + `errors[]` con `field` y `message` |
| JSON ilegible o enum desconocido | 400 | `Bad Request` | `Malformed request body` |
| `id` de ruta no numérico | 400 | `Bad Request` | `Invalid value for parameter 'id'` |
| Posición fuera de rango al mover | 400 | `Bad Request` | `Position P is out of range for column D (max N)` |
| Recurso inexistente | 404 | `Not Found` | `Project 99 not found` / `Task 99 not found` |
| Método no soportado | 405 | `Method Not Allowed` | mensaje de Spring |
| Error inesperado | 500 | `Internal Server Error` | `Unexpected error` (**sin** stack trace ni detalles internos) |

El campo `errors` solo aparece en errores de validación de cuerpo. El 500 se registra en el log del servidor con su traza.

---

## 8. Comportamiento del frontend

### 8.1 Rutas

| Ruta | Página | Notas |
|---|---|---|
| `/` | redirige a `/projects` | |
| `/projects` | `project-list-page` | Lista de proyectos. |
| `/projects/:id` | `board-page` | Tablero del proyecto. `:id` no numérico o inexistente → estado "no encontrado". |
| `**` | redirige a `/projects` | |

### 8.2 Textos de interfaz (español, literales)

| Elemento | Texto |
|---|---|
| Columnas | `Por hacer`, `En curso`, `Hecha` |
| Prioridades | `Baja`, `Media`, `Alta` |
| Carga | `Cargando…` |
| Lista de proyectos vacía | `Aún no hay proyectos. Crea el primero.` |
| Columna vacía | `Sin tareas` |
| Error de carga | `No se pudieron cargar los datos.` + botón `Reintentar` |
| Proyecto no encontrado | `Proyecto no encontrado.` + enlace `Volver a proyectos` |
| Error en una acción | `No se pudo completar la acción. Inténtalo de nuevo.` (si la API devuelve `detail` de validación, mostrar ese texto tal cual) |
| Confirmar borrar proyecto | `Se eliminará el proyecto y todas sus tareas. Esta acción no se puede deshacer.` |
| Confirmar borrar tarea | `¿Eliminar esta tarea?` |
| Validación: obligatorio | `Este campo es obligatorio.` |
| Validación: longitud | `Máximo N caracteres.` |
| Botones | `Nuevo proyecto`, `Nueva tarea`, `Guardar`, `Cancelar`, `Editar`, `Eliminar` |

### 8.3 Pantalla "Proyectos"

- Muestra tarjetas o filas con nombre y descripción (si existe). Clic en el nombre navega al tablero.
- Acciones por proyecto: `Editar` (abre diálogo con el formulario) y `Eliminar` (abre diálogo de confirmación).
- Acción global: `Nuevo proyecto` (diálogo con formulario).
- Carga con `httpResource`. Tras crear/editar/borrar con éxito se llama a `reload()` del recurso.
- Estados: **carga** (`Cargando…`), **vacío**, **error** (con `Reintentar` que llama a `reload()`), **datos**.

### 8.4 Pantalla "Tablero"

- Cabecera: enlace `Volver a proyectos`, nombre del proyecto y botón `Nueva tarea`.
- Tres columnas en este orden: `Por hacer`, `En curso`, `Hecha`. Cada una muestra su contador de tareas.
- Tarjeta de tarea: título, insignia de prioridad, descripción recortada a 2 líneas con CSS, botones `Editar` y `Eliminar`.
- `Nueva tarea` crea siempre en `Por hacer`, al final.
- Arrastrar y soltar con CDK: una tarea se puede soltar en cualquier columna (incluida la propia) y en cualquier posición.
- Estados: **carga** inicial; **error** de carga con `Reintentar`; **404** de proyecto; **columna vacía**; **datos**.
- Los errores de acciones (crear, editar, borrar, mover) se muestran en un aviso (`role="alert"`) en la parte superior del tablero, descartable.

### 8.5 Flujo de mover (arrastrar y soltar)

```text
Usuario suelta tarjeta
   ↓
board-column emite {taskId, targetStatus, targetIndex}
   ↓
BoardStore.move():
   1. snapshot = copia de tasks()
   2. tasks.set( moveTask(tasks(), taskId, targetStatus, targetIndex) )   ← actualización optimista (board.utils)
   3. PATCH /api/tasks/{id}/move {status, position}
        ├─ éxito → recarga silenciosa GET /api/projects/{id}/tasks y tasks.set(respuesta)   (reconciliación; sin spinner)
        └─ error → tasks.set(snapshot) (rollback) + aviso de error
```

Si origen y destino (estado e índice) coinciden, no se hace ninguna petición.

### 8.5.1 Reglas de `moveTask` (función pura)

- Entrada: lista de tareas del proyecto, `taskId`, `targetStatus`, `targetIndex`.
- Salida: lista nueva (no muta la original) con la tarea en su nuevo estado e índice y las posiciones de las columnas afectadas renumeradas `0..n-1`.
- Mismo algoritmo que la sección 7.4. Si `targetIndex` excede el tamaño, se ajusta al final (en el cliente no se lanza error, porque el CDK nunca produce índices fuera de rango).

### 8.6 Formularios (Signal Forms)

- Proyecto: `name` (obligatorio, máx. 100), `description` (opcional, máx. 1000).
- Tarea: `title` (obligatorio, máx. 200), `description` (opcional, máx. 2000), `priority` (selector `Baja/Media/Alta`, defecto `Media`).
- Los formularios usan la API estable `form()` de `@angular/forms/signals` con los validadores integrados (`required`, `maxLength`). OpenCode debe consultar la guía oficial de Signal Forms en angular.dev para la sintaxis exacta de la versión instalada; no usar `FormGroup`/`FormControl`/`ngModel`.
- Botón `Guardar` deshabilitado mientras el formulario sea inválido o haya una petición en curso.
- Los errores de validación se muestran bajo el campo con los textos de 8.2. El cliente replica las reglas del servidor; el servidor sigue siendo la autoridad.

### 8.7 Convenciones de Angular

- Componentes `standalone` (por defecto en 22; no declarar `standalone: true` si el CLI no lo genera).
- Detección de cambios `OnPush` (por defecto en Angular 22; no hace falta declararlo). Estado local con `signal`/`computed`; sin `zone.js` si el CLI generó el proyecto sin él.
- Plantillas con control de flujo nativo (`@if`, `@for` con `track`, `@switch`). Prohibido `*ngIf`/`*ngFor`.
- `inject()` en lugar de constructor para DI. `HttpClient` con `provideHttpClient()` (en Angular 22 usa Fetch por defecto; no añadir `withFetch()` redundante).
- TypeScript en modo `strict` (el que genera el CLI). Prohibido `any`.
- Proxy de desarrollo (`proxy.conf.json`): `/api` → `http://localhost:8080`, `changeOrigin: true`. Se referencia desde `angular.json` (`serve.options.proxyConfig`).

---

## 9. Dependencias

### 9.1 Backend

| Tipo | Dependencia | Motivo |
|---|---|---|
| Estándar | JDK 25 (`java.time`, `java.util`) | Fechas con `Instant`; nada de librerías de fechas. |
| Externa | `spring-boot-starter-webmvc` | API REST (en Boot 4 sustituye a `spring-boot-starter-web`). |
| Externa | `spring-boot-starter-validation` | Bean Validation en DTOs. |
| Externa | `spring-boot-starter-data-jpa` | JPA/Hibernate + Spring Data. |
| Externa | `spring-boot-starter-flyway` + `org.flywaydb:flyway-database-postgresql` | Migraciones; el módulo específico de PostgreSQL es necesario en Flyway ≥ 10. |
| Externa (runtime) | `org.postgresql:postgresql` | Driver JDBC. |
| Test | `spring-boot-starter-test` y `spring-boot-starter-webmvc-test` | JUnit 5, AssertJ, Mockito, MockMvc. |
| Test | `spring-boot-testcontainers` + módulo PostgreSQL de Testcontainers | Integración con PostgreSQL real y `@ServiceConnection`. Consultar la documentación de Boot 4.1 para el `artifactId` exacto del módulo (Testcontainers 2.x renombró módulos con prefijo `testcontainers-`). |
| Dev | Maven Wrapper | Build reproducible. |

**Prohibido añadir:** Lombok, MapStruct, ModelMapper, Spring Security, springdoc, Actuator, DevTools, H2, Hibernate Envers, Liquibase, Spring Boot Docker Compose support (el Compose se gestiona a mano).

> Nota: Boot 4 modularizó los starters. Si algún test necesita un starter de test específico (por ejemplo para `@DataJpaTest`), añadirlo y anotarlo en el README. No usar `spring-boot-starter-classic` ni `spring-boot-starter-test-classic` (puentes temporales).

### 9.2 Frontend

| Tipo | Dependencia | Motivo |
|---|---|---|
| Externa | `@angular/core`, `common`, `forms`, `router`, `platform-browser`, `compiler`, `rxjs` | Los instala `ng new`. |
| Externa | `@angular/cdk` (misma versión mayor que Angular) | Solo `drag-drop`. |
| Dev | `@angular/cli`, `@angular/build`, `typescript` 6.0.x, el ejecutor de tests que genere `ng new` | Los instala `ng new`. |

**Prohibido añadir:** Angular Material, Tailwind, Bootstrap, NgRx, lodash, date-fns, moment, axios, librerías de iconos (usar texto o símbolos Unicode). Fechas, si se muestran, con `DatePipe`.

### 9.3 Herramientas del entorno

| Herramienta | Versión |
|---|---|
| JDK | 25 |
| Node.js / npm | 24.x ≥ 24.13.1 / el de Node |
| Docker + Docker Compose v2 | actuales; Docker en marcha es **necesario también para los tests del backend** |
| Git | cualquier versión actual |

---

## 10. Configuración

### 10.1 Puertos

| Servicio | Puerto |
|---|---|
| PostgreSQL | 5432 (configurable con `POSTGRES_PORT`) |
| Backend | 8080 (configurable con `SERVER_PORT`) |
| Frontend (`ng serve`) | 4200 |

### 10.2 Variables de entorno

| Variable | Usada por | Defecto | Notas |
|---|---|---|---|
| `POSTGRES_DB` | compose | `kanban` | |
| `POSTGRES_USER` | compose | `kanban` | |
| `POSTGRES_PASSWORD` | compose | `kanban_dev` | Credencial **solo de desarrollo local**. |
| `POSTGRES_PORT` | compose | `5432` | Puerto publicado en el host. |
| `DB_URL` | backend | `jdbc:postgresql://localhost:5432/kanban` | Si se cambia `POSTGRES_PORT`, cambiar aquí también. |
| `DB_USER` | backend | `kanban` | |
| `DB_PASSWORD` | backend | `kanban_dev` | |
| `SERVER_PORT` | backend | `8080` | Si se cambia, actualizar `proxy.conf.json`. |

`.env.example` documenta estas variables. `.env` está en `.gitignore`. No hay secretos reales en el repositorio.

### 10.3 `application.yaml` (valores obligatorios)

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/kanban}
    username: ${DB_USER:kanban}
    password: ${DB_PASSWORD:kanban_dev}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
server:
  port: ${SERVER_PORT:8080}
```

No hay perfiles propios en el MVP. Los tests no usan perfil: Testcontainers sustituye el `DataSource` mediante `@ServiceConnection`. No hay perfil de producción [FUERA].

### 10.4 Docker Compose (`compose.yaml`)

Un único servicio `db`:

- Imagen `postgres:18`.
- Variables `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` con los defectos de 10.2 (`${VAR:-defecto}`).
- Puerto `${POSTGRES_PORT:-5432}:5432`.
- Volumen con nombre `kanban_pgdata` para persistir datos. **Verificar en la documentación de la imagen oficial `postgres:18`** la ruta de montaje correcta (en la 18 cambió el directorio de datos por defecto y se recomienda montar en `/var/lib/postgresql`); no asumir la ruta de versiones anteriores.
- `healthcheck` con `pg_isready -U $POSTGRES_USER -d $POSTGRES_DB`, intervalo 5 s, 10 reintentos.
- `restart: unless-stopped` no se usa (es un entorno de desarrollo).

Comandos de referencia: `docker compose up -d` (arrancar), `docker compose down` (parar conservando datos), `docker compose down -v` (parar y **borrar** datos).

### 10.5 `.gitignore` (mínimo)

`.env`, `backend/target/`, `frontend/node_modules/`, `frontend/dist/`, `frontend/.angular/`, `.idea/`, `.vscode/` (salvo que se decida compartirlo), `*.log`, `.DS_Store`.

### 10.6 Git

- Rama principal `main`. Commits en inglés con Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`).
- Mínimo un commit por tarea terminada; nunca commits gigantes que mezclen backend y frontend sin motivo.
- El repositorio se inicializa en la Fase 1 con el primer commit `chore: initial project structure`.

---

## 11. Errores y casos límite

| # | Caso | Comportamiento esperado |
|---|---|---|
| E-01 | `GET/PUT/DELETE /api/projects/{id}` con id inexistente | 404 ProblemDetail `Project {id} not found`. |
| E-02 | Operación sobre tarea inexistente (`GET`, `PUT`, `DELETE`, `move`) | 404 `Task {id} not found`. |
| E-03 | Crear tarea en proyecto inexistente | 404 `Project {id} not found`; no se crea nada. |
| E-04 | `name`/`title` vacío o solo espacios | 400 con `errors[0].field` correcto. |
| E-05 | `name` de 101 caracteres / `title` de 201 | 400. Exactamente 100/200 → válido. |
| E-06 | `priority` o `status` con valor desconocido | 400 `Malformed request body`. |
| E-07 | `position` negativa en `move` | 400 (validación). |
| E-08 | `position` mayor que el tamaño de la columna destino | 400 `Position P is out of range…`; ningún cambio persistido. |
| E-09 | Mover una tarea a su misma posición | 200 sin cambios. |
| E-10 | Mover a una columna vacía con `position = 0` | 200; la columna origen queda renumerada. |
| E-11 | Mover a una columna vacía con `position = 1` | 400 (fuera de rango). |
| E-12 | Borrar la tarea del medio de una columna | 204; las posiciones de esa columna quedan `0..n-1`. |
| E-13 | Borrar un proyecto con tareas | 204; no quedan filas en `task` con ese `project_id`. |
| E-14 | `id` de ruta no numérico (`/api/tasks/abc`) | 400 `Invalid value for parameter 'id'`. |
| E-15 | Cuerpo JSON mal formado | 400 `Malformed request body`. |
| E-16 | Base de datos no disponible al arrancar | El backend falla al arrancar con el error de conexión (comportamiento por defecto de Spring); README indica `docker compose up -d` primero. |
| E-17 | Base de datos caída con el backend en marcha | 500 `Unexpected error`, sin detalles internos. |
| E-18 | Backend caído y el frontend intenta cargar | Estado de **error** con `Reintentar`. |
| E-19 | Fallo de red al mover | Rollback de la UI al estado anterior + aviso de error. |
| E-20 | Dos pestañas mueven tareas a la vez | Última escritura gana; la reconciliación silenciosa tras mover deja la UI consistente con el servidor. Limitación documentada (SUP-05). |
| E-21 | Doble clic en `Guardar` | El botón se deshabilita durante la petición; solo se envía una. |
| E-22 | Puerto 5432 u 8080 ocupado | Documentado en README (troubleshooting) con las variables de 10.2. |

---

## 12. Estrategia de tests

### 12.1 Qué se prueba y dónde

| Nivel | Qué | Herramientas | Obligatorio |
|---|---|---|---|
| Unitario backend | `TaskService` (crear, mover, borrar y renumerar) y `ProjectService` con repositorios simulados | JUnit 5 + Mockito + AssertJ | Sí |
| Integración backend | Endpoints completos (HTTP → servicio → PostgreSQL real con Flyway) | `@SpringBootTest` + MockMvc + Testcontainers (`@ServiceConnection`) | Sí |
| Unitario frontend | `board.utils.moveTask` (función pura) | Ejecutor de tests de `ng new` | Sí |
| Componente frontend | Formulario de tarea (validación) y `BoardStore`/página con `HttpTestingController` (estados carga, vacío, error, rollback al mover) | Ejecutor de tests de `ng new` + utilidades de testing de Angular | Sí |
| E2E | — | — | [FUERA] |

Todos los tests del backend se ejecutan con `./mvnw test` (no se separan en surefire/failsafe). Los tests de integración requieren Docker en marcha. Los de frontend con `npm test` (o el script que genere `ng new`; ejecución no interactiva con el modo `--no-watch` o equivalente de la versión instalada).

Cada test de integración empieza con la base de datos limpia (por ejemplo, borrar `project` al inicio de cada test con `@BeforeEach`; el cascade borra `task`). Un único contenedor PostgreSQL compartido por toda la ejecución de tests.

### 12.2 Escenarios (Dado/Cuando/Entonces)

```text
TEST-001  (integración)
Dado: no hay proyectos
Cuando: POST /api/projects {name:"Demo"}
Entonces: 201, cabecera Location, cuerpo con id, name "Demo", createdAt y updatedAt no nulos

TEST-002  (integración)
Dado: POST /api/projects {name:"   "}
Entonces: 400, errors contiene field "name"

TEST-003  (integración)
Dado: GET /api/projects/999
Entonces: 404, detail "Project 999 not found"

TEST-004  (integración)
Dado: un proyecto con 2 tareas
Cuando: DELETE /api/projects/{id}
Entonces: 204 y GET /api/projects/{id}/tasks devuelve 404; no quedan filas de task con ese project_id

TEST-005  (integración)
Dado: un proyecto vacío
Cuando: se crean tareas A, B y C con POST
Entonces: todas en TODO con position 0, 1 y 2 respectivamente

TEST-006  (integración)
Dado: TODO=[A,B,C]
Cuando: PATCH move de A a {IN_PROGRESS, 0}
Entonces: 200; GET tasks → TODO=[B(0),C(1)], IN_PROGRESS=[A(0)]

TEST-007  (integración)
Dado: TODO=[A,B,C]
Cuando: PATCH move de A a {TODO, 2}
Entonces: TODO=[B(0),C(1),A(2)]

TEST-008  (integración)
Dado: TODO=[A,B,C]
Cuando: PATCH move de C a {TODO, 0}
Entonces: TODO=[C(0),A(1),B(2)]

TEST-009  (integración)
Dado: TODO=[A,B], IN_PROGRESS vacío
Cuando: PATCH move de A a {IN_PROGRESS, 1}
Entonces: 400 y el estado de las tareas no cambia

TEST-010  (integración)
Dado: TODO=[A,B,C]
Cuando: DELETE de B
Entonces: 204; TODO=[A(0),C(1)]

TEST-011  (integración)
Dado: una tarea con priority HIGH
Cuando: PUT con {title:"Nuevo", priority:"LOW"}
Entonces: 200; title y priority cambian; status y position no cambian; updatedAt >= createdAt

TEST-012  (integración)
Dado: PATCH move con {status:"BLOCKED", position:0}
Entonces: 400 "Malformed request body"

TEST-013  (integración)
Dado: GET /api/tasks/abc
Entonces: 400 "Invalid value for parameter 'id'"

TEST-014  (integración)
Dado: POST de tarea con title de 201 caracteres
Entonces: 400; con 200 caracteres → 201

TEST-015  (unitario backend)
Dado: TaskService con columna TODO=[A(0),B(1),C(2)] simulada
Cuando: move(A, TODO, 0)
Entonces: no se modifica ninguna posición

TEST-016  (unitario backend)
Dado: la misma columna
Cuando: move(A, IN_PROGRESS, 5) con IN_PROGRESS vacío
Entonces: lanza InvalidPositionException y no se guarda nada

TEST-017  (unitario backend)
Dado: cualquier secuencia de create/move/delete sobre un proyecto
Entonces: tras cada operación las posiciones de cada columna son exactamente 0..n-1

TEST-018  (unitario frontend)
Dado: tareas TODO=[A,B,C]
Cuando: moveTask(tareas, A, "IN_PROGRESS", 0)
Entonces: lista nueva con A en IN_PROGRESS(0), TODO=[B(0),C(1)]; la lista original no se modifica

TEST-019  (unitario frontend)
Dado: tareas TODO=[A,B,C]
Cuando: moveTask(tareas, A, "TODO", 2)
Entonces: TODO=[B(0),C(1),A(2)]

TEST-020  (componente frontend)
Dado: el formulario de tarea con título vacío
Entonces: muestra "Este campo es obligatorio." y "Guardar" está deshabilitado

TEST-021  (componente frontend)
Dado: la lista de proyectos con respuesta []
Entonces: muestra "Aún no hay proyectos. Crea el primero."

TEST-022  (componente frontend)
Dado: la carga del tablero responde 500
Entonces: muestra "No se pudieron cargar los datos." y un botón "Reintentar"

TEST-023  (componente frontend)
Dado: un tablero cargado y PATCH move responde 500
Cuando: se solicita mover una tarea
Entonces: la tarea vuelve a su posición original y se muestra el aviso de error
```

---

## 13. Plan de fases (4 días)

> **Aviso de calendario:** el plan está escrito para vie-sáb-dom-lun. Hoy es sábado 10 de octubre. Si la Fase 1 no se hizo el viernes, se hace **Fase 1 + Fase 2 el sábado**, Fase 3 el domingo y Fase 4 el lunes. La entrevista es el martes 13: el lunes por la tarde no se empieza nada nuevo.

| Fase | Día | Entrega funcional (qué debe funcionar al terminar) |
|---|---|---|
| **1. Base** | Vie | Repo Git con `main`. Estructura backend/frontend. `compose.yaml` con PostgreSQL. Backend arranca, Flyway aplica `V1`. CRUD de proyectos por API (`curl`). `AGENTS.md` y `CLAUDE.md`. README mínimo con arranque. Test de integración de proyectos (TEST-001…004). |
| **2. Tareas en el backend** | Sáb | CRUD de tareas, `move` con renumeración, ProblemDetail completo, todos los casos E-01…E-15. Tests TEST-005…017 en verde. |
| **3. Frontend** | Dom | Angular 22 creado con proxy. Lista de proyectos (CRUD con `httpResource`). Tablero con columnas, formularios (Signal Forms), crear/editar/borrar tareas y arrastrar y soltar con rollback. Estados de carga/vacío/error. |
| **4. Cierre** | Lun | Tests de frontend (TEST-018…023). README completo con troubleshooting. Revisión de criterios de aceptación AC-001…AC-024. Repaso del Anexo A. Etiqueta Git `v1.0.0`. |

Regla de recorte si hay retraso (en este orden): (1) quitar la reconciliación silenciosa tras mover; (2) sustituir arrastrar y soltar por un botón `Mover a…` por tarjeta (el backend ya soporta ambos; **requiere aprobación del usuario**); (3) recortar tests de componente de frontend, manteniendo TEST-018/019. No recortar: tests de integración del backend, `AGENTS.md`, README.

---

## 14. Criterios de aceptación

```text
AC-001  `docker compose up -d` deja el servicio db en estado "healthy" (docker compose ps).
AC-002  `cd backend && ./mvnw spring-boot:run` arranca sin errores y el log muestra que Flyway aplicó la migración V1.
AC-003  Con el backend arrancado, `GET /api/projects` devuelve 200 y `[]` en una base vacía.
AC-004  `POST /api/projects` con {"name":"Demo"} devuelve 201, cabecera Location y un id.
AC-005  `POST /api/projects` con {"name":""} devuelve 400 con application/problem+json y errors[].field = "name".
AC-006  `GET /api/projects/999` devuelve 404 con detail "Project 999 not found".
AC-007  Crear tres tareas en un proyecto las deja en TODO con position 0, 1, 2.
AC-008  `PATCH /api/tasks/{id}/move` con una posición válida devuelve 200 y GET de tareas refleja el nuevo orden con posiciones 0..n-1 en ambas columnas.
AC-009  `PATCH /api/tasks/{id}/move` con una posición fuera de rango devuelve 400 y no cambia ningún dato.
AC-010  `DELETE /api/projects/{id}` devuelve 204 y elimina también sus tareas (la consulta `SELECT count(*) FROM task WHERE project_id = {id}` devuelve 0).
AC-011  Los datos persisten tras `docker compose down` + `docker compose up -d` (sin `-v`) y reiniciar el backend.
AC-012  `cd backend && ./mvnw test` termina con 0 fallos con Docker en marcha, incluyendo los tests con Testcontainers.
AC-013  Con `spring.jpa.hibernate.ddl-auto=validate`, el backend no modifica el esquema (no existe ninguna sentencia DDL en el log salvo las de Flyway).
AC-014  `cd frontend && npm install && npm start` sirve la app en http://localhost:4200 sin errores de compilación ni de TypeScript.
AC-015  En http://localhost:4200 un usuario puede crear un proyecto, verlo en la lista y abrirlo.
AC-016  En el tablero, `Nueva tarea` crea una tarea en `Por hacer`; editarla cambia título/prioridad; eliminarla pide confirmación y la quita.
AC-017  Arrastrar una tarea a otra columna la mueve; tras recargar la página (F5) permanece en esa columna y posición.
AC-018  Si el backend se detiene y se arrastra una tarea, la tarea vuelve a su sitio y aparece el aviso de error.
AC-019  Con el backend detenido, abrir /projects muestra "No se pudieron cargar los datos." y `Reintentar`; con el backend de nuevo en marcha, `Reintentar` carga la lista.
AC-020  Una base vacía muestra "Aún no hay proyectos. Crea el primero."; una columna sin tareas muestra "Sin tareas".
AC-021  Eliminar un proyecto muestra el texto de confirmación de 8.2 y solo lo elimina tras confirmar.
AC-022  `npm test` (modo no interactivo) termina con 0 fallos.
AC-023  Existen `AGENTS.md` (con las secciones de 15.4) y `CLAUDE.md` (con `@AGENTS.md`); el README permite a una persona nueva levantar todo siguiendo solo sus pasos.
AC-024  El repositorio no contiene secretos reales ni archivos `.env`; `git status` está limpio y existe la etiqueta `v1.0.0`.
```

---

# 15. Instrucciones para OpenCode

Resumen operativo. Si hay conflicto con otra parte del documento, **prevalece lo más restrictivo**; si la duda persiste, pregunta al usuario antes de inventar.

## 15.1 Objetivo

Construir un tablero Kanban (proyectos + tareas con tres estados, prioridad y orden, arrastrar y soltar) con Java 25 + Spring Boot 4.1.x + PostgreSQL 18 (Docker Compose) + Angular 22.0.x. Proyecto local, usuario único, sin autenticación.

## 15.2 Arquitectura, estructura y tecnologías

- Monorepo: `backend/`, `frontend/`, `compose.yaml`, `docs/SPEC.md`, `AGENTS.md`, `CLAUDE.md`, `README.md`.
- Backend por capas: `web → service → repository → domain` (reglas de dependencia de la sección 4.2). Entidades JPA nunca salen de `service`; DTOs son `record`.
- Persistencia: Spring Data JPA, Flyway (`V1__init.sql`), `ddl-auto=validate`, `open-in-view=false`.
- API: sección 7. Errores: ProblemDetail.
- Frontend: Angular 22 standalone, signals, Signal Forms, `httpResource` solo en la lista de proyectos, `BoardStore` con signals en el tablero, `@angular/cdk/drag-drop`, `<dialog>` nativo, CSS propio.
- Versiones: sección 2. Confirmar la patch más reciente con los comandos indicados.

## 15.3 Reglas de implementación (obligatorias)

1. Implementa **por fases** (sección 13). Al terminar cada fase: tests en verde, commit, y continúa. No adelantes funcionalidades de fases posteriores.
2. No implementes nada del apartado "Después del MVP" ni "Fuera de alcance".
3. No añadas dependencias fuera de la sección 9. Si crees que falta una, pregunta.
4. Código, nombres, mensajes de API y commits en inglés. Textos de UI en español exactamente como en 8.2.
5. Controladores finos; reglas de negocio solo en `service`. Mover una tarea = una transacción (`@Transactional`).
6. La invariante de posiciones (6.1) se cumple siempre; está cubierta por tests.
7. `DTO.from(entity)` para la conversión; no devolver entidades.
8. Nada de `*ngIf`/`*ngFor`, `any`, `FormGroup`/`ngModel`, ni llamadas a `HttpClient` desde componentes.
9. No inventes endpoints, campos, pantallas ni textos. No dejes `TODO`/`FIXME` sin ticket en el README.
10. Antes de usar una API de Boot 4, Angular 22 o Testcontainers cuyo nombre exacto dudes (starters, paquetes de test, Signal Forms, `httpResource`), **consulta la documentación oficial de la versión instalada**; no la escribas de memoria.
11. Logs: sin datos sensibles; sin `System.out`.
12. Si un requisito es ambiguo, anota la decisión mínima en README → "Notas de implementación" y sigue.

## 15.4 Contenido obligatorio de `AGENTS.md`

Secciones: (1) Qué es el proyecto, en 5 líneas. (2) Comandos exactos: arrancar BD, backend, frontend; tests de ambos; build. (3) Arquitectura y reglas de dependencia (copia de 4.2/4.3). (4) Convenciones de código (backend y frontend, de 8.7 y 15.3). (5) Reglas de tests: qué se testea y cómo, Docker necesario. (6) Reglas de Git: Conventional Commits, commits pequeños, nunca commitear `.env`. (7) "Antes de dar una tarea por terminada": ejecutar tests, comprobar que no hay cambios fuera de alcance. (8) Límites: lo que NO se hace (lista de "Fuera de alcance"). (9) Dónde está la especificación (`docs/SPEC.md`) y que prevalece sobre cualquier suposición. Máximo ~120 líneas. `CLAUDE.md`: una línea `@AGENTS.md`.

## 15.5 README (contenido mínimo)

Requisitos previos (tabla de 9.3) · Arranque en 3 pasos (`docker compose up -d`; backend; frontend) con URLs · Cómo ejecutar tests · Resumen de la API (tabla de 7.1) · Estructura del repositorio · Troubleshooting (E-16, E-22, Docker apagado) · Notas de implementación · Enlace a `docs/SPEC.md`.

## 15.6 MVP y fuera de alcance

MVP: sección 1.5. Fuera: autenticación, Compose completo, OpenAPI, etiquetas/fechas/asignados, filtros, paginación, E2E, teclado en DnD, `@Version`.

## 15.7 Tests esperados

TEST-001…TEST-023 (sección 12.2). Integración con Testcontainers, no H2.

## 15.8 Criterios de aceptación

AC-001…AC-024 (sección 14).