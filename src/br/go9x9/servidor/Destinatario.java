package br.go9x9.servidor;

import br.go9x9.protocolo.Evento;

/**
 * Quem recebe eventos da partida. Abstrai o socket para que
 * {@link PartidaCompartilhada} não dependa de detalhes de rede.
 */
public interface Destinatario {

    /** Entrega o evento; falhas de rede não devem interromper a partida. */
    void enviar(Evento evento);
}
