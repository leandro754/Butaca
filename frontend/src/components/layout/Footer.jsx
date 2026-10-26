import { Link } from "react-router-dom";
import ButacaLogo from "../brand/ButacaLogo";
function Footer() {
  return (
    <footer className="footer" id="ayuda">
      <div className="container footer-grid">
        <div>
          <Link className="footer-brand" to="/" aria-label="Butaca, inicio">
            <ButacaLogo decorative />
          </Link>
          <p>
            Un lugar para encontrar tu próxima salida.
            <br />
            Recitales, teatro, deportes y festivales.
          </p>
        </div>
        <div>
          <h4>Seguí explorando</h4>
          <Link to="/buscar">Cartelera</Link>
          <Link to="/dashboard">Mi cuenta y entradas</Link>
          <Link to="/registro?modo=organizacion">
            Quiero organizar un evento ↗
          </Link>
        </div>
        <div className="footer-help">
          <h4>Antes de salir</h4>
          <details>
            <summary>¿Dónde encuentro mis entradas?</summary>
            <p>
              Ingresá a tu cuenta y abrí Mis entradas. Podés ver el QR o
              descargarlo para llevarlo en tu teléfono.
            </p>
          </details>
          <details>
            <summary>¿Qué presento en la puerta?</summary>
            <p>
              El código QR de tu entrada y el DNI físico de su titular. Cada
              código permite un solo ingreso.
            </p>
          </details>
          <details>
            <summary>¿Cómo funciona la compra?</summary>
            <p>
              Esta es una demo académica con pagos simulados. El comprobante y
              las entradas quedan disponibles en tu cuenta.
            </p>
          </details>
        </div>
      </div>
      <div className="container footer-bottom">
        <span>Butaca</span>
        <span>Proyecto académico / Entradas nominales</span>
      </div>
    </footer>
  );
}
export default Footer;
