# BUTACA — Identidad visual, dirección de arte y animación de entrada

> **Documento de diseño y especificación de implementación**  
> Concepto: **«La función empieza desde que entrás».**  
> Versión: 1.0 · Octubre de 2026

---

## 1. Idea central

**Butaca** es una plataforma para descubrir eventos y comprar entradas. Su estética combina el lenguaje del cine clásico (carteles, marquesinas, tipografía de exhibición, tickets y luces de sala) con la claridad de una interfaz digital actual.

La marca se reconoce por tres elementos inseparables:

1. Una **B roja** construida como un recipiente de pochoclos.
2. Un pequeño grupo de **pochoclos dorados**, agrupados sobre esa B como una pila estable y no como piezas que flotan permanentemente.
3. La palabra **Butaca**, en una sola línea: **la B roja con pochoclos *es* la primera letra**; a su derecha se escribe solamente **«utaca»**, en negro/carbón. Nunca repetir otra B.

Lema: **MÁS EVENTOS PARA VOS**.

La propuesta debe sentirse **cinematográfica, cálida, moderna, reconocible y funcional**. No imitar un sitio antiguo en su usabilidad; rescatar su atmósfera visual.

---

## 2. Personalidad y dirección de arte

| Rasgo | Cómo expresarlo |
|---|---|
| Cinematográfico | Títulos protagonistas, fotografía inmersiva y tarjetas tipo entrada. |
| Cercano | Español argentino, microcopys breves: «Tu próxima salida», «Ver entradas». |
| Elegante | Pocos colores bien usados, ritmo tipográfico y espacio negativo. |
| Enérgico | Rojo para acciones y estados activos; dorado para detalles puntuales. |
| Claro | Precio, fecha, ubicación y disponibilidad fáciles de encontrar. |
| Alegre sin infantilizar | Pochoclos simples y animación breve; no caricaturas, confeti ni exceso de rebotes. |

**Regla estética:** la interfaz debe parecer una plataforma contemporánea que *tiene alma de cine*, no un afiche retro que accidentalmente tiene botones.

### Qué evitar

- Un logotipo independiente seguido por una segunda B en «Butaca».
- Pochoclos suspendidos como partículas permanentes, alejados del recipiente.
- Gradientes multicolor, neón indiscriminado o demasiadas texturas vintage.
- Cortinas gigantes tapando contenido o dificultando la navegación.
- Fuente de exhibición en párrafos, formularios o información pequeña.
- Reproducir una intro larga **en cada navegación interna**.
- Bloquear el sitio esperando una animación si el contenido ya puede mostrarse.

---

## 3. Paleta cromática

Los siguientes códigos son **valores de diseño sugeridos**, no una medición exacta de los píxeles de las referencias. Usar los tokens como fuente única de verdad y ajustar el rojo del símbolo final cuando exista el SVG maestro.

| Token | Color | HEX | Uso |
|---|---|---|---|
| `brand-red` | Rojo pochoclera / identidad | `#E6003E` | B del logo, CTAs primarios, indicadores de selección. |
| `deep-red` | Bordó cinematográfico | `#8E2431` | Cortinas y acentos editoriales secundarios. |
| `red-hover` | Rojo de interacción | `#BC0032` | Hover y estados presionados. |
| `popcorn` | Dorado pochoclo | `#FFC36B` | Pochoclos, detalles de énfasis, líneas y algunos microestados. |
| `popcorn-deep` | Dorado tostado | `#E9A842` | Sombra/acento opcional de granos; no es color de texto sobre blanco. |
| `ink` | Negro carbón | `#17191B` | Wordmark «utaca», header, textos principales. |
| `charcoal` | Gris negro | `#252526` | Superficies oscuras, overlays y tarjetas destacadas. |
| `warm-paper` | Marfil palomita | `#F7EBDD` | Secciones editoriales, tarjetas de fecha, superficies suaves. |
| `white` | Blanco puro | `#FFFFFF` | **Fondo obligatorio de la animación inicial**, áreas limpias y contraste. |
| `muted` | Gris secundario | `#62666A` | Metadatos, ayuda, información auxiliar. |
| `border` | Borde neutro | `#E7E2DC` | Separadores, inputs y contornos. |

### Proporción visual orientativa

- **65 %** blanco / superficies claras.
- **23 %** negro carbón / fotografía oscura.
- **9 %** rojo.
- **3 %** dorado pochoclo.

Son proporciones de intención, **no cuotas rígidas**. En el hero fotográfico puede dominar el negro. En el loader debe dominar el blanco.

### Variables CSS propuestas

```css
:root {
  --brand-red: #E6003E;
  --deep-red: #8E2431;
  --red-hover: #BC0032;
  --popcorn: #FFC36B;
  --popcorn-deep: #E9A842;
  --ink: #17191B;
  --charcoal: #252526;
  --warm-paper: #F7EBDD;
  --white: #FFFFFF;
  --muted: #62666A;
  --border: #E7E2DC;

  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 18px;
  --ease-cinema: cubic-bezier(.22, 1, .36, 1);
}
```

**Accesibilidad:** los textos pequeños deben contrastar claramente. El dorado sobre blanco es decorativo, no un color apropiado para cuerpo de texto. Evitar letras blancas pequeñas sobre el rojo más brillante sin verificar contraste; para botones puede utilizarse un rojo más oscuro o texto carbón, según medición.

---

## 4. Sistema tipográfico

La referencia gráfica aportada corresponde a un estilo de letra **display cinematográfica geométrica y angulosa**, semejante al ejemplar rotulado **«Willordy Night»** de las imágenes. Utilizar la fuente exacta **solo si el equipo dispone del archivo y los derechos de uso para web**. No asumir que está libremente licenciada.

### Jerarquías

| Nivel | Estilo | Aplicación |
|---|---|---|
| Display | **Willordy Night** (si está licenciada) o una alternativa geométrica condensada | Hero, «LA CARTELERA», campañas, grandes títulos de secciones. |
| Marca | B personalizada + `utaca` en sans geométrica de peso muy alto, ajustada ópticamente | Logo oficial; mantener un wordmark propio. |
| Interface | **Manrope** o **Inter** | Búsqueda, navegación, botones, filtros, precio, fecha, ubicaciones y párrafos. |
| Microetiquetas | Sans semibold en mayúsculas y tracking moderado | «PRÓXIMA FUNCIÓN», «TU PRÓXIMA SALIDA», categorías. |
| Lema | Sans medium, mayúsculas, tracking amplio | «MÁS EVENTOS PARA VOS». |

**No sacrificar legibilidad por coherencia ornamental:** los titulares grandes pueden tener la tipografía de cine; el contenido y los controles no.

### Escala recomendada (desktop)

- Hero principal: `clamp(3rem, 5.8vw, 6.3rem)`, interlineado `0.92–1.0`.
- Título sección: `clamp(2.5rem, 4.2vw, 4.8rem)`.
- Nombre del evento en tarjeta: `1.25–1.65rem`, peso 700.
- Textos UI: `0.875–1rem`.
- Metadata: `0.8–0.9rem`.
- Lema bajo logo: `0.55–0.7rem`, tracking `0.18–0.28em`.

Para títulos muy largos, aplicar límites de ancho y cortes de línea deliberados. Evitar que la tipografía decorativa reduzca o desborde campos importantes.

---

## 5. Logotipo principal: una sola palabra

### Construcción

```text
       ✿  ✿  ✿
         ✿ ✿
      [ B roja ]utaca
       MÁS EVENTOS PARA VOS
```

*Esquema conceptual, no el diseño final del glifo.*

- La **B roja** es simultáneamente inicial y contenedor de pochoclos.
- Los **5 granos estilizados** pueden formar una pirámide compacta; base ancha y cima centrada.
- En reposo los granos deben verse **apoyados visualmente** en el borde superior del recipiente, evitando grandes huecos.
- `utaca` empieza inmediatamente después de la B, con kerning óptico, **sobre la misma línea de base**.
- El icono B y «utaca» deben mantener una **altura de letra coherente**. Los pochoclos sobresalen por encima, como rasgo característico.
- El lema puede aparecer debajo de toda la palabra, alineado a su ancho óptico. En header angosto puede ocultarse en móvil, nunca duplicarse.
- Generar variantes maestras en **SVG**: horizontal completa, horizontal sin lema, isotipo B+pochoclos, una tinta y versión clara para fondo oscuro.

### Uso cromático

- B: `brand-red`.
- Pochoclos: `popcorn`.
- Letras `utaca`: `ink` sobre fondos claros o `white` sobre fondos oscuros.
- Lema: `muted` en claro, `warm-paper` en oscuro.

El **isotipo** es útil para favicon/app, pero en cabecera de escritorio conviene el **wordmark horizontal completo**.

---

## 6. Estética general de la página

### Header

- Fondo `ink` o `charcoal`, altura aprox. `76–88px` en escritorio.
- Logo integrado horizontal a la izquierda. El rojo y el dorado destacan sobre el fondo oscuro; `utaca` se muestra en blanco/marfil.
- Buscador centrado con borde sutil y placeholder «Evento, artista o lugar…».
- Links: **Cartelera**, **Categorías**, cuenta/usuario, salir.
- Indicador activo: acento rojo o dorado de forma **discreta y consistente**.
- Header `sticky` opcional con ligera reducción de alto al hacer scroll.
- No añadir grandes cortinas al header; pueden vivir en hero o como detalle mínimo lateral.

### Hero editorial

- Foto amplia del evento actual: estadio, concierto, teatro o festival.
- Overlay gradual **negro/carbón** para garantizar lectura en la zona del texto.
- Titular con tipografía de cine en marfil/blanco, sobre fondo oscuro.
- Etiquetas de categoría, locación, fecha y hora separadas visualmente.
- Botón principal rojo: **VER ENTRADAS**; el precio acompaña sin competir.
- La fecha destacada puede vivir dentro de una tarjeta tipo ticket en marfil.
- Numeración del carrusel y flechas discretas, con controles reales y accesibles.
- Texturas y cortinas rojas son **recursos escenográficos secundarios**, nunca interfieren con los elementos interactivos.

### Franja de eventos destacados

- Debajo del hero: cuatro eventos con nombre, fecha y número (`01–04`).
- Fondo carbón, tipografía clara, estado activo con subrayado rojo.
- Si la pantalla es estrecha, convertir en scroll horizontal accesible.

### Sección «La cartelera»

- **Fondo blanco** o marfil muy claro. Tipografía display negra para el título.
- Supratítulo rojo: «TU PRÓXIMA SALIDA».
- Detalle editorial mínimo inspirado en perforaciones de cinta cinematográfica, sin ocupar toda la sección.
- Filtros: Todos, Conciertos, Teatro, Deportes, Festivales y Cine.
- Activo rojo; resto neutro, con hover suave.
- Selector «Fecha más cercana» simple y legible.
- Tarjetas de eventos en grilla con foto, título, fecha, lugar, precio y acción visible.

### Tarjetas

- Imagen dominante `16:10` o `4:3`, esquinas redondeadas moderadas.
- Contenido sobre blanco, borde tenue `border`.
- Chip de categoría; fecha y precio en lugares predecibles.
- Hover: elevar `2–4px`, revelar sombra muy leve y acentuar el enlace, sin desplazar el layout vecino.
- Cuidar estados loading, sin resultados y error, con la misma estética.

### Footer

- Fondo carbón, logo blanco/rojo/dorado.
- Lema, enlaces de ayuda/contacto, redes y legales.
- Pequeño elemento editorial (una línea dorada o perforación) como cierre.

---

## 7. Animación de carga: «Empieza la función»

### Objetivo narrativo

El visitante abre Butaca. Encuentra un **fondo blanco puro**, aparece primero la **B roja**, **unos pocos pochoclos saltan y se acomodan**, la palabra se completa como **Butaca** con la B funcionando como primera letra, el logo viaja suavemente a su ubicación definitiva mientras **aparece la página**, y **el lema «MÁS EVENTOS PARA VOS» es el último elemento en revelarse**.

Esto **no es un vídeo independiente** ni un logo separado que luego se reemplaza por otro: idealmente es el **mismo conjunto visual**, animado hasta llegar a su lugar final en el header.

### Storyboard y línea de tiempo (duración objetivo ~3.1 segundos)

| Momento | Escena | Detalle del movimiento |
|---|---|---|
| `0.00–0.20 s` | **Pantalla blanca** | Sin spinner, textura ni cortinas. La página puede precargarse detrás. |
| `0.20–0.62 s` | **Entra la B roja** | Opacidad `0→1`, escala `0.93→1`, desplazamiento vertical `12px→0`, easing suave. Centrada. |
| `0.52–1.20 s` | **Saltan los pochoclos** | 3–5 granos dorados emergen del borde superior de la B con un pequeño rebote escalonado, se acomodan en una **pirámide** compacta y quedan quietos. |
| `1.10–1.70 s` | **Se forma «Butaca»** | Aparecen las letras **`u t a c a`** a la derecha, con máscara/revelado de izquierda a derecha, opacidad suave y desplazamiento de `8px`. **No aparece otra B.** |
| `1.68–2.28 s` | **El logotipo se traslada** | El conjunto B+pochoclos+`utaca` reduce su tamaño y se ubica en la cabecera final usando una transición FLIP o coordenadas medidas. **No se corta ni se sustituye bruscamente**. |
| `2.05–2.85 s` | **Aparece la página** | El overlay blanco pierde opacidad. Header, hero y contenido se revelan en una transición leve; no mover toda la página desde lejos. |
| `2.70–3.12 s` | **Último acto: lema** | Debajo del logotipo ya ubicado en el header aparece **«MÁS EVENTOS PARA VOS»**, letra por letra o con reveal en máscara, opacidad `0→1`. Termina el movimiento. |

**Criterio final:** el logo queda integrado en el header, el sitio está visible e interactivo y el último detalle animado es el lema. Si se prefiere que el lema se lea en grande durante la intro, usar un duplicado solo visual y transitorio, invisible para lectores de pantalla, pero mantener **una única identidad visible al terminar**.

### Cómo deberían rebotar los pochoclos

- Solo se animan unos **3–5 granos**, no una lluvia.
- El salto máximo sugerido es de **16–28px**, según tamaño del isotipo.
- Entradas escalonadas cada `60–90ms`.
- Trayectoria corta, casi vertical, con ligeros offsets laterales.
- Rebote tipo `back.out(1.2)` o spring amortiguado (`stiffness ~280`, `damping ~20`, a calibrar).
- **Una sola caída/asentamiento**. Nada de flotar ni vibrar después.
- La composición final queda centrada, como pochoclos **apoyados sobre la B**.
- Recomendación: definir posiciones finales manualmente, no física aleatoria; así la marca se ve siempre igual.

### Animación de las letras

- Los caracteres añadidos son **exactamente** `u`, `t`, `a`, `c`, `a`.
- Evitar efecto máquina de escribir con cursor parpadeante: no corresponde a la marca.
- Preferir revelado por máscara (`clip-path`/`overflow: hidden`) y una entrada vertical pequeña.
- La B debe seguir visible y en la misma línea de base mientras se añade `utaca`.
- Considerar `font-display` y precarga del recurso tipográfico para que no haya saltos de tamaño.

### Transición del logo al header

Una forma sólida de implementarla:

1. Renderizar la página **debajo** de un overlay blanco. El header contiene un elemento destino reservado con las medidas finales del logo.
2. Animar un **wordmark único** en una capa por encima del overlay; calcular la posición de partida centrada y la posición del destino con `getBoundingClientRect()`.
3. Usar **FLIP** (`First, Last, Invert, Play`) o transición equivalente para hacer escala/traslación mediante `transform`, sin modificar `top/left` en cada frame.
4. Cuando llega al header, fijarlo en su contenedor o efectuar un intercambio imperceptible con el SVG final (ambos visualmente idénticos).
5. Revelar el lema **después** de mostrar el contenido. El lema pertenece al logotipo final y se ubica inmediatamente debajo del wordmark, no como segundo encabezado gigante.

En móviles, el logo puede terminar como variante horizontal compacta y el lema puede reservarse para un tamaño menor o para un área visible de cabecera ampliada: **siempre respetar la intención «lema al final» donde sea visible**.

### Sensación que buscamos

**Suave, ligera, precisa y memorable**. No parecer una pantalla de carga de videojuego ni una presentación de 10 segundos. El concepto de pochoclos aporta personalidad, pero debe sentirse como una firma de marca profesional.

---

## 8. Rendimiento y experiencia real

- Preferir **SVG** para B, granos y wordmark; evitar PNG escalado para elementos que se animan.
- Animar principalmente `transform` y `opacity` por rendimiento.
- Usar GSAP/Framer Motion/Motion si el stack ya los incluye; si no, Web Animations API es suficiente. **No añadir una librería pesada solo para esto**.
- Cargar fuentes con `font-display: swap` y reservar métricas o precargar la fuente crítica de la marca.
- Predecir tamaño y destino del logo: sin cambios de layout (CLS).
- El contenido principal debe estar disponible en el DOM desde el inicio, con navegación y lectura accesibles.
- Ejecutar la secuencia **solo en la primera entrada de la sesión** (o según una regla de UX definida), nunca al cambiar filtros, abrir un evento o volver atrás.
- No retrasar la interacción innecesariamente: opción **«Saltar animación»** accesible por teclado y un límite estricto si hay carga lenta.
- No asociar el fin de la intro a la resolución de todas las imágenes remotas; usar placeholders/estados de carga reales.
- No iniciar audio sin acción del usuario.

### Movimiento reducido

```css
@media (prefers-reduced-motion: reduce) {
  /* Omitir saltos, escalados, traslaciones y apariciones secuenciales. */
  /* Mostrar la página y el logotipo final de inmediato. */
}
```

Con `prefers-reduced-motion`, mostrar directamente el logo integrado, la página y el lema. La marca mantiene su diseño aunque no haya movimiento.

---

## 9. Comportamiento responsive

### Escritorio (`≥1024px`)

- Logo horizontal a la izquierda, búsqueda visible, menú a la derecha.
- Hero de gran formato con texto en tercio izquierdo y fotografía.
- Tarjetas de cartelera en `3–4` columnas según ancho.
- Intro: logo inicia centrado y termina en header, con margen suficiente.

### Tablet (`768–1023px`)

- Reducir navegación visible, mantener acceso a búsqueda y cuenta.
- Hero con título más compacto y menos elementos escénicos.
- Tarjetas en `2–3` columnas.

### Móvil (`<768px`)

- Header compacto; búsqueda accesible con icono o segunda fila.
- Logo **sigue siendo B + `utaca`**. Nunca sustituirlo permanentemente por una B separada si hay espacio suficiente.
- Hero con texto sobre fotografía y overlays reforzados.
- Una columna de tarjetas o carrusel horizontal con buen tamaño táctil.
- Intro más breve, aprox. `2.0–2.4s`, menos desplazamiento y **menos granos animados** (el símbolo final puede mantener los cinco).
- No forzar texto del lema minúsculo: colocarlo bajo el logo en una cabecera de dos niveles o en el área de presentación móvil, si corresponde.

---

## 10. Componentes y estados de diseño

| Componente | Aspecto / animación |
|---|---|
| Botón primario | Rojo, texto de alto contraste, radio medio, `150–220ms` de transición. |
| Botón secundario | Borde gris carbón / transparente; hover marfil. |
| Filtro activo | Fondo rojo o subrayado rojo con estado semántico `aria-pressed`/`aria-selected`. |
| Tarjeta de fecha | Marfil cálido, fecha grande oscura, mes rojo. |
| Buscador | Sobre carbón: borde claro translúcido, focus visible cálido; icono de búsqueda. |
| Tarjeta de evento | Blanco, imagen protagonista, hover sobrio, fecha y precio inmediatamente visibles. |
| Estados vacíos | Ilustración mínima si se usa: B/isotipo estilizado. Mensaje claro, sin animaciones pesadas. |
| Skeleton | Bloques neutros, sin shimmer agresivo. |
| Modales | Fondo oscuro atenuado, elevación delicada y cierre accesible. |

---

## 11. Texturas, iconografía y fotografía

- **Textura opcional:** grano finísimo o papel solo en grandes superficies editoriales; opacidad muy baja. En la intro, **blanco puro sin textura**.
- **Decoración:** líneas de perforación de película, ticket recortado, pequeñas cortinas rojas en campañas puntuales. No mezclar todos en una misma sección.
- **Iconos UI:** línea simple, trazo constante, sin iconografía retro difícil de interpretar.
- **Imágenes de eventos:** reales, de alto impacto, recortadas cuidadosamente; no aplicar tinte rojo a todas, porque destruye diferenciación entre eventos.
- **Hero:** foto protagonista con overlay gradual; reservar contraste para títulos y CTA.

---

## 12. Estructura de implementación sugerida

```text
src/
  components/
    brand/
      ButacaLogo.tsx          # B con pochoclos + «utaca» + lema opcional
      PopcornMark.tsx         # B roja y granos en SVG con IDs animables
      IntroAnimation.tsx      # Escena de carga y movimiento al header
    layout/
      Header.tsx
      Footer.tsx
    events/
      HeroCarousel.tsx
      FeaturedEventStrip.tsx
      EventFilters.tsx
      EventCard.tsx
  styles/
    tokens.css
    typography.css
    motion.css
  assets/
    brand/butaca-logo.svg
    brand/butaca-mark.svg
    fonts/                  # Solo fuentes con licencia válida
```

### Contrato visual del componente logo

```tsx
<ButacaLogo
  variant="horizontal"
  theme="onDark"          // onLight | onDark
  showTagline={true}
/>
```

Internamente, la composición conceptual debe ser:

```text
<wordmark>
  <PopcornB />       ← representa la letra B (UNA SOLA)
  <text>utaca</text>  ← no escribir «Butaca» aquí
</wordmark>
<tagline>MÁS EVENTOS PARA VOS</tagline>
```

Para accesibilidad, el logo como conjunto puede exponerse con `aria-label="Butaca. Más eventos para vos"`, mientras que los trozos visuales internos se marcan `aria-hidden="true"` para evitar que un lector anuncie «utaca» en vez de «Butaca».

### Estados propuestos

```text
idle
  ↓
showB
  ↓
popcornBounce
  ↓
revealUtaca
  ↓
moveLogoToHeader
  ↓
revealPage
  ↓
revealTagline
  ↓
completed
```

Controlar la animación con una timeline centralizada, en vez de muchos timeouts dispersos. Si el usuario solicita saltar o reduce motion, ir directamente a `completed`.

---

## 13. Criterios de aceptación para desarrollo

- [ ] El fondo de la animación inicial es **#FFFFFF**.
- [ ] La B roja aparece primero, con el mismo diseño del logotipo final.
- [ ] Saltan pocos pochoclos y terminan **acomodados en pirámide** sobre la B.
- [ ] Se revelan solo las letras **«utaca»** junto a la B.
- [ ] Nunca existe una **segunda B** delante de `utaca`.
- [ ] La palabra completa se lee claramente como **Butaca**.
- [ ] El logotipo se integra al header sin salto visible ni corte abrupto.
- [ ] **La página aparece** antes del último detalle animado.
- [ ] **«MÁS EVENTOS PARA VOS» aparece al final**, debajo del logo final.
- [ ] La paleta roja, negra, marfil y pochoclo es consistente en la web.
- [ ] La fuente tipo póster de cine se usa en títulos grandes, no en texto pequeño.
- [ ] La intro no se repite en cada navegación interna.
- [ ] Los ajustes `prefers-reduced-motion` funcionan.
- [ ] No hay bloqueo de interacción, saltos de diseño ni problemas de contraste.
- [ ] Desktop, tablet y móvil tienen comportamientos definidos.

---

## 14. Brief listo para entregar a diseño o a Codex

> Rediseñar la identidad e interfaz de **Butaca**, plataforma de eventos y entradas, con estética cinematográfica contemporánea inspirada en marquesinas y tipografía display tipo póster de cine, sin perder usabilidad. Paleta base: rojo `#E6003E`, negro carbón `#17191B`, dorado pochoclo `#FFC36B`, blanco `#FFFFFF` y marfil `#F7EBDD`. La marca es **una sola palabra**: una **B roja con cinco pochoclos agrupados encima** es la primera letra de **Butaca**; escribir **solo `utaca`** a su lado en sans geométrica de gran peso. Implementar una intro fluida sobre **fondo blanco**: primero aparece la B; algunos pochoclos saltan y se acomodan como pirámide; aparece `utaca` progresivamente; el logo completo se reduce y se integra al header; luego se revela la home; y **finalmente aparece el lema «MÁS EVENTOS PARA VOS»** bajo el logo en el header. Duración escritorio objetivo `~3.1s`, móvil más corta, sin aleatoriedad ni rebotes permanentes. Preferir SVG, transforms y opacidad, y contemplar reducción de movimiento, salto de intro, estabilidad de layout y accesibilidad. Usar fuente display cinematográfica como la referencia **Willordy Night** (si existe licencia de uso web) para titulares y una sans clara para UI. Mantener la estructura de home: header con buscador, gran hero editorial, tira de destacados, filtros de cartelera, tarjetas de eventos y footer coherente.

---

**Idea rectora:** *El logo no aparece para luego desaparecer: abre la función, se transforma en parte de la interfaz y deja que los eventos sean los protagonistas.*
