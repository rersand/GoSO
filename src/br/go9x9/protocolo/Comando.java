package br.go9x9.protocolo;

import br.go9x9.modelo.Posicao;

import java.util.Locale;

/**
 * Mensagem enviada do cliente para o servidor. Objeto imutável.
 *
 * <p>Notação compacta, no estilo do exemplo "2Cc3" do enunciado:
 * {@code <jogador><ação>[<posição>]}</p>
 * <ul>
 *   <li>{@code 1JE5} - jogador 1 joga em E5</li>
 *   <li>{@code 2P} - jogador 2 passa a vez</li>
 *   <li>{@code 1R} - jogador 1 desiste (resign)</li>
 * </ul>
 */
public final class Comando {

    /** Ações que um jogador pode pedir ao servidor. */
    public enum Acao {
        JOGAR('J'),
        PASSAR('P'),
        DESISTIR('R');

        private final char codigo;

        Acao(char codigo) {
            this.codigo = codigo;
        }

        public char getCodigo() {
            return codigo;
        }

        static Acao doCodigo(char codigo) {
            for (Acao acao : values()) {
                if (acao.codigo == codigo) {
                    return acao;
                }
            }
            return null;
        }
    }

    private final int jogador;
    private final Acao acao;
    private final Posicao posicao;

    private Comando(int jogador, Acao acao, Posicao posicao) {
        this.jogador = jogador;
        this.acao = acao;
        this.posicao = posicao;
    }

    public static Comando jogar(int jogador, Posicao posicao) {
        return new Comando(jogador, Acao.JOGAR, posicao);
    }

    public static Comando passar(int jogador) {
        return new Comando(jogador, Acao.PASSAR, null);
    }

    public static Comando desistir(int jogador) {
        return new Comando(jogador, Acao.DESISTIR, null);
    }

    /**
     * Interpreta uma linha recebida pela rede.
     *
     * @throws MensagemInvalidaException se a linha não seguir a notação
     */
    public static Comando analisar(String linha) throws MensagemInvalidaException {
        if (linha == null) {
            throw new MensagemInvalidaException("Linha ausente.");
        }
        String texto = linha.trim().toUpperCase(Locale.ROOT);
        if (texto.length() < 2) {
            throw new MensagemInvalidaException("Comando muito curto: '" + linha + "'");
        }

        char digito = texto.charAt(0);
        if (digito != '1' && digito != '2') {
            throw new MensagemInvalidaException("Jogador deve ser 1 ou 2: '" + linha + "'");
        }
        int jogador = digito - '0';

        Acao acao = Acao.doCodigo(texto.charAt(1));
        if (acao == null) {
            throw new MensagemInvalidaException("Ação desconhecida: '" + linha + "'");
        }

        if (acao == Acao.JOGAR) {
            try {
                return jogar(jogador, Posicao.analisar(texto.substring(2)));
            } catch (IllegalArgumentException e) {
                throw new MensagemInvalidaException("Posição inválida: '" + linha + "'", e);
            }
        }
        if (texto.length() != 2) {
            throw new MensagemInvalidaException("Sobrou texto após a ação: '" + linha + "'");
        }
        return new Comando(jogador, acao, null);
    }

    public int getJogador() {
        return jogador;
    }

    public Acao getAcao() {
        return acao;
    }

    /** Posição jogada; {@code null} para passar e desistir. */
    public Posicao getPosicao() {
        return posicao;
    }

    /** Linha pronta para enviar pela rede, sem o terminador de linha. */
    public String paraLinha() {
        return String.valueOf(jogador) + acao.getCodigo() + (posicao != null ? posicao.toString() : "");
    }

    @Override
    public String toString() {
        return paraLinha();
    }
}
