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
