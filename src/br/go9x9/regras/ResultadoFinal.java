package br.go9x9.regras;

public final class ResultadoFinal {
    private final double pontosPreto;
    private final double pontosBranco;
    private final int vencedor;
    private final boolean porDesistencia;

    private ResultadoFinal(double pontosPreto, double pontosBranco, int vencedor, boolean porDesistencia) {
        this.pontosPreto = pontosPreto;
        this.pontosBranco = pontosBranco;
        this.vencedor = vencedor;
        this.porDesistencia = porDesistencia;
    }

    public static ResultadoFinal porPontos(double pontosPreto, double pontosBranco) {
        int vencedor = pontosPreto > pontosBranco ? 1 : 2;
        return new ResultadoFinal(pontosPreto, pontosBranco, vencedor, false);
    }

    public static ResultadoFinal porDesistencia(int vencedor) {
        return new ResultadoFinal(0, 0, vencedor, true);
    }

    public double getPontosPreto() {
        return pontosPreto;
    }

    public double getPontosBranco() {
        return pontosBranco;
    }

    public int getVencedor() {
        return vencedor;
    }

    public boolean isPorDesistencia() {
        return porDesistencia;
    }
}
