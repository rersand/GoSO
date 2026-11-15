# Roteiro da apresentação — Go 9x9 em rede

Dois roteiros: o **vídeo de 1 minuto** (obrigatório no formulário) e uma **explicação oral**
de 3 minutos, caso o professor peça para apresentar.

---

## 1. Antes de gravar (preparação)

1. Pare qualquer servidor antigo: no IntelliJ, o quadrado vermelho da aba `Servidor`; feche abas de terminal com jogos antigos. Isso libera a porta 5000 e o arquivo `dist\go9x9.jar`.
2. Compile: `scripts\compilar.bat` (deve terminar com "Compilado com sucesso").
3. Abra **três janelas de terminal** lado a lado, com fonte grande (zoom `Ctrl +`), todas na pasta do projeto:
   - Janela **A** (esquerda, menor): servidor
   - Janela **B** (meio): jogador 1, Preto
   - Janela **C** (direita): jogador 2, Branco
4. Dê `cls` nas três para a tela começar limpa.
5. Teste uma vez antes de gravar. Se algo travar, feche tudo e recomece.

Ordem de início (importante): **primeiro o servidor, depois o jogador 1, depois o jogador 2.**

```bat
scripts\servidor.bat        :: janela A
scripts\jogador.bat         :: janela B (vira o jogador 1, Preto, X)
scripts\jogador.bat         :: janela C (vira o jogador 2, Branco, O)
```

---

## 2. Vídeo de 1 minuto

Os comandos abaixo foram testados de ponta a ponta. Digite na janela indicada e dê Enter.
O tempo é uma referência: o vídeo pode ser acelerado ou cortado na edição.

| Tempo | Janela | Digite | O que aparece | O que falar |
|---|---|---|---|---|
| 0:00 | A, B, C | (mostrar as três janelas) | Servidor aguardando; os dois jogadores conectados e o tabuleiro vazio | "Este é o Go 9x9 em Java. Há um servidor e dois jogadores, cada um em um processo separado, falando por sockets." |
| 0:08 | B | `e4` | Preto joga; o tabuleiro muda nas duas janelas; o servidor mostra `recebido <- 1JE4` e `enviado -> JOGADA:...` | "O jogador 1 jogou E4. A mensagem `1JE4` vai ao servidor, que valida e avisa os dois." |
| 0:16 | C | `e5` | Branco joga no centro | "O servidor é o árbitro: ele guarda o tabuleiro oficial e a vez." |
| 0:22 | B | `e5` | `Recusado: Essa posição já está ocupada.` | "Jogada inválida: o servidor recusa e só avisa quem errou." |
| 0:27 | C | `a1` (fora da vez) | `Não é a sua vez.` | "E ninguém joga fora da vez." |
| 0:31 | B, C | B: `d5`, C: `a1`, B: `f5`, C: `a2` | Preto cerca o E5 do Branco | "Agora o Preto cerca a pedra branca do centro." |
| 0:42 | B | `e6` | `capturou 1 pedra(s): [E5]` nas duas janelas | "Captura: a pedra sem liberdades sai do tabuleiro nos dois jogadores." |
| 0:47 | C | `e5` | `Recusado: ... (suicídio).` | "A regra do suicídio também é aplicada." |
| 0:51 | C, B | C: `pass`, B: `pass` | `Dois passes seguidos: contando os pontos...` e o placar: Preto 5,0 × Branco 7,5 | "Dois passes encerram a partida. A pontuação é por área, com komi de 5,5." |
| 0:57 | A | (mostrar o log do servidor) | Todas as mensagens trocadas | "Todo o jogo é feito por mensagens; o estado compartilhado fica no servidor, protegido por lock." |

Resultado final esperado: **Preto 5.0 × Branco 7.5; vence o Branco.**

**Final alternativo (mais curto):** no lugar dos dois `pass`, digite `resign` em uma das janelas.
Aparece "FIM DE JOGO por desistência" e o outro jogador vence.

**Dicas:**
- Ensaie duas vezes: o limite de 1 minuto é apertado.
- Se errar uma jogada no vídeo, o servidor recusa e você só continua; dá até para aproveitar para mostrar o erro.
- Termine a partida com `pass`/`resign` para os três terminais fecharem sozinhos.

---

## 3. Explicação oral (cerca de 3 minutos)

**1. O jogo (20 s)**
"Escolhi o Go 9x9. Dois jogadores alternam colocando pedras; grupos sem liberdades são capturados; a partida termina com dois passes ou desistência, e a pontuação é por área com komi de 5,5."

**2. Arquitetura (40 s)**
"São três processos em Java: um servidor e dois clientes de console. Eles se comunicam por sockets TCP. O servidor é a autoridade: guarda o tabuleiro oficial e valida cada jogada. Cada cliente tem uma cópia local do tabuleiro e da variável `vez`, atualizada só com o que o servidor confirma."

**3. Mensagens (40 s)**
"A notação segue a ideia do `2Cc3` do professor. O cliente envia `1JE5`: jogador 1, jogar, E5; ou `2P` para passar e `1R` para desistir. O servidor responde, para os dois, algo como `JOGADA:1:E5:D5,D6:2`, que diz quem jogou, onde, o que foi capturado e de quem é a próxima vez. Erros como `ERRO:ko` vão só para quem errou."

**4. Exclusão mútua (50 s)**
"No servidor há uma thread por jogador. As duas disputam o mesmo tabuleiro e a mesma vez, então o estado fica na classe `PartidaCompartilhada`, protegido por um `ReentrantLock`: validar, alterar e avisar acontecem dentro do lock. Assim as mensagens chegam aos dois clientes na mesma ordem em que o estado mudou. No cliente, `EstadoLocal` é `synchronized`, porque a thread de rede e a de teclado o usam."

**5. Memória (30 s)**
"O tabuleiro oficial vive só no servidor. Cada jogada é validada numa cópia e só substitui o tabuleiro se for legal, então nunca sobra estado pela metade. Os objetos de mensagem e de posição são imutáveis. Entre processos a comunicação é por mensagens; a memória compartilhada é entre as threads do servidor."

**6. Testes (20 s)**
"São 106 verificações automáticas: regras do Go, protocolo e uma partida completa pela rede com sockets reais."

---

## 4. Se o professor perguntar...

As respostas completas estão no `GUIA_DE_ESTUDO.md` (seção 14). Resumo das mais prováveis:

- **Onde está a exclusão mútua?** No `ReentrantLock` de `PartidaCompartilhada`.
- **Por que o servidor é a autoridade?** Para ter uma única fonte de verdade; os clientes não divergem.
- **Pode dar deadlock?** Não: a ordem de bloqueio é fixa (partida, depois conexão) e `enviar` nunca volta à partida.
- **O que acontece se um jogador fecha a janela?** O servidor detecta, encerra a partida e avisa o outro com `ADVERSARIO_SAIU`.
- **Limitações?** Uma partida por execução do servidor, sem reconexão e sem marcação automática de pedras mortas.

---

## 5. Checklist final de entrega

- [ ] Vídeo gravado (até 1 minuto) e link colocado no formulário.
- [ ] `.zip` **sem executável**: `src`, `test`, `scripts` e os `.md`, sem `build`, `dist`, `out` e `.git`.
- [ ] `.zip` **com executável**: o mesmo, mais `dist\go9x9.jar`.
- [ ] Nomes dos alunos preenchidos no `README.md`.
- [ ] Respostas do formulário (mensagens trocadas e exclusão mútua) copiadas do `README.md`.
- [ ] Testado a partir do `.zip` extraído em outra pasta.
- [ ] Enviado **antes das 23:59 de 15/11/2026**, na turma correta, por apenas um aluno da dupla.
