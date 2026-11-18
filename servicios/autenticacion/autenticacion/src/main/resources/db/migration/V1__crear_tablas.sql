CREATE TABLE usuarios (
  id UUID PRIMARY KEY,
  correo VARCHAR(160) NOT NULL UNIQUE,
  hash_contrasena VARCHAR(100) NOT NULL,
  nombre_completo VARCHAR(120) NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE roles (
  id SERIAL PRIMARY KEY,
  nombre VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE usuario_roles (
  usuario_id UUID REFERENCES usuarios(id) ON DELETE CASCADE,
  rol_id INT REFERENCES roles(id) ON DELETE CASCADE,
  PRIMARY KEY (usuario_id, rol_id)
);

CREATE TABLE tokens_renovacion (
  id UUID PRIMARY KEY,
  usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  hash_token VARCHAR(100) NOT NULL,
  expira_en TIMESTAMPTZ NOT NULL,
  revocado BOOLEAN NOT NULL DEFAULT false,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE codigos_recuperacion (
  id UUID PRIMARY KEY,
  usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  hash_codigo VARCHAR(100) NOT NULL,
  expira_en TIMESTAMPTZ NOT NULL,
  usado BOOLEAN NOT NULL DEFAULT false,
  intentos INT NOT NULL DEFAULT 0,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE intentos_inicio_sesion (
  id BIGSERIAL PRIMARY KEY,
  correo VARCHAR(160),
  ip VARCHAR(45),
  exitoso BOOLEAN NOT NULL,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO roles(nombre) VALUES ('USER'), ('SERVICE');