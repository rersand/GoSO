package br.go9x9.regras;

import br.go9x9.modelo.MotivoErro;
import br.go9x9.modelo.Pedra;
import br.go9x9.modelo.Posicao;
import br.go9x9.modelo.ResultadoJogada;
import br.go9x9.modelo.Tabuleiro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Regras de uma partida de Go 9x9: vez, captura, suicídio, ko simples, passes
 * e desistência. Não conhece rede nem console e <strong>não é thread-safe</strong>:
 * o servidor a protege com um lock em {@code PartidaCompartilhada}.
 */
public final class Partida {

    private static final int PASSES_PARA_ENCERRAR = 2;

    private Tabuleiro tabuleiro = new Tabuleiro();
    /** Posição existente antes da última ação; serve para detectar o ko. */
    private Tabuleiro posicaoAnterior;
    private Pedra vez = Pedra.PRETA;
    private int passesSeguidos;
    private boolean encerrada;
    /** Jogador que desistiu (0 se ninguém desistiu). */
    private int desistente;

    /** Número do jogador (1 ou 2) que deve jogar agora. */
    public int getVez() {
        return vez.getJogador();
    }

    public boolean isEncerrada() {
        return encerrada;
    }

    /** Cópia do tabuleiro atual. */
    public Tabuleiro getTabuleiro() {
        return tabuleiro.copiar();
    }

    /** Coloca uma pedra do jogador na posição, aplicando captura, suicídio e ko. */
    public ResultadoJogada jogar(int jogador, Posicao posicao) {
        MotivoErro impedimento = verificarVez(jogador);
        if (impedimento != null) {
            return ResultadoJogada.falha(impedimento);
        }
        if (tabuleiro.obter(posicao) != Pedra.VAZIA) {
            return ResultadoJogada.falha(MotivoErro.OCUPADA);
        }

        Pedra cor = vez;
        Tabuleiro hipotetico = tabuleiro.copiar();
        hipotetico.colocar(posicao, cor);
        List<Posicao> capturadas = capturarVizinhosSemLiberdade(hipotetico, posicao, cor.oponente());

        Set<Posicao> grupoProprio = Regras.grupoDe(hipotetico, posicao);
        if (capturadas.isEmpty() && Regras.liberdadesDe(hipotetico, grupoProprio).isEmpty()) {
            return ResultadoJogada.falha(MotivoErro.SUICIDIO);
        }
        if (hipotetico.equals(posicaoAnterior)) {
            return ResultadoJogada.falha(MotivoErro.KO);
        }

        posicaoAnterior = tabuleiro;
        tabuleiro = hipotetico;
        passesSeguidos = 0;
        vez = cor.oponente();
        return ResultadoJogada.sucesso(capturadas);
    }

    /** Passa a vez. Dois passes seguidos encerram a partida. */
    public ResultadoJogada passar(int jogador) {
        MotivoErro impedimento = verificarVez(jogador);
        if (impedimento != null) {
            return ResultadoJogada.falha(impedimento);
        }
        posicaoAnterior = tabuleiro.copiar();
        passesSeguidos++;
        vez = vez.oponente();
        if (passesSeguidos >= PASSES_PARA_ENCERRAR) {
            encerrada = true;
        }
        return ResultadoJogada.sucesso(Collections.<Posicao>emptyList());
    }

    /** Desistência: pode ser feita a qualquer momento, mesmo fora da vez. */
    public ResultadoJogada desistir(int jogador) {
        if (jogador != Pedra.PRETA.getJogador() && jogador != Pedra.BRANCA.getJogador()) {
            return ResultadoJogada.falha(MotivoErro.JOGADOR_INVALIDO);
        }
        if (encerrada) {
            return ResultadoJogada.falha(MotivoErro.PARTIDA_ENCERRADA);
        }
        encerrada = true;
        desistente = jogador;
        return ResultadoJogada.sucesso(Collections.<Posicao>emptyList());
    }

    /** Desfecho da partida; só pode ser pedido depois que ela terminou. */
    public ResultadoFinal getResultadoFinal() {
        if (!encerrada) {
            throw new IllegalStateException("A partida ainda não terminou.");
        }
        if (desistente != 0) {
            return ResultadoFinal.porDesistencia(Pedra.doJogador(desistente).oponente().getJogador());
        }
        return Pontuacao.calcular(tabuleiro);
    }

    private MotivoErro verificarVez(int jogador) {
        if (jogador != Pedra.PRETA.getJogador() && jogador != Pedra.BRANCA.getJogador()) {
            return MotivoErro.JOGADOR_INVALIDO;
        }
        if (encerrada) {
            return MotivoErro.PARTIDA_ENCERRADA;
        }
        if (jogador != vez.getJogador()) {
            return MotivoErro.NAO_E_SUA_VEZ;
        }
        return null;
    }

    /** Remove do tabuleiro os grupos adversários vizinhos da jogada que ficaram sem liberdades. */
    private static List<Posicao> capturarVizinhosSemLiberdade(Tabuleiro tabuleiro, Posicao jogada, Pedra adversaria) {
        List<Posicao> capturadas = new ArrayList<>();
        for (Posicao vizinha : tabuleiro.vizinhas(jogada)) {
            if (tabuleiro.obter(vizinha) != adversaria) {
                continue; // vazia, própria, ou já removida nesta mesma jogada
            }
            Set<Posicao> grupo = Regras.grupoDe(tabuleiro, vizinha);
            if (Regras.liberdadesDe(tabuleiro, grupo).isEmpty()) {
                for (Posicao pedra : grupo) {
                    tabuleiro.remover(pedra);
                    capturadas.add(pedra);
                }
            }
        }
        return capturadas;
    }
}
