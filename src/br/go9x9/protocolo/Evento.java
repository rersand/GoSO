package br.go9x9.protocolo;

import br.go9x9.modelo.MotivoErro;
import br.go9x9.modelo.Posicao;
import br.go9x9.regras.ResultadoFinal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Evento {
    private static final String SEPARADOR = ":";
    private static final String VAZIO = "-";

    public static final String MOTIVO_PASSES = "PASSES";
    public static final String MOTIVO_DESISTENCIA = "DESISTENCIA";

    private final TipoEvento tipo;
    private final List<String> campos;

    private Evento(TipoEvento tipo, String... campos) {
        this.tipo = tipo;
        this.campos = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(campos)));
    }

    public static Evento jogador(int numero) {
        return new Evento(TipoEvento.JOGADOR, String.valueOf(numero));
    }

    public static Evento espera() {
        return new Evento(TipoEvento.ESPERA);
    }

    public static Evento inicio(int vez) {
        return new Evento(TipoEvento.INICIO, String.valueOf(vez));
    }

    public static Evento jogada(int jogador, Posicao posicao, List<Posicao> capturadas, int proximaVez) {
        return new Evento(TipoEvento.JOGADA, String.valueOf(jogador), posicao.toString(),
                codificarPosicoes(capturadas), String.valueOf(proximaVez));
    }

    public static Evento passe(int jogador, int proximaVez) {
        return new Evento(TipoEvento.PASSE, String.valueOf(jogador), String.valueOf(proximaVez));
    }

    public static Evento fim(ResultadoFinal resultado) {
        if (resultado.isPorDesistencia()) {
            return new Evento(TipoEvento.FIM, MOTIVO_DESISTENCIA, VAZIO, VAZIO,
                    String.valueOf(resultado.getVencedor()));
        }
        return new Evento(TipoEvento.FIM, MOTIVO_PASSES,
                String.valueOf(resultado.getPontosPreto()),
                String.valueOf(resultado.getPontosBranco()),
                String.valueOf(resultado.getVencedor()));
    }

    public static Evento erro(MotivoErro motivo) {
        return new Evento(TipoEvento.ERRO, motivo.getCodigo());
    }

    public static Evento adversarioSaiu() {
        return new Evento(TipoEvento.ADVERSARIO_SAIU);
    }

    public static Evento analisar(String linha) throws MensagemInvalidaException {
        if (linha == null || linha.trim().isEmpty()) {
            throw new MensagemInvalidaException("Linha vazia.");
        }
        String[] partes = linha.trim().split(SEPARADOR, -1);
        TipoEvento tipo;
        try {
            tipo = TipoEvento.valueOf(partes[0]);
        } catch (IllegalArgumentException e) {
            throw new MensagemInvalidaException("Tipo de mensagem desconhecido: '" + linha + "'", e);
        }
        return new Evento(tipo, Arrays.copyOfRange(partes, 1, partes.length));
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public int quantidadeDeCampos() {
        return campos.size();
    }

    public String texto(int indice) throws MensagemInvalidaException {
        if (indice < 0 || indice >= campos.size()) {
            throw new MensagemInvalidaException("Faltou o campo " + indice + " em " + paraLinha());
        }
        return campos.get(indice);
    }

    public int inteiro(int indice) throws MensagemInvalidaException {
        String valor = texto(indice);
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new MensagemInvalidaException("Campo " + indice + " não é um número: " + paraLinha(), e);
        }
    }

    public Posicao posicao(int indice) throws MensagemInvalidaException {
        try {
            return Posicao.analisar(texto(indice));
        } catch (IllegalArgumentException e) {
            throw new MensagemInvalidaException("Campo " + indice + " não é uma posição: " + paraLinha(), e);
        }
    }

    public List<Posicao> posicoes(int indice) throws MensagemInvalidaException {
        String valor = texto(indice);
        List<Posicao> lista = new ArrayList<>();
        if (VAZIO.equals(valor)) {
            return lista;
        }
        for (String item : valor.split(",")) {
            try {
                lista.add(Posicao.analisar(item));
            } catch (IllegalArgumentException e) {
                throw new MensagemInvalidaException("Lista de posições inválida: " + paraLinha(), e);
            }
        }
        return lista;
    }

    public String paraLinha() {
        StringBuilder linha = new StringBuilder(tipo.name());
        for (String campo : campos) {
            linha.append(SEPARADOR).append(campo);
        }
        return linha.toString();
    }

    @Override
    public String toString() {
        return paraLinha();
    }

    private static String codificarPosicoes(List<Posicao> posicoes) {
        if (posicoes.isEmpty()) {
            return VAZIO;
        }
        StringBuilder texto = new StringBuilder();
        for (Posicao posicao : posicoes) {
            if (texto.length() > 0) {
                texto.append(',');
            }
            texto.append(posicao);
        }
        return texto.toString();
    }
}
