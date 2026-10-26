import PopcornMark from "./PopcornMark";
import { brandGeometry } from "./brandGeometry";

function ButacaLogo({
  theme = "onDark",
  showTagline = true,
  decorative = false,
}) {
  return (
    <span
      className={`butaca-logo butaca-logo--${theme}`}
      role={decorative ? undefined : "img"}
      aria-label={decorative ? undefined : "Butaca. Más eventos para vos"}
      aria-hidden={decorative || undefined}
    >
      <svg
        className="butaca-wordmark"
        viewBox="0 0 580 190"
        fill="none"
        aria-hidden="true"
      >
        <PopcornMark />
        <g className="brand-letters" data-logo-letters>
          {brandGeometry.letters.map((letter, index) => (
            <path
              d={letter.path}
              key={index}
              data-logo-letter={letter.character}
            />
          ))}
        </g>
      </svg>
      {showTagline ? (
        <span className="butaca-tagline" data-logo-tagline aria-hidden="true">
          {Array.from("MÁS EVENTOS PARA VOS").map((letter, index) => (
            <span key={index}>{letter === " " ? "\u00a0" : letter}</span>
          ))}
        </span>
      ) : null}
    </span>
  );
}
export default ButacaLogo;
