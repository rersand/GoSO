package br.go9x9.servidor;

import br.go9x9.protocolo.Evento;

public interface Destinatario {
    void enviar(Evento evento);
}
