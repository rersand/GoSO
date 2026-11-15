package br.go9x9.modelo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Tabuleiro {
    public static final int TAMANHO = 9;

    private static final int[][] DIRECOES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final Pedra[][] grade;

    public Tabuleiro() {
        grade = new Pedra[TAMANHO][TAMANHO];
        for (Pedra[] linha : grade) {
            Arrays.fill(linha, Pedra.VAZIA);
        }
    }

    private Tabuleiro(Pedra[][] origem) {
        grade = new Pedra[TAMANHO][];
        for (int i = 0; i < TAMANHO; i++) {
            grade[i] = origem[i].clone();
        }
    }

    public Pedra obter(Posicao posicao) {
        return grade[posicao.getLinha()][posicao.getColuna()];
    }

    public void colocar(Posicao posicao, Pedra pedra) {
        grade[posicao.getLinha()][posicao.getColuna()] = pedra;
    }

    public void remover(Posicao posicao) {
        colocar(posicao, Pedra.VAZIA);
    }

    public List<Posicao> vizinhas(Posicao posicao) {
        List<Posicao> vizinhas = new ArrayList<>(4);
        for (int[] direcao : DIRECOES) {
            int linha = posicao.getLinha() + direcao[0];
            int coluna = posicao.getColuna() + direcao[1];
            if (linha >= 0 && linha < TAMANHO && coluna >= 0 && coluna < TAMANHO) {
                vizinhas.add(new Posicao(linha, coluna));
            }
        }
        return vizinhas;
    }

    public Tabuleiro copiar() {
        return new Tabuleiro(grade);
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Tabuleiro)) {
            return false;
        }
        return Arrays.deepEquals(grade, ((Tabuleiro) outro).grade);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(grade);
    }
}
