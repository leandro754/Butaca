const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

export async function request(path, options = {}) {
  const { headers: optionHeaders, ...rest } = options;
  const isFormData = typeof FormData !== "undefined" && rest.body instanceof FormData;
  const headers = {
    ...(isFormData ? {} : { "Content-Type": "application/json" }),
    ...(optionHeaders || {})
  };
  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...rest, headers });
  } catch (error) {
    throw new Error("No pudimos conectar con Butaca. Intenta de nuevo en unos instantes.", { cause: error });
  }

  if (!response.ok) {
    const message = await readErrorMessage(response);
    throw new Error(message || "Ocurrio un error al conectar con la API");
  }

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

async function readErrorMessage(response) {
  const text = await response.text();

  if (!text) {
    return "";
  }

  try {
    const parsed = JSON.parse(text);
    if (parsed.error && parsed.fields) {
      return `${parsed.error}: ${Object.values(parsed.fields).join(", ")}`;
    }
    return parsed.error || parsed.message || text;
  } catch {
    return text;
  }
}
