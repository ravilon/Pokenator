import { useEffect, useMemo, useRef, useState } from "react";
import type { Answer, StartResponse, StepResponse } from "./api/backend";
import { answerGame, startGame } from "./api/backend";
import { fetchPokemon, toPokeApiSlug, type PokeApiPokemon } from "./api/pokeapi";

type UiGuess = {
  label: string;
  uri?: string | null;
};

type ConfettiPiece = {
  id: string;
  leftPct: number;
  size: number;
  durationMs: number;
  delayMs: number;
  rotateDeg: number;
  driftPx: number;
  hue: number;
};

function makeConfetti(count = 120): ConfettiPiece[] {
  const out: ConfettiPiece[] = [];
  for (let i = 0; i < count; i++) {
    out.push({
      id: `${Date.now()}-${i}-${Math.random().toString(16).slice(2)}`,
      leftPct: Math.random() * 100,
      size: 6 + Math.random() * 10,
      durationMs: 900 + Math.random() * 1100,
      delayMs: Math.random() * 220,
      rotateDeg: Math.floor(Math.random() * 860),
      driftPx: -90 + Math.random() * 180,
      hue: Math.floor(Math.random() * 360),
    });
  }
  return out;
}

type LockState =
  | null
  | {
      reason: "CORRECT" | "NO_MORE_GUESSES" | "REPEATED_GUESS";
      title: string;
      subtitle: string;
    };

function PokeballMark() {
  return (
    <svg width="54" height="54" viewBox="0 0 64 64" aria-hidden="true">
      <circle cx="32" cy="32" r="28" fill="#f7f7f7" stroke="#111" strokeWidth="4" />
      <path d="M4 32a28 28 0 0 1 56 0H4z" fill="#e3352b" stroke="#111" strokeWidth="4" />
      <rect x="4" y="28" width="56" height="8" fill="#111" />
      <circle cx="32" cy="32" r="9" fill="#f7f7f7" stroke="#111" strokeWidth="4" />
      <circle cx="32" cy="32" r="3" fill="#111" />
    </svg>
  );
}

/**
 * Extracts pokemon label from texts like:
 * "Is it Marshtomp?" -> "Marshtomp"
 */
function extractGuessLabelFromText(text: string): string | null {
  if (!text) return null;
  const t = text.trim().replace(/\s+/g, " ");
  const m = t.match(/^is it\s+(.+?)\s*\?\s*$/i);
  if (!m) return null;
  const name = (m[1] || "").trim();
  return name.length ? name : null;
}

export default function App() {
  const [sessionId, setSessionId] = useState("");
  const [questionText, setQuestionText] = useState("Loading...");
  const [remaining, setRemaining] = useState<number | null>(null);

  const [guess, setGuess] = useState<UiGuess | null>(null);
  const [poke, setPoke] = useState<PokeApiPokemon | null>(null);

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState("");

  const [lock, setLock] = useState<LockState>(null);
  const [confetti, setConfetti] = useState<ConfettiPiece[]>([]);

  const lastGuessKeyRef = useRef<string | null>(null);

  const sprite = useMemo(() => {
    return (
      poke?.sprites.other?.["official-artwork"]?.front_default ??
      poke?.sprites.front_default ??
      null
    );
  }, [poke]);

  async function loadPoke(label: string) {
    const slug = toPokeApiSlug(label);
    try {
      const p = await fetchPokemon(slug);
      setPoke(p);
    } catch {
      setPoke(null);
      setErr(
        `Guess "${label}" found, but PokeAPI lookup failed for "${slug}". ` +
          `Tip: returning a PokeAPI-compatible name from the backend is the most reliable option.`
      );
    }
  }

  function setRemainingIfPresent(value: any) {
    if (value !== null && value !== undefined && typeof value === "number" && Number.isFinite(value)) {
      setRemaining(value);
    }
  }

  async function init() {
    setBusy(true);
    setErr("");
    setGuess(null);
    setPoke(null);
    setQuestionText("Loading...");
    setLock(null);
    setConfetti([]);
    lastGuessKeyRef.current = null;

    try {
      const start: StartResponse = await startGame();
      setSessionId(start.sessionId);
      setQuestionText(start.question.text);
      setRemaining(null);
    } catch (e: any) {
      setErr(e?.message ?? "Failed to start game");
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => {
    init();
  }, []);

  async function applyGuess(nextGuess: UiGuess, remainingCandidates: any) {
    // Render guess mode immediately
    setGuess(nextGuess);
    setPoke(null);

    setRemainingIfPresent(remainingCandidates);

    const key = `${(nextGuess.label || "").trim().toLowerCase()}|${(nextGuess.uri || "").trim().toLowerCase()}`;
    if (lastGuessKeyRef.current && key === lastGuessKeyRef.current) {
      setLock({
        reason: "REPEATED_GUESS",
        title: "NO NEW GUESSES 😵",
        subtitle: "The backend repeated the same guess. Click NEW GAME to restart.",
      });
      setConfetti(makeConfetti(90));
      window.setTimeout(() => setConfetti([]), 1700);
      return;
    }
    lastGuessKeyRef.current = key;

    await loadPoke(nextGuess.label);
  }

  async function handleStep(step: StepResponse) {
    // 1) Normal QUESTION
    if (step.kind === "QUESTION") {
      // ✅ Special case: GUESS embedded in question payload
      const qAny = step.question as any;
      const embeddedKind = (qAny?.kind ?? "").toString().toUpperCase();

      if (embeddedKind === "GUESS") {
        const label = extractGuessLabelFromText(step.question.text) ?? "Unknown";
        const uri = (step.question as any)?.objectUri ?? null;

        await applyGuess({ label, uri }, (step as any).remainingCandidates);
        return;
      }

      // Normal question flow
      setGuess(null);
      setPoke(null);
      lastGuessKeyRef.current = null;

      setQuestionText(step.question.text);
      setRemainingIfPresent((step as any).remainingCandidates);
      return;
    }

    // 2) Normal GUESS
    if (step.kind === "GUESS") {
      const label = (step as any).guessLabel;
      const uri = (step as any).guessUri ?? null;

      await applyGuess({ label, uri }, (step as any).remainingCandidates);
      return;
    }

    // 3) NO_CANDIDATES
    setGuess(null);
    setPoke(null);
    setQuestionText("No candidates left 😅");

    const rem = (step as any).remainingCandidates;
    if (typeof rem === "number") setRemaining(rem);
    else setRemaining(0);

    setLock({
      reason: "NO_MORE_GUESSES",
      title: "OUT OF GUESSES 😅",
      subtitle: "No candidates left. Click NEW GAME to restart.",
    });
    setConfetti(makeConfetti(85));
    window.setTimeout(() => setConfetti([]), 1700);
  }

  async function onAnswer(answer: Answer) {
    if (!sessionId) return;
    if (lock) return;

    setBusy(true);
    setErr("");

    try {
      const step = await answerGame(sessionId, answer);
      await handleStep(step);
    } catch (e: any) {
      setErr(e?.message ?? "Failed to submit answer");
    } finally {
      setBusy(false);
    }
  }

  function onCorrect() {
    if (lock) return;

    setErr("");
    setLock({
      reason: "CORRECT",
      title: "I GOT IT! 🎉",
      subtitle: "Nice! You can start a new game now.",
    });
    setConfetti(makeConfetti(130));
    window.setTimeout(() => setConfetti([]), 1900);
  }

  async function onWrongTryNext() {
    // Wrong => answer NO and let backend pick another candidate/flow
    await onAnswer("NO");
  }

  const showOnlyNewGame = lock !== null;

  return (
    <div className="page">
      {confetti.length > 0 && (
        <div style={{ position: "fixed", inset: 0, pointerEvents: "none", overflow: "hidden", zIndex: 9999 }}>
          {confetti.map((c) => (
            <span
              key={c.id}
              style={{
                position: "absolute",
                left: `${c.leftPct}%`,
                top: "-22px",
                width: `${c.size}px`,
                height: `${c.size * 0.64}px`,
                background: `hsl(${c.hue} 92% 60%)`,
                borderRadius: 2,
                opacity: 0.95,
                transform: `rotate(${c.rotateDeg}deg)`,
                animation: `confetti-fall ${c.durationMs}ms cubic-bezier(.21,.61,.2,1) ${c.delayMs}ms forwards`,
                ["--drift" as any]: `${c.driftPx}px`,
              }}
            />
          ))}
          <style>{`
            @keyframes confetti-fall {
              0% { transform: translate(0px, 0px) rotate(0deg); opacity: 1; }
              100% { transform: translate(var(--drift, 0px), 112vh) rotate(780deg); opacity: 0; }
            }
          `}</style>
        </div>
      )}

      <div className="topBar">
        <div style={{ display: "flex", gap: 12, alignItems: "flex-start" }}>
          <PokeballMark />
          <div>
            <h1 className="title">POKENATOR</h1>
            <div className="subtitle">Game Boy-style guessing • Pokéball vibes</div>
          </div>
        </div>

        <div className="badges">
          <span className="badge">
            ID <code>{sessionId || "..."}</code>
          </span>
          {remaining !== null && (
            <span className="badge">
              LEFT <code>{remaining}</code>
            </span>
          )}
        </div>
      </div>

      {err && (
        <div className="toast">
          <b>Error:</b> {err}
        </div>
      )}

      {/* QUESTION MODE */}
      {!guess && (
        <div className="card" style={{ marginTop: 18 }}>
          <div className="cardInner">
            <div className="row" style={{ justifyContent: "space-between" }}>
              <div className="arrow">▶ QUESTION</div>
              <div className="small">Answer honestly 😄</div>
            </div>

            <div className="hr" />

            <p className="bigQ">{questionText}</p>

            <div className="row" style={{ marginTop: 16 }}>
              <button className="btn-yes" disabled={busy || !!lock} onClick={() => onAnswer("YES")}>
                YES
              </button>
              <button className="btn-no" disabled={busy || !!lock} onClick={() => onAnswer("NO")}>
                NO
              </button>
              <button className="btn-unk" disabled={busy || !!lock} onClick={() => onAnswer("UNKNOWN")}>
                I DON'T KNOW
              </button>

              <div style={{ flex: 1 }} />

              <button className="btn-ghost" disabled={busy} onClick={init}>
                RESET
              </button>
            </div>

            <div className="small" style={{ marginTop: 10, opacity: 0.9 }}>
              Tip: “I DON'T KNOW” helps when you're unsure.
            </div>
          </div>
        </div>
      )}

      {/* GUESS MODE */}
      {guess && (
        <div className="card" style={{ marginTop: 18 }}>
          <div className="cardInner">
            <div className="row" style={{ justifyContent: "space-between" }}>
              <div className="arrow">▶ GUESS</div>
              <div className="small">Did I get it right?</div>
            </div>

            <div className="hr" />

            <div className="row" style={{ alignItems: "flex-start", gap: 16 }}>
              <div className="pokeFrame">
                {sprite ? <img src={sprite} alt={poke?.name ?? guess.label} /> : <div className="small">Loading Pokémon...</div>}
              </div>

              <div className="stack" style={{ flex: 1 }}>
                <div style={{ display: "grid", gap: 8 }}>
                  <div style={{ fontFamily: "var(--font-pixel)", fontSize: 16, lineHeight: 1.4 }}>
                    {guess.label.toUpperCase()}
                  </div>

                  {guess.uri && (
                    <div className="small">
                      uri: <code>{guess.uri}</code>
                    </div>
                  )}

                  {poke && (
                    <div className="row" style={{ gap: 10, flexWrap: "wrap" }}>
                      <span className="badge">
                        # <code>{poke.id}</code>
                      </span>
                      <span className="badge">
                        TYPE <code>{poke.types.map((t) => t.type.name.toUpperCase()).join(", ")}</code>
                      </span>
                      <span className="badge">
                        HT <code>{(poke.height / 10).toFixed(1)}m</code>
                      </span>
                      <span className="badge">
                        WT <code>{(poke.weight / 10).toFixed(1)}kg</code>
                      </span>
                    </div>
                  )}
                </div>

                {lock && (
                  <div
                    style={{
                      padding: 14,
                      borderRadius: 12,
                      border: "4px solid #111",
                      background:
                        lock.reason === "CORRECT"
                          ? "rgba(185,255,203,0.85)"
                          : "rgba(255,193,189,0.85)",
                      boxShadow: "6px 6px 0 rgba(0,0,0,0.65)",
                      fontFamily: "var(--font-pixel)",
                      fontSize: 12,
                      lineHeight: 1.4,
                    }}
                  >
                    <div style={{ fontSize: 14, marginBottom: 6 }}>{lock.title}</div>
                    <div>{lock.subtitle}</div>
                  </div>
                )}

                <div className="row" style={{ marginTop: 10 }}>
                  {!showOnlyNewGame ? (
                    <>
                      <button className="btn-primary" disabled={busy} onClick={onCorrect}>
                        CORRECT
                      </button>
                      <button className="btn-ghost" disabled={busy} onClick={onWrongTryNext}>
                        WRONG
                      </button>
                      <div style={{ flex: 1 }} />
                      <button className="btn-ghost" disabled={busy} onClick={init}>
                        NEW GAME
                      </button>
                    </>
                  ) : (
                    <>
                      <div className="small">Actions locked — only NEW GAME is available.</div>
                      <div style={{ flex: 1 }} />
                      <button className="btn-primary" disabled={busy} onClick={init}>
                        NEW GAME
                      </button>
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
