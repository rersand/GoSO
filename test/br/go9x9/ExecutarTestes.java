package br.go9x9;

public final class ExecutarTestes {
    private ExecutarTestes() {
    }

    public static void main(String[] args) throws Exception {
        TestesRegras.executar();
        TestesProtocolo.executar();
        TestePartidaEmRede.executar();
        System.exit(Verificador.finalizar());
    }
}
