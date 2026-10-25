import { useEffect, useRef, useState } from "react";

export function useThemeCurtain() {
  const [theme, setTheme] = useState(
    () => document.documentElement.dataset.theme || "light",
  );
  const [changing, setChanging] = useState(false);
  const curtainRef = useRef(null);
  const motion = useRef({
    busy: false,
    animations: [],
    timer: null,
    disposed: false,
  });
  useEffect(() => {
    motion.current.disposed = false;
    return () => {
      motion.current.disposed = true;
      clearTimeout(motion.current.timer);
      motion.current.animations.forEach((animation) => animation.cancel());
    };
  }, []);
  function apply(next) {
    document.documentElement.dataset.theme = next;
    try {
      localStorage.setItem("butaca-theme", next);
    } catch {
      /* Storage can be disabled. */
    }
    setTheme(next);
  }
  async function toggleTheme() {
    if (motion.current.busy) return;
    const next = theme === "dark" ? "light" : "dark";
    const overlay = curtainRef.current;
    if (
      !overlay?.animate ||
      matchMedia("(prefers-reduced-motion: reduce)").matches
    ) {
      apply(next);
      return;
    }
    motion.current.busy = true;
    setChanging(true);
    overlay.dataset.active = "true";
    const panels = [...overlay.children];
    let finished = false;
    function finish() {
      if (finished) return;
      finished = true;
      clearTimeout(motion.current.timer);
      if (motion.current.disposed) return;
      apply(next);
      overlay.dataset.active = "false";
      motion.current.animations.forEach((animation) => animation.cancel());
      motion.current.animations = [];
      motion.current.busy = false;
      setChanging(false);
    }
    motion.current.timer = setTimeout(finish, 1900);
    function animate(closing) {
      return panels.map((panel, index) => {
        const outside = `translateX(${index === 0 ? "-102%" : "102%"})`;
        const animation = panel.animate(
          closing
            ? [
                { transform: outside },
                {
                  transform: `translateX(${index === 0 ? "1%" : "-1%"}) scaleX(1.015)`,
                  offset: 0.88,
                },
                { transform: "translateX(0) scaleX(1)" },
              ]
            : [
                { transform: "translateX(0) scaleX(1)" },
                {
                  transform: `translateX(${index === 0 ? "-76%" : "76%"}) scaleX(.96)`,
                  offset: 0.72,
                },
                { transform: outside },
              ],
          {
            duration: closing ? 440 : 640,
            delay: closing ? index * 24 : 70 + index * 24,
            easing: "cubic-bezier(.55,0,.2,1)",
            fill: "forwards",
          },
        );
        motion.current.animations.push(animation);
        return animation.finished;
      });
    }
    try {
      await Promise.all(animate(true));
      if (finished) return;
      apply(next);
      await new Promise(requestAnimationFrame);
      if (finished) return;
      await Promise.all(animate(false));
    } catch {
      /* A cancelled animation still leaves a usable selected appearance. */
    } finally {
      finish();
    }
  }
  return { theme, changing, toggleTheme, curtainRef };
}
