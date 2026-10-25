import { useEffect, useRef, useState } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { getCategories } from "../../services/eventService";
import ButacaLogo from "../brand/ButacaLogo";

function Navbar({
  currentUser,
  onLogout,
  logoRef,
  headerRef,
  theme,
  themeChanging,
  onToggleTheme,
}) {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [catOpen, setCatOpen] = useState(false);
  const [categories, setCategories] = useState([]);
  const [search, setSearch] = useState("");
  const dropdownRef = useRef(null);
  useEffect(() => {
    getCategories()
      .then(setCategories)
      .catch(() => {});
  }, []);
  useEffect(() => {
    function outside(event) {
      if (!dropdownRef.current?.contains(event.target)) setCatOpen(false);
    }
    function escape(event) {
      if (event.key === "Escape") setCatOpen(false);
    }
    document.addEventListener("mousedown", outside);
    document.addEventListener("keydown", escape);
    return () => {
      document.removeEventListener("mousedown", outside);
      document.removeEventListener("keydown", escape);
    };
  }, []);
  const role = currentUser?.rol;
  const dashboardLabel =
    role === "ADMIN"
      ? "Administración"
      : role === "ORGANIZADOR"
        ? "Mis eventos"
        : "Mis entradas";
  function selectCategory(name) {
    setCatOpen(false);
    navigate(
      name ? `/buscar?categoria=${encodeURIComponent(name)}` : "/buscar",
    );
  }
  return (
    <header className="navbar" ref={headerRef}>
      <div className="container navbar-inner">
        <Link to="/" className="brand" aria-label="Butaca, inicio">
          <span className="brand-motion" ref={logoRef}>
            <ButacaLogo decorative />
          </span>
        </Link>
        <form
          className="nav-search"
          data-intro-reveal
          role="search"
          onSubmit={(event) => {
            event.preventDefault();
            navigate(`/buscar?q=${encodeURIComponent(search.trim())}`);
          }}
        >
          <span aria-hidden="true">⌕</span>
          <input
            aria-label="Buscar eventos"
            type="search"
            placeholder="Evento, artista o lugar…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <button type="submit" aria-label="Buscar en la cartelera">
            ↗
          </button>
        </form>
        <nav
          className="nav-links"
          aria-label="Navegación principal"
          data-intro-reveal
        >
          <NavLink
            to="/buscar"
            className={({ isActive }) =>
              isActive || pathname === "/" ? "active" : undefined
            }
          >
            Cartelera
          </NavLink>
          <div className="category-menu" ref={dropdownRef}>
            <button
              className="text-link"
              type="button"
              aria-expanded={catOpen}
              aria-controls="nav-categories"
              onClick={() => setCatOpen(!catOpen)}
            >
              Categorías <span aria-hidden="true">⌄</span>
            </button>
            {catOpen ? (
              <div className="category-dropdown" id="nav-categories">
                <button type="button" onClick={() => selectCategory("")}>
                  Toda la cartelera <span>↗</span>
                </button>
                {categories.map((category) => (
                  <button
                    type="button"
                    key={category.id}
                    onClick={() => selectCategory(category.nombre)}
                  >
                    {category.nombre}
                    <span>↗</span>
                  </button>
                ))}
              </div>
            ) : null}
          </div>
        </nav>
        <div className="navbar-actions" data-intro-reveal>
          <button
            type="button"
            className="theme-toggle"
            aria-label={
              theme === "dark"
                ? "Cambiar a modo claro"
                : "Cambiar a modo oscuro"
            }
            title={theme === "dark" ? "Modo claro" : "Modo oscuro"}
            onClick={onToggleTheme}
            disabled={themeChanging}
            aria-pressed={theme === "dark"}
          >
            {theme === "dark" ? (
              <svg
                viewBox="0 0 24 24"
                width="20"
                height="20"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.6"
                aria-hidden="true"
              >
                <circle cx="12" cy="12" r="4" />
                <path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5" />
              </svg>
            ) : (
              <svg
                viewBox="0 0 24 24"
                width="20"
                height="20"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.6"
                aria-hidden="true"
              >
                <path d="M20.5 14A8.5 8.5 0 0 1 10 3.5 8.5 8.5 0 1 0 20.5 14Z" />
              </svg>
            )}
          </button>
          {currentUser ? (
            <>
              <Link className="account-link" to="/dashboard">
                <span className="user-avatar" aria-hidden="true">
                  {currentUser.nombre?.[0]}
                </span>
                <span>
                  <strong>{currentUser.nombre}</strong>
                  <small>{dashboardLabel}</small>
                </span>
              </Link>
              <button
                type="button"
                className="text-link logout-link"
                title="Cerrar sesión"
                onClick={onLogout}
              >
                Salir
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-link">
                Ingresar
              </Link>
              <Link to="/registro" className="pill-link">
                Crear cuenta
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
export default Navbar;
