package br.go9x9.cliente;

import br.go9x9.modelo.Pedra;
import br.go9x9.modelo.Posicao;
import br.go9x9.modelo.Tabuleiro;

public final class TabuleiroConsole {
    private static final String COLUNAS = "ABCDEFGHJ";
    private static final int[][] HOSHI = {{2, 2}, {2, 6}, {6, 2}, {6, 6}, {4, 4}};

    private TabuleiroConsole() {
    }

    public static String desenhar(Tabuleiro tabuleiro, Posicao ultimaJogada) {
        StringBuilder desenho = new StringBuilder();
        desenho.append(cabecalho()).append('\n');

        for (int linha = Tabuleiro.TAMANHO - 1; linha >= 0; linha--) {
            desenho.append(String.format("%2d ", linha + 1));
            for (int coluna = 0; coluna < Tabuleiro.TAMANHO; coluna++) {
                Posicao posicao = new Posicao(linha, coluna);
                desenho.append(desenharCasa(tabuleiro.obter(posicao), posicao, posicao.equals(ultimaJogada)));
            }
            desenho.append(String.format(" %d", linha + 1)).append('\n');
        }

        desenho.append(cabecalho());
        return desenho.toString();
    }

    private static String cabecalho() {
        StringBuilder letras = new StringBuilder("    ");
        for (int coluna = 0; coluna < Tabuleiro.TAMANHO; coluna++) {
            if (coluna > 0) {
                letras.append("  ");
            }
            letras.append(COLUNAS.charAt(coluna));
        }
        return letras.toString();
    }

    private static String desenharCasa(Pedra pedra, Posicao posicao, boolean ultima) {
        char simbolo = pedra.getSimbolo();
        if (pedra == Pedra.VAZIA && ehPontoDeReferencia(posicao)) {
            simbolo = '+';
        }
        return ultima ? "[" + simbolo + "]" : " " + simbolo + " ";
    }

    private static boolean ehPontoDeReferencia(Posicao posicao) {
        for (int[] ponto : HOSHI) {
            if (ponto[0] == posicao.getLinha() && ponto[1] == posicao.getColuna()) {
                return true;
            }
        }
        return false;
    }
}
