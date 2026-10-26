# Glosario y modelo de datos

Butaca implementa 16 entidades. El modelo de referencia estÃ¡ en [DrawDB](drawdb-butaca-modelado-datos.json), su versiÃ³n editable textual en [DBML](../butaca.dbml) y la explicaciÃ³n de relaciones en [normalizaciÃ³n](normalizacion-base-datos.md).

## Responsabilidades

| Entidad | Responsabilidad |
| --- | --- |
| rol | CatÃ¡logo ADMIN, ORGANIZADOR y CLIENTE |
| persona | DNI Ãºnico, nombre, apellido y datos de contacto de una persona real |
| usuario | Credenciales, estado, rol y vÃ­nculo con persona |
| verificacion_identidad | Estado KYC del usuario, token, proveedor y hashes de evidencias |
| ubicacion | Lugar fÃ­sico, capacidad mÃ¡xima y disponibilidad |
| categoria | ClasificaciÃ³n del evento |
| evento | Fecha, lugar, cupo total, organizador y versiÃ³n |
| tipo_entrada | Tarifas activas por evento y moneda |
| compra | Comprador, fecha, moneda y estado de la operaciÃ³n |
| entrada | Ticket con UUID, tarifa, persona titular y precio histÃ³rico |
| historial_estado_entrada | Transiciones de emisiÃ³n, uso y anulaciÃ³n |
| pago | Intento de pago, proveedor, referencia y monto autorizado |
| reembolso | DevoluciÃ³n asociada a un pago |
| comprobante | Recibo asociado al pago y hash de integridad |
| notificacion | Mensajes, compra/evento relacionado y estado de envÃ­o/lectura |
| auditoria_seguridad | Registro de operaciones y rechazos de seguridad |

## Conceptos para la defensa

**Identidad y cuenta.** Una persona puede ser titular de un ticket sin tener usuario. El comprador sÃ­ necesita una cuenta activa y KYC verificado. El nombre y el DNI que devuelve la API de entradas proceden de persona; no son columnas nominales duplicadas en entrada.

**Tarifa e histÃ³rico.** tipo_entrada define el precio actual de catÃ¡logo. entrada conserva el precio y la moneda pagados. Una modificaciÃ³n del tarifario no modifica las entradas ya emitidas.

**Datos calculados.** Cantidad, total de compra y cupo disponible se obtienen de las entradas. monto_pago es un dato externo autorizado por la pasarela y se cruza con la suma de los precios histÃ³ricos.

**Comprobante.** Se relaciona directamente con pago. Compra y cliente se recuperan a travÃ©s de ese vÃ­nculo. El hash SHA-256 permite detectar cambios respecto de los datos cruzados; no es una firma digital de una autoridad certificante.

**Concurrencia.** Las versiones de evento y entrada permiten detectar escrituras concurrentes. No sustituyen una prueba de carga ni demuestran por sÃ­ solas toda la polÃ­tica de autorizaciÃ³n.

**Nominalidad.** El personal compara visualmente el DNI fÃ­sico con los datos del ticket. La operaciÃ³n de uso rechaza entradas que no estÃ¡n CONFIRMADA. La API todavÃ­a necesita autorizaciÃ³n por sesiÃ³n y rol.

**AuditorÃ­a.** Los servicios insertan registros de operaciones exitosas y usan transacciones independientes para preservar los rechazos. Esto no garantiza inmutabilidad frente a un administrador con acceso directo a la base.

**Integraciones.** KYC y pagos son simulados. El correo es SMTP real solo cuando se configura y habilita; sin credenciales permanece pendiente. La aceptaciÃ³n SMTP no demuestra recepciÃ³n ni lectura del mensaje.
