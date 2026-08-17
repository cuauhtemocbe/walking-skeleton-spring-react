import type { components } from "./schema.d.ts";

export type Nota = components["schemas"]["NotaResponse"];

async function manejarRespuesta<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new Error(`El backend respondió con status ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export function listarNotas(signal?: AbortSignal): Promise<Nota[]> {
  return fetch("/api/notas", { signal }).then((response) => manejarRespuesta<Nota[]>(response));
}

export function crearNota(texto: string, signal?: AbortSignal): Promise<Nota> {
  const cuerpo: components["schemas"]["CrearNota"] = { texto };

  return fetch("/api/notas", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(cuerpo),
    signal,
  }).then((response) => manejarRespuesta<Nota>(response));
}
