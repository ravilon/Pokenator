import { useEffect, useMemo, useRef, useState } from "react";
import { fetchPokemon, toPokeApiSlug, type PokeApiPokemon } from "../api/pokeapi";

export type Candidate = { uri: string; label: string };

type HoverState = { c: Candidate } | null;

function hashToUnit(str: string) {
  let h = 2166136261;
  for (let i = 0; i < str.length; i++) {
    h ^= str.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return ((h >>> 0) % 100000) / 100000;
}

function clamp(n: number, min: number, max: number) {
  return Math.max(min, Math.min(max, n));
}

function extractSlugFromUri(uri: string, fallbackLabel: string) {
  const m = uri.match(/\/([^\/#]+)(?:[#\/]?)$/);
  const last = m?.[1];
  if (last && last.length > 1) return last;
  return toPokeApiSlug(fallbackLabel);
}

type Placed = Candidate & { x: number; y: number };

export default function PointCloud({
  candidates,
  maxLabels = 90,
}: {
  candidates: Candidate[];
  maxLabels?: number;
}) {
  const safe = Array.isArray(candidates) ? candidates : [];

  // viewport (janela) e "mundo" (conteúdo maior com scroll)
  const viewportRef = useRef<HTMLDivElement | null>(null);
  const worldRef = useRef<HTMLDivElement | null>(null);

  const [viewportW, setViewportW] = useState(0);
  const [viewportH, setViewportH] = useState(260);

  // zoom e pan (pan via translate do "mundo")
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });

  const [hover, setHover] = useState<HoverState>(null);
  const [hoverPoke, setHoverPoke] = useState<PokeApiPokemon | null>(null);
  const cacheRef = useRef<Map<string, PokeApiPokemon | null>>(new Map());
  const hoverReqId = useRef(0);

  // Drag para pan
  const dragRef = useRef<{ down: boolean; startX: number; startY: number; panX: number; panY: number }>({
    down: false,
    startX: 0,
    startY: 0,
    panX: 0,
    panY: 0,
  });

  // medir viewport
  useEffect(() => {
    if (!viewportRef.current) return;
    const el = viewportRef.current;

    const ro = new ResizeObserver(() => {
      const r = el.getBoundingClientRect();
      setViewportW(Math.floor(r.width));
      setViewportH(260);
    });
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const { points, extraCount } = useMemo(() => {
    const list = safe.filter((c) => c?.uri && c?.label).slice();
    list.sort((a, b) => hashToUnit(a.uri) - hashToUnit(b.uri));

    const labeled = list.slice(0, maxLabels);
    const extra = Math.max(0, list.length - labeled.length);

    // posições determinísticas (mundo em coordenadas 0..1)
    const placed: Placed[] = labeled.map((c) => {
      const x = clamp(hashToUnit(c.uri + "|x"), 0.03, 0.97);
      const y = clamp(0.10 + hashToUnit(c.uri + "|y") * 0.80, 0.08, 0.92);
      return { ...c, x, y };
    });

    return { points: placed, extraCount: extra };
  }, [safe, maxLabels]);

  async function loadHoverPokemon(c: Candidate) {
    const slug = extractSlugFromUri(c.uri, c.label);
    const key = slug.toLowerCase();

    if (cacheRef.current.has(key)) {
      setHoverPoke(cacheRef.current.get(key) ?? null);
      return;
    }

    const myId = ++hoverReqId.current;
    try {
      const p = await fetchPokemon(slug);
      if (hoverReqId.current !== myId) return;
      cacheRef.current.set(key, p);
      setHoverPoke(p);
    } catch {
      if (hoverReqId.current !== myId) return;
      cacheRef.current.set(key, null);
      setHoverPoke(null);
    }
  }

  const sprite = useMemo(() => {
    return (
      hoverPoke?.sprites.other?.["official-artwork"]?.front_default ??
      hoverPoke?.sprites.front_default ??
      null
    );
  }, [hoverPoke]);

  // Wheel zoom (ctrl+wheel opcional, aqui é wheel normal)
  function onWheel(e: React.WheelEvent) {
    e.preventDefault();

    const delta = -e.deltaY; // wheel up => zoom in
    const factor = delta > 0 ? 1.08 : 1 / 1.08;

    // zoom ancorado no centro da viewport (mais previsível e “bonito”)
    const next = clamp(zoom * factor, 0.75, 2.8);
    setZoom(next);
  }

  // Pan drag
  function onMouseDown(e: React.MouseEvent) {
    if (e.button !== 0) return;
    dragRef.current.down = true;
    dragRef.current.startX = e.clientX;
    dragRef.current.startY = e.clientY;
    dragRef.current.panX = pan.x;
    dragRef.current.panY = pan.y;
  }
  function onMouseMove(e: React.MouseEvent) {
    if (!dragRef.current.down) return;
    const dx = e.clientX - dragRef.current.startX;
    const dy = e.clientY - dragRef.current.startY;
    setPan({ x: dragRef.current.panX + dx, y: dragRef.current.panY + dy });
  }
  function onMouseUp() {
    dragRef.current.down = false;
  }

  // dimensões do “mundo” (conteúdo maior pra permitir scroll + pan + zoom)
  const worldW = Math.max(900, viewportW * 1.4);
  const worldH = 520;

  // helper para posicionar item dentro do mundo (com padding)
  function toWorldPx(p: Placed) {
    const pad = 26;
    const x = pad + p.x * (worldW - pad * 2);
    const y = pad + p.y * (worldH - pad * 2);
    return { x, y };
  }

  // UI: zoom buttons
  function zoomIn() {
    setZoom((z) => clamp(z * 1.15, 0.75, 2.8));
  }
  function zoomOut() {
    setZoom((z) => clamp(z / 1.15, 0.75, 2.8));
  }
  function resetView() {
    setZoom(1);
    setPan({ x: 0, y: 0 });
  }

  // Tooltip centralizado: sempre no centro do viewport
  const tooltipLeft = viewportW ? viewportW / 2 : 320;
  const tooltipTop = 18; // topo com margem, “paira” acima do mundo

  return (
    <div style={{ position: "relative" }}>
      {/* Toolbar */}
      <div
        style={{
          position: "absolute",
          right: 10,
          top: 10,
          zIndex: 30,
          display: "flex",
          gap: 8,
          alignItems: "center",
          fontFamily: "var(--font-pixel)",
          fontSize: 12,
        }}
      >
        {extraCount > 0 && (
          <span style={{ opacity: 0.9 }} title="Showing labels for a subset to keep it readable.">
            +{extraCount} more…
          </span>
        )}

        <button
          type="button"
          onClick={zoomOut}
          style={btnStyle()}
          title="Zoom out"
        >
          −
        </button>
        <button
          type="button"
          onClick={zoomIn}
          style={btnStyle()}
          title="Zoom in"
        >
          +
        </button>
        <button
          type="button"
          onClick={resetView}
          style={btnStyle()}
          title="Reset view"
        >
          reset
        </button>
      </div>

      {/* Viewport (com scroll) */}
      <div
        ref={viewportRef}
        onWheel={onWheel}
        onMouseDown={onMouseDown}
        onMouseMove={onMouseMove}
        onMouseUp={onMouseUp}
        onMouseLeave={onMouseUp}
        style={{
          position: "relative",
          width: "100%",
          height: viewportH,
          border: "2px dashed rgba(0,0,0,0.35)",
          borderRadius: 14,
          background:
            "radial-gradient(circle at 10% 15%, rgba(255,255,255,0.18), rgba(255,255,255,0) 45%), rgba(0,0,0,0.06)",
          overflow: "auto", // scroll!
          cursor: dragRef.current.down ? "grabbing" : "grab",
        }}
        title="Mouse wheel to zoom • Drag to pan • Scroll if needed"
      >
        {/* Mundo (conteúdo maior). Aplicamos pan+zoom via transform */}
        <div
          ref={worldRef}
          style={{
            position: "relative",
            width: worldW,
            height: worldH,
            transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})`,
            transformOrigin: "0 0",
          }}
        >
          {/* Dots de densidade (pra nuvem parecer viva) */}
          <div style={{ position: "absolute", inset: 0, pointerEvents: "none" }}>
            {safe.slice(0, 220).map((c) => {
              const x = hashToUnit(c.uri + "|dx");
              const y = hashToUnit(c.uri + "|dy");
              return (
                <span
                  key={c.uri + "|dot"}
                  style={{
                    position: "absolute",
                    left: `${x * 100}%`,
                    top: `${(0.08 + y * 0.84) * 100}%`,
                    width: 4,
                    height: 4,
                    borderRadius: 999,
                    background: "rgba(227,53,43,0.50)",
                    transform: "translate(-50%,-50%)",
                  }}
                />
              );
            })}
          </div>

          {/* Labels (mais legíveis) */}
          {points.map((c) => {
            const pos = toWorldPx(c);
            return (
              <button
                key={c.uri}
                type="button"
                onMouseEnter={() => {
                  setHover({ c });
                  setHoverPoke(null);
                  loadHoverPokemon(c);
                }}
                onMouseLeave={() => {
                  setHover(null);
                }}
                style={{
                  position: "absolute",
                  left: pos.x,
                  top: pos.y,
                  transform: "translate(-50%,-50%)",
                  // 👇 Legibilidade
                  background: "rgba(255,255,255,0.94)",
                  border: "2px solid rgba(0,0,0,0.72)",
                  borderRadius: 999,
                  padding: "6px 10px",
                  boxShadow: "4px 4px 0 rgba(0,0,0,0.50)",
                  fontFamily: "var(--font-pixel)",
                  fontSize: 12,
                  lineHeight: 1,
                  cursor: "default",
                  maxWidth: 240,
                  whiteSpace: "nowrap",
                  overflow: "hidden",
                  textOverflow: "ellipsis",

                  // “halo” em volta (ajuda MUITO em fundo pontilhado)
                  outline: "6px solid rgba(255,255,255,0.45)",
                  outlineOffset: 0,
                }}
                title={c.label}
              >
                <span
                  style={{
                    display: "inline-block",
                    width: 8,
                    height: 8,
                    borderRadius: 999,
                    background: "rgba(227,53,43,0.95)",
                    marginRight: 8,
                    verticalAlign: "middle",
                  }}
                />
                {/* text-shadow dá borda no texto pixel */}
                <span
                  style={{
                    textShadow:
                      "1px 0 rgba(255,255,255,0.9), -1px 0 rgba(255,255,255,0.9), 0 1px rgba(255,255,255,0.9), 0 -1px rgba(255,255,255,0.9)",
                  }}
                >
                  {c.label}
                </span>
              </button>
            );
          })}
        </div>

        {/* Tooltip centralizado (fora do mundo, preso no viewport) */}
        {hover && (
          <div
            style={{
              position: "absolute",
              left: tooltipLeft,
              top: tooltipTop,
              transform: "translateX(-50%)",
              width: 280,
              borderRadius: 14,
              border: "3px solid #111",
              background: "rgba(255,255,255,0.97)",
              boxShadow: "6px 6px 0 rgba(0,0,0,0.6)",
              padding: 12,
              zIndex: 40,
              pointerEvents: "none",
            }}
          >
            <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
              <div
                style={{
                  width: 70,
                  height: 70,
                  borderRadius: 12,
                  border: "2px solid rgba(0,0,0,0.6)",
                  background: "rgba(0,0,0,0.06)",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  overflow: "hidden",
                  flex: "0 0 auto",
                }}
              >
                {sprite ? (
                  <img
                    src={sprite}
                    alt={hoverPoke?.name ?? hover.c.label}
                    style={{ width: "100%", height: "100%", objectFit: "cover" }}
                  />
                ) : (
                  <div style={{ fontFamily: "var(--font-pixel)", fontSize: 10, opacity: 0.8 }}>
                    loading...
                  </div>
                )}
              </div>

              <div style={{ minWidth: 0 }}>
                <div
                  style={{
                    fontFamily: "var(--font-pixel)",
                    fontSize: 14,
                    whiteSpace: "nowrap",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                  }}
                >
                  {hover.c.label.toUpperCase()}
                </div>

                {hoverPoke ? (
                  <div style={{ marginTop: 6, display: "flex", gap: 8, flexWrap: "wrap" }}>
                    <span style={{ fontFamily: "var(--font-pixel)", fontSize: 11, opacity: 0.9 }}>
                      # {hoverPoke.id}
                    </span>
                    <span style={{ fontFamily: "var(--font-pixel)", fontSize: 11, opacity: 0.9 }}>
                      {hoverPoke.types.map((t) => t.type.name.toUpperCase()).join(", ")}
                    </span>
                  </div>
                ) : (
                  <div style={{ marginTop: 6, fontFamily: "var(--font-pixel)", fontSize: 11, opacity: 0.75 }}>
                    PokeAPI not found
                  </div>
                )}
              </div>
            </div>

            <div style={{ marginTop: 10, fontSize: 10, opacity: 0.75 }}>
              <code style={{ fontSize: 10 }}>{hover.c.uri}</code>
            </div>
          </div>
        )}
      </div>

      {/* hintzinho */}
      <div style={{ marginTop: 10, fontSize: 12, opacity: 0.85 }}>
        Wheel = zoom • Drag = pan • Scroll = navigate
      </div>
    </div>
  );
}

function btnStyle(): React.CSSProperties {
  return {
    border: "2px solid rgba(0,0,0,0.75)",
    borderRadius: 10,
    padding: "6px 10px",
    background: "rgba(255,255,255,0.9)",
    boxShadow: "3px 3px 0 rgba(0,0,0,0.45)",
    fontFamily: "var(--font-pixel)",
    fontSize: 12,
    cursor: "pointer",
  };
}
