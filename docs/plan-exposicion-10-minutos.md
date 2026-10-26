# Plan de exposiciÃ³n de Butaca en diez minutos

Guion de trabajo para la versiÃ³n integrada de 16 entidades. Ensayar con las cuentas y la conexiÃ³n que se usarÃ¡n en la presentaciÃ³n. KYC, pagos y reembolsos son simulados; SMTP es opcional y real cuando se configura.

| Tiempo | Responsable | Recorrido |
| --- | --- | --- |
| 0:00â€“2:00 | Mariano | CatÃ¡logo, selecciÃ³n de tarifa, compra nominal y descarga de QR |
| 2:00â€“3:30 | Santiago | Actores, alcance y separaciÃ³n de paneles |
| 3:30â€“5:30 | Ignacio | Modelo DrawDB de 16 entidades, persona y precio histÃ³rico |
| 5:30â€“7:30 | Leandro | KYC simulado, cruce de integridad y auditorÃ­a |
| 7:30â€“9:15 | Mariano | EdiciÃ³n de tarifas, cancelaciÃ³n y resultados de pruebas |
| 9:15â€“10:00 | Santiago | Pendientes y prÃ³ximos pasos |

## PreparaciÃ³n

Iniciar con `iniciar-windows.bat` o `bash scripts/demo.sh`. La demo estÃ¡ en http://localhost:8081; en desarrollo se abre Vite en 5173. Revisar [la guÃ­a de consultas H2](guia-presentacion.md).

Usar las cuentas semilla de cliente, organizador y administrador del README. Estas cuentas ya tienen KYC. Registrar una cuenta nueva si se quiere mostrar la carga de imÃ¡genes y el rechazo de una evidencia invÃ¡lida.

## Cliente

Mostrar catÃ¡logo y tarifas disponibles. Comprar declarando nombre y DNI de cada asistente. Explicar que el servidor calcula el importe y que las imÃ¡genes KYC no se guardan. Descargar los tickets y mostrar el recibo.

## Organizador

Consultar el UUID y comparar visualmente el DNI fÃ­sico. Marcar la entrada como usada y demostrar el rechazo de un segundo uso.

Editar un evento y sus tarifas. Explicar que una entrada emitida mantiene su precio histÃ³rico. Para cancelar, demostrar las dos confirmaciones y el reembolso simulado; revisar el estado del ticket.

## Administrador y modelo

Mostrar compras, usuarios, ubicaciones, categorÃ­as y auditorÃ­a. Aclarar que la separaciÃ³n visual de paneles todavÃ­a no equivale a autorizaciÃ³n en la API.

En el diagrama, seguir persona â†’ usuario â†’ compra â†’ pago â†’ comprobante y evento â†’ tipo_entrada â†’ entrada â†’ persona titular. Mostrar que el total de compra y el cupo disponible se calculan.

## Correo y pruebas

Si SMTP no estÃ¡ configurado, mostrar la notificaciÃ³n pendiente. Si se habilita para la exposiciÃ³n, explicar que la aceptaciÃ³n SMTP no demuestra recepciÃ³n ni lectura y mantener el destinatario de prueba.

Presentar las pruebas ejecutadas para compra, KYC, pago rechazado, tarifas histÃ³ricas, cancelaciÃ³n y adjuntos de correo. No presentar una prueba de carga o concurrencia como realizada si todavÃ­a estÃ¡ pendiente.

Cerrar con los [pendientes del proyecto](estado-del-proyecto.md).
