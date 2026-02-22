# Pokenator

Jogo de adivinhação ao estilo Akinator, focado apenas em Pokémon, que utiliza a **Ontologia PokémonKG** e conceitos de **Web Semântica** para inferir qual criatura o usuário está pensando.

## 1. O que é — Descrição do Escopo e Objetivo

O projeto propõe o desenvolvimento de um **“Akinator de Pokémon”**, um sistema que adivinha qual Pokémon o usuário está pensando por meio de perguntas interativas e consultas semânticas a uma **ontologia formal (PokémonKG)**.

**Objetivos principais:**

- Aplicar conceitos de **Web Semântica** e **ontologias OWL/RDF** em um caso lúdico e interativo.
- Criar uma aplicação web que traduza respostas do usuário em **consultas SPARQL** que refinam dinamicamente um conjunto de candidatos.
- Demonstrar a **capacidade inferencial e explicativa** de um sistema baseado em conhecimento semântico e grafos de conhecimento.

---

## 2. Ontologia(s) Utilizada(s)

A ontologia base utilizada é a **PokémonKG Ontology**, disponível em:
[https://pokemonkg.org/ontology/version/1.0.0](https://pokemonkg.org/ontology/version/1.0.0)

### Principais conceitos e relações

- **Classes:** `pok:Pokemon`, `pok:Type`, `pok:Ability`, `pok:Habitat`, `pok:Generation`
- **Propriedades:** `pok:hasType`, `pok:hasAbility`, `pok:fromGeneration`, `pok:hasEggGroup`, `pok:baseStatAttack`

### Exemplo de Consulta SPARQL

```sparql
PREFIX pok: <http://pokemonkg.org/ontology#>
SELECT ?pokemon ?label WHERE {
  ?pokemon a pok:Pokemon ;
           pok:hasType pok:Fire ;
           pok:fromGeneration pok:Generation_I ;
           rdfs:label ?label .
}
```

### Inferência e Raciocínio

- O triplestore deve possuir raciocínio **RDFS/OWL** habilitado.
- Regras adicionais (SWRL ou Jena Rules) podem criar categorias derivadas, como `DualType` ou `HighAttack`.

---

## 3. Informações Arquiteturais

### Arquitetura e Fluxo

1. **Frontend (React + Vite + Tailwind CSS):** apresenta perguntas e coleta respostas (sim/não/talvez), atualizando em tempo real a lista de candidatos.
2. **Backend (Spring Boot / Java):** implementa a engine de raciocínio e expõe os endpoints (`/ask`, `/guess`). Constrói consultas SPARQL dinamicamente com base nas respostas recebidas e consulta o triplestore para refinar o conjunto de candidatos.
3. **Triplestore (Apache Jena Fuseki ou GraphDB):** executa as consultas sobre a ontologia PokémonKG e retorna as entidades que satisfazem as restrições acumuladas.
4. **Módulo de Heurística:** escolhe a próxima pergunta avaliando famílias de predicados (tipo, cor, forma, habitat, geração). Para cada valor, conta quantos dos candidatos restantes o satisfazem e ordena as perguntas pela proximidade de uma divisão 50/50, escolhendo aleatoriamente dentre as melhores para maximizar o ganho de informação e evitar determinismo.
5. **Resultado:** quando restam dois ou menos candidatos, o sistema apresenta um palpite e exibe as triplas usadas como justificativa da inferência.

### Tecnologias e Ferramentas

- **Ontologia/Dados:** PokémonKG (OWL/RDF) e snapshots de instância integrados via PokéAPI.
- **Servidor SPARQL:** Apache Jena Fuseki / GraphDB.
- **Backend:** Spring Boot (Java) com Apache Jena para consultas SPARQL.
- **Frontend:** React com Vite e Tailwind CSS.
- **Ambiente:** Docker para o triplestore; Postman/Insomnia para testes de API.

---

## 4. Lógica de Funcionamento

O Pokenator mantém um conjunto de Pokémon candidatos e o refina progressivamente conforme o usuário responde às perguntas:

- Para cada resposta **sim**, adiciona-se um padrão triple `?p <predicado> <valor>` à consulta; para respostas **não**, adiciona-se um filtro `FILTER NOT EXISTS { ?p <predicado> <valor> }` para excluir os Pokémon que possuem aquele valor.
- A engine constrói dinamicamente uma consulta SPARQL combinando todas as restrições e executa-a no triplestore. Como as restrições se acumulam, cada consulta subsequente é mais restritiva e tende a ser mais rápida.
- A seleção da próxima pergunta baseia-se no cálculo de quantos candidatos satisfazem cada valor de um conjunto de propriedades. Perguntas que dividem o conjunto de forma equilibrada fornecem mais informação. Entre as melhores divisões, seleciona-se uma de forma aleatória para reduzir o determinismo.
- Quando o conjunto de candidatos fica reduzido (duas espécies ou menos), o sistema passa para o modo de palpite e pergunta diretamente se o Pokémon é um dos remanescentes. Independentemente do resultado, é exibida uma explicação que lista os tipos, habilidades, geração e outras triplas relevantes que levaram ao palpite.

---

## 5. Etapas do Desenvolvimento e Cronograma

| Data           | Etapa                                                    | Entregável                  | Responsável |
| -------------- | -------------------------------------------------------- | --------------------------- | ----------- |
| 02–03/11       | Setup do ambiente e importação da ontologia              | Endpoint SPARQL funcional   | —           |
| 04/11          | Mapeamento de atributos (Type, Generation, Habitat etc.) | Documento de atributos      | —           |
| 05–06/11       | Protótipos SPARQL e testes de inferência                 | Scripts de consulta         | —           |
| 06–07/11       | Implementação do backend e API (Spring Boot)            | Endpoints `/ask` e `/guess` | —           |
| 07–09/11       | Implementação do frontend (React/Vite)                   | Interface do jogo           | —           |
| 09–10/11       | Integração, testes e refinamentos                        | Fluxo completo funcional    | —           |
| 11/11 ou 12/11 | Apresentação                                             | PDF e demonstração          | —           |

---

## 6. Cenário de Uso

O sistema atua como um **assistente interativo de conhecimento**, simulando um jogo de adivinhação:

- O usuário pensa em um Pokémon.
- O sistema faz perguntas binárias (“É do tipo Fogo?”, “É da geração I?”, “Possui a habilidade Overgrow?”).
- Cada resposta filtra o conjunto de Pokémon possíveis via SPARQL e a engine seleciona a próxima pergunta com base em heurísticas de ganho de informação.
- Quando restam poucos candidatos, o sistema apresenta um palpite justificado, exibindo as triplas utilizadas para a inferência.

### Utilidade Prática

- **Educação:** demonstração lúdica de como ontologias estruturam conhecimento e suportam raciocínio automático.
- **Pesquisa:** exemplo didático de integração entre Web Semântica, heurísticas de busca e interfaces web modernas.
- **Explicabilidade:** o sistema exibe por que chegou a determinada conclusão, listando as tripas semânticas envolvidas.

---

## 7. Participação no Grupo e Avaliação Individual

Cada integrante contribuiu em uma área específica do desenvolvimento:

- **Ontologia e Dados:** importação, modelagem e extensão do grafo.
- **Consultas SPARQL:** criação e otimização de queries, construção dinâmica das restrições.
- **Backend/API:** implementação da engine de raciocínio em Spring Boot e integração com o triplestore.
- **Frontend:** desenvolvimento da interface de usuário em React/Vite e integração com a API.
- **Testes e Apresentação:** integração de componentes, testes de usabilidade e elaboração da apresentação final.

*(Os nomes dos integrantes serão adicionados antes da submissão final.)*

---

## 8. Trabalhos Futuros

- Suporte a múltiplas ontologias (itens, regiões, locais, movimentos) para enriquecer o modelo.
- Aprimoramento da heurística de seleção de perguntas com métricas como entropia e probabilidades condicionais.
- Internacionalização da aplicação (EN/PT).
- Expansão do conceito para outras franquias ou domínios (ex.: Digimon, Star Wars).

---

## Documentação

Esta pasta do repositório contém documentos de apoio ao projeto **Pokenator**:

- **Pokenator‑Apresentação.pdf** – Apresentação do projeto em português, com slides que resumem o objetivo, a arquitetura e os resultados alcançados.
- **Pokenator‑en.pdf** – Artigo técnico em inglês descrevendo a motivação, as ontologias utilizadas, a arquitetura do sistema, a implementação, as heurísticas de seleção de perguntas e a avaliação do Pokenator.
- **Pokenator‑pt.pdf** – Versão em português do artigo técnico, adaptada a partir da versão em inglês.
- **postman/** – Pasta com coleções do Postman para testar a API do Pokenator (endpoints como `/ask` e `/guess`).

Consulte cada documento conforme a necessidade para compreender a visão geral do projeto, os detalhes técnicos e como interagir com a API.
