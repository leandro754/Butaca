import { useEffect, useRef, useState } from "react";

export const CAROUSEL_INTERVAL = 7000;

export function nextSlide(index, count, direction = 1) {
  if (count < 1) return 0;
  return (((index + direction) % count) + count) % count;
}

export function useCarousel(count) {
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [hovered, setHovered] = useState(false);
  const [focused, setFocused] = useState(false);
  const [hidden, setHidden] = useState(() => document.hidden);
  const [reducedMotion, setReducedMotion] = useState(
    () => window.matchMedia("(prefers-reduced-motion: reduce)").matches,
  );
  const activeIndex = Math.min(selectedIndex, Math.max(0, count - 1));
  const clock = useRef({ cycle: "", remaining: CAROUSEL_INTERVAL });
  const playing =
    count > 1 && !hovered && !focused && !hidden && !reducedMotion;

  useEffect(() => {
    const media = window.matchMedia("(prefers-reduced-motion: reduce)");
    const onMotion = () => setReducedMotion(media.matches);
    const onVisibility = () => setHidden(document.hidden);
    media.addEventListener("change", onMotion);
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      media.removeEventListener("change", onMotion);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, []);

  useEffect(() => {
    const cycle = `${activeIndex}-${count}`;
    if (clock.current.cycle !== cycle) {
      clock.current = { cycle, remaining: CAROUSEL_INTERVAL };
    }
    if (!playing) return;
    const started = performance.now();
    const timer = window.setTimeout(
      () => setSelectedIndex(nextSlide(activeIndex, count)),
      clock.current.remaining,
    );
    return () => {
      window.clearTimeout(timer);
      clock.current.remaining = Math.max(
        0,
        clock.current.remaining - (performance.now() - started),
      );
    };
  }, [playing, activeIndex, count]);

  return {
    activeIndex,
    playing,
    reducedMotion,
    select: setSelectedIndex,
    step: (direction) =>
      setSelectedIndex(nextSlide(activeIndex, count, direction)),
    interactionProps: {
      onPointerEnter: (event) => {
        if (event.pointerType === "mouse" || event.pointerType === "pen")
          setHovered(true);
      },
      onPointerLeave: () => setHovered(false),
      onFocusCapture: (event) =>
        setFocused(event.target.matches(":focus-visible")),
      onBlurCapture: (event) => {
        if (!event.currentTarget.contains(event.relatedTarget))
          setFocused(false);
      },
    },
  };
}
