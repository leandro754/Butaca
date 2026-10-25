/**
 * Generador liviano y autónomo de Códigos QR (versiones 1 a 6 con corrección de error M)
 * No requiere librerías externas y genera matrices de módulos booleanos para Canvas / SVG.
 */

// Tablas Galois Field GF(256)
const GF_EXP = new Uint8Array(512);
const GF_LOG = new Uint8Array(256);
(function initGalois() {
  let x = 1;
  for (let i = 0; i < 255; i++) {
    GF_EXP[i] = x;
    GF_EXP[i + 255] = x;
    GF_LOG[x] = i;
    x <<= 1;
    if (x & 0x100) x ^= 0x11d;
  }
})();

function gfMul(x, y) {
  if (x === 0 || y === 0) return 0;
  return GF_EXP[GF_LOG[x] + GF_LOG[y]];
}

function rsGeneratorPoly(degree) {
  let poly = [1];
  for (let i = 0; i < degree; i++) {
    const next = Array(poly.length + 1).fill(0);
    const factor = GF_EXP[i];
    for (let j = 0; j < poly.length; j++) {
      next[j] ^= poly[j];
      next[j + 1] ^= gfMul(poly[j], factor);
    }
    poly = next.slice(0, poly.length + 1);
  }
  return poly;
}

function rsEncode(data, ecCount) {
  const gen = rsGeneratorPoly(ecCount);
  const res = new Uint8Array(data.length + ecCount);
  res.set(data);
  for (let i = 0; i < data.length; i++) {
    const coef = res[i];
    if (coef !== 0) {
      for (let j = 0; j < gen.length; j++) {
        res[i + j] ^= gfMul(gen[j], coef);
      }
    }
  }
  return res.slice(data.length);
}

// Capacidades de bytes para modo M (Medium Error Correction)
const VERSION_CAPACITY_M = [0, 14, 26, 42, 62, 84, 106];
const VERSION_DATA_BYTES = [0, 16, 28, 44, 64, 86, 108];
const VERSION_EC_PER_BLOCK = [0, 10, 16, 26, 18, 24, 16];
const VERSION_BLOCKS = [0, 1, 1, 1, 2, 2, 4];

export function generateQRMatrix(text) {
  const utf8 = new TextEncoder().encode(text);
  let version = 1;
  while (
    version < VERSION_CAPACITY_M.length &&
    utf8.length > VERSION_CAPACITY_M[version]
  ) {
    version++;
  }
  if (version >= VERSION_CAPACITY_M.length)
    throw new RangeError(
      "El contenido supera los 106 bytes admitidos por el QR local.",
    );

  const moduleCount = version * 4 + 17;
  const matrix = Array.from({ length: moduleCount }, () =>
    Array(moduleCount).fill(null),
  );
  const isFunction = Array.from({ length: moduleCount }, () =>
    Array(moduleCount).fill(false),
  );

  // 1. Finder patterns
  function addFinderPattern(r0, c0) {
    for (let r = -1; r <= 7; r++) {
      for (let c = -1; c <= 7; c++) {
        const row = r0 + r;
        const col = c0 + c;
        if (row >= 0 && row < moduleCount && col >= 0 && col < moduleCount) {
          isFunction[row][col] = true;
          if (r >= 0 && r <= 6 && c >= 0 && c <= 6) {
            matrix[row][col] =
              r === 0 ||
              r === 6 ||
              c === 0 ||
              c === 6 ||
              (r >= 2 && r <= 4 && c >= 2 && c <= 4);
          } else {
            matrix[row][col] = false;
          }
        }
      }
    }
  }
  addFinderPattern(0, 0);
  addFinderPattern(0, moduleCount - 7);
  addFinderPattern(moduleCount - 7, 0);

  // 2. Alignment patterns (version >= 2)
  if (version >= 2) {
    const alignPositions = [
      [],
      [],
      [6, 18],
      [6, 22],
      [6, 26],
      [6, 30],
      [6, 34],
    ][version];

    for (let r of alignPositions) {
      for (let c of alignPositions) {
        if (isFunction[r][c]) continue;
        for (let dr = -2; dr <= 2; dr++) {
          for (let dc = -2; dc <= 2; dc++) {
            const row = r + dr;
            const col = c + dc;
            isFunction[row][col] = true;
            matrix[row][col] =
              Math.abs(dr) === 2 ||
              Math.abs(dc) === 2 ||
              (dr === 0 && dc === 0);
          }
        }
      }
    }
  }

  // 3. Timing patterns
  for (let i = 8; i < moduleCount - 8; i++) {
    if (!isFunction[6][i]) {
      isFunction[6][i] = true;
      matrix[6][i] = i % 2 === 0;
    }
    if (!isFunction[i][6]) {
      isFunction[i][6] = true;
      matrix[i][6] = i % 2 === 0;
    }
  }

  // Dark module
  isFunction[4 * version + 9][8] = true;
  matrix[4 * version + 9][8] = true;

  // Reserve format info
  for (let i = 0; i < 9; i++) {
    if (i !== 6) {
      isFunction[8][i] = true;
      isFunction[i][8] = true;
    }
  }
  for (let i = 0; i < 8; i++) {
    isFunction[8][moduleCount - 1 - i] = true;
    isFunction[moduleCount - 1 - i][8] = true;
  }

  // 4. Data encoding (Byte mode: 0100)
  const bitBuffer = [];
  function pushBits(val, len) {
    for (let i = len - 1; i >= 0; i--) {
      bitBuffer.push((val >> i) & 1);
    }
  }
  pushBits(0b0100, 4); // Byte mode indicator
  const countBits = version <= 9 ? 8 : 16;
  pushBits(utf8.length, countBits);
  for (let byte of utf8) {
    pushBits(byte, 8);
  }

  const dataCapBytes = VERSION_DATA_BYTES[version];
  const maxBits = dataCapBytes * 8;

  // Terminator
  for (let i = 0; i < 4 && bitBuffer.length < maxBits; i++) {
    bitBuffer.push(0);
  }
  // Byte alignment
  while (bitBuffer.length % 8 !== 0 && bitBuffer.length < maxBits) {
    bitBuffer.push(0);
  }
  // Pad bytes
  const padBytes = [0xec, 0x11];
  let padIdx = 0;
  while (bitBuffer.length < maxBits) {
    pushBits(padBytes[padIdx % 2], 8);
    padIdx++;
  }

  const rawBytes = new Uint8Array(dataCapBytes);
  for (let i = 0; i < dataCapBytes; i++) {
    let byteVal = 0;
    for (let b = 0; b < 8; b++) {
      byteVal = (byteVal << 1) | bitBuffer[i * 8 + b];
    }
    rawBytes[i] = byteVal;
  }

  // Error correction
  const blockCount = VERSION_BLOCKS[version];
  const blockSize = dataCapBytes / blockCount;
  const blocks = Array.from({ length: blockCount }, (_, index) =>
    rawBytes.slice(index * blockSize, (index + 1) * blockSize),
  );
  const correction = blocks.map((block) =>
    rsEncode(block, VERSION_EC_PER_BLOCK[version]),
  );

  // Combine data + EC
  const finalBits = [];
  function appendCodeword(byte) {
    for (let bit = 7; bit >= 0; bit--) finalBits.push((byte >>> bit) & 1);
  }
  for (let column = 0; column < blockSize; column++) {
    for (const block of blocks) appendCodeword(block[column]);
  }
  for (let column = 0; column < VERSION_EC_PER_BLOCK[version]; column++) {
    for (const block of correction) appendCodeword(block[column]);
  }

  // 5. Place data bits
  let bitIdx = 0;
  let upwards = true;
  for (let right = moduleCount - 1; right > 0; right -= 2) {
    if (right === 6) right--; // Skip vertical timing line
    const rows = upwards
      ? Array.from({ length: moduleCount }, (_, idx) => moduleCount - 1 - idx)
      : Array.from({ length: moduleCount }, (_, idx) => idx);

    for (let r of rows) {
      for (let c of [right, right - 1]) {
        if (!isFunction[r][c]) {
          const bit = bitIdx < finalBits.length ? finalBits[bitIdx++] : 0;
          // Apply standard Mask 0: (row + col) % 2 == 0
          const mask = (r + c) % 2 === 0;
          matrix[r][c] = (bit ^ (mask ? 1 : 0)) === 1;
        }
      }
    }
    upwards = !upwards;
  }

  // Format bits for Level M (00) and Mask 0 (000) -> Format string 101010000010010
  const formatBits = 0x5412; // M, máscara 0, BCH y máscara de formato.
  const formatBit = (index) => ((formatBits >>> index) & 1) === 1;
  for (let i = 0; i <= 5; i++) matrix[i][8] = formatBit(i);
  matrix[7][8] = formatBit(6);
  matrix[8][8] = formatBit(7);
  matrix[8][7] = formatBit(8);
  for (let i = 9; i < 15; i++) matrix[8][14 - i] = formatBit(i);
  for (let i = 0; i < 8; i++) matrix[8][moduleCount - 1 - i] = formatBit(i);
  for (let i = 8; i < 15; i++) matrix[moduleCount - 15 + i][8] = formatBit(i);
  matrix[moduleCount - 8][8] = true;

  return matrix;
}
