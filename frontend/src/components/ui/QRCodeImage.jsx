import { useMemo } from "react";
import { generateQRMatrix } from "../../utils/qrCode";

function QRCodeImage({ value, size = 150, className = "" }) {
  const matrix = useMemo(() => {
    try {
      return value ? generateQRMatrix(String(value)) : null;
    } catch {
      return null;
    }
  }, [value]);
  if (!matrix) return <span role="alert">Código QR no disponible</span>;
  const length = matrix.length;
  // Quiet zone de cuatro módulos: parte del SVG, también al cambiar su tamaño.
  const path = matrix
    .flatMap((row, y) =>
      row.flatMap((dark, x) => (dark ? [`M${x},${y}h1v1h-1z`] : [])),
    )
    .join("");
  return (
    <svg
      role="img"
      aria-label="Código QR de entrada"
      width={size}
      height={size}
      viewBox={`-4 -4 ${length + 8} ${length + 8}`}
      className={className}
      shapeRendering="crispEdges"
      style={{ display: "block", background: "white", flexShrink: 0 }}
    >
      <rect x="-4" y="-4" width={length + 8} height={length + 8} fill="white" />
      <path d={path} fill="#000" />
    </svg>
  );
}

export function downloadTicketAsImage({
  ticket,
  eventTitle,
  venue,
  date,
  time,
  buyerName,
}) {
  const code = ticket.codigo_unico_entrada || ticket.codigoUnicoEntrada;
  if (!code) return;
  const matrix = generateQRMatrix(String(code));
  const canvas = document.createElement("canvas");
  canvas.width = 1000;
  canvas.height = 480;
  const ctx = canvas.getContext("2d");
  ctx.fillStyle = "#f5f3ee";
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.strokeStyle = "#d6d8cc";
  ctx.strokeRect(16, 16, 968, 448);
  ctx.fillStyle = "#bb4025";
  ctx.font = "bold 22px sans-serif";
  ctx.fillText("butaca.", 42, 61);
  ctx.fillStyle = "#62645d";
  ctx.font = "13px sans-serif";
  ctx.fillText("ENTRADA NOMINAL / PRESENTAR CON DNI", 42, 90);
  ctx.fillStyle = "#202222";
  ctx.font = "bold 28px sans-serif";
  const title = eventTitle || "Entrada Butaca";
  const words = title.split(/\s+/);
  let line = "",
    titleY = 145;
  for (const word of words) {
    const candidate = `${line} ${word}`.trim();
    if (ctx.measureText(candidate).width > 560 && line) {
      ctx.fillText(line, 42, titleY, 560);
      titleY += 34;
      line = word;
    } else line = candidate;
    if (titleY >= 179) break;
  }
  ctx.fillText(line, 42, titleY, 560);
  ctx.font = "17px sans-serif";
  ctx.fillText(venue || "Recinto a confirmar", 42, 245, 560);
  ctx.fillText(
    `${date || "Fecha a confirmar"}${time ? ` · ${time} h` : ""}`,
    42,
    278,
    560,
  );
  ctx.fillText(buyerName || "Titular de la entrada", 42, 316, 560);
  ctx.fillStyle = "#62645d";
  ctx.font = "13px sans-serif";
  ctx.fillText(
    "Presentá este QR junto con tu DNI físico. Válido para un único ingreso.",
    42,
    392,
    850,
  );
  ctx.font = "12px monospace";
  ctx.fillText(String(code), 42, 430);
  const scale = 7;
  const side = (matrix.length + 8) * scale;
  const x = 1000 - side - 42;
  const y = 100;
  ctx.fillStyle = "white";
  ctx.fillRect(x, y, side, side);
  ctx.fillStyle = "black";
  matrix.forEach((row, rowIndex) =>
    row.forEach((dark, column) => {
      if (dark)
        ctx.fillRect(
          x + (column + 4) * scale,
          y + (rowIndex + 4) * scale,
          scale,
          scale,
        );
    }),
  );
  const link = document.createElement("a");
  const cleanTitle = (eventTitle || "entrada")
    .toLowerCase()
    .replace(/[^a-z0-9]/g, "-")
    .slice(0, 65);
  link.download = `entrada-butaca-${cleanTitle}-${String(code).slice(0, 8)}.png`;
  link.href = canvas.toDataURL("image/png");
  link.click();
}
export default QRCodeImage;
