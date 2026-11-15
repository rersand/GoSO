package br.go9x9.servidor;

import br.go9x9.modelo.MotivoErro;
import br.go9x9.modelo.ResultadoJogada;
import br.go9x9.protocolo.Comando;
import br.go9x9.protocolo.Evento;
import br.go9x9.regras.Partida;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

public final class PartidaCompartilhada {
    private final ReentrantLock trava = new ReentrantLock();
    private final Partida partida = new Partida();
    private final Map<Integer, Destinatario> jogadores = new TreeMap<>();
    private boolean iniciada;

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

    // Os métodos abaixo exigem que a trava já esteja adquirida (só são chamados por processar()).
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
