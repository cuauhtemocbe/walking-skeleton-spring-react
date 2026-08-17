import { afterEach, describe, expect, it, vi } from "vitest";
import { crearNota, listarNotas } from "./cliente";

function stubFetchResolvedWith(response: Partial<Response>) {
  const fetchMock = vi.fn().mockResolvedValue(response as Response);
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("listarNotas", () => {
  it("devuelve las notas cuando el backend responde 200", async () => {
    const notas = [{ id: "1", texto: "hola", creadaEn: "2026-01-01T00:00:00Z" }];
    stubFetchResolvedWith({ ok: true, json: () => Promise.resolve(notas) });

    await expect(listarNotas()).resolves.toEqual(notas);
  });

  it("rechaza con un mensaje legible cuando el backend responde con error", async () => {
    stubFetchResolvedWith({ ok: false, status: 500 });

    await expect(listarNotas()).rejects.toThrow("status 500");
  });

  it("propaga el error cuando la petición falla a nivel de red", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("falla de red")));

    await expect(listarNotas()).rejects.toThrow("falla de red");
  });
});

describe("crearNota", () => {
  it("hace POST con el texto y devuelve la nota creada", async () => {
    const nota = { id: "2", texto: "nueva", creadaEn: "2026-01-01T00:00:00Z" };
    const fetchMock = stubFetchResolvedWith({ ok: true, json: () => Promise.resolve(nota) });

    await expect(crearNota("nueva")).resolves.toEqual(nota);

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/notas",
      expect.objectContaining({
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ texto: "nueva" }),
      }),
    );
  });

  it("rechaza con un mensaje legible cuando el backend responde 400", async () => {
    stubFetchResolvedWith({ ok: false, status: 400 });

    await expect(crearNota("")).rejects.toThrow("status 400");
  });
});
