import { useEffect, useState } from "react";
import type { Answer, StartResponse, StepResponse } from "./api/backend";
import { answerGame, startGame } from "./api/backend";
import { fetchPokemon, toPokeApiSlug, type PokeApiPokemon } from "./api/pokeapi";

type UiGuess = {
  label: string;
  uri?: string | null;
  confidence?: number;
};

export default function App() {
  const [sessionId, setSessionId] = useState("");
  const [questionText, setQuestionText] = useState("Carregando...");
  const [remaining, setRemaining] = useState<number | null>(null);

  const [guess, setGuess] = useState<UiGuess | null>(null);
  const [poke, setPoke] = useState<PokeApiPokemon | null>(null);

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState("");

  async function init() {
    setBusy(true);
    setErr("");
    setGuess(null);
    setPoke(null);
    setRemaining(null);
    setQuestionText("Carregando...");

    try {
      const start: StartResponse = await startGame();
      setSessionId(start.sessionId);
      setQuestionText(start.question.text);
    } catch (e: any) {
      setErr(e?.message ?? "Falha ao iniciar");
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => {
    init();
  }, []);

  async function onAnswer(answer: Answer) {
    if (!sessionId) return;

    setBusy(true);
    setErr("");

    try {
      const step: StepResponse = await answerGame(sessionId, answer);

      if (step.kind === "QUESTION") {
        setRemaining(step.remainingCandidates);
        setQuestionText(step.question.text);
        setGuess(null);
        setPoke(null);
        return;
      }

      if (step.kind === "GUESS") {
        setRemaining(step.remainingCandidates);
        setGuess({ label: step.guessLabel, uri: step.guessUri });

        // tenta PokeAPI usando slug derivado do label
        const slug = toPokeApiSlug(step.guessLabel);
        try {
          const p = await fetchPokemon(slug);
          setPoke(p);
        } catch (pokeErr: any) {
          // não quebra o jogo se a PokeAPI não achar
          setPoke(null);
          setErr(
            `Palpite: "${step.guessLabel}". Não consegui achar na PokeAPI via "${slug}". ` +
              `Considere enviar também um pokeApiName no backend.`
          );
        }
        return;
      }

      // NO_CANDIDATES
      setRemaining(step.remainingCandidates ?? 0);
      setQuestionText("Não encontrei candidatos 😅");
      setGuess(null);
      setPoke(null);
    } catch (e: any) {
      setErr(e?.message ?? "Erro ao responder");
    } finally {
      setBusy(false);
    }
  }

  const sprite =
    poke?.sprites.other?.["official-artwork"]?.front_default ??
    poke?.sprites.front_default ??
    null;

  return (
    <div
      style={{
        maxWidth: 860,
        margin: "40px auto",
        padding: 16,
        fontFamily: "system-ui, Arial",
        color: "#eee",
      }}
    >
      <h1 style={{ margin: 0 }}>Pokenator</h1>

      <div style={{ marginTop: 8, opacity: 0.75, fontSize: 12 }}>
        session: <code>{sessionId || "..."}</code>
        {remaining !== null && (
          <>
            {" "}
            | candidates: <code>{remaining}</code>
          </>
        )}
      </div>

      {err && (
        <div
          style={{
            marginTop: 12,
            padding: 12,
            border: "1px solid #ff4d4d",
            borderRadius: 10,
            background: "rgba(255,77,77,0.08)",
          }}
        >
          <b>Erro:</b> {err}
        </div>
      )}

      {!guess && (
        <div
          style={{
            marginTop: 18,
            padding: 16,
            border: "1px solid #333",
            borderRadius: 14,
            background: "rgba(255,255,255,0.03)",
          }}
        >
          <h2 style={{ marginTop: 0 }}>Pergunta</h2>
          <p style={{ fontSize: 18, marginBottom: 12 }}>{questionText}</p>

          <div style={{ display: "flex", gap: 10, marginTop: 12, flexWrap: "wrap" }}>
            <button disabled={busy} onClick={() => onAnswer("YES")}>
              Sim
            </button>
            <button disabled={busy} onClick={() => onAnswer("NO")}>
              Não
            </button>
            <button disabled={busy} onClick={() => onAnswer("UNKNOWN")}>
              Não sei
            </button>
            <button disabled={busy} onClick={init} style={{ marginLeft: "auto" }}>
              Reiniciar
            </button>
          </div>
        </div>
      )}

      {guess && (
        <div
          style={{
            marginTop: 18,
            padding: 16,
            border: "1px solid #333",
            borderRadius: 14,
            background: "rgba(255,255,255,0.03)",
          }}
        >
          <h2 style={{ marginTop: 0 }}>Meu palpite</h2>

          <div style={{ display: "flex", gap: 16, flexWrap: "wrap" }}>
            <div style={{ width: 180 }}>
              {sprite ? (
                <img
                  src={sprite}
                  width={180}
                  height={180}
                  alt={poke?.name ?? guess.label}
                  style={{ objectFit: "contain" }}
                />
              ) : (
                <div style={{ width: 180, height: 180, display: "grid", placeItems: "center" }}>
                  Sem imagem
                </div>
              )}
            </div>

            <div style={{ flex: 1, minWidth: 260 }}>
              <div style={{ fontSize: 22, fontWeight: 800 }}>{guess.label}</div>
              {guess.uri && (
                <div style={{ marginTop: 6, opacity: 0.8, fontSize: 12 }}>
                  uri: <code>{guess.uri}</code>
                </div>
              )}

              {poke && (
                <div style={{ marginTop: 10, lineHeight: 1.6 }}>
                  <div>
                    <b>ID:</b> {poke.id}
                  </div>
                  <div>
                    <b>Tipos:</b> {poke.types.map((t) => t.type.name).join(", ")}
                  </div>
                  <div>
                    <b>Altura:</b> {poke.height / 10} m
                  </div>
                  <div>
                    <b>Peso:</b> {poke.weight / 10} kg
                  </div>
                </div>
              )}

              <div style={{ display: "flex", gap: 10, marginTop: 14, flexWrap: "wrap" }}>
                <button disabled={busy} onClick={() => onAnswer("YES")}>
                  Acertou ✅
                </button>
                <button disabled={busy} onClick={() => onAnswer("NO")}>
                  Errou ❌
                </button>
                <button disabled={busy} onClick={init} style={{ marginLeft: "auto" }}>
                  Novo jogo
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
