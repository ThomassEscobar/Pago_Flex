CREATE DATABASE bd_autenticacion;
CREATE DATABASE bd_compromisos;
CREATE DATABASE bd_pagos;
CREATE DATABASE bd_notificaciones;

CREATE USER usuario_autenticacion  WITH PASSWORD 'cambiar_auth_123';
CREATE USER usuario_compromisos    WITH PASSWORD 'cambiar_comp_123';
CREATE USER usuario_pagos          WITH PASSWORD 'cambiar_pagos_123';
CREATE USER usuario_notificaciones WITH PASSWORD 'cambiar_notif_123';

GRANT ALL PRIVILEGES ON DATABASE bd_autenticacion  TO usuario_autenticacion;
GRANT ALL PRIVILEGES ON DATABASE bd_compromisos    TO usuario_compromisos;
GRANT ALL PRIVILEGES ON DATABASE bd_pagos          TO usuario_pagos;
GRANT ALL PRIVILEGES ON DATABASE bd_notificaciones TO usuario_notificaciones;