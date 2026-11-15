package br.go9x9.regras;

import br.go9x9.modelo.Pedra;
import br.go9x9.modelo.Posicao;
import br.go9x9.modelo.Tabuleiro;

import java.util.HashSet;
import java.util.Set;

public final class Pontuacao {
    public static final double KOMI = 5.5;

    private Pontuacao() {
    }

    public static ResultadoFinal calcular(Tabuleiro tabuleiro) {
        int preto = 0;
        int branco = 0;
        Set<Posicao> visitadas = new HashSet<>();

        for (int linha = 0; linha < Tabuleiro.TAMANHO; linha++) {
            for (int coluna = 0; coluna < Tabuleiro.TAMANHO; coluna++) {
                Posicao posicao = new Posicao(linha, coluna);
                Pedra conteudo = tabuleiro.obter(posicao);
                if (conteudo == Pedra.PRETA) {
                    preto++;
                } else if (conteudo == Pedra.BRANCA) {
                    branco++;
                } else if (!visitadas.contains(posicao)) {
                    Set<Posicao> regiao = Regras.grupoDe(tabuleiro, posicao);
                    visitadas.addAll(regiao);
                    Pedra dono = donoDaRegiao(tabuleiro, regiao);
                    if (dono == Pedra.PRETA) {
                        preto += regiao.size();
                    } else if (dono == Pedra.BRANCA) {
                        branco += regiao.size();
                    }
                }
            }
        }
        return ResultadoFinal.porPontos(preto, branco + KOMI);
    }

    private static Pedra donoDaRegiao(Tabuleiro tabuleiro, Set<Posicao> regiao) {
        boolean tocaPreto = false;
        boolean tocaBranco = false;
        for (Posicao vazia : regiao) {
            for (Posicao vizinha : tabuleiro.vizinhas(vazia)) {
                Pedra pedra = tabuleiro.obter(vizinha);
                if (pedra == Pedra.PRETA) {
                    tocaPreto = true;
                } else if (pedra == Pedra.BRANCA) {
                    tocaBranco = true;
                }
            }
        }
        if (tocaPreto && !tocaBranco) {
            return Pedra.PRETA;
        }
        if (tocaBranco && !tocaPreto) {
            return Pedra.BRANCA;
        }
        return Pedra.VAZIA;
    }
}
