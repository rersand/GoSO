package br.go9x9.protocolo;

public class MensagemInvalidaException extends Exception {
    private static final long serialVersionUID = 1L;

    public MensagemInvalidaException(String mensagem) {
        super(mensagem);
    }

    public MensagemInvalidaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
