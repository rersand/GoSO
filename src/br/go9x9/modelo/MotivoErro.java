package br.go9x9.modelo;

/**
 * Motivos pelos quais uma ação é recusada. O {@link #getCodigo() código}
 * viaja nas mensagens do protocolo e a {@link #getDescricao() descrição}
 * é mostrada ao jogador.
 */
public enum MotivoErro {

    OCUPADA("ocupada", "Essa posição já está ocupada."),
    SUICIDIO("suicidio", "Jogada proibida: a pedra ficaria sem liberdades (suicídio)."),
    KO("ko", "Jogada proibida pela regra do ko: ela repetiria a posição anterior."),
    NAO_E_SUA_VEZ("nao_e_sua_vez", "Não é a sua vez."),
    PARTIDA_ENCERRADA("partida_encerrada", "A partida já terminou."),
    JOGADOR_INVALIDO("jogador_invalido", "Número de jogador inválido para esta conexão."),
    AGUARDANDO_ADVERSARIO("aguardando_adversario", "A partida ainda não começou: aguardando o adversário."),
    MENSAGEM_INVALIDA("mensagem_invalida", "Mensagem não reconhecida pelo protocolo.");

    private final String codigo;
    private final String descricao;

    MotivoErro(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Procura o motivo pelo código recebido na rede; {@code null} se desconhecido. */
    public static MotivoErro doCodigo(String codigo) {
        for (MotivoErro motivo : values()) {
            if (motivo.codigo.equals(codigo)) {
                return motivo;
            }
        }
        return null;
    }
}
