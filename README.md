# Estande Sombrio — cenários de estande de tiro com IA

Sistema web da **Atividade GenAI**, feito com **Spring Boot 3 + Spring AI**. Ele cadastra os estandes de tiro de
minigames inspirados no estande de tiro do Mercador (*Resident Evil 4*) e usa IA generativa (**Google Gemini**) para
criar a **descrição**, o **prompt de imagem** e a **imagem de fundo** de cada estande.

Cada minigame tem uma **imagem de fundo fixa** (o cenário do estande). A parte dinâmica são os **alvos**, que aparecem
e somem por cima dela. O botão **▶ Jogar** abre o minigame:
- Os inimigos mudam conforme o tipo de estande.
- Há uma refém que não pode ser atingida.
- Há 6 armas, cada uma com o seu diferencial (veja [Minigame](#minigame)).

## Requisitos da atividade × onde estão

| Requisito | Onde |
|---|---|
| Integração com banco de dados | Spring Data JPA + H2 em arquivo (`./data/estandes.mv.db`), tabelas `estande_tiro`, `log_geracao_imagem` e `arma` |
| Interface web | Thymeleaf + CSS (`src/main/resources/templates`, `static/css/estilo.css`) |
| Integração com GenAI via Spring AI | `ChatClient` do Spring AI com o starter `spring-ai-starter-model-google-genai` (`config/GenAiConfig.java`) |
| Funcionalidades que usam GenAI | Gerar descrição e gerar prompt de imagem (texto); gerar cenário (imagem) |
| Chave da API fora do GitHub | As chaves ficam só no `.env`, que está no `.gitignore`. O `application.properties` só referencia variáveis |

## Tecnologias

- **Java 21**, **Spring Boot 3.5.16**, Maven (com Maven Wrapper: não precisa instalar o Maven)
- **Spring AI 1.1.8**, a versão estável mais recente compatível com Spring Boot 3 (a linha 2.x do Spring AI exige
  Spring Boot 4), com o starter **`spring-ai-starter-model-google-genai`**
- Spring Data JPA + **H2 em modo arquivo**
- Thymeleaf + CSS + um pouco de JavaScript

## Como rodar

**1. Pré-requisito:** JDK 21 instalado (com `JAVA_HOME` configurado).

**2. Crie o arquivo `.env`** na raiz do projeto, a partir do modelo `.env.example`:

```powershell
Copy-Item .env.example .env
notepad .env
```

(Linux/macOS: `cp .env.example .env`.) Preencha os três valores:

```properties
GEMINI_API_KEY_TEXT=sua-chave-gratuita
GEMINI_API_KEY_IMAGE=sua-chave-faturada
MAX_IMAGE_GENERATIONS=5
```

- As chaves são criadas no Google AI Studio: <https://aistudio.google.com/apikey>.
- A chave de **imagem** precisa de **faturamento ativo**, porque os modelos de imagem não têm cota gratuita.
- O `.env` **nunca** vai para o GitHub: ele está no `.gitignore`.

**3. Rode o sistema** (de dentro da pasta do projeto, porque o `.env` e o banco são procurados na pasta atual):

```powershell
.\mvnw.cmd spring-boot:run
```

(Linux/macOS/Git Bash: `./mvnw spring-boot:run`.)

**4. Abra <http://localhost:8080>.** Na primeira execução são criados 4 estandes de exemplo (vila, castelo, ilha e
mina), ainda sem textos nem imagens, e as 6 armas do minigame. Se a porta 8080 estiver ocupada, rode antes
`$env:PORT=8081` (PowerShell).

**Como o `.env` é carregado:** a linha `spring.config.import=optional:file:.env[.properties]` do
`application.properties` faz o Spring Boot ler o `.env` como arquivo de propriedades, sem nenhuma dependência extra.
Variáveis de ambiente do sistema, se existirem, têm prioridade sobre o `.env`.

**Arquivos CSS e JS com versão:** o nome de cada arquivo leva um código do conteúdo (ex.: `/js/app-5123….js`), então o navegador sempre baixa a versão nova depois de uma atualização, em vez de usar a antiga do cache.

**Testes:** `.\mvnw.cmd test` roda 34 testes. Neles a IA é simulada (mocks), então eles não usam as chaves nem gastam
cota.

## As duas chaves de API

| Variável | Tipo | Usada para | Como chega no código | Modelo (`application.properties`) |
|---|---|---|---|---|
| `GEMINI_API_KEY_TEXT` | gratuita | **todas** as requisições de texto | `ChatClient` do Spring AI (`GoogleGenAiChatModel`) | `gemini.text.model=gemini-3.5-flash-lite` |
| `GEMINI_API_KEY_IMAGE` | faturada | **somente** geração de imagens | `ImageService` (RestClient, cabeçalho `x-goog-api-key`) | `gemini.image.model=gemini-3.1-flash-lite-image` |

- A autoconfiguração do Spring AI aceita uma chave só. Por isso ela está desligada (`spring.ai.model.chat=none`) e os
  dois beans são criados à mão em [`config/GenAiConfig.java`](src/main/java/com/faculdade/estandetiro/config/GenAiConfig.java),
  cada um com a sua chave.
- O Spring AI 1.1.x **não tem** modelo de imagem do Gemini: o módulo `spring-ai-google-genai-image` só existe no
  Spring AI 2.x, que exige Spring Boot 4. Por isso a imagem é gerada com uma chamada REST direta na API do Gemini
  (`POST https://generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent`), feita com `RestClient` em
  [`ImageService`](src/main/java/com/faculdade/estandetiro/model/service/ia/ImageService.java). Nenhuma requisição de
  texto passa por essa chave.
- Os IDs dos modelos foram conferidos na documentação oficial do Gemini e na lista de modelos da própria API
  (novembro de 2026):
  - `gemini-3.1-flash-lite-image` é o **Nano Banana 2 Lite**, versão estável e o mais barato da família Nano Banana,
    cerca de **US$ 0,0336 por imagem** 1K.
  - `gemini-3.5-flash-lite` é um modelo flash-lite com cota gratuita.
- Para trocar um modelo, basta mudar uma linha no `application.properties`.

## Funcionalidades com GenAI

Na tela do estande, em **Gerar com IA**:

1. **Gerar descrição com IA** (texto, chave gratuita). A partir do nome, do tipo e do estilo, a IA escreve uma
   descrição curta do cenário com clima de terror e suspense. O resultado vai para `descricaoGeradaPorIA`.
2. **Gerar prompt de imagem com IA** (texto, chave gratuita). A IA transforma a descrição em um prompt otimizado que
   segue a base abaixo. O resultado vai para `promptImagem`. Depois da resposta, o sistema:
   - remove nomes proibidos (jogo, franquia, personagens), em `FiltroTermosProibidos`;
   - acrescenta de novo as regras obrigatórias se a IA tiver esquecido alguma, em `PromptImagem.garantirRegras`.
3. **Gerar cenário com IA** (imagem, chave faturada). Gera a imagem de fundo em 16:9, salva em
   `assets/cenarios/<id>.png` (servida em `/assets/cenarios/<id>.png`) e guarda o caminho no banco.
   O botão **Regenerar…** só aparece quando o estande já tem imagem e sempre passa por uma tela de confirmação.
4. **Gerar alvos com IA** (imagem, chave faturada), na tela **Alvos**. Gera cada inimigo e a refém de cada tipo
   como personagens originais. O prompt pede fundo magenta liso e não cita jogo nem personagem; o da refém pede uma
   vítima assustada, sem armas e sem olhos brilhando. Depois
   [`RecorteAlvo`](src/main/java/com/faculdade/estandetiro/model/service/ia/RecorteAlvo.java):
   - tira o fundo (inclusive os pedaços presos entre braço e corpo);
   - corta no personagem e reduz o tamanho;
   - põe um contorno claro de papelão com borda escura.

   O recorte vai para `assets/alvos/<id>.png` e o jogo passa a usar essa imagem no lugar do desenho. A imagem
   original fica guardada em `data/alvos-originais` (fora do Git), então **"Refazer recorte" é grátis**: não chama a
   API. A refém e os 4 aldeões da vila já foram gerados assim.

Base do prompt de imagem. Os trechos entre colchetes vêm do tipo e do estilo do estande, em
[`TipoEstande`](src/main/java/com/faculdade/estandetiro/model/entity/TipoEstande.java):

> Cenário de fundo 2D para minigame de estande de tiro, visão em primeira pessoa olhando para o fundo do estande.
> Ambientação [TIPO]: [AMBIENTE], clima de terror e suspense. [BALCÃO] em primeiro plano na parte inferior. Área
> central VAZIA, com [PISO] e alguns trilhos/suportes, onde os alvos vão aparecer; [FUNDO]. Sem personagens, sem
> pessoas, sem alvos, sem armas, sem texto, sem logotipos. Estilo [ESTILO], proporção 16:9.

**O cenário inteiro muda com o tipo, não só o fundo.** A base original da atividade fixava "galpão de madeira rústico"
para todos os tipos, então só o painel pintado ao fundo mudava. Agora cada tipo define o próprio ambiente:

| Tipo | Ambiente ao redor | Balcão | Fundo |
|---|---|---|---|
| Vila rural | celeiro com feno, ferramentas e lampiões | madeira gasta | a vila pelas portas do celeiro |
| Castelo | salão gótico de pedra com arcos, tapeçarias e candelabros | pedra com tampo entalhado | vitral com relâmpagos e portão de ferro |
| Ilha / base militar | bunker de concreto com sacos de areia, holofotes e luz de emergência | chapa de aço com faixas de alerta | parede marcada de tiros e porta blindada |
| Mina | galeria na rocha com vigas, dinamite e lanternas | tábuas sobre caixotes | boca de túnel com vagonete |
| Porto / navio | armazém do cais com redes, barris e lanternas | tábuas de convés sobre barris | navios antigos no mar revolto |

As telas também acompanham o tema: a moldura do cenário e um efeito de ambiente mudam em cada tipo (névoa na vila,
velas e relâmpagos no castelo, holofote e luz de emergência na base, poeira na mina, lanterna balançando no porto).

**O nome "Resident Evil" e nomes de personagens nunca vão no prompt de imagem.** Mesmo que alguém os escreva à mão no
formulário, eles são removidos antes do envio.

## Minigame

Na tela do estande, **▶ Jogar** abre o estande de tiro. A partida dura 60 segundos.

- **Inimigos:** surgem em 3 trilhos (perto, meio e longe), parados ou deslizando de um lado a outro. Cada tipo de
  estande tem o seu grupo: aldeões (vila), cultistas encapuzados (castelo), soldados mascarados (base militar),
  mineiros possuídos (mina) e piratas (porto).
  - Os comuns caem com 1 de dano e valem 100.
  - Os ágeis são mais rápidos e valem 150.
  - Os fortes têm 4 de vida (barra acima da cabeça) e valem 300; alguns carregam **escudo**.
- **Refém:** é a moça com a placa "REFÉM" e o contorno dourado. Ela aparece às vezes sozinha e às vezes colada num
  inimigo. **Se você acertá-la, perde na hora.** Se ela sair ilesa, você ganha +200.
- **Tiro na cabeça:** dobra o dano e vale 50% a mais. Com o rifle, triplica o dano e dobra os pontos.
- **Resultado:** no fim aparecem a nota (S, A, B, C ou D), a precisão e o recorde do estande (guardado no navegador).

| Arma (tipo) | Dano | Pente | Diferencial |
|---|---|---|---|
| Pistola 9mm | 1 | 12 | Precisa e com recarga rápida |
| Revólver Magnum | 4 | 6 | Atravessa escudos e todos os alvos na linha do tiro, inclusive a refém |
| Escopeta | 1 por chumbo (6 chumbos) | 8 | Espalha numa área e derruba grupos |
| Rifle de Precisão | 3 | 5 | Zoom com o botão direito ou Shift; na cabeça, triplo de dano |
| Submetralhadora | 1 | 40 | Automática: segure o clique para atirar em rajada |
| Lança-granadas | 5 | 3 | Explosão em área que ignora escudos |

As armas ficam no banco, na tela **Armas**: dá para mudar dano, pente, cadência e recarga, ou criar outras. O tipo da
arma define o comportamento, que está programado em `static/js/jogo.js`.

**Controles:**
- Clique: atirar.
- `1` a `6` ou rolagem do mouse: trocar de arma.
- `R`: recarregar.
- Botão direito ou `Shift`: zoom do rifle (no celular, use o botão 🔍 Mira).

**Aparência dos alvos:**
- Cada inimigo e a refém usam a **imagem gerada pela IA** quando ela existe (tela **Alvos**). Senão, usam o desenho
  original em SVG (`static/js/alvos.js`), no estilo de recorte de madeira pintado.
- Nas imagens, o acerto é conferido no pixel: tiro na parte transparente passa direto. A cabeça é achada
  automaticamente no topo do recorte.
- A refém com imagem da IA ganha uma placa "REFÉM" acima da cabeça.
- Se o tipo do estande ainda não tem nenhuma imagem da IA, o jogo **usa os alvos já gerados de outro tipo** (hoje, os aldeões e a refém da vila) em vez dos desenhos. A tela do estande avisa e tem o link "gerar os deste tipo". Quando o tipo ganha imagens próprias, passa a usá-las.
- Nada usa imagens ou personagens de outros jogos.

**Som e música:** tudo é sintetizado na hora com a Web Audio API, em
[`static/js/som.js`](src/main/resources/static/js/som.js).
- **Tiro de cada arma:**
  - Pistola: estalo seco.
  - Revólver: estrondo grave com anel metálico.
  - Escopeta: explosão larga, seguida do bombeamento.
  - Rifle: estalo agudo com eco pelo galpão, seguido do ferrolho.
  - Submetralhadora: estalinhos que variam um pouco a cada tiro.
  - Lança-granadas: "tump" oco, e a explosão vem depois.
- **Recarga de cada arma:** cada uma tem a sua sequência de cliques. O revólver, por exemplo, abre o tambor, solta as
  cápsulas, põe 6 balas e fecha.
- **Outros sons:** acerto na madeira, "plim" do tiro na cabeça, bala no escudo, alvo tombando, refém salva e refém
  atingida, e o relógio tiquetaqueando nos últimos 10 segundos.
- **Música:** uma **valsa sinistra de parque de diversões composta para este projeto** (lá menor, 3/4), que acelera
  nos últimos 10 segundos. Não é a música do jogo original.
- Os botões **Efeitos** e **Música** da tela do jogo ligam e desligam cada parte separadamente.
- **Faixas opcionais em arquivo:** se a pasta `audio-local/` (na raiz, **fora do Git**) tiver estes `.mp3`, o jogo os usa;
  sem eles, fica tudo sintetizado:
  - `abertura.mp3`: toca uma vez no primeiro clique/tecla na tela do jogo (o navegador exige um gesto).
  - `menu.mp3`: música da tela inicial, antes e entre as partidas.
  - `partida.mp3`: substitui a valsa durante a partida.
  - `derrota.mp3`: toca ao perder (refém atingida).
  - `grito-1.mp3` a `grito-5.mp3`: um deles, sorteado, quando um inimigo cai.
  - `grito-feminino.mp3`: quando cai uma inimiga.
  - Esses arquivos não são publicados porque os direitos não puderam ser confirmados.

**Modelos das armas:** cada tipo tem um modelo desenhado em SVG original
([`static/js/armas-modelos.js`](src/main/resources/static/js/armas-modelos.js)), mostrado na tela Armas, na barra de armas
e no HUD da partida.

**Visual:** paleta cinza grafite com detalhes em azul bem claro (variáveis em `:root` de `static/css/estilo.css`).

## Controle de custo (chave faturada)

- **Cache:** se o estande já tem imagem, "Gerar cenário" não chama a API. Só o **Regenerar** gera de novo, e ele
  pede confirmação.
- **Limite por execução:** no máximo `MAX_IMAGE_GENERATIONS` imagens por execução do sistema (o padrão é 5). A vaga é
  reservada antes da chamada e devolvida se a API não entregar imagem. Reiniciar o sistema zera o contador.
- **Sem clique duplo:** o botão é bloqueado depois do clique, e o sistema só aceita uma geração por vez para cada
  estande.
- **Sem retentativas automáticas:** cada clique faz no máximo uma requisição.
- **Log:** toda chamada à API de imagem fica na tela **Log de imagens**, com data, estande, modelo, sucesso e erro.
  A tela também mostra um gasto estimado (imagens geradas × preço por imagem).

**Erros tratados, com mensagem amigável na tela:**

| Situação | O que aparece |
|---|---|
| Chave ausente no `.env` | Avisa qual variável falta (o sistema sobe mesmo assim) |
| Chave inválida, expirada ou sem permissão (400 `API_KEY_INVALID`, 401, 403) | "A chave … foi recusada pelo Gemini" |
| Cota estourada (429) | "Cota da chave … estourada (erro 429)" |
| Modelo não encontrado (404) | "Modelo "…" não encontrado", com a dica de onde trocar |
| Serviço instável (5xx) ou sem internet | Pede para tentar de novo |
| A IA não devolveu imagem (bloqueio de segurança) | Mostra o motivo (`finishReason`) |

## Telas

| URL | Tela |
|---|---|
| `/estandes` | Lista de estandes, com miniatura da imagem quando existe |
| `/estandes/novo` e `/estandes/{id}/editar` | Cadastro e edição |
| `/estandes/{id}` | Detalhe, com os botões de IA e o botão Jogar |
| `/estandes/{id}/jogar` | Minigame do estande |
| `/estandes/{id}/regenerar` | Confirmação do Regenerar |
| `/armas`, `/armas/nova`, `/armas/{id}/editar` | Cadastro das armas do minigame |
| `/alvos` e `/alvos/{id}/regenerar` | Refém e inimigos de cada tipo: gerar com IA, regenerar (com confirmação) e refazer o recorte |
| `/logs` | Log das gerações de imagem e gasto estimado |
| `/h2-console` | Console do banco: JDBC URL `jdbc:h2:file:./data/estandes`, usuário `sa`, sem senha |

## Banco de dados

```mermaid
erDiagram
    ESTANDE_TIRO ||--o{ LOG_GERACAO_IMAGEM : "gerações de imagem"
    ALVO_INIMIGO ||--o{ LOG_GERACAO_IMAGEM : "gerações de imagem"
    ALVO_INIMIGO {
        BIGINT id PK
        VARCHAR tipo_estande
        VARCHAR nome
        VARCHAR classe "COMUM, AGIL, FORTE"
        VARCHAR descricao_visual
        INT indice_desenho
        VARCHAR caminho_imagem
        TIMESTAMP data_imagem
    }
    ESTANDE_TIRO {
        BIGINT id PK
        VARCHAR nome
        VARCHAR tipo "VILA_RURAL, CASTELO, BASE_MILITAR, MINA, PORTO"
        VARCHAR estilo_visual "REALISTA, PIXEL_ART, CARTOON"
        VARCHAR descricao_gerada_por_ia
        VARCHAR prompt_imagem
        VARCHAR caminho_imagem
        TIMESTAMP data_criacao
    }
    LOG_GERACAO_IMAGEM {
        BIGINT id PK
        BIGINT estande_tiro_id FK "cenário (nulo se for alvo ou se o estande for excluído)"
        BIGINT alvo_inimigo_id FK "alvo gerado (nulo se for cenário)"
        TIMESTAMP data_hora
        VARCHAR modelo_usado
        BOOLEAN sucesso
        VARCHAR mensagem_erro
    }
    ARMA {
        BIGINT id PK
        VARCHAR nome
        VARCHAR tipo "PISTOLA, REVOLVER, ESCOPETA, RIFLE, SUBMETRALHADORA, LANCA_GRANADAS"
        INT dano
        INT capacidade
        INT cadencia_ms
        INT recarga_ms
        VARCHAR diferencial
        INT ordem
    }
```

Quando um estande é excluído, o log dele continua existindo (com o estande vazio), para não perder o histórico de
gastos. Para zerar os dados, pare o sistema e apague a pasta `data/`.

## Estrutura do projeto

```
estande-tiro-ia/
├── .env.example               modelo do .env (sem valores; este vai para o GitHub)
├── .gitignore                 ignora .env, *.db, data/ e target/
├── pom.xml                    Spring Boot 3.5.16 + Spring AI 1.1.8 (BOM) + H2
├── mvnw, mvnw.cmd, .mvn/      Maven Wrapper
├── assets/cenarios/           cenários gerados (<id>.png)
├── assets/alvos/              inimigos gerados e recortados (<id>.png)
└── src/
    ├── main/java/com/faculdade/estandetiro/
    │   ├── EstandeTiroApplication.java
    │   ├── config/
    │   │   ├── GenAiConfig.java        ChatClient (chave de texto) + ImageService (chave de imagem)
    │   │   ├── WebConfig.java          serve /assets/cenarios/**
    │   │   ├── AjusteColunasEnum.java  migra colunas ENUM antigas do H2 para texto (sem perder dados)
    │   │   └── DadosIniciais.java      estandes, alvos (inimigos e refém) e armas de exemplo
    │   ├── controller/
    │   │   ├── EstandeTiroController   CRUD + ações de IA (descrição, prompt, cenário, regenerar)
    │   │   ├── JogoController          página do minigame (/estandes/{id}/jogar)
    │   │   ├── AlvoController          inimigos: gerar com IA, regenerar, refazer recorte
    │   │   ├── ArmaController          CRUD das armas
    │   │   ├── LogGeracaoController    tela de log
    │   │   ├── StatusIaAdvice          status da IA no topo de todas as telas
    │   │   ├── ErroController          páginas de erro amigáveis
    │   │   ├── HomeController, EstandeForm, ArmaForm
    │   └── model/
    │       ├── entity/       EstandeTiro, LogGeracaoImagem, Arma, AlvoInimigo,
    │       │                 TipoEstande, EstiloVisual, TipoArma, ClasseAlvo
    │       ├── repository/   EstandeTiroRepository, LogGeracaoImagemRepository, ArmaRepository,
    │       │                 AlvoInimigoRepository
    │       └── service/
    │           ├── EstandeTiroService, LogGeracaoImagemService, ArmaService
    │           ├── AlvoInimigoService       alvos gerados pela IA (cache, limite, log, recorte grátis)
    │           ├── ArmazenamentoAlvos       assets/alvos e data/alvos-originais
    │           ├── CenarioService           cache, limite, log e gravação da imagem
    │           ├── LimiteGeracoesImagem     MAX_IMAGE_GENERATIONS por execução
    │           ├── ArmazenamentoCenarios    salva assets/cenarios/<id>.png
    │           └── ia/
    │               ├── IaTextoService       funcionalidades 1 e 2 (Spring AI ChatClient)
    │               ├── ImageService         funcionalidade 3 (RestClient, chave faturada)
    │               ├── PromptImagem         base do prompt + regras obrigatórias
    │               ├── PromptAlvo, RecorteAlvo   prompt dos alvos (inimigos e refém) e recorte do fundo magenta
    │               ├── FiltroTermosProibidos
    │               └── TradutorErrosIa, IaException, TipoErroIa, ImagemGerada
    ├── main/resources/
    │   ├── application.properties          só referências ${...} para as chaves
    │   ├── templates/                      layout, estandes/(lista, form, detalhe, jogo, regenerar),
    │   │                                   armas/(lista, form), alvos/(lista, regenerar), logs/lista, error
    │   └── static/
    │       ├── css/estilo.css, css/jogo.css   visual, minigame e temas de cada tipo
    │       └── js/app.js, js/alvos.js, js/armas-modelos.js, js/jogo.js, js/som.js   telas, desenhos dos alvos,
    │                                                          motor do minigame, sons e música
    └── test/java/...                       34 testes (IA simulada, sem gastar cota)
```

## Segurança das chaves

- As chaves existem **só** no `.env`. Não há chave no código Java, no `application.properties`, no README nem nos
  testes (eles usam valores falsos).
- Antes do primeiro commit, rode `git status` e confira que `.env`, `data/` e `target/` **não** aparecem.
  O comando `git check-ignore -v .env` mostra a regra que ignora o arquivo.
- Se uma chave vazar, revogue-a no Google AI Studio e gere outra.

### Subindo para o GitHub

```powershell
git init
git add .
git status
git commit -m "Estande Sombrio: cenarios de estande de tiro com Spring AI"
git branch -M main
git remote add origin https://github.com/SEU-USUARIO/estande-tiro-ia.git
git push -u origin main
```
