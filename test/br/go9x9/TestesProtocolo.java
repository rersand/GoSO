package br.go9x9;

import br.go9x9.modelo.MotivoErro;
import br.go9x9.modelo.Posicao;
import br.go9x9.protocolo.Comando;
import br.go9x9.protocolo.Evento;
import br.go9x9.protocolo.MensagemInvalidaException;
import br.go9x9.protocolo.TipoEvento;
import br.go9x9.regras.ResultadoFinal;

import java.util.Arrays;

import static br.go9x9.Verificador.igual;
import static br.go9x9.Verificador.secao;
import static br.go9x9.Verificador.verdadeiro;

public final class TestesProtocolo {
    private TestesProtocolo() {
    }

    public static void executar() throws MensagemInvalidaException {
        comandos();
        comandosInvalidos();
        eventos();
    }

    private static Posicao p(String texto) {
        return Posicao.analisar(texto);
    }

    private static void comandos() throws MensagemInvalidaException {
        secao("Comandos (cliente -> servidor)");
        Comando jogada = Comando.analisar("1JE5");
        igual("1JE5: jogador", 1, jogada.getJogador());
        igual("1JE5: ação", Comando.Acao.JOGAR, jogada.getAcao());
        igual("1JE5: posição", p("E5"), jogada.getPosicao());
        igual("ida e volta", "1JE5", jogada.paraLinha());

        igual("2p: passar (minúsculas aceitas)", Comando.Acao.PASSAR, Comando.analisar("2p").getAcao());
        igual("1R: desistir", Comando.Acao.DESISTIR, Comando.analisar("1R").getAcao());
        igual("espaços nas pontas são ignorados", "2JA1", Comando.analisar("  2ja1 ").paraLinha());
        igual("fábrica jogar", "2JJ9", Comando.jogar(2, p("J9")).paraLinha());
        igual("fábrica passar", "1P", Comando.passar(1).paraLinha());
        igual("fábrica desistir", "2R", Comando.desistir(2).paraLinha());
    }

    private static void comandosInvalidos() {
        secao("Comandos inválidos");
        for (String ruim : Arrays.asList("", "1", "3JE5", "1XE5", "1JI5", "1JE0", "1JE", "1JE55", "1PE5", "abc")) {
            verdadeiro("rejeita '" + ruim + "'", falha(ruim));
        }
    }

    private static boolean falha(String linha) {
        try {
            Comando.analisar(linha);
            return false;
        } catch (MensagemInvalidaException esperado) {
            return true;
        }
    }

    private static void eventos() throws MensagemInvalidaException {
        secao("Eventos (servidor -> clientes)");
        igual("JOGADOR", "JOGADOR:2", Evento.jogador(2).paraLinha());
        igual("ESPERA", "ESPERA", Evento.espera().paraLinha());
        igual("INICIO", "INICIO:1", Evento.inicio(1).paraLinha());
        igual("PASSE", "PASSE:1:2", Evento.passe(1, 2).paraLinha());
        igual("ERRO", "ERRO:ko", Evento.erro(MotivoErro.KO).paraLinha());
        igual("ADVERSARIO_SAIU", "ADVERSARIO_SAIU", Evento.adversarioSaiu().paraLinha());

        Evento semCaptura = Evento.jogada(1, p("E5"), Arrays.<Posicao>asList(), 2);
        igual("JOGADA sem captura", "JOGADA:1:E5:-:2", semCaptura.paraLinha());
        Evento comCaptura = Evento.jogada(2, p("A1"), Arrays.asList(p("D5"), p("D6")), 1);
        igual("JOGADA com captura", "JOGADA:2:A1:D5,D6:1", comCaptura.paraLinha());

        Evento lido = Evento.analisar("JOGADA:2:A1:D5,D6:1");
        igual("leitura: tipo", TipoEvento.JOGADA, lido.getTipo());
        igual("leitura: jogador", 2, lido.inteiro(0));
        igual("leitura: posição", p("A1"), lido.posicao(1));
        igual("leitura: capturadas", Arrays.asList(p("D5"), p("D6")), lido.posicoes(2));
        igual("leitura: próxima vez", 1, lido.inteiro(3));
        igual("leitura: lista vazia", 0, Evento.analisar("JOGADA:1:E5:-:2").posicoes(2).size());

        igual("FIM por pontos", "FIM:PASSES:45.0:41.5:1", Evento.fim(ResultadoFinal.porPontos(45, 41.5)).paraLinha());
        igual("FIM por desistência", "FIM:DESISTENCIA:-:-:2", Evento.fim(ResultadoFinal.porDesistencia(2)).paraLinha());

        verdadeiro("tipo desconhecido é rejeitado", eventoInvalido("XPTO:1"));
        verdadeiro("linha vazia é rejeitada", eventoInvalido(""));
        verdadeiro("campo ausente é rejeitado", campoAusente());
    }

    private static boolean eventoInvalido(String linha) {
        try {
            Evento.analisar(linha);
            return false;
        } catch (MensagemInvalidaException esperado) {
            return true;
        }
    }

    private static boolean campoAusente() {
        try {
            Evento.analisar("PASSE:1").inteiro(1);
            return false;
        } catch (MensagemInvalidaException esperado) {
            return true;
        }
    }
}
