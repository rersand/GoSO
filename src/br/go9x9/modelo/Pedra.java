package br.go9x9.modelo;

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

    public int getJogador() {
        return jogador;
    }

    public char getSimbolo() {
        return simbolo;
    }

    public String getNome() {
        return nome;
    }

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

    public static Pedra doJogador(int numero) {
        for (Pedra pedra : values()) {
            if (pedra != VAZIA && pedra.jogador == numero) {
                return pedra;
            }
        }
        throw new IllegalArgumentException("Jogador inválido: " + numero);
    }
}
