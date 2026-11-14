package br.go9x9.cliente;

import br.go9x9.modelo.MotivoErro;
import br.go9x9.modelo.Pedra;
import br.go9x9.modelo.Posicao;
import br.go9x9.protocolo.Comando;
import br.go9x9.protocolo.Evento;
import br.go9x9.protocolo.MensagemInvalidaException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Cliente do Go 9x9 para console.
 *
 * <p>Usa duas threads: a principal recebe os eventos do servidor e atualiza o
 * {@link EstadoLocal}; a de teclado lê os comandos digitados e os envia.</p>
 *
 * <p>Uso: {@code java br.go9x9.cliente.Cliente [host] [porta]}
 * (padrão localhost:5000).</p>
 */
public final class Cliente {

    private static final String HOST_PADRAO = "localhost";
    private static final int PORTA_PADRAO = 5000;

    private final String host;
    private final int porta;
    private final EstadoLocal estado = new EstadoLocal();
    private volatile boolean saindo;
    private Socket socket;
    private BufferedWriter saida;

    private Cliente(String host, int porta) {
        this.host = host;
        this.porta = porta;
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : HOST_PADRAO;
        int porta = PORTA_PADRAO;
        if (args.length > 1) {
            try {
                porta = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Porta inválida: " + args[1]);
                System.exit(1);
            }
        }
        try {
            new Cliente(host, porta).executar();
        } catch (IOException e) {
            System.err.println("Não foi possível comunicar com o servidor " + host + ":" + porta
                    + " (" + e.getMessage() + ")");
            System.exit(1);
        }
    }

    private void executar() throws IOException {
        try (Socket conexao = new Socket(host, porta);
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(conexao.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter escritor = new BufferedWriter(
                     new OutputStreamWriter(conexao.getOutputStream(), StandardCharsets.UTF_8))) {
            this.socket = conexao;
            this.saida = escritor;
            System.out.println("Conectado a " + host + ":" + porta);

            // Daemon: se o jogo acabar, o programa pode terminar mesmo com o teclado bloqueado.
            Thread teclado = new Thread(this::lerTeclado, "teclado");
            teclado.setDaemon(true);
            teclado.start();

            receberEventos(entrada);
        }
    }

    // ---------- thread principal: rede ----------

    private void receberEventos(BufferedReader entrada) {
        try {
            String linha;
            while ((linha = entrada.readLine()) != null) {
                try {
                    tratarEvento(Evento.analisar(linha));
                } catch (MensagemInvalidaException | IllegalArgumentException e) {
                    System.err.println("Mensagem inválida do servidor ('" + linha + "'): " + e.getMessage());
                }
                if (estado.isEncerrada()) {
                    return;
                }
            }
            if (!saindo && !estado.isEncerrada()) {
                System.out.println("A conexão com o servidor foi encerrada.");
            }
        } catch (IOException e) {
            if (!saindo) {
                System.out.println("Conexão perdida: " + e.getMessage());
            }
        }
    }

    private void tratarEvento(Evento evento) throws MensagemInvalidaException {
        switch (evento.getTipo()) {
            case JOGADOR:
                estado.definirMeuJogador(evento.inteiro(0));
                Pedra minha = Pedra.doJogador(estado.getMeuJogador());
                System.out.println("Você é o jogador " + estado.getMeuJogador()
                        + " (" + minha.getNome() + ", " + minha.getSimbolo() + ").");
                break;
            case ESPERA:
                System.out.println("Aguardando o adversário se conectar...");
                break;
            case INICIO:
                estado.iniciar(evento.inteiro(0));
                System.out.println("A partida começou! Preto (X) joga primeiro. Digite 'ajuda' para ver os comandos.");
                mostrarTabuleiroEVez();
                break;
            case JOGADA:
                tratarJogada(evento);
                break;
            case PASSE:
                int quemPassou = evento.inteiro(0);
                estado.aplicarPasse(evento.inteiro(1));
                System.out.println("O jogador " + quemPassou + " passou a vez.");
                if (estado.partidaPrestesAEncerrar()) {
                    System.out.println("Dois passes seguidos: contando os pontos...");
                } else {
                    mostrarVez();
                }
                break;
            case FIM:
                tratarFim(evento);
                break;
            case ERRO:
                MotivoErro motivo = MotivoErro.doCodigo(evento.texto(0));
                System.out.println("Recusado: " + (motivo != null ? motivo.getDescricao() : evento.texto(0)));
                break;
            case ADVERSARIO_SAIU:
                estado.encerrar();
                System.out.println("O adversário desconectou. Partida encerrada.");
                break;
            default:
                break;
        }
    }

    private void tratarJogada(Evento evento) throws MensagemInvalidaException {
        int jogador = evento.inteiro(0);
        Posicao posicao = evento.posicao(1);
        List<Posicao> capturadas = evento.posicoes(2);
        estado.aplicarJogada(jogador, posicao, capturadas, evento.inteiro(3));

        String quem = jogador == estado.getMeuJogador() ? "Você jogou" : "O adversário jogou";
        System.out.println(quem + " em " + posicao
                + (capturadas.isEmpty() ? "." : " e capturou " + capturadas.size() + " pedra(s): " + capturadas));
        mostrarTabuleiroEVez();
    }

    private void tratarFim(Evento evento) throws MensagemInvalidaException {
        estado.encerrar();
        int vencedor = evento.inteiro(3);
        System.out.println();
        System.out.println(estado.renderizar());
        System.out.println();
        if (Evento.MOTIVO_DESISTENCIA.equals(evento.texto(0))) {
            System.out.println("FIM DE JOGO por desistência.");
        } else {
            System.out.println("FIM DE JOGO (dois passes seguidos).");
            System.out.println("Preto: " + evento.texto(1) + " pontos | Branco: " + evento.texto(2)
                    + " pontos (komi incluso).");
        }
        System.out.println(vencedor == estado.getMeuJogador()
                ? "*** VOCÊ VENCEU! ***"
                : "Você perdeu. Vencedor: " + Pedra.doJogador(vencedor).getNome() + ".");
    }

    private void mostrarTabuleiroEVez() {
        System.out.println();
        System.out.println(estado.renderizar());
        System.out.println();
        mostrarVez();
    }

    private void mostrarVez() {
        if (estado.ehMinhaVez()) {
            System.out.println("Sua vez! Digite uma posição (ex: E5), 'pass' ou 'resign':");
        } else if (estado.isIniciada()) {
            System.out.println("Aguardando a jogada do adversário...");
        }
    }

    // ---------- thread de teclado ----------

    private void lerTeclado() {
        try (BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {
            String linha;
            while ((linha = teclado.readLine()) != null) {
                tratarComandoDigitado(linha.trim());
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o teclado: " + e.getMessage());
        }
    }

    private void tratarComandoDigitado(String texto) {
        String comando = texto.toLowerCase(Locale.ROOT);
        if (comando.isEmpty()) {
            return;
        }
        switch (comando) {
            case "ajuda":
            case "help":
                imprimirAjuda();
                return;
            case "tabuleiro":
            case "t":
                System.out.println(estado.renderizar());
                return;
            case "sair":
                sair();
                return;
            default:
                break;
        }

        if (!estado.isIniciada()) {
            System.out.println("A partida ainda não começou.");
            return;
        }
        if (estado.isEncerrada()) {
            System.out.println("A partida já terminou.");
            return;
        }

        int eu = estado.getMeuJogador();
        switch (comando) {
            case "resign":
            case "desistir":
                enviar(Comando.desistir(eu)); // pode desistir mesmo fora da vez
                return;
            case "pass":
            case "passar":
                if (verificarMinhaVez()) {
                    enviar(Comando.passar(eu));
                }
                return;
            default:
                break;
        }

        try {
            Posicao posicao = Posicao.analisar(texto);
            if (verificarMinhaVez()) {
                enviar(Comando.jogar(eu, posicao));
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Comando não reconhecido. Digite 'ajuda'.");
        }
    }

    /** Checagem rápida com a variável local de vez; o servidor ainda confere de novo. */
    private boolean verificarMinhaVez() {
        if (!estado.ehMinhaVez()) {
            System.out.println("Não é a sua vez.");
            return false;
        }
        return true;
    }

    private void enviar(Comando comando) {
        try {
            synchronized (this) {
                saida.write(comando.paraLinha());
                saida.write('\n');
                saida.flush();
            }
        } catch (IOException e) {
            System.out.println("Falha ao enviar para o servidor: " + e.getMessage());
        }
    }

    private void sair() {
        saindo = true;
        estado.encerrar();
        try {
            socket.close(); // desbloqueia a thread de rede
        } catch (IOException e) {
            System.err.println("Erro ao fechar a conexão: " + e.getMessage());
        }
    }

    private static void imprimirAjuda() {
        System.out.println("Comandos:");
        System.out.println("  E5          joga uma pedra na posição (colunas A-J sem I, linhas 1-9)");
        System.out.println("  pass        passa a vez (dois passes seguidos encerram a partida)");
        System.out.println("  resign      desiste da partida");
        System.out.println("  tabuleiro   mostra o tabuleiro de novo");
        System.out.println("  sair        fecha o cliente");
    }
}
