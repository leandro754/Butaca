// Apply the saved appearance before styles paint; no authentication data is read.
try {
  const saved = localStorage.getItem("butaca-theme");
  document.documentElement.dataset.theme =
    saved === "dark" || saved === "light"
      ? saved
      : matchMedia("(prefers-color-scheme: dark)").matches
        ? "dark"
        : "light";
} catch {
  document.documentElement.dataset.theme = "light";
}
