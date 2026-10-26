# Documentación de Butaca

| Documento | Propósito |
| --- | --- |
| [README principal](../README.md) | Instalación, arranque y estructura |
| [SRS](../srs_proyecto.md) | Requisitos, actores y casos de uso |
| [Arquitectura](arquitectura.md) | Compra transaccional, adaptadores y límites de la demo |
| [API](api.md) | Rutas y ejemplo de compra |
| [Desarrollo](desarrollo.md) | Configuración, empaquetado y diagnóstico |
| [Estado del proyecto](estado-del-proyecto.md) | Funcionalidades disponibles y pendientes |
| [Normalización](normalizacion-base-datos.md) | Entidades, relaciones y justificación de 3FN |
| [Glosario](glosario-y-modelo-base-de-datos.md) | Conceptos y preguntas para la defensa |
| [Guía de presentación](guia-presentacion.md) | Inicio de la demo y consultas H2 |
| [Plan de exposición](plan-exposicion-10-minutos.md) | Recorrido de diez minutos |
| [DBML](../butaca.dbml) | Modelo relacional de referencia de 13 entidades |
| [Draw.io](../diagrama%20Butaca.drawio) | Diagrama editable para exposición |
| [Migración V2](migraciones/V2__kyc_pago_seguridad.sql) | Script manual para una base PostgreSQL previa |

## Fuentes y archivos generados

Las entidades JPA definen el esquema ejecutado por la aplicación. El DBML documenta el modelo; al cambiarlo se deben revisar las entidades y los diagramas. Los ejemplos SQL del SRS son ilustrativos; para consultas ejecutables usar la guía de presentación.

La demo crea sus 13 tablas con Hibernate y carga datos mediante `DataLoader`. Los scripts de `migraciones/` no se ejecutan automáticamente. No importar un esquema anterior de nueve tablas para iniciar la demo.

Se conserva una sola copia editable de Draw.io. El antiguo `docs/schema-3fn-seguridad.sql` se retiró porque tenía columnas y relaciones que no correspondían al modelo actual. Para obtener DDL, exportar el DBML actualizado o el esquema de la base ejecutada.

`target/`, `frontend/dist/`, `node_modules/`, `.m2/`, archivos de respaldo y herramientas locales quedan fuera del control de versiones. El SRS vigente se mantiene en Markdown para evitar copias desactualizadas. Una futura exportación Word o PDF debe generarse desde esa fuente y revisarse antes de incorporarse como entregable.
