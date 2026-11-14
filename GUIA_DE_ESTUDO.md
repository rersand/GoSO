# Guia de estudo — Go 9x9 em rede (Java)

Este documento explica **todo o projeto** para estudo: o que o trabalho pede,
os conceitos de Sistemas Operacionais envolvidos, a arquitetura, cada classe,
o fluxo das mensagens, a concorrência, os testes, as limitações e perguntas
que o professor pode fazer. Foi escrito para ser colado em uma conversa nova
com uma IA (veja a seção 15) ou lido sozinho.

> Código: https://github.com/rersand/GoSO — pastas `src/` (código), `test/`
> (testes), `scripts/` (.bat) e `README.md` (como rodar + respostas do formulário).

---

## 1. O trabalho em uma página

**Disciplina:** Sistemas Operacionais (Prof. Maurilio Campano Jr.) — Trabalho 02
do 2º bimestre. **Entrega:** 15/11/2026, 23:59, por formulário do Google, em dupla.

**O que o enunciado pede:**

- Um jogo qualquer para **pelo menos dois usuários**, em **processos separados**.
- Comunicação por **sockets** (no enunciado: C com winsock2/pthreads; Java é permitido).
- **Troca de mensagens** com uma **notação própria** (exemplo do professor no xadrez:
  `2Cc3` = vez do jogador 2, cavalo para c3). O controle do jogo se faz por códigos
  nas mensagens; uma variável inteira `vez` deve existir **nos dois processos**.
- **Memória compartilhada** e **exclusão mútua**.
- Não precisa de interface gráfica bonita.
- Itens avaliados: originalidade, uso da troca de mensagens, uso do gerenciamento de
  memória, técnicas implementadas, criatividade, organização do código, vídeo de
  gameplay de 1 minuto.
- Formulário: nomes, jogo, explicação, linguagem, **como e quais informações são
  trocadas**, **como é feita a exclusão mútua**, código zipado (sem e com executável),
  link do vídeo.

**O que foi construído:** Go (Weiqi/Baduk) em tabuleiro 9x9, em Java 8+, com um
**servidor** (árbitro) e **dois clientes** de console, tabuleiro desenhado em ASCII.

---

## 2. Conceitos de SO que o projeto demonstra

| Conceito | Onde aparece no projeto |
|---|---|
| **Processo** | O servidor e cada cliente são processos (JVMs) distintos, cada um com seu espaço de memória. Eles **não** compartilham variáveis. |
| **Thread** | Dentro do servidor há 1 thread por jogador (`ConexaoJogador`). Dentro de cada cliente há 2 threads (rede e teclado). |
| **Socket TCP** | Canal de comunicação entre processos (`java.net.Socket` / `ServerSocket`), em `localhost:5000` por padrão. |
| **Troca de mensagens** | Linhas de texto: `1JE5` (cliente→servidor) e `JOGADA:1:E5:-:2` (servidor→clientes). |
| **Memória compartilhada** | As threads do servidor compartilham o mesmo objeto `PartidaCompartilhada` (tabuleiro, vez, passes) no mesmo heap. |
| **Condição de corrida (race condition)** | Duas threads mexendo no tabuleiro ao mesmo tempo poderiam deixar o estado inconsistente. |
| **Exclusão mútua** | `ReentrantLock` em `PartidaCompartilhada`; `synchronized` em `ConexaoJogador.enviar` e `EstadoLocal`. |
| **Seção crítica** | Todo trecho entre `trava.lock()` e `trava.unlock()` em `PartidaCompartilhada`. |
| **Deadlock** | Evitado: há uma ordem fixa de aquisição (lock da partida → monitor da conexão) e nunca o contrário. |
| **Sincronização por turno** | A variável `vez` impede que o jogador errado altere o tabuleiro (exclusão mútua lógica). |

**Resumo honesto da "memória compartilhada":** entre *processos* (cliente↔servidor) o
modelo é **troca de mensagens**; entre *threads do servidor* o modelo é **memória
compartilhada com lock**. Os dois conceitos do enunciado aparecem, em camadas diferentes.

---

## 3. Regras do Go implementadas

**Vocabulário:**

- **Interseção:** cada ponto do tabuleiro (9x9 = 81). As pedras ficam nas interseções.
- **Grupo:** pedras da mesma cor ligadas na vertical/horizontal.
- **Liberdade:** interseção vazia adjacente a um grupo.
- **Captura:** quando um grupo fica **sem liberdades**, é removido.

**Regras no código:**

1. Preto joga primeiro; os jogadores alternam.
2. Só se joga em interseção **vazia**.
3. Depois de colocar a pedra, os grupos **adversários** vizinhos sem liberdade são **capturados**.
4. **Suicídio proibido:** se a pedra colocada fica com o grupo sem liberdade **e não capturou nada**, a jogada é recusada. (Se capturou, é legal, porque a captura cria liberdades.)
5. **Ko simples:** é proibido criar uma posição **igual à que existia antes da última ação**. Isso evita ciclos infinitos de captura e recaptura.
6. **Passar (`pass`)**; **dois passes seguidos encerram** a partida. Uma jogada entre os passes zera a contagem.
7. **Desistir (`resign`)** encerra na hora; o outro vence. Pode ser feito mesmo fora da vez.
8. **Pontuação por área** (regras chinesas): pontos = suas pedras no tabuleiro + intersecções vazias cercadas **só** por você. Regiões vazias tocadas pelas duas cores ("neutras") não contam.
9. **Komi 5,5:** pontos extras para o Branco. O ",5" evita empate.

**Notação das casas:** colunas `A B C D E F G H J` (**sem I**, como no Go tradicional),
linhas `1`–`9` com a linha 1 **embaixo**. Internamente `linha` e `coluna` vão de 0 a 8.

**Limitação assumida:** não há marcação automática de "pedras mortas". Os jogadores
devem capturar o que está morto antes de passar.

---

## 4. Arquitetura

```
 Cliente 1 (Preto, X) ─┐                          ┌─ thread jogador-1 ─┐
   processo separado   ├── socket TCP ── Servidor ┤                    ├─ PartidaCompartilhada
 Cliente 2 (Branco, O)─┘   (porta 5000)  (processo)└─ thread jogador-2 ─┘   (lock + Partida)
```

**Três processos em localhost:** um servidor e dois clientes. O **servidor é a
autoridade**: valida tudo e é o único dono do tabuleiro oficial. Os clientes mantêm
uma **cópia local** (tabuleiro, vez, capturas) que só muda quando o servidor confirma.

**Camadas (pacotes `br.go9x9.*`):**

```
modelo     ← tipos básicos, sem regras de jogo nem rede
regras     ← regras do Go, sem rede nem console
protocolo  ← tradução entre objetos e linhas de texto
servidor   ← rede + threads + lock (usa regras e protocolo)
cliente    ← rede + console (usa modelo e protocolo)
```

A dependência vai sempre "para baixo": `regras` não conhece `servidor`; `modelo` não
conhece nada. Isso permite testar as regras sem abrir socket algum.

### Estrutura de arquivos

```
Go9x9/
├── src/br/go9x9/
│   ├── modelo/     Pedra, Posicao, Tabuleiro, ResultadoJogada, MotivoErro
│   ├── regras/     Partida, Regras, Pontuacao, ResultadoFinal
│   ├── protocolo/  Comando, Evento, TipoEvento, MensagemInvalidaException
│   ├── servidor/   Servidor, ConexaoJogador, PartidaCompartilhada, Destinatario
│   └── cliente/    Cliente, EstadoLocal, TabuleiroConsole
├── test/br/go9x9/  Verificador, TestesRegras, TestesProtocolo,
│                   TestePartidaEmRede, ExecutarTestes
├── scripts/        ambiente.bat, compilar.bat, servidor.bat, jogador.bat, testes.bat
├── README.md       como rodar + respostas do formulário
└── GUIA_DE_ESTUDO.md  (este arquivo)
```

---

## 5. Protocolo de mensagens (a "notação própria")

TCP é um fluxo de bytes sem fronteiras de mensagem, então o protocolo é **uma mensagem
por linha** (`\n`), em UTF-8. `BufferedReader.readLine()` devolve exatamente uma mensagem.

### 5.1 Cliente → servidor (`Comando`)

Formato compacto, no estilo do `2Cc3` do professor: `<jogador><ação>[<posição>]`

| Linha | Significado |
|---|---|
| `1JE5` | jogador 1 **J**oga em E5 |
| `2P` | jogador 2 **P**assa |
| `1R` | jogador 1 desiste (**R**esign) |

O primeiro dígito funciona como a variável **`vez`** do enunciado: o servidor só aceita
se o número bate com a conexão e com a vez atual. Letras minúsculas e espaços nas pontas
são aceitos (`2ja1` = `2JA1`).

### 5.2 Servidor → clientes (`Evento`)

Formato `TIPO:campo:campo:...`

| Linha | Quando | Para quem |
|---|---|---|
| `JOGADOR:2` | ao conectar: "você é o jogador 2" | só quem conectou |
| `ESPERA` | primeiro jogador conectado, aguardando o outro | só ele |
| `INICIO:1` | os dois conectaram; vez do jogador 1 | **ambos** |
| `JOGADA:1:E5:D5,D6:2` | jogada aceita: quem jogou, onde, capturadas (`-` se nenhuma), próxima vez | **ambos** |
| `PASSE:1:2` | alguém passou: quem passou, próxima vez | **ambos** |
| `FIM:PASSES:45.0:41.5:1` | fim por passes: pontos do Preto, do Branco (com komi), vencedor | **ambos** |
| `FIM:DESISTENCIA:-:-:2` | fim por desistência (ou abandono); vencedor | **ambos** |
| `ERRO:ko` | ação recusada | **só** quem errou |
| `ADVERSARIO_SAIU` | o outro desconectou no meio da partida | o que ficou |

Códigos de erro: `ocupada`, `suicidio`, `ko`, `nao_e_sua_vez`, `partida_encerrada`,
`jogador_invalido`, `aguardando_adversario`, `mensagem_invalida`.

### 5.3 Exemplo de sequência

```
[P1 conecta]  S→P1: JOGADOR:1        S→P1: ESPERA
[P2 conecta]  S→P2: JOGADOR:2        S→P1,P2: INICIO:1
P1→S: 1JE5                           S→P1,P2: JOGADA:1:E5:-:2
P2→S: 2JE5   (ocupada)               S→P2: ERRO:ocupada
P2→S: 2JD5                           S→P1,P2: JOGADA:2:D5:-:1
P1→S: 1P                             S→P1,P2: PASSE:1:2
P2→S: 2P                             S→P1,P2: PASSE:2:1   e depois   FIM:PASSES:1.0:6.5:2
```

---

## 6. Fluxo completo de uma jogada (passo a passo)

Jogador 1 digita `e5` quando é a vez dele:

1. **Cliente, thread de teclado** (`Cliente.tratarComandoDigitado`): converte para `Posicao`, confere com a variável local `vez` (`EstadoLocal.ehMinhaVez()`). Se não for a vez, avisa e **nem envia**. Se for, envia `1JE5\n`.
2. **Servidor, thread `jogador-1`** (`ConexaoJogador.run`): `readLine()` devolve `1JE5`. Chama `Comando.analisar` e depois `PartidaCompartilhada.processar(1, comando)`.
3. **`processar`** pega o lock e confere, em ordem: partida iniciada? número do comando = número da conexão? Então decide por `JOGAR`, `PASSAR` ou `DESISTIR`.
4. **`Partida.jogar`** (regras puras): vez, posição ocupada, captura, suicídio, ko. Se algo falhar, devolve `ResultadoJogada.falha(motivo)` **sem alterar nada**.
5. Se for válida, o servidor **transmite** `JOGADA:1:E5:<capturadas>:2` aos dois jogadores (ainda dentro do lock) e solta o lock.
6. **Cada cliente, thread de rede** (`receberEventos` → `tratarEvento` → `tratarJogada`): atualiza `EstadoLocal` (coloca a pedra, remove capturadas, soma capturas, troca `vez`), redesenha o tabuleiro e mostra de quem é a vez.

Por que o cliente só atualiza ao receber o `JOGADA`, e não ao digitar? Porque o servidor
pode recusar. Assim as duas cópias nunca divergem do oficial.

---

## 7. As classes, uma a uma

### 7.1 Pacote `modelo`

- **`Pedra`** (`enum`): `VAZIA(0,'.')`, `PRETA(1,'X')`, `BRANCA(2,'O')`. Cada constante tem número do jogador, símbolo e nome. `oponente()` troca a cor; `doJogador(n)` converte 1/2 em pedra.
- **`Posicao`** (classe `final` imutável): `linha` e `coluna` (0–8). `analisar("E5")` converte texto → posição (lança `IllegalArgumentException` se inválido); `toString()` faz o inverso. Tem `equals`/`hashCode` para funcionar em `HashSet`.
- **`Tabuleiro`** (mutável): matriz `Pedra[9][9]`. `obter`, `colocar`, `remover`, `vizinhas` (até 4 adjacentes), `copiar` (cópia profunda) e `equals`/`hashCode` via `Arrays.deepEquals`/`deepHashCode` (usado para o ko).
- **`ResultadoJogada`** (imutável): ou **sucesso** (com lista de capturadas) ou **falha** (com `MotivoErro`). Evita usar exceções para erros esperados do jogo.
- **`MotivoErro`** (`enum`): cada erro tem `codigo` (vai na rede) e `descricao` (mostrada ao usuário em português).

### 7.2 Pacote `regras`

- **`Regras`**: funções puras.
  - `grupoDe(tabuleiro, origem)`: **busca em largura** (BFS) com `ArrayDeque` e `HashSet` para achar todas as casas conectadas com o **mesmo conteúdo** da origem. Funciona para um grupo de pedras e também para uma **região vazia** (reaproveitado na pontuação).
  - `liberdadesDe(tabuleiro, grupo)`: conjunto de casas vazias adjacentes ao grupo.
- **`Partida`**: o "motor" do jogo. **Não conhece rede nem threads** e **não é thread-safe** (a thread-safety é responsabilidade de quem a usa).
  - Estado: `tabuleiro`, `posicaoAnterior` (para o ko), `vez` (`Pedra`), `passesSeguidos`, `encerrada`, `desistente`.
  - `jogar(jogador, posicao)`, `passar(jogador)`, `desistir(jogador)`, `getResultadoFinal()`, `getVez()`, `isEncerrada()`, `getTabuleiro()` (devolve **cópia**).
- **`Pontuacao`**: `calcular(tabuleiro)` soma pedras + regiões vazias de dono único, e soma `KOMI = 5.5` ao Branco.
- **`ResultadoFinal`** (imutável): pontos, vencedor e se foi por desistência.

### 7.3 Pacote `protocolo`

- **`Comando`**: objeto do que o cliente envia. Tem `enum Acao {JOGAR('J'), PASSAR('P'), DESISTIR('R')}`. `analisar(linha)` valida (dígito 1/2, ação conhecida, posição válida, sem lixo no fim) e lança `MensagemInvalidaException`. `paraLinha()` serializa.
- **`Evento`** + **`TipoEvento`**: objeto do que o servidor envia. Fábricas (`jogada(...)`, `passe(...)`, `fim(...)`, `erro(...)` etc.) montam cada mensagem; `analisar(linha)` lê; `inteiro(i)`, `posicao(i)`, `posicoes(i)`, `texto(i)` leem os campos com validação.
- **`MensagemInvalidaException`**: exceção **checada**; obriga quem lê da rede a tratar linha malformada.

### 7.4 Pacote `servidor`

- **`Servidor`**: `main` cria o `ServerSocket`; `executar()` aceita **exatamente 2** conexões (a 1ª vira Preto, a 2ª Branco), cria uma `Thread` para cada `ConexaoJogador` e faz `join()` nelas. Quando as duas terminam, o servidor encerra. Recebe o `ServerSocket` pelo construtor para ser testável (os testes usam porta 0).
- **`ConexaoJogador`** (`Runnable` + `Destinatario`): a thread de um jogador. Loop: `readLine` → `Comando.analisar` → `partida.processar`. `enviar(Evento)` é `synchronized` e escreve no socket. Imprime um log de tudo que recebe/envia (útil no vídeo).
- **`PartidaCompartilhada`**: **o coração da exclusão mútua**. Guarda a `Partida`, o mapa de jogadores (`TreeMap<Integer, Destinatario>`) e a flag `iniciada`, tudo protegido por `ReentrantLock`. Métodos públicos: `registrar`, `processar`, `responderErro`, `desconectar`, `isEncerrada`.
- **`Destinatario`** (interface): `enviar(Evento)`. Faz `PartidaCompartilhada` depender de uma abstração, não do socket.

### 7.5 Pacote `cliente`

- **`Cliente`**: `main` conecta e roda **duas threads**: a principal recebe eventos do servidor; a de teclado (`daemon`) lê comandos digitados. Comandos: `E5`, `pass`, `resign`, `tabuleiro`, `ajuda`, `sair`.
- **`EstadoLocal`**: cópia local do estado (tabuleiro, `meuJogador`, `vez`, iniciada/encerrada, capturas, última jogada, passes seguidos). **Todos os métodos são `synchronized`**, porque as duas threads do cliente o usam.
- **`TabuleiroConsole`**: desenha o ASCII. `X` Preto, `O` Branco, `.` vazio, `+` pontos de referência (hoshi: C3, G3, C7, G7, E5), e `[X]` marca a última jogada.

---

## 8. Concorrência em detalhe (parte mais cobrada)

### 8.1 Quais threads existem

| Processo | Threads |
|---|---|
| Servidor | `main` (aceita conexões e faz `join`), `jogador-1`, `jogador-2` |
| Cliente (cada um) | `main` (recebe da rede), `teclado` (daemon) |

### 8.2 O que seria uma condição de corrida

As threads `jogador-1` e `jogador-2` leem e alteram o **mesmo** tabuleiro e a mesma
`vez`. Sem lock, por exemplo, enquanto uma thread valida uma jogada, a outra poderia
passar a vez ou desistir; ou as duas poderiam ler "vez = 1" e ambas jogarem; ou uma
thread leria um tabuleiro pela metade durante a remoção de capturas.

### 8.3 Como o lock resolve

`PartidaCompartilhada` toma `trava.lock()` em **todos** os métodos públicos que tocam
no estado e solta em `finally`. Assim, só uma thread por vez executa "validar + alterar
+ avisar". Cada ação (jogar/passar/desistir) é **atômica** do ponto de vista das demais.

### 8.4 Por que `ReentrantLock` (reentrante)?

`processar` já está com o lock e chama `tratarJogada`, que pode chamar `responderErro`,
que **toma o lock de novo**. A mesma thread pode readquirir o lock que já possui
(reentrância); um lock não-reentrante travaria a si mesmo. (`synchronized` também é
reentrante; o `ReentrantLock` foi escolhido pela clareza do `lock()/unlock()` explícito
e por permitir recursos como `tryLock`, não por ser obrigatório.)

### 8.5 Por que o envio aos clientes fica **dentro** do lock?

Para garantir **ordem**. Se a mensagem fosse enviada depois de soltar o lock, duas
jogadas seguidas poderiam chegar a um cliente em ordem trocada, e as cópias locais
divergiriam. Dentro do lock, ambos os clientes recebem os eventos na mesma ordem em que
o estado mudou. Custo: se um cliente estiver lento, o lock fica ocupado um pouco mais
(aceitável: são mensagens minúsculas de um jogo por turnos).

### 8.6 Segunda camada: `synchronized` em `ConexaoJogador.enviar`

Quem escreve no socket do jogador 2 pode ser a thread do jogador 1 (ao transmitir). O
`synchronized` evita duas threads escrevendo no mesmo `BufferedWriter` ao mesmo tempo
(mensagens misturadas). Hoje todos os envios já ocorrem sob o lock da partida, então é
uma **defesa em profundidade**.

### 8.7 Por que não há deadlock

Deadlock exige aquisição **circular** de locks. Aqui a ordem é sempre
**lock da partida → monitor da conexão** (`enviar`). `enviar` **nunca** chama a
partida de volta. Sem ciclo, sem deadlock.

### 8.8 No cliente

`EstadoLocal` é `synchronized` porque a thread de rede escreve (aplica jogadas) e a de
teclado lê (`ehMinhaVez`). `Cliente.enviar` usa `synchronized(this)` ao escrever. A flag
`saindo` é `volatile` (visibilidade entre threads, sem precisar de exclusão).

### 8.9 Encerramento limpo

- Fim por passes/desistência: o servidor envia `FIM`; as `ConexaoJogador` saem do loop (`isEncerrada()`), fecham os sockets (try-with-resources) e `Servidor.executar` termina no `join`.
- O cliente, ao receber `FIM`, sai do loop de rede. Como a thread de teclado é **daemon**, a JVM termina mesmo com ela bloqueada em `readLine`.
- Cliente que cai: `readLine` devolve `null` ou lança `IOException` → `finally` chama `desconectar`, que, se a partida não tinha acabado, a encerra e avisa o outro com `ADVERSARIO_SAIU`.

---

## 9. Algoritmos principais

### 9.1 `Partida.jogar` (ordem exata)

1. `verificarVez`: jogador 1/2? partida não encerrada? é a vez dele?
2. A casa está vazia? Senão, `OCUPADA`.
3. Cria **cópia** do tabuleiro e coloca a pedra nela.
4. `capturarVizinhosSemLiberdade`: para cada vizinha com pedra adversária, calcula o grupo; se não tem liberdade, remove o grupo todo (na cópia) e registra as capturadas. A checagem `obter(v) != adversaria` pula casas já removidas na mesma jogada.
5. Se **nada foi capturado** e o grupo da pedra nova não tem liberdades → `SUICIDIO`.
6. Se a cópia é **igual** a `posicaoAnterior` → `KO`.
7. **Commit:** `posicaoAnterior = tabuleiro` (o antigo), `tabuleiro = cópia`, zera passes, troca a vez, devolve sucesso.

**Detalhe de projeto:** a validação trabalha na **cópia**; o tabuleiro oficial só é trocado
no passo 7. Uma jogada recusada não deixa estado pela metade. Como o objeto `tabuleiro`
nunca é alterado "no lugar" (sempre substituído), guardar a referência antiga em
`posicaoAnterior` é seguro.

### 9.2 Ko — exemplo

```
     C D E F          Preto joga E5 e captura o D5 branco.
  6  . X O .          Branco jogar D5 agora capturaria E5 e recriaria
  5  X O . O          exatamente a posição de antes => KO (proibido).
  4  . X O .          Depois de uma jogada em outro lugar, a posição já é
                      diferente e a recaptura é liberada.
```

### 9.3 Pontuação (área)

Percorre as 81 casas. Pedra: soma 1 para a cor. Casa vazia ainda não visitada: acha a
**região vazia** com `grupoDe`, olha as cores nas bordas; só Preto → pontos do Preto;
só Branco → pontos do Branco; as duas (ou nenhuma) → neutra. Branco recebe +5,5.
Vencedor: `pontosPreto > pontosBranco ? 1 : 2` (empate impossível por causa do ",5").

---

## 10. Decisões de projeto e boas práticas Java

- **Servidor autoritativo:** simplifica a consistência; clientes não "confiam" um no outro.
- **Classes `final` imutáveis (`Posicao`, `Comando`, `Evento`, `ResultadoJogada`, `ResultadoFinal`):** campos `private final`, sem setters. Podem ser compartilhadas entre threads sem lock e servir de chave em `HashSet`. (Na primeira ideia seriam `record`, mas `record` exige Java 16+ e o Java instalado era o 8; classes `final` mantêm a compatibilidade.)
- **`enum`:** conjunto fechado de valores (`Pedra`, `TipoEvento`, `MotivoErro`, `Comando.Acao`); o compilador impede valores inválidos e permite `switch` seguro.
- **Compatibilidade:** compilado com `--release 8`, sem `var`, `List.of`, `record`, etc.
- **`try-with-resources`** em sockets/readers/writers (fecham sozinhos, mesmo com exceção).
- **Sem `catch` vazio**; erros são tratados ou registrados.
- **Defesa de cópia:** `Partida.getTabuleiro()` devolve cópia; `ResultadoJogada` copia a lista e a torna imutável.
- **Constantes nomeadas:** `Tabuleiro.TAMANHO`, `Pontuacao.KOMI`, `PORTA_PADRAO`.
- **Classes utilitárias** com construtor `private` (`Regras`, `Pontuacao`, `TabuleiroConsole`).
- **Separação de responsabilidades:** regras sem rede; rede sem regras; desenho separado do estado.
- **Abstração `Destinatario`:** a partida não conhece `Socket`.
- **Exceções checadas só onde se espera tratar** (`MensagemInvalidaException`); erros normais de jogo usam `ResultadoJogada`, não exceções.
- **Codificação:** UTF-8 explícito nos sockets e `-encoding UTF-8` no `javac`.

---

## 11. Testes

106 verificações, rodadas com `scripts\testes.bat` (ou executando `br.go9x9.ExecutarTestes`).
Não há JUnit na máquina, então existe um mini-verificador (`Verificador`).

- **`TestesRegras`:** conversão de posições (inclui rejeitar `I5`, `A0`, `A10`); captura simples e de grupo de 2 pedras; suicídio recusado e a exceção "suicídio que captura"; posição ocupada, jogada fora da vez, jogador 3; **ko** (recusa na hora e liberação depois de uma jogada em outro lugar); passes, desistência, fim; pontuação (vazio, uma pedra = 81, tabuleiro dividido 45 × 41,5, região neutra).
- **`TestesProtocolo`:** ida e volta de comandos e eventos; vários formatos inválidos rejeitados.
- **`TestePartidaEmRede`:** sobe o servidor de verdade (porta 0) e dois sockets crus; confere a sequência real de mensagens, erros que só um jogador recebe (`nao_e_sua_vez`, `jogador_invalido`, `mensagem_invalida`, `ocupada`), dois passes e o `FIM`, e também a **desconexão do adversário** (`ADVERSARIO_SAIU`). Também confere que o servidor termina sozinho.

---

## 12. Como rodar

```bat
scripts\compilar.bat    :: gera dist\go9x9.jar
scripts\servidor.bat    :: janela 1
scripts\jogador.bat     :: janela 2 (Preto)
scripts\jogador.bat     :: janela 3 (Branco)
scripts\testes.bat
```

Entre computadores: `scripts\jogador.bat <ip-do-servidor> 5000`.
No IntelliJ: rodar `Servidor` e duas vezes `Cliente` (ativar "Allow multiple instances").
`scripts\ambiente.bat` procura o JDK no PATH ou em `%USERPROFILE%\.jdks`. As saídas dos
scripts vão para `build\` e `dist\`; o IntelliJ usa `out\`.

---

## 13. Limitações conhecidas (seja honesto com o professor)

- **Uma partida por execução do servidor:** ao terminar, o servidor encerra.
- **Sem reconexão:** se um jogador cai, a partida acaba.
- **Sem marcação automática de pedras mortas:** é preciso capturá-las antes de passar.
- **Ko simples**, não *superko* (ciclos longos de repetição não são detectados).
- **Sem autenticação nem criptografia** (é um trabalho local de aula).
- **Sem timeout** de leitura: um cliente que fica mudo não é desconectado.
- **Se o jogador 1 sair antes do 2 entrar**, o servidor ainda espera o 2º `accept`; ao chegar, o 2º jogador é avisado de `ADVERSARIO_SAIU`.
- **Console:** acentos dependem da codificação do terminal; os símbolos são ASCII (`X`/`O`) justamente por isso.
- O desenho do tabuleiro é só ASCII (não há interface gráfica).

**Possíveis melhorias:** vários jogos simultâneos (uma `PartidaCompartilhada` por sala), relógio de jogo, marcação de pedras mortas, superko, interface Swing, bot de IA, persistência de partidas (SGF).

---

## 14. Perguntas que o professor pode fazer (com respostas)

**Onde está a memória compartilhada?**
No servidor: o objeto `PartidaCompartilhada` (tabuleiro, vez, passes, jogadores) é acessado pelas duas threads de jogador no mesmo heap. Entre os processos cliente/servidor a comunicação é por mensagens.

**Onde está a exclusão mútua e por quê?**
`ReentrantLock` em `PartidaCompartilhada`: todas as operações que lêem/alteram o estado são seções críticas. Sem isso, duas threads poderiam alterar o tabuleiro/vez ao mesmo tempo (condição de corrida).

**O que é uma seção crítica no seu código?**
O trecho entre `trava.lock()` e `trava.unlock()` em `registrar`, `processar`, `responderErro`, `desconectar` e `isEncerrada`.

**Por que o servidor é a autoridade?**
Para ter uma única fonte de verdade. Os clientes só atualizam suas cópias com o que o servidor confirma, então não divergem e não conseguem trapacear.

**Por que enviar as respostas dentro do lock?**
Para os dois clientes receberem os eventos na mesma ordem em que o estado mudou.

**Pode ocorrer deadlock?**
Não: só há dois níveis de bloqueio com ordem fixa (partida → conexão) e o `enviar` nunca volta à partida.

**Como a vez é controlada?**
Cada comando carrega o número do jogador; o servidor compara com a conexão e com `Partida.vez`. O cliente também guarda `vez` (confirmada em `INICIO`, `JOGADA`, `PASSE`) e já barra jogadas fora da vez antes de enviar. Mas quem decide é o servidor.

**O que acontece se alguém digita fora da vez?**
O cliente avisa localmente. Se mesmo assim chegar ao servidor, ele responde `ERRO:nao_e_sua_vez` só para quem errou.

**Como o servidor sabe onde termina uma mensagem?**
Cada mensagem é uma linha terminada em `\n`; `readLine()` entrega uma por vez.

**Por que TCP e não UDP?**
TCP garante entrega e ordem, essenciais para manter o tabuleiro sincronizado.

**Qual a diferença entre processo e thread aqui?**
Servidor e clientes são processos (memória separada, falam por socket). As threads dentro do servidor compartilham memória e por isso precisam de lock.

**O que acontece se um jogador fecha o programa?**
A thread dele recebe `null`/`IOException`, o `finally` chama `desconectar`, que encerra a partida e envia `ADVERSARIO_SAIU` ao outro.

**Por que o cliente tem duas threads?**
`readLine()` do teclado e `readLine()` do socket bloqueiam; sem duas threads o cliente não conseguiria receber a jogada do adversário enquanto espera digitar.

**Por que a thread de teclado é daemon?**
Para a JVM poder terminar quando o jogo acaba, mesmo com ela ainda bloqueada lendo o teclado.

**Como a captura é detectada?**
Depois de colocar a pedra (numa cópia), para cada vizinha adversária calcula-se o grupo por BFS e suas liberdades; sem liberdades, o grupo é removido.

**Como o ko é detectado?**
Guarda-se o tabuleiro de antes da última ação; se a nova jogada resultaria num tabuleiro **igual** (`equals` profundo), é proibida.

**Por que objetos imutáveis?**
Podem ser compartilhados entre threads sem sincronização e usados com segurança em `HashSet`.

**Por que o enum `Pedra` e não `int`/`char`?**
Tipo seguro: o compilador só aceita os valores válidos e o código fica mais legível.

---

## 15. Glossário rápido

- **Autoridade (servidor autoritativo):** quem decide o estado oficial do jogo.
- **BFS:** busca em largura; visita vizinhos em camadas com uma fila.
- **Daemon (thread):** thread de segundo plano que não impede a JVM de terminar.
- **Deadlock:** threads esperando umas pelas outras para sempre.
- **Hoshi:** pontos de referência marcados no tabuleiro (`+`).
- **Ko:** regra que impede repetir a posição imediatamente anterior.
- **Komi:** pontos dados ao Branco por jogar em segundo (5,5).
- **Liberdade:** casa vazia adjacente a um grupo.
- **Lock reentrante:** lock que a própria thread dona pode adquirir de novo.
- **Race condition:** resultado depende da ordem imprevisível de execução de threads.
- **Seção crítica:** trecho de código que acessa estado compartilhado e exige exclusão mútua.
- **Socket:** ponto final de comunicação entre processos pela rede.

---

## 16. Como usar este guia em outra conversa

Cole o conteúdo deste arquivo (ou o link do repositório) e peça, por exemplo:

> "Li o guia de estudo do meu trabalho de Sistemas Operacionais (Go 9x9 em Java com
> sockets e exclusão mútua). Quero me preparar para explicar o projeto ao professor.
> Faça perguntas orais uma de cada vez sobre sockets, threads, exclusão mútua e as
> regras do Go, corrija minhas respostas e explique o que eu errar."

Outros pedidos úteis: "explique `PartidaCompartilhada` linha por linha", "simule
uma condição de corrida se eu tirar o lock", "monte um roteiro de apresentação de 3
minutos", "o que mudaria para suportar várias partidas ao mesmo tempo?".
