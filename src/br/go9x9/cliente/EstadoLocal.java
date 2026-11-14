package br.go9x9.cliente;

import br.go9x9.modelo.Pedra;
import br.go9x9.modelo.Posicao;
import br.go9x9.modelo.Tabuleiro;

import java.util.List;

/**
 * Cópia local do estado da partida mantida pelo cliente: tabuleiro, de quem é
 * a vez e se o jogo já terminou. Só é atualizada com o que o servidor confirma.
 *
 * <p>Duas threads usam este objeto (a que lê o teclado e a que recebe mensagens
 * da rede), então todos os métodos são {@code synchronized}.</p>
 */
public final class EstadoLocal {

    private static final int PASSES_PARA_ENCERRAR = 2;

    private final Tabuleiro tabuleiro = new Tabuleiro();
    /** Pedras capturadas por cada jogador; o índice é o número do jogador. */
    private final int[] prisioneiros = new int[3];
    private int meuJogador;
    private int vez;
    private boolean iniciada;
    private boolean encerrada;
    private int passesSeguidos;
    private Posicao ultimaJogada;

    public synchronized void definirMeuJogador(int jogador) {
        this.meuJogador = jogador;
    }

    public synchronized int getMeuJogador() {
        return meuJogador;
    }

    public synchronized void iniciar(int primeiraVez) {
        this.iniciada = true;
        this.vez = primeiraVez;
    }

    public synchronized void aplicarJogada(int jogador, Posicao posicao, List<Posicao> capturadas, int proximaVez) {
        tabuleiro.colocar(posicao, Pedra.doJogador(jogador));
        for (Posicao capturada : capturadas) {
            tabuleiro.remover(capturada);
        }
        prisioneiros[jogador] += capturadas.size();
        passesSeguidos = 0;
        ultimaJogada = posicao;
        vez = proximaVez;
    }

    public synchronized void aplicarPasse(int proximaVez) {
        vez = proximaVez;
        passesSeguidos++;
    }

    /** Verdadeiro quando os dois últimos lances foram passes, ou seja, o servidor vai anunciar o fim. */
    public synchronized boolean partidaPrestesAEncerrar() {
        return passesSeguidos >= PASSES_PARA_ENCERRAR;
    }

    public synchronized void encerrar() {
        encerrada = true;
    }

    public synchronized int getVez() {
        return vez;
    }

    public synchronized boolean isIniciada() {
        return iniciada;
    }

    public synchronized boolean isEncerrada() {
        return encerrada;
    }

    public synchronized boolean ehMinhaVez() {
        return iniciada && !encerrada && vez == meuJogador;
    }

    /** Desenho do tabuleiro com o placar de capturas. */
    public synchronized String renderizar() {
        return TabuleiroConsole.desenhar(tabuleiro, ultimaJogada)
                + "\n   Capturas -> Preto (X): " + prisioneiros[1] + " | Branco (O): " + prisioneiros[2];
    }
}
