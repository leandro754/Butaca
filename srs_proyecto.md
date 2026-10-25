# **BUTACA - Sistema web de gestión y venta simulada de entradas para eventos**

## **Documento de Especificación de Requerimientos de Software (SRS)**

### **Integrantes (MLSN Group):**
*   **Mariano Piovesan** (Desarrollador de Interfaz y Pruebas)
*   **Leandro Gramajo Gargiulo** (Scrum Master)
*   **Santiago González Fernández** (Product Owner)
*   **Ignacio Molina Tula** (Desarrollador de Sistema y Base de Datos)

---

### **Versión:** 2.5
### **Fecha:** 24 de octubre de 2026
### **Estándar:** Basado en IEEE 830 y SIA Académico  

---

## **1. Introducción**

### **1.1 Propósito**
El presente documento tiene como finalidad definir y especificar de forma rigurosa y formal los requerimientos funcionales y no funcionales que rigen el sistema **BUTACA**. Está dirigido al equipo de desarrollo MLSN Group y a los evaluadores académicos del proyecto, proporcionando una visión fiel al estado técnico e implementación real de la plataforma.

### **1.2 Ámbito del Sistema**
La fuente del modelo es DrawDB `butaca-modelado-datos`. Los requisitos de seguridad son objetivos del sistema: la demo actual no implementa autorización por sesión y rol en la API; KYC y pagos siguen simulados. El correo usa SMTP real al configurar una cuenta remitente y concentra las pruebas en un destinatario fijo configurable; la configuración local usa entrega directa al Gmail del usuario para evitar el reenvío de Cloudflare a la misma cuenta. Ver [correo y gestión de eventos](docs/correo-real-y-eventos.md).

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
*   **UUID:** Identificador único utilizado para consultar la validez de cada ticket en el servidor. El QR representa ese identificador; no sustituye la comprobación de estado, autorización y titularidad.

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
**BUTACA** se presenta como una solución desacoplada, moderna y sumamente robusta para la democratización de la venta de entradas. Técnicamente, está construida sobre un backend RESTful sólido en **Java 17 con Spring Boot**, y una interfaz de usuario interactiva estructurada en **React y Vite** gestionada mediante **pnpm**. 

El sistema admite dos perfiles de ejecución:
1.  **Perfil de Producción/Desarrollo Real:** Conectado a una base de datos relacional **PostgreSQL**.
2.  **Perfil de Demostración (`demo`):** Diseñado para pruebas y clases rápidas con una base de datos local **H2 en memoria** autoejecutable mediante el script `./scripts/demo.sh`, sin dependencias externas complejas.

### **2.2 Funciones del Producto**
*   **Autenticación Uniforme y Verificación de Identidad (KYC):** Registro e inicio de sesión seguro con contraseñas encriptadas con **BCrypt**, DNI único por usuario y validación de identidad obligatoria (`verificacion_identidad`) antes de habilitar la compra de entradas.
*   **Gestor de Ubicaciones Físicas:** Registro de establecimientos físicos (`ubicacion`) con control estricto de capacidad máxima para evitar la asignación de eventos que excedan la capacidad logística.
*   **Ciclo de Vida de Eventos:** Creación de publicaciones en estado `BORRADOR`, publicación abierta al público (`PUBLICADO`) o anulación del espectáculo por fuerza mayor (`CANCELADO`).
*   **Pasarela de Pago Tokenizada (PCI-DSS) y Comprobante con Cruce SHA-256:** Procesamiento del pago en tabla dedicada (`pago`) almacenando únicamente tokens de idempotencia y últimos 4 dígitos (sin PAN ni CVV), emitiendo un `comprobante` con firma criptográfica SHA-256 que cruza comprador verificado, pago y tickets.
*   **Entradas Nominales y Congelamiento de Precios Históricos:** La compra genera una cabecera mínima y congela precio y moneda en `entrada`. Cada ticket se vincula con `persona` mediante `id_persona_titular`; el nombre y el DNI del asistente en la API son proyecciones, no columnas duplicadas.
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
*   **Pasarela de Pago Desacoplada bajo PCI-DSS:** Las transacciones registran la operación en `pago` mediante referencias externas tokenizadas y últimos 4 dígitos, prohibiendo almacenar numeración completa de tarjetas (PAN) o códigos CVV en la base de datos.
*   **Conectividad de Red:** Al ser una arquitectura desacoplada Frontend-Backend, es requisito que el backend exponga la API bajo el puerto `8080` (con fallback de CORS) y el frontend consuma en el puerto `5173`.

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

*   **RF1 (Cuentas, seguridad y KYC):** El registro público debe crear cuentas `CLIENTE` con email único en `usuario`. La identidad civil y su DNI único pertenecen a `persona`. La administración habilita organizadores. Las contraseñas usan BCrypt y las compras requieren `VERIFICADA`. La autorización de la API y la restricción del rol público son pendientes de implementación de la demo.
*   **RF2 (Ciclo de Vida de Eventos):** Los organizadores pueden cargar eventos en estado `BORRADOR` y luego pasarlos a `PUBLICADO` o `CANCELADO`. Desde Mis eventos abren una configuración por evento para reprogramar fecha/hora, editar cupo y gestionar tarifas en ARS; al menos una debe permanecer activa y los precios emitidos no cambian. Cancelar exige aceptar los reembolsos, escribir el título exacto y enviar la versión vigente; anula entradas y registra reembolsos simulados sin duplicarlos. Los clientes solo ven eventos `PUBLICADO` con cupo disponible.
*   **RF3 (Emisión nominal y códigos únicos):** Cada entrada asocia UUID, evento, tarifa (`id_tipo_entrada`), persona titular (`id_persona_titular`) y compra opcional. Congela precio y moneda. El historial conserva transiciones y `version_entrada` protege el uso concurrente. Los QR se generan y descargan localmente. La fecha del ticket es la del evento, diferenciada de la fecha de compra.
*   **RF4 (Cruce de compra, pago y comprobante):** La compra cruza atómicamente KYC, stock con `@Version`, pago, entradas y comprobante vinculado a `pago` con hash SHA-256 de integridad. Guarda una notificación enlazada con la compra y el comprobante en `PENDIENTE`. Un trabajador posterior al commit envía correo SMTP real con entradas nominales PNG/QR y recibo. Solo la aceptación SMTP habilita `ENVIADA` / `ENVIADO`; ante error queda `FALLIDA` / `ERROR_ENVIO` sin revertir la compra. Sin configuración siguen pendientes. Se notifican reprogramaciones y cancelaciones. La aceptación SMTP no equivale a entrega ni lectura.
*   **RF5 (Gestión de Ubicaciones y Capacidad Logística):** Todo evento debe ocurrir obligatoriamente en una ubicación física registrada. El cupo total jamás podrá exceder la capacidad máxima de la ubicación (`evento.cupo_total_evento` <= `ubicacion.capacidad_maxima_ubicacion`).
*   **RF6 (Métricas, Consultas y Tracking de Seguridad):** El organizador tiene una sola agenda Mis eventos con eventos activos, borradores, entradas vendidas y total vendido. Las ventas se calculan desde entradas vigentes de compras confirmadas con pago aprobado, excluyendo reembolsadas y conservando importes históricos. Por evento se muestran capacidad, disponibilidad y ocupación. El Administrador visualiza métricas globales y auditoría. Los endpoints de puerta siguen disponibles; sus pestañas se retiran del dashboard del organizador.

### **3.2 Requerimientos No Funcionales**

*   **RNF8 (Modo claro y oscuro):** La apariencia se cambia desde la cabecera, persiste localmente y se aplica antes del primer pintado. Un telón rojo se cierra, cambia el tema y vuelve a abrirse con duración acotada y limpieza de animaciones. La intro de Butaca tiene una versión clara sobre blanco y una oscura sobre carbón con letras marfil; ambas conservan B roja y pochoclos dorados. Con movimiento reducido el cambio de tema es inmediato y se omite la intro.

*   **RNF7 (Descubrimiento de eventos):** El inicio recomienda hasta cuatro eventos publicados con cupo priorizando variedad de categorías y cercanía de fecha. El carrusel avanza cada 7 segundos con controles manuales; se pausa al interactuar con mouse/teclado o al ocultar la pestaña y es manual con movimiento reducido. La cartelera combina categoría, fechas rápidas y orden cronológico, cuenta resultados y carga tarjetas progresivamente. Los eventos ficticios adicionales pertenecen exclusivamente al perfil `demo` y su carga es idempotente.

*   **RNF6 (Identidad y apertura):** Butaca utiliza la B roja con cinco pochoclos como inicial y solo `utaca` a su derecha; el lema es «MÁS EVENTOS PARA VOS». Al abrir o recargar el documento se reproduce una intro adaptada al tema claro u oscuro que integra el mismo logo en la cabecera y revela el lema al final. No se repite en navegación interna; admite salto, movimiento reducido y un límite aproximado de 3.12 s en escritorio o 2.31 s en móvil. Ver [detalle de identidad y apertura](docs/identidad-y-apertura.md).

*   **RNF5 (Interfaz operativa y fechas):** La cartelera, agenda del organizador, ventas y actividad permiten ordenar fechas en ambos sentidos. Las listas operativas ofrecen búsqueda, rango inclusivo de días, estado y paginación. El cliente distingue fecha del evento y fecha de compra. Los paneles separan tareas y mantienen la auditoría técnica desplegable; deben poder operarse con teclado y adaptarse a móvil.

*   **RNF1 (Seguridad Informática, PCI-DSS y Ley 25.326):** Ningún DTO expone hashes de contraseña. La tabla `pago` cumple PCI-DSS (sin almacenar PAN ni CVV) y `verificacion_identidad` cumple minimización de datos biométricos almacenando únicamente el token de validación KYC.
*   **RNF2 (Normalización en 3FN):** El modelo vigente usa 16 tablas: `rol`, `persona`, `usuario`, `verificacion_identidad`, `ubicacion`, `categoria`, `evento`, `tipo_entrada`, `compra`, `entrada`, `historial_estado_entrada`, `pago`, `reembolso`, `comprobante`, `notificacion` y `auditoria_seguridad`. Cantidad y total se calculan desde entradas. Comprobante solo referencia a pago, sin duplicar compra ni comprador.
*   **RNF3 (Usabilidad y Compatibilidad API):** Las respuestas JSON de la API REST del backend usan nomenclatura en formato **snake_case** (mediante anotaciones `@JsonProperty`), mientras que el código de Java mantiene la convención interna de **camelCase**.
*   **RNF4 (Facilidad de Instalación y Demo):** La aplicación cuenta con un script automatizado (`./scripts/demo.sh`) capaz de arrancar backend, frontend e inicializar datos semilla de prueba de forma transparente con un solo comando.

---

### **3.3 Casos de Uso**

#### **CU-01: Registro e Inicio de Sesión de Usuario**

> [!NOTE]
> Este caso de uso asegura el flujo de acceso seguro a la plataforma validando roles.

| Elemento | Descripción |
| :--- | :--- |
| **Actor Principal** | Cliente, Organizador o Administrador. |
| **Objetivo** | Registrarse en la plataforma o autenticarse para obtener un token/sesión que habilite las funciones del rol correspondiente. |
| **Requisito Asociado**| RF1 - Gestión de Cuentas y Seguridad. |
| **Precondición** | El usuario debe contar con un correo válido no registrado previamente si es un flujo de registro. |
| **Flujo Principal** | 1. El usuario accede al formulario (Registro o Login).<br>2. **Registro:** El usuario ingresa nombre, apellido, email, contraseña, teléfono; el registro público debe asignar rol CLIENTE. El sistema encripta la clave mediante BCrypt y persiste al usuario asignándole su rol correspondiente.<br>3. **Login:** El usuario ingresa email y contraseña.<br>4. El sistema valida la contraseña encriptada comparándola con la de la base de datos.<br>5. El sistema responde con el perfil del usuario (nombre, email, rol) y redirige a la vista correspondiente. |
| **Flujos de Excepción**| **E1 (Correo duplicado):** Si el email ya existe en `usuario`, la base de datos rechaza el registro lanzando una excepción controlada que se informa en la interfaz.<br>**E2 (Credenciales inválidas):** Si el correo o la contraseña no coinciden, se retorna error de credenciales incorrectas. |
| **Persistencia (BD)** | Registro transaccional de la identidad en `persona` y de la cuenta en `usuario` con `id_persona`, `id_rol`, email y hash BCrypt. El rol de registro público debe ser `CLIENTE`. |

---

#### **CU-02: Creación y Publicación de Eventos**

> [!IMPORTANT]
> El sistema restringe que los cupos totales excedan la capacidad máxima de la ubicación física seleccionada.

| Elemento | Descripción |
| :--- | :--- |
| **Actor Principal** | Organizador. |
| **Objetivo** | Registrar un nuevo espectáculo asociándolo a una categoría, una ubicación válida y definir su stock y precio de entradas. |
| **Requisito Asociado**| RF2, RF5. |
| **Precondición** | La categoría y la ubicación física ya deben existir y estar activas en el sistema. |
| **Flujo Principal** | 1. El Organizador abre su panel de gestión y presiona "Crear Evento".<br>2. Completa los datos: título, descripción, fecha, hora, precio, cupos totales, imagen y selecciona la ubicación y categoría.<br>3. El sistema valida que los `cupos_totales_evento` sean menores o iguales a la `capacidad_maxima` de la ubicación.<br>4. El Organizador elige el estado: guardar como `BORRADOR` o `PUBLICADO` inmediatamente.<br>5. El sistema persiste el evento y, si fue publicado, se vuelve visible en el catálogo de los clientes. |
| **Flujos de Excepción**| **E1 (Capacidad excedida):** Si los cupos del evento superan la capacidad física de la ubicación, el sistema rechaza la operación informando el límite máximo permitido.<br>**E2 (Fecha inválida):** Si la fecha del evento es anterior al día actual, el validador del backend rechaza la persistencia. |
| **Persistencia (BD)** | `evento` conserva `id_usuario_organizador`, `id_ubicacion`, `id_categoria`, fecha, hora, cupo total, estado y versión. El precio se guarda en `tipo_entrada`, asociado al evento. |

---

#### **CU-03: Compra de Entradas y Emisión de Tickets**

> [!WARNING]
> La compra es una operación atómica protegida por transaccionalidad y bloqueo optimista para evitar la sobreventa.

| Elemento | Descripción |
| :--- | :--- |
| **Actor Principal** | Cliente. |
| **Objetivo** | Adquirir una o más entradas para un espectáculo con disponibilidad de cupos, registrando la transacción y emitiendo los tickets UUID. |
| **Requisito Asociado**| RF3, RF4. |
| **Precondición** | El evento debe estar en estado `PUBLICADO`, contar con stock y el cliente debe estar autenticado. |
| **Flujo Principal** | 1. El cliente inicia sesión y navega al catálogo.<br>2. Selecciona un evento y una cantidad.<br>3. Confirma la compra simulada, sin ingresar datos bancarios.<br>4. El servicio bloquea optimistamente el evento y cuenta sus entradas vigentes.<br>5. Si hay cupo, inserta una cabecera de `compra` con cliente, fecha y estado.<br>6. Genera una fila en `entrada` por cada unidad, con UUID y precio histórico.<br>7. Registra la emisión en `historial_estado_entrada`.<br>8. Crea una `notificacion` y devuelve cantidad y total calculados. |
| **Flujos de Excepción**| **E1 (Cupos agotados):** Si no hay stock suficiente, el sistema bloquea el flujo.<br>**E2 (Conflicto de Concurrencia):** Si dos transacciones intentan comprar simultáneamente el último cupo, el motor JPA intercepta la colisión mediante el campo `version` del evento, fallando la segunda de forma segura y deshaciendo el descuento de inventario (Rollback) para evitar sobreventa. |
| **Persistencia (BD)** | Leer el evento con control de versión y contar entradas vigentes. Insertar `compra` con `id_usuario_comprador`, fecha, moneda y estado. Emitir `entrada` con evento, tarifa, persona titular, compra, UUID, precio y moneda históricos. Registrar pago, comprobante referenciado a pago, historial, notificación y auditoría. Cantidad y total no se persisten en compra. |

---

#### **CU-04: Gestión Administrativa y Auditoría**

| Elemento | Descripción |
| :--- | :--- |
| **Actor Principal** | Administrador (`ADMIN`). |
| **Objetivo** | Gestionar los catálogos base del negocio (categorías y ubicaciones) y auditar las métricas de compras y tickets a nivel global de la plataforma. |
| **Requisito Asociado**| RF6. |
| **Precondición** | El usuario debe estar autenticado con rol `ADMIN`. |
| **Flujo Principal** | 1. El Administrador accede a su panel de control administrativo.<br>2. Visualiza las tarjetas de resumen: ingresos totales cobrados por la plataforma, total de usuarios registrados y cantidad de eventos activos.<br>3. Accede al gestor de ubicaciones y añade un nuevo establecimiento físico indicando nombre, dirección y capacidad física máxima.<br>4. Accede al gestor de categorías para registrar nuevas categorías aplicables a los espectáculos. |
| **Persistencia (BD)** | Consultar usuarios, eventos y entradas. Calcular recaudación desde entradas de compras confirmadas con pago aprobado. Crear `ubicacion` con `nombre_ubicacion`, dirección, capacidad máxima y `activa_ubicacion`. |

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
| **Flujos de Excepción**| **E1 (Entrada Inexistente):** Si el código UUID no se encuentra en el sistema, se muestra la alerta: "Ticket Inválido o Falso".<br>**E2 (Entrada Ya Usada):** Si el estado del ticket es `USADA`, el sistema responde: **"Acceso Denegado: Entrada ya utilizada"** junto con la fecha y hora en la que fue validada originalmente.<br>**E3 (Evento Cancelado):** Si el estado es `ANULADA` (debido a la cancelación del evento), el sistema niega el acceso. |
| **Persistencia (BD)** | Consultar `entrada` por UUID, cambiar `CONFIRMADA` a `USADA` con fecha de validación y control de `version_entrada`, e insertar `historial_estado_entrada` en la misma transacción. La comprobación de DNI y autorización del organizador en backend es un requisito pendiente. |

---

#### **CU-06: Panel del Cliente y Notificaciones**

| Elemento | Descripción |
| :--- | :--- |
| **Actor Principal** | Cliente. |
| **Objetivo** | Visualizar la bandeja de notificaciones operativas, consultar el historial de compras realizadas y descargar los códigos de validación de sus tickets. |
| **Requisito Asociado**| RF5, RF6. |
| **Precondición** | El cliente debe tener una cuenta activa e iniciar sesión. |
| **Flujo Principal** | 1. El cliente ingresa a su Dashboard.<br>2. El sistema recupera y muestra su listado de compras realizadas ordenadas cronológicamente.<br>3. El cliente puede hacer clic en una compra para ver el detalle e inspeccionar los tickets individuales (UUIDs) asociados a ella.<br>4. El cliente ingresa a su bandeja de notificaciones internas para revisar avisos del sistema, marcándolos como leídos. |
| **Persistencia (BD)** | Consultar compras por `id_usuario_comprador` y ordenarlas por `fecha_compra`. Consultar notificaciones por `id_usuario`, ordenarlas por `created_at` y actualizar únicamente su indicador `leida_notificacion` al leerlas. |

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

La fuente editable vigente es [DrawDB: butaca-modelado-datos](https://drawdb.leandro754.moe/editor/diagrams/butaca-modelado-datos), con 16 entidades en 3FN. Los archivos Draw.io/DBML y las guías históricas deben contrastarse con esa fuente antes de usarse como DDL. El README incluye el resumen vigente de relaciones.
