CREATE TABLE organizaciones (
  id UUID PRIMARY KEY,
  nombre VARCHAR(120) NOT NULL,
  rut VARCHAR(20),
  tipo VARCHAR(40) NOT NULL
);

CREATE TABLE compromisos (
  id UUID PRIMARY KEY,
  usuario_id UUID NOT NULL,
  organizacion_id UUID NOT NULL REFERENCES organizaciones(id),
  descripcion VARCHAR(200) NOT NULL,
  monto_clp BIGINT NOT NULL CHECK (monto_clp > 0),
  fecha_vencimiento DATE NOT NULL,
  estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE','PAGADO','VENCIDO')),
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_compromisos_usuario_estado ON compromisos(usuario_id, estado);
CREATE INDEX ix_compromisos_usuario_vencimiento ON compromisos(usuario_id, fecha_vencimiento);