# DocumentaciÃ³n de Butaca

| Documento | PropÃ³sito |
| --- | --- |
| [README principal](../README.md) | Requisitos, demo, desarrollo y trabajo entre equipos |
| [SRS](../srs_proyecto.md) | Alcance y casos de uso del modelo de 16 entidades |
| [Arquitectura](arquitectura.md) | Compra transaccional, KYC, pago y lÃ­mites de la demo |
| [API](api.md) | Rutas y ejemplo de compra |
| [Desarrollo](desarrollo.md) | ConfiguraciÃ³n, empaquetado y diagnÃ³stico |
| [Estado del proyecto](estado-del-proyecto.md) | Funcionalidades disponibles y pendientes |
| [NormalizaciÃ³n](normalizacion-base-datos.md) | Modelo y justificaciÃ³n de diseÃ±o |
| [Glosario](glosario-y-modelo-base-de-datos.md) | Responsabilidad de las 16 entidades |
| [GuÃ­a de presentaciÃ³n](guia-presentacion.md) | Arranque y consultas H2 |
| [Plan de exposiciÃ³n](plan-exposicion-10-minutos.md) | Recorrido de diez minutos |
| [DrawDB](drawdb-butaca-modelado-datos.json) | Snapshot del diagrama de referencia, 16 tablas |
| [DBML](../butaca.dbml) | Modelo local actualizado, incluidas las evidencias KYC |
| [DDL](schema-3fn-seguridad.sql) | Referencia PostgreSQL del modelo vigente |
| [Correo y eventos](correo-real-y-eventos.md) | Cola SMTP, tarifas, reprogramaciÃ³n y cancelaciÃ³n |
| [Identidad visual](identidad-y-apertura.md) | Marca y apertura de la interfaz |

## Fuentes y archivos generados

Las entidades JPA definen el esquema que ejecuta la aplicaciÃ³n. El snapshot de DrawDB se conserva como referencia; DBML y DDL documentan ademÃ¡s las evidencias KYC integradas en esta rama. No se modificÃ³ el diagrama remoto durante la fusiÃ³n.

El modelo vigente tiene 16 entidades: persona separa la identidad civil de las credenciales de usuario; tipo_entrada concentra las tarifas; reembolso registra devoluciones. DNI y nombre del asistente en los DTO se proyectan desde persona, sin duplicarlos en entrada.

La demo genera el esquema mediante Hibernate y carga datos con DataLoader. No requiere importar SQL. Los scripts de migraciones son histÃ³ricos y manuales: revisar su versiÃ³n de origen antes de aplicarlos a una base persistente.

Los builds, dependencias, cachÃ©s y secretos se mantienen fuera de Git. El SRS vigente estÃ¡ en Markdown para evitar copias desactualizadas. El Draw.io anterior representa el modelo previo de 13 tablas; para el modelo actual usar el snapshot DrawDB y el diagrama Mermaid de normalizaciÃ³n.
