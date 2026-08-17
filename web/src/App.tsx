import { type FormEvent, useCallback, useEffect, useState } from "react";
import "./App.css";
import { crearNota, listarNotas, type Nota } from "./api/cliente";

type HealthStatus =
  | { kind: "loading" }
  | { kind: "ok"; body: string }
  | { kind: "error"; message: string };

function useHealthCheck(): HealthStatus {
  const [status, setStatus] = useState<HealthStatus>({ kind: "loading" });

  useEffect(() => {
    const controller = new AbortController();

    fetch("/api/health", { signal: controller.signal })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`El backend respondió con status ${response.status}`);
        }
        return response.text();
      })
      .then((body) => setStatus({ kind: "ok", body }))
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }
        const message = error instanceof Error ? error.message : "Error desconocido";
        setStatus({ kind: "error", message });
      });

    return () => controller.abort();
  }, []);

  return status;
}

type NotasStatus =
  | { kind: "loading" }
  | { kind: "ok"; notas: Nota[] }
  | { kind: "error"; message: string };

function useNotas() {
  const [status, setStatus] = useState<NotasStatus>({ kind: "loading" });

  const recargar = useCallback((signal?: AbortSignal) => {
    setStatus({ kind: "loading" });
    listarNotas(signal)
      .then((notas) => setStatus({ kind: "ok", notas }))
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }
        const message = error instanceof Error ? error.message : "Error desconocido";
        setStatus({ kind: "error", message });
      });
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    recargar(controller.signal);
    return () => controller.abort();
  }, [recargar]);

  return { status, recargar };
}

function FormularioNota({ onNotaCreada }: { onNotaCreada: () => void }) {
  const [texto, setTexto] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (texto.trim() === "") {
      setError("Escribí un texto antes de guardar.");
      return;
    }

    setEnviando(true);
    setError(null);
    crearNota(texto)
      .then(() => {
        setTexto("");
        onNotaCreada();
      })
      .catch((error: unknown) => {
        const message = error instanceof Error ? error.message : "Error desconocido";
        setError(message);
      })
      .finally(() => setEnviando(false));
  }

  return (
    <form onSubmit={handleSubmit}>
      <label htmlFor="texto-nota">Nueva nota</label>
      <input
        id="texto-nota"
        value={texto}
        disabled={enviando}
        onChange={(event) => setTexto(event.target.value)}
      />
      <button type="submit" disabled={enviando}>
        Guardar
      </button>
      {error && <p className="status-error">{error}</p>}
    </form>
  );
}

function ListaNotas({ notas }: { notas: Nota[] }) {
  if (notas.length === 0) {
    return <p>Todavía no hay notas.</p>;
  }

  return (
    <ul>
      {notas.map((nota) => (
        <li key={nota.id}>{nota.texto}</li>
      ))}
    </ul>
  );
}

function App() {
  const health = useHealthCheck();
  const { status: notas, recargar: recargarNotas } = useNotas();

  return (
    <div className="card">
      <h1>walking-skeleton-spring-react</h1>
      <p>Estado de /api/health:</p>
      {health.kind === "loading" && <p>Consultando el backend…</p>}
      {health.kind === "ok" && <p className="status-ok">{health.body}</p>}
      {health.kind === "error" && (
        <p className="status-error">No se pudo contactar al backend: {health.message}</p>
      )}

      <h2>Notas</h2>
      <FormularioNota onNotaCreada={() => recargarNotas()} />
      {notas.kind === "loading" && <p>Cargando notas…</p>}
      {notas.kind === "ok" && <ListaNotas notas={notas.notas} />}
      {notas.kind === "error" && (
        <p className="status-error">No se pudieron cargar las notas: {notas.message}</p>
      )}
    </div>
  );
}

export default App;
