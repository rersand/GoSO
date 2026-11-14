package br.go9x9.modelo;

/**
 * Conteúdo possível de uma interseção do tabuleiro.
 *
 * <p>Cada pedra também conhece o número do jogador que a controla:
 * jogador 1 = Preto (joga primeiro) e jogador 2 = Branco.</p>
 */
public enum Pedra {

    VAZIA(0, '.', "Vazio"),
    PRETA(1, 'X', "Preto"),
    BRANCA(2, 'O', "Branco");

    private final int jogador;
    private final char simbolo;
    private final String nome;

    Pedra(int jogador, char simbolo, String nome) {
        this.jogador = jogador;
        this.simbolo = simbolo;
        this.nome = nome;
    }

    /** Número do jogador dono da pedra (0 para {@link #VAZIA}). */
    public int getJogador() {
        return jogador;
    }

    /** Caractere usado para desenhar a pedra no console. */
    public char getSimbolo() {
        return simbolo;
    }

    /** Nome da cor, para mensagens ao usuário. */
    public String getNome() {
        return nome;
    }

    /** Cor adversária. Não existe para {@link #VAZIA}. */
    public Pedra oponente() {
        switch (this) {
            case PRETA:
                return BRANCA;
            case BRANCA:
                return PRETA;
            default:
                throw new IllegalStateException("Uma interseção vazia não tem oponente.");
        }
    }

    /** Converte o número do jogador (1 ou 2) na pedra correspondente. */
    public static Pedra doJogador(int numero) {
        for (Pedra pedra : values()) {
            if (pedra != VAZIA && pedra.jogador == numero) {
                return pedra;
            }
        }
        throw new IllegalArgumentException("Jogador inválido: " + numero);
    }
}
