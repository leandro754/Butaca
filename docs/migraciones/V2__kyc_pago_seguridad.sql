-- Butaca - migracion V2: KYC con evidencias y pago con monto autorizado
-- Pensada para PostgreSQL cuando se deje de recrear el esquema con ddl-auto=update/create-drop.
-- En la demo H2 no hace falta ejecutarla: application-demo.properties usa create-drop.
--
-- Importante:
--   * No se guardan fotos, selfies, PAN ni CVV.
--   * Solo se guardan hashes SHA-256 de las evidencias y los ultimos cuatro digitos.
--   * monto_pago es un hecho externo (lo aprobo la pasarela), no un valor calculado;
--     se cruza con la suma de entrada.precio_pagado_entrada.

BEGIN;

ALTER TABLE verificacion_identidad
    ADD COLUMN IF NOT EXISTS hash_evidencia_documental varchar(64),
    ADD COLUMN IF NOT EXISTS hash_selfie varchar(64),
    ADD COLUMN IF NOT EXISTS proveedor_identidad varchar(60),
    ADD COLUMN IF NOT EXISTS puntaje_coincidencia numeric(5, 4);

ALTER TABLE pago
    ADD COLUMN IF NOT EXISTS monto_pago numeric(10, 2),
    ADD COLUMN IF NOT EXISTS moneda_pago varchar(3),
    ADD COLUMN IF NOT EXISTS codigo_autorizacion_pago varchar(50);

-- Backfill de pagos historicos: el monto se reconstruye desde las entradas.
UPDATE pago p
SET monto_pago = (
        SELECT COALESCE(SUM(e.precio_pagado_entrada), 0)
        FROM entrada e
        WHERE e.id_compra = p.id_compra
    ),
    moneda_pago = COALESCE(p.moneda_pago, 'ARS')
WHERE p.monto_pago IS NULL;

-- Los comprobantes historicos conservan su hash original. Para que el endpoint
-- /api/compras/{id}/verificar-cruce pueda recalcularlos, se necesita reemitirlos
-- o generar un hash de migracion a partir del pago, KYC y entradas existentes.
-- No se actualiza automaticamente para no falsear la evidencia historica.

CREATE INDEX IF NOT EXISTS idx_verificacion_proveedor
    ON verificacion_identidad (proveedor_identidad, estado_verificacion);

CREATE INDEX IF NOT EXISTS idx_pago_estado_fecha
    ON pago (estado_pago, fecha_pago);

COMMIT;
