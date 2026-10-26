export function introTiming(compact = false) {
  const factor = compact ? 0.74 : 1;
  const at = (milliseconds) => Math.round(milliseconds * factor);
  return {
    duration: at(3120),
    bucketStart: at(200),
    bucketDuration: at(420),
    popcornStart: at(520),
    popcornStagger: at(72),
    popcornDuration: at(480),
    lettersStart: at(1100),
    lettersStagger: at(80),
    lettersDuration: at(360),
    pageStart: at(2050),
    pageDuration: at(800),
    taglineStart: at(2700),
    taglineDuration: at(420),
    at,
  };
}

export function logoFlightFrames(rect, mark, viewport) {
  const width = Math.min(viewport.width < 768 ? 350 : 560, viewport.width - 48);
  const scale = Math.min(
    width / rect.width,
    (viewport.height * 0.56) / rect.height,
  );
  const markCenterX = mark.left - rect.left + mark.width / 2;
  const markCenterY = mark.top - rect.top + mark.height / 2;
  const start = `translate(${viewport.width / 2 - rect.left - markCenterX * scale}px, ${viewport.height / 2 - rect.top - markCenterY * scale}px) scale(${scale})`;
  const center = `translate(${(viewport.width - rect.width * scale) / 2 - rect.left}px, ${(viewport.height - rect.height * scale) / 2 - rect.top}px) scale(${scale})`;
  return [
    { transform: start, offset: 0 },
    { transform: start, offset: 0.34, easing: "cubic-bezier(.22,1,.36,1)" },
    { transform: center, offset: 0.54 },
    { transform: center, offset: 0.56, easing: "cubic-bezier(.22,1,.36,1)" },
    { transform: "translate(0px, 0px) scale(1)", offset: 0.735 },
    { transform: "translate(0px, 0px) scale(1)", offset: 1 },
  ];
}
export function introPalette(theme = "light") {
  return theme === "dark"
    ? {
        backdrop: "#111115",
        letters: "#FFF9F0",
        header: "#17191B",
      }
    : {
        backdrop: "#FFFFFF",
        letters: "#17191B",
        header: "#17191B",
      };
}
