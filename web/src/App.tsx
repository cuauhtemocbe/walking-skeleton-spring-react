import { useEffect, useState } from "react";
import "./App.css";

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

function App() {
  const health = useHealthCheck();

  return (
    <div className="card">
      <h1>walking-skeleton-spring-react</h1>
      <p>Estado de /api/health:</p>
      {health.kind === "loading" && <p>Consultando el backend…</p>}
      {health.kind === "ok" && <p className="status-ok">{health.body}</p>}
      {health.kind === "error" && (
        <p className="status-error">No se pudo contactar al backend: {health.message}</p>
      )}
    </div>
  );
}

export default App;
