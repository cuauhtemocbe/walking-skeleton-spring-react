import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

function stubFetchResolvedWith(response: Partial<Response>) {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response as Response));
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("App", () => {
  it("muestra el estado de carga y luego el resultado cuando el backend responde", async () => {
    stubFetchResolvedWith({ ok: true, text: () => Promise.resolve("OK") });

    render(<App />);

    expect(screen.getByText("Consultando el backend…")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText("OK")).toBeInTheDocument());
  });

  it("muestra un error legible cuando el backend responde con error", async () => {
    stubFetchResolvedWith({ ok: false, status: 503, text: () => Promise.resolve("") });

    render(<App />);

    await waitFor(() =>
      expect(screen.getByText(/No se pudo contactar al backend: .*status 503/)).toBeInTheDocument(),
    );
  });

  it("muestra un error legible cuando la petición falla a nivel de red", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("falla de red")));

    render(<App />);

    await waitFor(() =>
      expect(screen.getByText("No se pudo contactar al backend: falla de red")).toBeInTheDocument(),
    );
  });
});
