import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import Button from "../components/ui/Button";
import { loginUser } from "../services/authService";

function LoginPage({ currentUser, onLogin }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [formData, setFormData] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  useEffect(() => {
    if (currentUser)
      navigate(currentUser.rol === "CLIENTE" ? "/" : "/dashboard", {
        replace: true,
      });
  }, [currentUser, navigate]);
  function handleChange(event) {
    setFormData({ ...formData, [event.target.name]: event.target.value });
  }
  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setLoading(true);
    try {
      const user = await loginUser(formData);
      onLogin(user);
      navigate(user.rol === "CLIENTE" ? "/" : "/dashboard");
    } catch (err) {
      setError(err.message || "No pudimos iniciar sesión. Revisá tus datos.");
    } finally {
      setLoading(false);
    }
  }
  return (
    <section className="container login-page">
      <div className="login-art">
        <img
          src="https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?auto=format&fit=crop&w=1400&q=85"
          alt="Público frente a un escenario en vivo"
        />
        <div>
          <p className="billboard-category">Butaca / En vivo</p>
          <h1>
            Lo mejor
            <br />
            es estar ahí.
          </h1>
          <p>Tu próxima salida empieza con una entrada.</p>
        </div>
        <span className="login-art-caption">
          Recitales · Teatro · Deportes · Festivales
        </span>
      </div>
      <div className="login-form-wrap">
        <Link className="back-link" to="/buscar">
          ← Volver a la cartelera
        </Link>
        <form className="auth-form-panel login-form" onSubmit={handleSubmit}>
          <p className="section-eyebrow">Tu cuenta Butaca</p>
          <h2>Qué bueno verte.</h2>
          <p className="section-text">
            Ingresá para encontrar tus entradas o gestionar tus eventos.
          </p>
          {location.state?.message ? (
            <p className="success-notice" role="status">
              {location.state.message}
            </p>
          ) : null}
          <div className="form-grid">
            <label>
              Correo electrónico
              <input
                name="email"
                type="email"
                autoComplete="username"
                value={formData.email}
                onChange={handleChange}
                placeholder="tu@email.com"
                required
              />
            </label>
            <label>
              Contraseña
              <span className="password-field">
                <input
                  name="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="Tu contraseña"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={
                    showPassword ? "Ocultar contraseña" : "Mostrar contraseña"
                  }
                >
                  {showPassword ? "Ocultar" : "Ver"}
                </button>
              </span>
            </label>
          </div>
          {error ? (
            <div className="auth-error" role="alert">
              {error}
            </div>
          ) : null}
          <div className="auth-form-actions">
            <Button type="submit" fullWidth disabled={loading}>
              {loading ? "Ingresando…" : "Ingresar a mi cuenta ↗"}
            </Button>
            <p className="auth-switch">
              ¿Primera vez acá? <Link to="/registro">Creá tu cuenta</Link>
            </p>
          </div>
        </form>
        <details className="demo-accounts">
          <summary>Probar la demo con un perfil</summary>
          <p>Completá los datos de prueba y presioná Ingresar.</p>
          <div className="row-actions">
            {[
              {
                label: "Cliente",
                email: "cliente@butaca.com",
                password: "cliente123",
              },
              {
                label: "Organizador",
                email: "org@butaca.com",
                password: "org123",
              },
              {
                label: "Administrador",
                email: "admin@butaca.com",
                password: "admin123",
              },
            ].map((account) => (
              <button
                type="button"
                key={account.email}
                className="btn-copy"
                onClick={() => {
                  setFormData({
                    email: account.email,
                    password: account.password,
                  });
                  setError("");
                }}
              >
                {account.label}
              </button>
            ))}
          </div>
        </details>
      </div>
    </section>
  );
}
export default LoginPage;
