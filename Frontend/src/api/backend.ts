export type Answer = "YES" | "NO" | "UNKNOWN";

export type ApiQuestion = {
  text: string;
  kind: string; // ex: "HAS_VALUE"
  predicateUri?: string | null;
  objectUri?: string | null;
};

export type StartResponse = {
  sessionId: string;
  question: ApiQuestion;
};

export type Candidate = { 
  uri: string; label: string 
};

export type CandidateResponse = { 
  candidates: Candidate[] 
};

export type StepResponse =
  | {
      kind: "QUESTION";
      remainingCandidates: number;
      question: ApiQuestion;
      guessUri: null;
      guessLabel: null;
    }
  | {
      kind: "GUESS";
      remainingCandidates: number;
      question?: null;
      guessUri: string;
      guessLabel: string;
    }
  | {
      kind: "NO_CANDIDATES";
      remainingCandidates: number;
      question?: null;
      guessUri?: null;
      guessLabel?: null;
    };

async function http<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init);

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} - ${text}`);
  }

  return (await res.json()) as T;
}

export function startGame(): Promise<StartResponse> {
  return http<StartResponse>("/api/game/start", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: "{}",
  });
}

export function answerGame(sessionId: string, answer: Answer): Promise<StepResponse> {
  return http<StepResponse>(`/api/game/${encodeURIComponent(sessionId)}/answer`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify({ answer }),
  });
}

// Nova função de API para listar candidatos
export function getCandidates(sessionId: string): Promise<CandidateResponse> {
  return http<CandidateResponse>(`/api/game/${encodeURIComponent(sessionId)}/candidates`, {
    method: "GET",
    headers: { Accept: "application/json" },
  });
}
