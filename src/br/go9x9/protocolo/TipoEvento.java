package br.go9x9.protocolo;

/**
 * Tipos de mensagem enviados do servidor para os clientes. O nome do tipo é
 * o primeiro campo da linha, por exemplo {@code JOGADA:1:E5:-:2}.
 */
public enum TipoEvento {

    /** {@code JOGADOR:<n>} - informa ao cliente qual jogador ele é. */
    JOGADOR,
    /** {@code ESPERA} - ainda falta o adversário se conectar. */
    ESPERA,
    /** {@code INICIO:<vez>} - a partida começou; informa de quem é a vez. */
    INICIO,
    /** {@code JOGADA:<n>:<posição>:<capturadas>:<próxima vez>} - jogada aceita. */
    JOGADA,
    /** {@code PASSE:<n>:<próxima vez>} - um jogador passou. */
    PASSE,
    /** {@code FIM:<motivo>:<pontos preto>:<pontos branco>:<vencedor>} - fim da partida. */
    FIM,
    /** {@code ERRO:<código>} - ação recusada; vai só para quem a enviou. */
    ERRO,
    /** {@code ADVERSARIO_SAIU} - o adversário desconectou. */
    ADVERSARIO_SAIU
}
