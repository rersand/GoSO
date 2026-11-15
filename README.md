
#André Ruan da Silva    - RA: 250008214-2

#Gabriel H. Maximiliano - RA: xxxxxxxxxxx

# Go 9x9 em rede (Java)

Trabalho 02 de Sistemas Operacionais: jogo para dois usuários, em processos
separados, com **troca de mensagens por socket** e **estado compartilhado com
exclusão mútua**.

- **Linguagem:** Java (código compatível com Java 8 ou superior, sem bibliotecas externas)
- **Jogo:** Go (Weiqi/Baduk) em tabuleiro 9x9, com tabuleiro desenhado em ASCII no console
- **Alunos:** _(preencher)_

## Como jogar

Precisa do Java instalado (8 ou mais novo). Em três janelas de terminal:

```bat
scripts\compilar.bat      :: uma vez (gera dist\go9x9.jar)
scripts\servidor.bat      :: janela 1: servidor (porta 5000)
scripts\jogador.bat       :: janela 2: jogador 1 (Preto, X)
scripts\jogador.bat       :: janela 3: jogador 2 (Branco, O)
```

Para jogar em dois computadores: `scripts\jogador.bat <ip-do-servidor> 5000`.

No IntelliJ: abra a pasta do projeto e execute, nesta ordem, `br.go9x9.servidor.Servidor`
e duas vezes `br.go9x9.cliente.Cliente` (marque "Allow multiple instances" na configuração).

Comandos do jogador:

| Digite | Efeito |
|---|---|
| `E5` | joga uma pedra em E5 (colunas A-J, sem I; linhas 1-9) |
| `pass` | passa a vez; dois passes seguidos encerram a partida |
| `resign` | desiste |
| `tabuleiro` / `ajuda` / `sair` | redesenha o tabuleiro / mostra comandos / fecha |

```
    A  B  C  D  E  F  G  H  J
 9  .  .  .  .  .  .  .  .  .  9
 8  .  .  .  .  .  .  .  .  .  8
 7  .  .  +  .  .  .  +  .  .  7
 6  .  .  .  .  .  .  .  .  .  6
 5  .  .  . [O] X  .  .  .  .  5     [ ] = última jogada
 4  .  .  .  .  .  .  .  .  .  4     X = Preto, O = Branco
 3  .  .  +  .  .  .  +  .  .  3
 2  .  .  .  .  .  .  .  .  .  2
 1  .  .  .  .  .  .  .  .  .  1
    A  B  C  D  E  F  G  H  J
```

## Regras implementadas

Pedra só em interseção vazia; Preto joga primeiro; grupos sem liberdades são
capturados; **suicídio proibido** (exceto se capturar); **ko simples** (não pode
recriar a posição anterior); `pass` e `resign`; fim com dois passes seguidos.
Pontuação por **área** (pedras + território cercado) com **komi 5,5** para o
Branco. Pedras mortas devem ser capturadas antes de passar (não há marcação
automática de pedras mortas).

## Arquitetura

```
 Jogador 1 (Preto) ─┐                      ┌─ thread jogador-1 ─┐
                    ├── socket TCP ── Servidor                   ├─ PartidaCompartilhada (lock)
 Jogador 2 (Branco)─┘                      └─ thread jogador-2 ─┘      tabuleiro + vez
```

| Pacote | Conteúdo |
|---|---|
| `modelo` | `Pedra` (enum), `Posicao`, `Tabuleiro`, `ResultadoJogada`, `MotivoErro` |
| `regras` | `Partida` (regras do jogo), `Regras` (grupos/liberdades), `Pontuacao`, `ResultadoFinal` |
| `protocolo` | `Comando` (cliente → servidor), `Evento` (servidor → clientes), `TipoEvento` |
| `servidor` | `Servidor`, `ConexaoJogador` (uma thread por jogador), `PartidaCompartilhada` (lock) |
| `cliente` | `Cliente` (2 threads), `EstadoLocal` (cópia local), `TabuleiroConsole` (desenho ASCII) |

## Respostas para o formulário

### Como e quais informações são trocadas entre os processos?

Os processos se comunicam por **sockets TCP** (`java.net`), uma mensagem por
linha de texto. O servidor é a autoridade do jogo; cada cliente mantém uma
**cópia local** do tabuleiro e da variável `vez`, atualizada apenas com o que o
servidor confirma.

**Cliente → servidor** (notação `<jogador><ação>[<posição>]`, no estilo do `2Cc3` do enunciado):

| Mensagem | Significado |
|---|---|
| `1JE5` | jogador 1 joga em E5 |
| `2P` | jogador 2 passa |
| `1R` | jogador 1 desiste |

**Servidor → clientes** (`TIPO:campo:campo...`):

| Mensagem | Significado |
|---|---|
| `JOGADOR:2` | informa ao cliente qual jogador ele é |
| `ESPERA` | aguardando o adversário |
| `INICIO:1` | a partida começou; a vez é do jogador 1 |
| `JOGADA:1:E5:D5,D6:2` | jogador 1 jogou em E5, capturou D5 e D6, próxima vez do jogador 2 (`-` = sem capturas) |
| `PASSE:1:2` | jogador 1 passou; próxima vez do 2 |
| `FIM:PASSES:45.0:41.5:1` | fim por passes: pontos do Preto, do Branco (com komi) e o vencedor (`FIM:DESISTENCIA:-:-:2` se alguém desistir) |
| `ERRO:ko` | ação recusada (`ocupada`, `suicidio`, `ko`, `nao_e_sua_vez`, ...); só quem errou recebe |
| `ADVERSARIO_SAIU` | o outro jogador desconectou |

O número do jogador no início do comando funciona como a variável `vez` do
enunciado: o servidor só aceita a jogada se o número bate com a conexão **e**
com a vez atual. O servidor responde `JOGADA`/`PASSE`/`FIM` para **os dois**
clientes, que então atualizam suas cópias locais (tabuleiro, capturas e `vez`).

### Como é feito o gerenciamento da exclusão mútua?

- Cada jogador é atendido por uma **thread própria** no servidor
  (`ConexaoJogador`), então duas threads podem tentar agir ao mesmo tempo.
- O estado compartilhado (tabuleiro, vez, contagem de passes, situação de
  fim de jogo, lista de conexões) fica em `PartidaCompartilhada`, protegido por
  um **`ReentrantLock`**: qualquer leitura ou alteração acontece entre
  `lock()` e `unlock()` (em `try/finally`).
- O envio das respostas também ocorre dentro do lock, garantindo que os dois
  clientes recebam os eventos **na mesma ordem** em que o estado mudou.
- Cada `ConexaoJogador.enviar` é `synchronized`, pois a thread do adversário
  também escreve no socket dele.
- No cliente, `EstadoLocal` tem métodos `synchronized`, porque é usado pela
  thread de teclado e pela thread de rede.
- A **vez** é, além disso, uma exclusão mútua lógica: só o jogador da vez pode
  alterar o tabuleiro; o servidor recusa as demais tentativas.

### Gerenciamento de memória

O **tabuleiro compartilhado** vive apenas no servidor e é a única fonte de
verdade. A validação de cada jogada trabalha numa **cópia** do tabuleiro, que só
substitui o oficial se a jogada for legal (sem estado parcial se for recusada).
A cópia do estado anterior é guardada para detectar o ko. Objetos de valor
(`Posicao`, `Comando`, `Evento`) são imutáveis e podem ser compartilhados
entre threads sem lock. Os clientes mantêm cópias locais, atualizadas só pelas
mensagens do servidor.

## Testes

```bat
scripts\testes.bat
```

106 verificações automatizadas: regras (captura, suicídio, ko, passes,
pontuação), protocolo e uma **partida completa pela rede** com servidor e dois
sockets reais (inclusive erros e desconexão do adversário).

