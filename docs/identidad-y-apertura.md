# Butaca: identidad cinematográfica y apertura

Implementación de la guía [BUTACA_identidad_visual_y_animacion.md](../referencias%20visuales/BUTACA_identidad_visual_y_animacion.md) y la referencia de cartelera aportada por el usuario.

## Marca y recursos

- La B roja hace de recipiente y primera letra. Se acompaña de cinco pochoclos dorados y únicamente `utaca`, vectorizado desde Manrope 800.
- `ButacaLogo` y `PopcornMark` comparten geometría con los SVG maestros. No se escala el PNG ni se añade una B duplicada.
- El favicon es `/brand/butaca-mark.svg`. El título de la pestaña es **Butaca · Más eventos para vos**.
- Los recursos públicos incluyen variante horizontal completa, sin lema, para fondo oscuro, isotipo y una tinta. Se regeneran con `node scripts/export-brand.mjs` desde `frontend/`.
- Manrope para interfaz y Barlow Condensed para titulares, etiquetas y acciones. No se incluyó Willordy Night porque no se aportó un archivo con licencia web. Las fuentes se sirven localmente en WOFF2, con sus licencias SIL OFL incluidas; fuentes originales: [Manrope](https://github.com/google/fonts/tree/main/ofl/manrope), [Barlow Condensed](https://github.com/google/fonts/tree/main/ofl/barlowcondensed).

## Intro

Por indicación expresa del usuario se reproduce **en cada apertura o recarga completa**, aunque ya se haya visto en la sesión. No se reproduce al cambiar filtros, abrir un evento, volver atrás ni navegar internamente: su estado pertenece a `AppShell`.

Respeta la apariencia guardada: blanco con letras oscuras en modo claro; carbón con luz roja tenue y letras marfil en modo oscuro. Mantiene B roja y pochoclos dorados en ambos temas.

Secuencia de escritorio, 3.12 segundos:

1. Overlay blanco puro y aparición de la B.
2. Cinco pochoclos con un salto corto escalonado, que terminan asentados en sus posiciones definitivas.
3. Revelado de las cinco letras `utaca`.
4. El mismo elemento SVG de la cabecera viaja desde el centro a su destino mediante transforms calculados con sus rectángulos reales.
5. La página se revela desde 2.05 s, sin depender de la respuesta de la API o de las fotos.
6. El lema aparece al final, desde 2.70 s hasta 3.12 s.

En móvil se acorta a aproximadamente 2.31 segundos. No hay vídeo, reproducción de audio ni librería de animaciones añadida. Se usa Web Animations API.

Escape, Tab o una interacción permiten terminar la secuencia inmediatamente. Con `prefers-reduced-motion` el sitio y el logo se muestran directamente. Cambiar esa preferencia o el tamaño de la ventana durante la intro también la finaliza. Un límite adicional de 350 ms impide que quede bloqueada si una animación no notifica su final. Todas las animaciones y listeners se limpian al desmontar. Por indicación del usuario no se muestran botones de salto de intro ni de pausa del carrusel.

## Interfaz

Paleta roja, carbón, marfil y pochoclo; hero ancho con foto y overlay, cortinas laterales discretas, fecha tipo ticket y controles anterior/siguiente reales. Los cuatro recomendados priorizan categorías diferentes entre los eventos publicados con cupo, ordenados por cercanía. Si faltan categorías, se completa sin repetir eventos; no se atribuye popularidad ni personalización inexistente.

El carrusel cambia cada 7 segundos, con barra de progreso, flechas, selección directa y deslizamiento horizontal en móvil. Se pausa con mouse encima, foco de teclado o pestaña oculta, conservando el tiempo restante. Con movimiento reducido funciona manualmente. La franja de selección acompaña el evento activo en pantallas pequeñas y se precarga la siguiente imagen.

La cartelera mantiene categorías de la API, fechas, precios y filtros reales. Ofrece «Próximos 7 días» (hoy y seis días siguientes), «Este finde» (viernes a domingo, o los días restantes del fin de semana actual) y «Este mes», combinables con categoría y orden de fecha. Muestra nueve tarjetas inicialmente y agrega seis por clic, con contador y restablecimiento de filtros cuando no hay resultados. Las tarjetas entran escalonadas y los botones tienen una respuesta breve al pulsar; el movimiento reducido desactiva estas animaciones.

La semilla adicional de 14 eventos ficticios está en `src/main/resources/demo-events.tsv` y se carga solo con perfil `demo`, sin alterar eventos ya existentes. Agrega Cine y Comedia, y amplía las cuatro categorías originales. `scripts/seed-demo-events.ps1` permite agregar la misma semilla a la demo en ejecución mediante los endpoints existentes, sin reiniciar H2 ni perder compras. Es idempotente por título del organizador de demo.

Títulos de exhibición y detalle de perforaciones, tarjetas claras y footer oscuro. Formularios, paneles, QR y checkout mantienen tipografía de lectura y sus operaciones existentes.

## Verificación técnica

```powershell
cd frontend
node scripts/export-brand.mjs
node --test src/components/brand/introMotion.test.js src/utils/collectionFilters.test.js src/utils/qrCode.test.js src/utils/homeFeed.test.js
pnpm run build
```

La revisión visual de la página y de la secuencia completa queda a cargo del usuario. No se levantan procesos adicionales de Vite para esta tarea.
