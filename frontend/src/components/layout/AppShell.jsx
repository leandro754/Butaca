import Navbar from "./Navbar";
import Footer from "./Footer";
import { useCallback, useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import IntroAnimation from "../brand/IntroAnimation";
import { useThemeCurtain } from "../../hooks/useThemeCurtain";

function AppShell({ children, currentUser, onLogout }) {
  const logoRef = useRef(null);
  const headerRef = useRef(null);
  const { theme, changing, toggleTheme, curtainRef } = useThemeCurtain();
  const [introActive, setIntroActive] = useState(
    () => !window.matchMedia("(prefers-reduced-motion: reduce)").matches,
  );
  const finishIntro = useCallback(() => setIntroActive(false), []);
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "instant" });
  }, [pathname]);
  return (
    <div className="app-shell" data-intro-active={introActive}>
      <Navbar
        currentUser={currentUser}
        onLogout={onLogout}
        logoRef={logoRef}
        headerRef={headerRef}
        theme={theme}
        themeChanging={changing || introActive}
        onToggleTheme={toggleTheme}
      />
      <main>{children}</main>
      <Footer />
      <div
        className="theme-curtain"
        ref={curtainRef}
        aria-hidden="true"
        data-active="false"
      >
        <div className="theme-curtain-panel theme-curtain-left" />
        <div className="theme-curtain-panel theme-curtain-right" />
      </div>
      <IntroAnimation
        active={introActive}
        logoRef={logoRef}
        headerRef={headerRef}
        onComplete={finishIntro}
        theme={theme}
      />
    </div>
  );
}

export default AppShell;
