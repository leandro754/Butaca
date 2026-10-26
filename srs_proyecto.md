# **BUTACA - Sistema web de gestión y venta simulada de entradas para eventos**

## **Documento de Especificación de Requerimientos de Software (SRS)**

### **Integrantes (MLSN Group):**
*   **Mariano Piovesan** (Desarrollador de Interfaz y Pruebas)
*   **Leandro Gramajo Gargiulo** (Scrum Master)
*   **Santiago González Fernández** (Product Owner)
*   **Ignacio Molina Tula** (Desarrollador de Sistema y Base de Datos)

---

### **Versión:** 2.2

### **Fecha de revisión:** 26 de octubre de 2026
### **Estándar:** Basado en IEEE 830 y SIA Académico  

> **Estado del proyecto:** implementación académica en desarrollo. La pasarela de pago y el proveedor de identidad son simulados. El proyecto no afirma certificación PCI-DSS ni integración real con RENAPER; las referencias a PCI-DSS describen minimización de datos. La autenticación de demo guarda el perfil localmente; la autorización de endpoints con sesiones o tokens está pendiente.

---

## **1. Introducción**

### **1.1 Propósito**
El presente documento tiene como finalidad definir y especificar de forma rigurosa y formal los requerimientos funcionales y no funcionales que rigen el sistema **BUTACA**. Está dirigido al equipo de desarrollo MLSN Group y a los evaluadores académicos del proyecto, proporcionando una visión fiel al estado técnico e implementación real de la plataforma.

### **1.2 Ámbito del Sistema**
**BUTACA** es un sistema web académico diseñado para centralizar la gestión de eventos, el control de ubicaciones físicas y la venta simulada de entradas. El sistema abarca la publicación de eventos, el control de concurrencia para evitar la sobreventa y la emisión de tickets electrónicos individuales con códigos de validación única.

### **1.3 Definiciones, Acrónimos y Abreviaturas**
*   **SRS:** Especificación de Requerimientos de Software (IEEE 830).
*   **MVP:** Producto Mínimo Viable; versión funcional inicial centrada en procesos críticos.
*   **3FN:** Tercera Forma Normal. Diseño de base de datos relacional libre de dependencias transitivas y anomalías de actualización.
*   **Cupo:** Capacidad logística del evento. Representa la cantidad de entradas físicas disponibles para la venta.
*   **Bloqueo Optimista:** Mecanismo de control de concurrencia apoyado en un campo `@Version` que evita que dos transacciones simultáneas compren el mismo cupo.
*   **Rol:** Nivel de autorización en el sistema (`ADMIN`, `ORGANIZADOR`, `CLIENTE`).
*   **Estado de Evento:** `BORRADOR`, `PUBLICADO`, `CANCELADO`.
*   **Estado de Entrada:** `CONFIRMADA`, `USADA`, `ANULADA`.
*   **UUID:** Identificador Único Universal de 128 bits utilizado como código de consulta de cada ticket. Su existencia, estado y uso se validan contra el backend.

### **1.4 Referencias**
*   **MLSN Group:** "Entregables del Taller Integrador de Ingeniería Informática".
*   **Norma IEEE 830-1998:** Prácticas recomendadas para especificaciones de requerimientos de software.
*   **Documento de Normalización Local:** `docs/normalizacion-base-datos.md`.
*   **Reglas de Trabajo de Agentes:** `AGENTS.MD`.

### **1.5 Visión General del Documento**
El documento se divide en cuatro bloques clave: la **Sección 1** introduce el marco conceptual y propósitos del software; la **Sección 2** detalla la perspectiva del producto, actores y restricciones del entorno; la **Sección 3** detalla los requisitos funcionales, no funcionales y despliega formalmente los casos de uso acoplados a la base de datos física; y la **Sección 4** consolida la información del equipo.

---

## **2. Descripción General**

### **2.1 Perspectiva del Producto**
**BUTACA** utiliza un backend REST en **Java 17 con Spring Boot** y una interfaz **React y Vite** gestionada mediante **pnpm**. El backend implementa las reglas de compra, persistencia, validación e integridad; el frontend consume la API bajo `/api`.

El sistema admite dos perfiles de ejecución:
1.  **Perfil con base externa:** Conectado a **PostgreSQL** usando las propiedades base y variables de entorno. Sigue siendo una configuración académica; no implica que el sistema esté preparado para producción.
2.  **Perfil de Demostración (`demo`):** Diseñado para pruebas y clases rápidas con una base de datos local **H2 en memoria** autoejecutable mediante el script `./scripts/demo.sh`, sin dependencias externas complejas.

### **2.2 Funciones del Producto**
*   **Autenticación Uniforme y Verificación de Identidad (KYC):** Registro e inicio de sesión seguro con contraseñas encriptadas con **BCrypt**, DNI único por usuario y validación de identidad obligatoria (`verificacion_identidad`) antes de habilitar la compra de entradas.
*   **Gestor de Ubicaciones Físicas:** Registro de establecimientos físicos (`ubicacion`) con control estricto de capacidad máxima para evitar la asignación de eventos que excedan la capacidad logística.
*   **Ciclo de Vida de Eventos:** Creación de publicaciones en estado `BORRADOR`, publicación abierta al público (`PUBLICADO`) o anulación del espectáculo por fuerza mayor (`CANCELADO`).
*   **Pasarela de Pago Tokenizada (minimización inspirada en PCI-DSS) y Comprobante con Cruce SHA-256:** Procesamiento del pago mediante un adaptador (`PagoGateway`, simulado en la demo) en tabla dedicada (`pago`), almacenando referencias externas, monto/moneda autorizados y últimos 4 dígitos (sin PAN ni CVV), emitiendo un `comprobante` con firma criptográfica SHA-256 que cruza comprador verificado, pago y tickets.
*   **Entradas Nominales y Congelamiento de Precios Históricos:** La compra genera una cabecera mínima (`compra`), congela el precio pagado real (`entrada.precio_pagado_entrada`) y asigna nominalmente cada ticket al DNI y nombre del asistente real (`dni_asistente_entrada`, `nombre_asistente_entrada`).
*   **Doble Nivel de Trazabilidad y Tracking de Seguridad:** Cada transición del ticket se registra en `historial_estado_entrada` y toda operación sensible del sistema se audita de forma inmutable en `auditoria_seguridad`.
*   **Buzón de Notificaciones Internas:** Envío automático de notificaciones a clientes y organizadores sobre verificaciones KYC, envío de comprobantes y entradas nominales o cancelaciones de espectáculos.

### **2.3 Características de los Usuarios (Actores)**

| Tipo de Usuario | Descripción | Objetivo dentro del Sistema |
| :--- | :--- | :--- |
| **Administrador (`ADMIN`)** | Usuario con control de auditoría general. | Administrar el catálogo de categorías y ubicaciones, auditar métricas globales de venta del sistema y supervisar la bitácora de tracking de seguridad (`auditoria_seguridad`). |
| **Organizador (`ORGANIZADOR`)** | Entidad, productora o emprendedor creador de eventos. | Cargar espectáculos en borradores, publicarlos en ubicaciones activas, validar entradas nominales en puerta (QR + DNI) y supervisar las métricas de ingresos e inventario. |
| **Cliente (`CLIENTE`)** | Consumidor o espectador final. | Verificar su identidad (KYC), explorar eventos publicados, comprar entradas nominales mediante pasarela tokenizada, recibir su comprobante SHA-256 y visualizar sus tickets con códigos UUID. |

### **2.4 Restricciones**
*   **Entorno Local Académico:** El modo demo utiliza H2 en memoria y destruye/recrea los datos en cada reinicio para asegurar un estado semilla limpio (`DataLoader`).
*   **Pasarela de Pago Desacoplada (minimización de datos):** Las transacciones registran la operación en `pago` mediante referencias externas y últimos 4 dígitos, prohibiendo almacenar numeración completa de tarjetas (PAN) o códigos CVV en la base de datos. La demo usa un adaptador simulado y no procesa pagos reales.
*   **Conectividad de Red:** La demo de presentación sirve frontend y API desde Spring Boot en el puerto `8080`. En desarrollo, Vite sirve la web en `5173` y redirige `/api` al backend. Los scripts permiten elegir puertos explícitos y rechazan puertos ocupados.

### **2.5 Suposiciones y Dependencias**
*   Se asume que los navegadores clientes soportan estándares modernos de JavaScript (ES6+) y HTML5.
*   Se asume que el Administrador y Organizador configuran correctamente la capacidad máxima de las ubicaciones físicas para restringir las compras excesivas.

### **2.6 Requerimientos Futuros**
*   Webhooks asincrónicos con pasarelas de pago externas en producción.
*   Reservas temporales de cupo con TTL en Redis durante el checkout.
*   Sistema de selección interactiva de asientos / mapa de butacas físicas mediante Canvas.

---

## **3. Requerimientos Específicos**

### **3.1 Requisitos Funcionales**

*   **RF1 (Gestión de Cuentas, Seguridad y Verificación KYC Obligatoria):** El sistema debe registrar usuarios asignándoles un rol (`rol`), email único y DNI único (`usuario.dni_usuario`). Las contraseñas se almacenan con **BCrypt**. Antes de realizar cualquier compra, el usuario debe contar obligatoriamente con identidad verificada (`verificacion_identidad.estado_verificacion = 'VERIFICADA'`).
*   **RF2 (Ciclo de Vida de Eventos):** Los organizadores pueden cargar eventos en estado `BORRADOR` y luego pasarlos a `PUBLICADO` o `CANCELADO`. Los clientes solo pueden ver eventos que estén en estado `PUBLICADO` y tengan cupos disponibles.
*   **RF3 (Emisión de Entradas Nominales y Códigos Únicos):** Por cada ticket emitido se asocia un `UUID` único (`entrada.codigo_unico_entrada`) y los datos nominales del asistente (`dni_asistente_entrada`, `nombre_asistente_entrada`). Su ciclo de vida (`CONFIRMADA` -> `USADA` o `ANULADA`) se registra en `historial_estado_entrada`.
*   **RF4 (Cruce Transaccional de Compra, Pasarela de Pago y Comprobante):** El método de compra debe cruzar atómicamente (`@Transactional`) la verificación KYC del comprador, el stock con **Bloqueo Optimista** (`@Version`), la aprobación en la pasarela (`pago`), la emisión de entradas nominales (`entrada`) y la generación del `comprobante` con firma SHA-256 (`hash_cruce_seguridad`) y envío digital (`ENVIADO`).
*   **RF5 (Gestión de Ubicaciones y Capacidad Logística):** Todo evento debe ocurrir obligatoriamente en una ubicación física registrada. El cupo total jamás podrá exceder la capacidad máxima de la ubicación (`evento.cupo_total_evento` <= `ubicacion.capacidad_maxima_ubicacion`).
*   **RF6 (Métricas, Consultas y Tracking de Seguridad):** Los organizadores visualizan ingresos y ocupación por evento; el Administrador visualiza estadísticas globales y la bitácora inmutable de tracking (`auditoria_seguridad`).

### **3.2 Requerimientos No Funcionales**

*   **RNF1 (Seguridad Informática y Ley 25.326):** Ningún DTO expone hashes de contraseña. La tabla `pago` no almacena PAN ni CVV y aplica minimización de datos; el proyecto no afirma certificación PCI-DSS. `verificacion_identidad` guarda únicamente token, proveedor, puntaje y hashes SHA-256 de las evidencias, sin imágenes crudas.
*   **RNF2 (Normalización Relacional de Base de Datos en 3FN):** La persistencia se realiza bajo un esquema relacional normalizado en **Tercera Forma Normal (3FN)** compuesto por 13 tablas en singular: `rol`, `usuario`, `verificacion_identidad`, `ubicacion`, `categoria`, `evento`, `compra`, `pago`, `comprobante`, `entrada`, `historial_estado_entrada`, `notificacion` y `auditoria_seguridad`.
*   **RNF3 (Usabilidad y Compatibilidad API):** Las respuestas JSON de la API REST del backend usan nomenclatura en formato **snake_case** (mediante anotaciones `@JsonProperty`), mientras que el código de Java mantiene la convención interna de **camelCase**.
*   **RNF4 (Facilidad de Instalación y Demo):** La aplicación cuenta con scripts equivalentes para Windows (`scripts/demo.ps1`) y macOS/Linux (`scripts/demo.sh`). Compilan el frontend para presentación, inician Spring Boot con H2 y datos semilla, comprueban disponibilidad y permiten un modo de desarrollo con Vite. Requieren Java, Node y pnpm; la primera ejecución necesita resolver dependencias.

---

### **3.3 Casos de Uso**

#### **CU-01: Registro e Inicio de Sesión de Usuario**

> [!NOTE]
> La demo verifica credenciales con BCrypt y muestra el panel del rol. La autorización del servidor para cada endpoint permanece pendiente.

| Elemento | Descripción |
| :--- | :--- |
| **Actor Principal** | Cliente, Organizador o Administrador. |
| **Objetivo** | Registrarse o validar credenciales para acceder a la vista del rol. La demo guarda el perfil en `butaca-auth-user` del navegador; no emite sesiones ni tokens de acceso del servidor. |
| **Requisito Asociado**| RF1 - Gestión de Cuentas y Seguridad. |
| **Precondición** | El usuario debe contar con un correo válido no registrado previamente si es un flujo de registro. |
| **Flujo Principal** | 1. El usuario accede al formulario de registro o login.<br>2. En el registro ingresa sus datos personales, DNI, email y contraseña. La interfaz registra clientes; el backend admite un rol válido en la solicitud.<br>3. El servidor guarda un hash BCrypt de la contraseña y el usuario asociado a su rol.<br>4. En el login verifica email y contraseña contra la cuenta.<br>5. Devuelve el perfil y la interfaz muestra el panel correspondiente. |
| **Flujos de Excepción**| **E1 (Correo duplicado):** Si el email ya existe en `usuario`, la base de datos rechaza el registro lanzando una excepción controlada que se informa en la interfaz.<br>**E2 (Credenciales inválidas):** Si el correo o la contraseña no coinciden, se retorna error de credenciales incorrectas. |
| **Persistencia (BD)** | **Registro:** `INSERT INTO usuario (nombre_usuario, apellido_usuario, email_usuario, password_hash_usuario, id_rol, estado_usuario) VALUES (...)`. `id_rol` referencia una fila de `rol`. |

---

#### **CU-02: Creación y Publicación de Eventos**

> [!IMPORTANT]
> El sistema restringe que los cupos totales excedan la capacidad máxima de la ubicación física seleccionada.

| Elemento | Descripción |
| :--- | :--- |
| **Actor Principal** | Organizador. |
| **Objetivo** | Registrar un nuevo espectáculo asociándolo a una categoría, una ubicación válida y definir su stock y precio de entradas. |
| **Requisito Asociado**| RF2, RF5. |
| **Precondición** | La categoría y la ubicación física deben existir; la ubicación debe estar activa. |
| **Flujo Principal** | 1. El Organizador abre su panel de gestión y presiona "Crear Evento".<br>2. Completa los datos: título, descripción, fecha, hora, precio, cupos totales, imagen y selecciona la ubicación y categoría.<br>3. El sistema valida que los `cupos_totales_evento` sean menores o iguales a la `capacidad_maxima` de la ubicación.<br>4. El Organizador elige el estado: guardar como `BORRADOR` o `PUBLICADO` inmediatamente.<br>5. El sistema persiste el evento y, si fue publicado, se vuelve visible en el catálogo de los clientes. |
| **Flujos de Excepción**| **E1 (Capacidad excedida):** Si los cupos del evento superan la capacidad física de la ubicación, el sistema rechaza la operación informando el límite máximo permitido.<br>**E2 (Fecha inválida):** Si la fecha del evento es anterior al día actual, el validador del backend rechaza la persistencia. |
| **Persistencia (BD)** | `INSERT INTO evento (titulo_evento, precio_actual_entrada, cupo_total_evento, estado_evento, id_categoria, id_ubicacion, id_organizador, version_evento) VALUES (..., ?, ?, 'BORRADOR'/'PUBLICADO', ?, ?, ?, 0)`. |

---

#### **CU-03: Compra de Entradas y Emisión de Tickets**

> [!WARNING]
> La compra es una operación atómica protegida por transaccionalidad y bloqueo optimista para evitar la sobreventa.

| Elemento | Descripción |
| :--- | :--- |
| **Actor Principal** | Cliente. |
| **Objetivo** | Adquirir una o más entradas para un espectáculo con disponibilidad de cupos, registrando la transacción y emitiendo los tickets UUID. |
| **Requisito Asociado**| RF3, RF4. |
| **Precondición** | El evento debe estar `PUBLICADO`, tener stock y el comprador debe ser un usuario activo con identidad `VERIFICADA`. |
| **Flujo Principal** | 1. El cliente inicia sesión y navega al catálogo.<br>2. Selecciona un evento y una cantidad.<br>3. Completa KYC simulado si corresponde y declara nombre/DNI de cada asistente.<br>4. El servidor valida identidad, estado, cupo y precio con bloqueo optimista.<br>5. Procesa el pago simulado y cruza el monto autorizado.<br>6. Registra compra, pago y entradas nominales con UUID y precio histórico.<br>7. Emite un comprobante con hash SHA-256 y registra historial, notificación y auditoría.<br>8. Devuelve cantidad y total calculados desde las entradas. |
| **Flujos de Excepción**| **E1 (Cupos agotados):** Si no hay stock suficiente, el sistema bloquea el flujo.<br>**E2 (Conflicto de Concurrencia):** Si dos transacciones intentan comprar simultáneamente el último cupo, el motor JPA intercepta la colisión mediante el campo `version` del evento, fallando la segunda de forma segura y deshaciendo el descuento de inventario (Rollback) para evitar sobreventa. |
| **Persistencia (BD)** | 1. `SELECT evento ...` con bloqueo optimista y conteo de `entrada` vigente.<br>2. `INSERT INTO compra (fecha_compra, estado_compra, id_cliente) VALUES (NOW(), 'CONFIRMADA', ?)`.<br>3. Por cada ticket: `INSERT INTO entrada (codigo_unico_entrada, estado_entrada, precio_pagado_entrada, fecha_emision_entrada, id_evento, id_cliente, id_compra) VALUES (...)`.<br>4. Se insertan las filas de historial y la notificación en la misma transacción. |

---

#### **CU-04: Gestión Administrativa y Auditoría**

| Elemento | Descripción |
| :--- | :--- |
| **Actor Principal** | Administrador (`ADMIN`). |
| **Objetivo** | Gestionar los catálogos base del negocio (categorías y ubicaciones) y auditar las métricas de compras y tickets a nivel global de la plataforma. |
| **Requisito Asociado**| RF6. |
| **Precondición** | El usuario debe estar autenticado con rol `ADMIN`. |
| **Flujo Principal** | 1. El Administrador accede a su panel de control administrativo.<br>2. Visualiza las tarjetas de resumen: ingresos totales cobrados por la plataforma, total de usuarios registrados y cantidad de eventos activos.<br>3. Accede al gestor de ubicaciones y añade un nuevo establecimiento físico indicando nombre, dirección y capacidad física máxima.<br>4. Accede al gestor de categorías para registrar nuevas categorías aplicables a los espectáculos. |
| **Persistencia (BD)** | **Auditoría:** `SELECT SUM(precio_pagado_entrada) FROM entrada` y `SELECT COUNT(*) FROM usuario`.<br>**Creación:** `INSERT INTO ubicacion (nombre_ubicacion, direccion_ubicacion, capacidad_maxima_ubicacion, activa_ubicacion) VALUES (?, ?, ?, true)`. |

---

#### **CU-05: Validación de Entradas (Control de Acceso en Puerta)**

> [!IMPORTANT]
> Este caso de uso asegura que un ticket no pueda reutilizarse de forma fraudulenta.

| Elemento | Descripción |
| :--- | :--- |
| **Actor Principal** | Organizador (personal de control de accesos). |
| **Objetivo** | Verificar la validez de un ticket mediante su código único e impedir el reingreso de entradas ya validadas. |
| **Requisito Asociado**| RF3. |
| **Precondición** | El ticket debe existir en la base de datos. |
| **Flujo Principal** | 1. El personal ingresa o escanea el UUID.<br>2. El sistema busca el ticket en `entrada`.<br>3. Comprueba que esté `CONFIRMADA`.<br>4. Cambia el estado a `USADA`, registra `fecha_validacion_entrada` e inserta el cambio en `historial_estado_entrada`, todo dentro de la misma transacción. |
| **Flujos de Excepción**| **E1 (Entrada inexistente):** La consulta de un UUID desconocido devuelve un error.<br>**E2 (Entrada usada o anulada):** La operación de uso rechaza cualquier entrada cuyo estado no sea `CONFIRMADA`. El dato nominal se contrasta visualmente con el DNI físico; el endpoint de uso actual no recibe un DNI para compararlo. |
| **Persistencia (BD)** | `SELECT * FROM entrada WHERE codigo_unico_entrada = ?`<br>`UPDATE entrada SET estado_entrada = 'USADA', fecha_validacion_entrada = NOW() WHERE id_entrada = ?`<br>`INSERT INTO historial_estado_entrada (...) VALUES (...)`. |

---

#### **CU-06: Panel del Cliente y Notificaciones**

| Elemento | Descripción |
| :--- | :--- |
| **Actor Principal** | Cliente. |
| **Objetivo** | Visualizar la bandeja de notificaciones operativas, consultar el historial de compras realizadas y descargar los códigos de validación de sus tickets. |
| **Requisito Asociado**| RF5, RF6. |
| **Precondición** | El cliente debe tener una cuenta activa e iniciar sesión. |
| **Flujo Principal** | 1. El cliente ingresa a su Dashboard.<br>2. El sistema recupera y muestra su listado de compras realizadas ordenadas cronológicamente.<br>3. El cliente puede hacer clic en una compra para ver el detalle e inspeccionar los tickets individuales (UUIDs) asociados a ella.<br>4. El cliente ingresa a su bandeja de notificaciones internas para revisar avisos del sistema, marcándolos como leídos. |
| **Persistencia (BD)** | `SELECT * FROM compra WHERE id_cliente = ? ORDER BY fecha_compra DESC`<br>`SELECT * FROM entrada WHERE id_compra = ?`<br>`SELECT * FROM notificacion WHERE id_usuario_destinatario = ? ORDER BY fecha_creacion_notificacion DESC`<br>`UPDATE notificacion SET leida_notificacion = true WHERE id_notificacion = ?`. |

---

## **4. Apéndices**

### **4.1 Equipo de Trabajo: MLSN Group**
*   **Mariano Piovesan** (Desarrollador frontend, encargado de las vistas interactivas en React y la integración con la API).
*   **Leandro Gramajo Gargiulo** (Scrum Master, responsable de metodologías de trabajo ágiles y control de calidad operativa).
*   **Santiago González Fernández** (Product Owner, encargado de definir requerimientos, reglas de negocio y priorización de funcionalidades).
*   **Ignacio Molina Tula** (Desarrollador backend, diseñador del modelo relacional 3FN y lógica transaccional de persistencia).

### **4.2 Contacto**
*   **Responsable de Documentación:** Santiago González Fernández.
*   **Teléfono de contacto:** `+54 3816288749`

### **4.3 Diagrama Descriptivo de la Base de Datos**

El modelo de las 13 entidades normalizadas en 3FN se entrega en [`butaca.dbml`](butaca.dbml). La explicación en Mermaid se encuentra en [`docs/normalizacion-base-datos.md`](docs/normalizacion-base-datos.md) y el diagrama editable en [`diagrama Butaca.drawio`](diagrama%20Butaca.drawio).
