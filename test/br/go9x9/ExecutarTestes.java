package br.go9x9;

/**
 * Executa todos os testes do projeto. Termina com código de saída 0 se tudo passar.
 */
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
