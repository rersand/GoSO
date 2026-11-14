package br.go9x9.servidor;

import br.go9x9.modelo.MotivoErro;
import br.go9x9.modelo.ResultadoJogada;
import br.go9x9.protocolo.Comando;
import br.go9x9.protocolo.Evento;
import br.go9x9.regras.Partida;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Estado da partida compartilhado entre as threads de cada jogador.
 *
 * <p><strong>Exclusão mútua:</strong> cada conexão roda em sua própria thread, mas
 * todas disputam o mesmo tabuleiro e a mesma variável de vez. Por isso toda
 * leitura ou escrita do estado acontece com {@link #trava} adquirida. A trava
 * também cobre o envio dos eventos aos clientes, de modo que os dois jogadores
 * recebem as mensagens na mesma ordem em que o estado foi alterado.</p>
 */
public final class PartidaCompartilhada {

    private final ReentrantLock trava = new ReentrantLock();
    private final Partida partida = new Partida();
    private final Map<Integer, Destinatario> jogadores = new TreeMap<>();
    private boolean iniciada;

    /** Cadastra um jogador; quando o segundo chega a partida começa. */
    public void registrar(int jogador, Destinatario destinatario) {
        trava.lock();
        try {
            jogadores.put(jogador, destinatario);
            if (partida.isEncerrada()) {
                destinatario.enviar(Evento.adversarioSaiu());
                return;
            }
            destinatario.enviar(Evento.jogador(jogador));
            if (jogadores.size() == 2) {
                iniciada = true;
                transmitir(Evento.inicio(partida.getVez()));
            } else {
                destinatario.enviar(Evento.espera());
            }
        } finally {
            trava.unlock();
        }
    }

    /**
     * Executa o comando recebido da conexão do jogador indicado. O servidor é
     * a autoridade: confere identidade, vez e regras antes de alterar o estado.
     */
    public void processar(int jogadorDaConexao, Comando comando) {
        trava.lock();
        try {
            if (!iniciada) {
                responderErro(jogadorDaConexao, MotivoErro.AGUARDANDO_ADVERSARIO);
                return;
            }
            if (comando.getJogador() != jogadorDaConexao) {
                responderErro(jogadorDaConexao, MotivoErro.JOGADOR_INVALIDO);
                return;
            }
            switch (comando.getAcao()) {
                case JOGAR:
                    tratarJogada(jogadorDaConexao, comando);
                    break;
                case PASSAR:
                    tratarPasse(jogadorDaConexao);
                    break;
                case DESISTIR:
                    tratarDesistencia(jogadorDaConexao);
                    break;
                default:
                    responderErro(jogadorDaConexao, MotivoErro.MENSAGEM_INVALIDA);
                    break;
            }
        } finally {
            trava.unlock();
        }
    }

    /** Envia um erro apenas ao jogador indicado (usado para linhas ilegíveis). */
    public void responderErro(int jogador, MotivoErro motivo) {
        trava.lock();
        try {
            Destinatario destinatario = jogadores.get(jogador);
            if (destinatario != null) {
                destinatario.enviar(Evento.erro(motivo));
            }
        } finally {
            trava.unlock();
        }
    }

    /** Chamado quando a conexão de um jogador termina; encerra a partida se ainda estava em andamento. */
    public void desconectar(int jogador) {
        trava.lock();
        try {
            jogadores.remove(jogador);
            if (!partida.isEncerrada()) {
                partida.desistir(jogador);
                transmitir(Evento.adversarioSaiu());
            }
        } finally {
            trava.unlock();
        }
    }

    public boolean isEncerrada() {
        trava.lock();
        try {
            return partida.isEncerrada();
        } finally {
            trava.unlock();
        }
    }

    // Os métodos abaixo só são chamados por processar(), que já detém a trava.

    private void tratarJogada(int jogador, Comando comando) {
        ResultadoJogada resultado = partida.jogar(jogador, comando.getPosicao());
        if (!resultado.isValida()) {
            responderErro(jogador, resultado.getErro());
            return;
        }
        transmitir(Evento.jogada(jogador, comando.getPosicao(), resultado.getCapturadas(), partida.getVez()));
    }

    private void tratarPasse(int jogador) {
        ResultadoJogada resultado = partida.passar(jogador);
        if (!resultado.isValida()) {
            responderErro(jogador, resultado.getErro());
            return;
        }
        transmitir(Evento.passe(jogador, partida.getVez()));
        if (partida.isEncerrada()) {
            transmitir(Evento.fim(partida.getResultadoFinal()));
        }
    }

    private void tratarDesistencia(int jogador) {
        ResultadoJogada resultado = partida.desistir(jogador);
        if (!resultado.isValida()) {
            responderErro(jogador, resultado.getErro());
            return;
        }
        transmitir(Evento.fim(partida.getResultadoFinal()));
    }

    private void transmitir(Evento evento) {
        for (Destinatario destinatario : jogadores.values()) {
            destinatario.enviar(evento);
        }
    }
}
