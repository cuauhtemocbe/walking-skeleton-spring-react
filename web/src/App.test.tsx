import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

const HEALTH_OK: Partial<Response> = { ok: true, text: () => Promise.resolve("OK") };
const NOTAS_VACIAS: Partial<Response> = { ok: true, json: () => Promise.resolve([]) };

function stubFetch(overrides: {
  health?: Partial<Response> | Error;
  notas?: Partial<Response> | Error | (() => Partial<Response>);
  crearNota?: Partial<Response> | Error;
}) {
  const fetchMock = vi.fn((url: RequestInfo | URL, init?: RequestInit) => {
    const path = String(url);

    if (path === "/api/health") {
      const health = overrides.health ?? HEALTH_OK;
      return health instanceof Error ? Promise.reject(health) : Promise.resolve(health as Response);
    }

    if (path === "/api/notas" && init?.method === "POST") {
      if (!overrides.crearNota) {
        throw new Error("fetch mock: falta stub de crearNota");
      }
      return overrides.crearNota instanceof Error
        ? Promise.reject(overrides.crearNota)
        : Promise.resolve(overrides.crearNota as Response);
    }

    if (path === "/api/notas") {
      const notas =
        typeof overrides.notas === "function"
          ? overrides.notas()
          : (overrides.notas ?? NOTAS_VACIAS);
      return notas instanceof Error ? Promise.reject(notas) : Promise.resolve(notas as Response);
    }

    throw new Error(`fetch mock: ruta no manejada ${init?.method ?? "GET"} ${path}`);
  });

  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("limpieza al desmontar", () => {
  it("no actualiza el estado si el componente se desmonta antes de que el backend responda", async () => {
    const errorConsola = vi.spyOn(console, "error").mockImplementation(() => {});
    const fetchMock = vi.fn((_url: RequestInfo | URL, init?: RequestInit) => {
      return new Promise<Response>((_resolve, reject) => {
        init?.signal?.addEventListener("abort", () => {
          reject(new DOMException("Aborted", "AbortError"));
        });
      });
    });
    vi.stubGlobal("fetch", fetchMock);

    const { unmount } = render(<App />);
    unmount();

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    expect(errorConsola).not.toHaveBeenCalled();
  });
});

describe("estado del backend", () => {
  it("muestra el estado de carga y luego el resultado cuando el backend responde", async () => {
    stubFetch({});

    render(<App />);

    expect(screen.getByText("Consultando el backend…")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText("OK")).toBeInTheDocument());
  });

  it("muestra un error legible cuando el backend responde con error", async () => {
    stubFetch({ health: { ok: false, status: 503, text: () => Promise.resolve("") } });

    render(<App />);

    await waitFor(() =>
      expect(screen.getByText(/No se pudo contactar al backend: .*status 503/)).toBeInTheDocument(),
    );
  });

  it("muestra un error legible cuando la petición falla a nivel de red", async () => {
    stubFetch({ health: new Error("falla de red") });

    render(<App />);

    await waitFor(() =>
      expect(screen.getByText("No se pudo contactar al backend: falla de red")).toBeInTheDocument(),
    );
  });
});

describe("lista de notas", () => {
  it("muestra un estado vacío legible cuando no hay notas", async () => {
    stubFetch({});

    render(<App />);

    await waitFor(() => expect(screen.getByText("Todavía no hay notas.")).toBeInTheDocument());
  });

  it("muestra las notas existentes", async () => {
    const notas = [{ id: "1", texto: "hola", creadaEn: "2026-01-01T00:00:00Z" }];
    stubFetch({ notas: { ok: true, json: () => Promise.resolve(notas) } });

    render(<App />);

    await waitFor(() => expect(screen.getByText("hola")).toBeInTheDocument());
  });

  it("muestra varias notas existentes en el mismo orden", async () => {
    const notas = [
      { id: "1", texto: "primera", creadaEn: "2026-01-01T00:00:00Z" },
      { id: "2", texto: "segunda", creadaEn: "2026-01-02T00:00:00Z" },
      { id: "3", texto: "tercera", creadaEn: "2026-01-03T00:00:00Z" },
    ];
    stubFetch({ notas: { ok: true, json: () => Promise.resolve(notas) } });

    render(<App />);

    await waitFor(() => expect(screen.getByText("tercera")).toBeInTheDocument());
    const items = screen.getAllByRole("listitem");
    expect(items.map((item) => item.textContent)).toEqual(["primera", "segunda", "tercera"]);
  });

  it("muestra un error legible cuando no se pueden cargar las notas", async () => {
    stubFetch({ notas: { ok: false, status: 500 } });

    render(<App />);

    await waitFor(() =>
      expect(screen.getByText(/No se pudieron cargar las notas: .*status 500/)).toBeInTheDocument(),
    );
  });
});

describe("formulario de nota", () => {
  it("crea una nota y la muestra en la lista tras recargar", async () => {
    const nota = { id: "2", texto: "hola desde el test", creadaEn: "2026-01-01T00:00:00Z" };
    let notasListadas: (typeof nota)[] = [];

    stubFetch({
      notas: () => ({ ok: true, json: () => Promise.resolve(notasListadas) }),
      crearNota: {
        ok: true,
        json: () => {
          notasListadas = [nota];
          return Promise.resolve(nota);
        },
      },
    });

    render(<App />);

    await waitFor(() => expect(screen.getByText("Todavía no hay notas.")).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText("Nueva nota"), {
      target: { value: "hola desde el test" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Guardar" }));

    await waitFor(() => expect(screen.getByText("hola desde el test")).toBeInTheDocument());
    expect(screen.getByLabelText("Nueva nota")).toHaveValue("");
  });

  it("no envía el formulario con un texto vacío", async () => {
    stubFetch({});

    render(<App />);
    await waitFor(() => expect(screen.getByText("Todavía no hay notas.")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: "Guardar" }));

    expect(screen.getByText("Escribí un texto antes de guardar.")).toBeInTheDocument();
  });

  it("muestra un error legible cuando el backend rechaza la nota", async () => {
    stubFetch({ crearNota: { ok: false, status: 400 } });

    render(<App />);
    await waitFor(() => expect(screen.getByText("Todavía no hay notas.")).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText("Nueva nota"), {
      target: { value: "texto cualquiera" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Guardar" }));

    await waitFor(() => expect(screen.getByText(/status 400/)).toBeInTheDocument());
  });
});
