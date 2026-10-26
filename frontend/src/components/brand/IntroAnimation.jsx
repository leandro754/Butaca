import { useLayoutEffect, useRef } from "react";
import { introTiming, logoFlightFrames, introPalette } from "./introMotion";

// Se monta una vez con AppShell: abrir/recargar repite la firma; cambiar de ruta no.
function IntroAnimation({
  active,
  logoRef,
  headerRef,
  onComplete,
  theme = "light",
}) {
  const overlayRef = useRef(null);
  useLayoutEffect(() => {
    if (!active) return;
    const logo = logoRef.current;
    const header = headerRef.current;
    const overlay = overlayRef.current;
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
    if (
      !logo ||
      !header ||
      !overlay ||
      reducedMotion.matches ||
      typeof logo.animate !== "function"
    ) {
      onComplete();
      return;
    }
    const mark = logo.querySelector("[data-logo-bucket]");
    const tagline = logo.querySelector("[data-logo-tagline]");
    if (!mark || !tagline) {
      onComplete();
      return;
    }
    const compact = window.innerWidth < 768;
    const timing = introTiming(compact);
    const palette = introPalette(theme);
    const animations = [];
    let completed = false;
    function finish() {
      if (completed) return;
      completed = true;
      onComplete();
    }
    function animate(element, frames, options) {
      if (!element) return null;
      const animation = element.animate(frames, {
        fill: "both",
        easing: "cubic-bezier(.22,1,.36,1)",
        ...options,
      });
      animations.push(animation);
      return animation;
    }
    // FLIP sobre el logo real del header; su espacio nunca desaparece del layout.
    animate(
      logo,
      logoFlightFrames(
        logo.getBoundingClientRect(),
        mark.getBoundingClientRect(),
        { width: window.innerWidth, height: window.innerHeight },
      ),
      { duration: timing.duration, easing: "linear" },
    );
    animate(
      logo.querySelector("[data-logo-bucket]"),
      [
        { opacity: 0, transform: "translateY(12px) scale(.93)" },
        { opacity: 1, transform: "translateY(0) scale(1)" },
      ],
      { delay: timing.bucketStart, duration: timing.bucketDuration },
    );
    logo.querySelectorAll("[data-logo-popcorn]").forEach((kernel, index) => {
      const frames =
        compact && index >= 3
          ? [{ opacity: 0 }, { opacity: 1 }]
          : [
              { opacity: 0, transform: "translateY(16px)", offset: 0 },
              { opacity: 1, transform: "translateY(-23px)", offset: 0.43 },
              { opacity: 1, transform: "translateY(3px)", offset: 0.78 },
              { opacity: 1, transform: "translateY(0)", offset: 1 },
            ];
      animate(kernel, frames, {
        delay: timing.popcornStart + index * timing.popcornStagger,
        duration: timing.popcornDuration,
      });
    });
    logo.querySelectorAll("[data-logo-letter]").forEach((letter, index) => {
      animate(
        letter,
        [
          { opacity: 0, transform: "translate(8px, 5px)" },
          { opacity: 1, transform: "translate(0, 0)" },
        ],
        {
          delay: timing.lettersStart + index * timing.lettersStagger,
          duration: timing.lettersDuration,
        },
      );
    });
    animate(
      logo.querySelector("[data-logo-letters]"),
      [{ fill: palette.letters }, { fill: "#FFF9F0" }],
      { delay: timing.pageStart, duration: timing.at(450) },
    );
    animate(
      header,
      [
        { backgroundColor: "transparent", borderColor: "transparent" },
        { backgroundColor: palette.header, borderColor: "#8E2431" },
      ],
      { delay: timing.pageStart, duration: timing.at(650) },
    );
    header.querySelectorAll("[data-intro-reveal]").forEach((element) =>
      animate(element, [{ opacity: 0 }, { opacity: 1 }], {
        delay: timing.pageStart,
        duration: timing.at(650),
      }),
    );
    animate(overlay, [{ opacity: 1 }, { opacity: 0 }], {
      delay: timing.pageStart,
      duration: timing.pageDuration,
    });
    const last = animate(
      tagline,
      [
        {
          opacity: 0,
          transform: "translateY(4px)",
          clipPath: "inset(0 100% 0 0)",
        },
        { opacity: 1, transform: "translateY(0)", clipPath: "inset(0 0 0 0)" },
      ],
      { delay: timing.taglineStart, duration: timing.taglineDuration },
    );
    last.finished.then(finish).catch(() => {});
    const deadline = window.setTimeout(finish, timing.duration + 350);
    function keyboard(event) {
      if (["Escape", "Tab"].includes(event.key)) finish();
    }
    window.addEventListener("resize", finish);
    window.addEventListener("pagehide", finish);
    document.addEventListener("pointerdown", finish, {
      capture: true,
      once: true,
    });
    document.addEventListener("wheel", finish, { passive: true, once: true });
    document.addEventListener("keydown", keyboard);
    reducedMotion.addEventListener("change", finish);
    return () => {
      completed = true;
      window.clearTimeout(deadline);
      animations.forEach((animation) => animation.cancel());
      window.removeEventListener("resize", finish);
      window.removeEventListener("pagehide", finish);
      document.removeEventListener("pointerdown", finish, true);
      document.removeEventListener("wheel", finish);
      document.removeEventListener("keydown", keyboard);
      reducedMotion.removeEventListener("change", finish);
    };
  }, [active, logoRef, headerRef, onComplete, theme]);
  if (!active) return null;
  return (
    <>
      <div
        ref={overlayRef}
        className="intro-overlay"
        data-appearance={theme}
        aria-hidden="true"
        style={{ backgroundColor: introPalette(theme).backdrop }}
      />
    </>
  );
}
export default IntroAnimation;
