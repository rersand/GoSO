package br.go9x9.protocolo;

import br.go9x9.modelo.Posicao;

import java.util.Locale;

public final class Comando {
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

    public Posicao getPosicao() {
        return posicao;
    }

    public String paraLinha() {
        return String.valueOf(jogador) + acao.getCodigo() + (posicao != null ? posicao.toString() : "");
    }

    @Override
    public String toString() {
        return paraLinha();
    }
}
