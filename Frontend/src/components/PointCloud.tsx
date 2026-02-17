import { useRef } from "react";
import type { Candidate } from "../api/backend";

// Renderiza um conjunto de pontos aleatórios estáveis para cada candidato.
export default function PointCloud({ candidates }: { candidates: Candidate[] }) {
  const positionsRef = useRef<Map<string, { x: number; y: number }>>(new Map());

  // Atribui uma posição aleatória a cada candidato e remove os que não existem mais
  candidates.forEach((c) => {
    if (!positionsRef.current.has(c.uri)) {
      positionsRef.current.set(c.uri, { x: Math.random(), y: Math.random() });
    }
  });
  for (const uri of Array.from(positionsRef.current.keys())) {
    if (!candidates.find((c) => c.uri === uri)) {
      positionsRef.current.delete(uri);
    }
  }

  return (
    <div style={{ position: "relative", width: "100%", height: 180 }}>
      {candidates.map((c) => {
        const pos = positionsRef.current.get(c.uri)!;
        return (
          <div
            key={c.uri}
            title={c.label}
            style={{
              position: "absolute",
              left: `${pos.x * 100}%`,
              top: `${pos.y * 100}%`,
              width: 6,
              height: 6,
              borderRadius: "50%",
              background: "#e3352b",
            }}
          />
        );
      })}
    </div>
  );
}
