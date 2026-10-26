"""Development-only: generate static licensed fonts for Java/PDF from Manrope."""
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools import subset

root = Path(__file__).resolve().parent.parent
destination = root / "src/main/resources/mail"
destination.mkdir(parents=True, exist_ok=True)
for weight, name in [(400, "regular"), (700, "bold")]:
    font = TTFont(root / "target/Manrope-Variable.ttf")
    font = instantiateVariableFont(font, {"wght": weight}, inplace=True)
    options = subset.Options()
    tool = subset.Subsetter(options=options)
    tool.populate(unicodes=list(range(0x20, 0x250)) + list(range(0x2010, 0x2030)))
    tool.subset(font)
    font.save(destination / f"manrope-{name}.ttf")
(destination / "Manrope-OFL.txt").write_bytes((root / "frontend/public/fonts/Manrope-OFL.txt").read_bytes())
print("Fuentes estáticas Manrope exportadas con licencia OFL.")
