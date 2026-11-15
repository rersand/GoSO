package br.go9x9.modelo;

public final class Posicao {
    private static final String COLUNAS = "ABCDEFGHJ";

    private final int linha;
    private final int coluna;

    public Posicao(int linha, int coluna) {
        if (linha < 0 || linha >= Tabuleiro.TAMANHO || coluna < 0 || coluna >= Tabuleiro.TAMANHO) {
            throw new IllegalArgumentException("Posição fora do tabuleiro: " + linha + "," + coluna);
        }
        this.linha = linha;
        this.coluna = coluna;
    }

    public static Posicao analisar(String texto) {
        if (texto == null || texto.length() != 2) {
            throw new IllegalArgumentException("Posição inválida: " + texto);
        }
        int coluna = COLUNAS.indexOf(Character.toUpperCase(texto.charAt(0)));
        int linha = texto.charAt(1) - '1';
        if (coluna < 0 || linha < 0 || linha >= Tabuleiro.TAMANHO) {
            throw new IllegalArgumentException("Posição inválida: " + texto);
        }
        return new Posicao(linha, coluna);
    }

    public int getLinha() {
        return linha;
    }

    public int getColuna() {
        return coluna;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Posicao)) {
            return false;
        }
        Posicao that = (Posicao) outro;
        return linha == that.linha && coluna == that.coluna;
    }

    @Override
    public int hashCode() {
        return 31 * linha + coluna;
    }

    @Override
    public String toString() {
        return String.valueOf(COLUNAS.charAt(coluna)) + (linha + 1);
    }
}
