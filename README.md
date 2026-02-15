# Pokenator
Akinator based on pokemon only using ontology and web semantics knowledge

# Projeto: Akinator de Pokémon utilizando Ontologia PokémonKG

## 1. O que é — Descrição do Escopo e Objetivo

O projeto propõe o desenvolvimento de um **“Akinator de Pokémon”**, um sistema que adivinha qual Pokémon o usuário está pensando por meio de perguntas interativas e consultas semânticas a uma **ontologia formal (PokémonKG)**.

**Objetivos principais:**

* Aplicar conceitos de **Web Semântica** e **ontologias OWL/RDF** em um caso lúdico e interativo.
* Criar uma aplicação web que traduza respostas do usuário em **consultas SPARQL**.
* Demonstrar a **capacidade inferencial e explicativa** de um sistema baseado em conhecimento semântico.

---

## 2. Ontologia(s) Utilizada(s)

A ontologia base utilizada será a **PokémonKG Ontology**, disponível em:
[https://www.pokemonkg.org/ontology](https://www.pokemonkg.org/ontology)

### Principais conceitos e relações:

* **Classes:** `pok:Pokemon`, `pok:Type`, `pok:Ability`, `pok:Habitat`, `pok:Generation`
* **Propriedades:** `pok:hasType`, `pok:hasAbility`, `pok:fromGeneration`, `pok:hasEggGroup`, `pok:baseStatAttack`

### Exemplo de Consulta SPARQL

```sparql
PREFIX pok: <http://pokemonkg.org/ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?pokemon ?label WHERE {
  ?pokemon a pok:Pokemon ;
           rdfs:label ?label ;
           pok:hasType pok:Fire ;
           pok:fromGeneration pok:Gen1 .
}
LIMIT 20
```

### Inferência e Raciocínio:

* O triplestore deve possuir raciocínio **RDFS/OWL** habilitado.
* Regras adicionais (SWRL ou Jena Rules) podem criar categorias derivadas, como `HighAttack` ou `DualType`.

---

## 3. Informações Arquiteturais

### Arquitetura e Fluxo

1. **Frontend (React/Vite):** apresenta perguntas e coleta respostas (sim/não/talvez).
2. **Backend (Node.js/Express):** gera consultas SPARQL com base nas respostas.
3. **Triplestore (Apache Jena Fuseki ou GraphDB):** executa as queries sobre a ontologia PokémonKG.
4. **Módulo de Heurística:** escolhe a próxima pergunta com base em ganho de informação.
5. **Resultado:** exibe o palpite com justificativas (triples usados para inferência).

### Tecnologias e Ferramentas

* **Ontologia/Dados:** PokémonKG (OWL/RDF)
* **Servidor SPARQL:** Apache Jena Fuseki / GraphDB
* **Backend:** Node.js com `sparql-http-client`
* **Frontend:** React + Tailwind
* **Ambiente:** Docker para triplestore, Postman para testes

---

## 4. Etapas do Desenvolvimento e Cronograma

| Data           | Etapa                                                    | Entregável                  | Responsável |
| -------------- | -------------------------------------------------------- | --------------------------- | ----------- |
| 02–03/11       | Setup do ambiente e importação da ontologia              | Endpoint SPARQL funcional   | —           |
| 04/11          | Mapeamento de atributos (Type, Generation, Habitat etc.) | Documento de atributos      | —           |
| 05–06/11       | Protótipos SPARQL e testes de inferência                 | Scripts de consulta         | —           |
| 06–07/11       | Implementação do backend e API                           | Endpoints `/ask` e `/guess` | —           |
| 07–09/11       | Implementação do frontend                                | Interface do jogo           | —           |
| 09–10/11       | Integração, testes e refinamentos                        | Fluxo completo funcional    | —           |
| 11/11 ou 12/11 | Apresentação                                             | PDF e demonstração          | —           |

---

## 5. Cenário de Uso

O sistema atua como um **assistente interativo de conhecimento**, simulando um jogo de adivinhação:

* O usuário pensa em um Pokémon.
* O sistema faz perguntas binárias (“É do tipo Fogo?”, “É da geração 1?”).
* Cada resposta filtra o conjunto de Pokémon possíveis via SPARQL.
* Quando restam poucos candidatos, o sistema apresenta um palpite justificado.

### Utilidade Prática

* **Educação:** demonstração lúdica de como ontologias estruturam conhecimento e suportam raciocínio automático.
* **Pesquisa:** exemplo didático de integração entre Web Semântica e interfaces web modernas.
* **Explicabilidade:** o sistema exibe *por que* chegou a determinada conclusão (justificativas semânticas).

---

## 6. Participação no Grupo e Avaliação Individual

Cada integrante contribuiu em uma área específica do desenvolvimento:

* **Ontologia e Dados:** importação e modelagem.
* **Consultas SPARQL:** criação e otimização de queries.
* **Backend/API:** integração e lógica de raciocínio.
* **Frontend:** interface e experiência do usuário.
* **Testes e Apresentação:** integração e documentação final.

*(Os nomes dos integrantes serão adicionados antes da submissão final.)*

---

## 7. Trabalhos Futuros

* Suporte a múltiplas ontologias (itens, regiões, locais).
* Aprimoramento da heurística de seleção de perguntas.
* Internacionalização (EN/PT).
* Expansão para outras franquias ou domínios (ex.: Digimon, Star Wars).

---

**Apresentação:**

* Duração: 10–12 minutos
* Estrutura: Contexto → Ontologia → Arquitetura → Cronograma → Demonstração → Conclusão

---

**Status:** Documento base preparado para conversão em PDF e envio oficial.
**Próximo passo:** Inserir nomes dos integrantes e gerar versão final para submissão.
