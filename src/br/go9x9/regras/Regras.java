package br.go9x9.regras;

import br.go9x9.modelo.Pedra;
import br.go9x9.modelo.Posicao;
import br.go9x9.modelo.Tabuleiro;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public final class Regras {
    private Regras() {
    }

    public static Set<Posicao> grupoDe(Tabuleiro tabuleiro, Posicao origem) {
        Pedra conteudo = tabuleiro.obter(origem);
        Set<Posicao> grupo = new HashSet<>();
        Deque<Posicao> fila = new ArrayDeque<>();
        grupo.add(origem);
        fila.add(origem);
        while (!fila.isEmpty()) {
            Posicao atual = fila.poll();
            for (Posicao vizinha : tabuleiro.vizinhas(atual)) {
                if (tabuleiro.obter(vizinha) == conteudo && grupo.add(vizinha)) {
                    fila.add(vizinha);
                }
            }
        }
        return grupo;
    }

    public static Set<Posicao> liberdadesDe(Tabuleiro tabuleiro, Set<Posicao> grupo) {
        Set<Posicao> liberdades = new HashSet<>();
        for (Posicao pedra : grupo) {
            for (Posicao vizinha : tabuleiro.vizinhas(pedra)) {
                if (tabuleiro.obter(vizinha) == Pedra.VAZIA) {
                    liberdades.add(vizinha);
                }
            }
        }
        return liberdades;
    }
}
