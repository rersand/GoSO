package br.go9x9.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ResultadoJogada {
    private final MotivoErro erro;
    private final List<Posicao> capturadas;

    private ResultadoJogada(MotivoErro erro, List<Posicao> capturadas) {
        this.erro = erro;
        this.capturadas = Collections.unmodifiableList(new ArrayList<>(capturadas));
    }

    public static ResultadoJogada sucesso(List<Posicao> capturadas) {
        return new ResultadoJogada(null, capturadas);
    }

    public static ResultadoJogada falha(MotivoErro erro) {
        return new ResultadoJogada(erro, Collections.<Posicao>emptyList());
    }

    public boolean isValida() {
        return erro == null;
    }

    public MotivoErro getErro() {
        return erro;
    }

    public List<Posicao> getCapturadas() {
        return capturadas;
    }
}
