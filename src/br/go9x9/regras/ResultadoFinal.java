package br.go9x9.regras;

/**
 * Desfecho de uma partida encerrada. Objeto imutável.
 */
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

    /** Fim por contagem de pontos; o komi já deve estar somado em {@code pontosBranco}. */
    public static ResultadoFinal porPontos(double pontosPreto, double pontosBranco) {
        int vencedor = pontosPreto > pontosBranco ? 1 : 2;
        return new ResultadoFinal(pontosPreto, pontosBranco, vencedor, false);
    }

    /** Fim por desistência (ou abandono) de um jogador; vence o outro. */
    public static ResultadoFinal porDesistencia(int vencedor) {
        return new ResultadoFinal(0, 0, vencedor, true);
    }

    public double getPontosPreto() {
        return pontosPreto;
    }

    public double getPontosBranco() {
        return pontosBranco;
    }

    /** Número do jogador vencedor (1 = Preto, 2 = Branco). */
    public int getVencedor() {
        return vencedor;
    }

    public boolean isPorDesistencia() {
        return porDesistencia;
    }
}
