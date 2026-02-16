export type PokeApiPokemon = {
  id: number;
  name: string;
  sprites: {
    front_default: string | null;
    other?: {
      ["official-artwork"]?: { front_default: string | null };
    };
  };
  types: { type: { name: string } }[];
  height: number; // decímetros
  weight: number; // hectogramas
  stats: { base_stat: number; stat: { name: string } }[];
};

export async function fetchPokemon(nameOrId: string): Promise<PokeApiPokemon> {
  const url = `https://pokeapi.co/api/v2/pokemon/${encodeURIComponent(
    nameOrId.toLowerCase()
  )}`;

  const res = await fetch(url);
  if (!res.ok) throw new Error(`PokeAPI: pokemon "${nameOrId}" not found`);
  return (await res.json()) as PokeApiPokemon;
}

/**
 * Conversão simples de label para slug PokeAPI.
 * (funciona para muitos casos comuns; casos especiais podem exigir mapeamento)
 */
export function toPokeApiSlug(label: string): string {
  return label
    .trim()
    .toLowerCase()
    .replace(/[’']/g, "") // remove apostrofos
    .replace(/♀/g, "-f")
    .replace(/♂/g, "-m")
    .replace(/\./g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-");
}
