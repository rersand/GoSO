package br.go9x9.regras;

import br.go9x9.modelo.Pedra;
import br.go9x9.modelo.Posicao;
import br.go9x9.modelo.Tabuleiro;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Funções puras sobre grupos de pedras. Não alteram o tabuleiro.
 */
public final class Regras {

    private Regras() {
        // classe utilitária
    }

    /**
     * Conjunto conectado de interseções com o mesmo conteúdo da origem
     * (busca em largura). Para uma pedra é o seu grupo; para uma interseção
     * vazia é a região vazia à qual ela pertence.
     */
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

    /** Interseções vazias adjacentes ao grupo (as suas "liberdades"). */
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
