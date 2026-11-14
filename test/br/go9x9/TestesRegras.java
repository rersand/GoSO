package br.go9x9;

import br.go9x9.modelo.MotivoErro;
import br.go9x9.modelo.Pedra;
import br.go9x9.modelo.Posicao;
import br.go9x9.modelo.ResultadoJogada;
import br.go9x9.modelo.Tabuleiro;
import br.go9x9.regras.Partida;
import br.go9x9.regras.Pontuacao;
import br.go9x9.regras.ResultadoFinal;

import java.util.Arrays;
import java.util.HashSet;

import static br.go9x9.Verificador.igual;
import static br.go9x9.Verificador.secao;
import static br.go9x9.Verificador.verdadeiro;

/** Testes das regras do Go, sem rede. */
public final class TestesRegras {

    private TestesRegras() {
    }

    public static void executar() {
        posicoes();
        captura();
        suicidio();
        casosBasicos();
        ko();
        passesEDesistencia();
        pontuacao();
    }

    private static Posicao p(String texto) {
        return Posicao.analisar(texto);
    }

    /** Executa a sequência alternada começando pelo jogador indicado; todas devem ser válidas. */
    private static void jogarSequencia(Partida partida, String... jogadas) {
        for (String jogada : jogadas) {
            ResultadoJogada r = partida.jogar(partida.getVez(), p(jogada));
            if (!r.isValida()) {
                throw new IllegalStateException("Montagem do teste falhou em " + jogada + ": " + r.getErro());
            }
        }
    }

    private static void posicoes() {
        secao("Posições");
        igual("E5 -> linha 4", 4, p("E5").getLinha());
        igual("E5 -> coluna 4", 4, p("E5").getColuna());
        igual("J9 é o canto superior direito (coluna 8)", 8, p("J9").getColuna());
        igual("minúsculas são aceitas", p("A1"), p("a1"));
        igual("toString", "H7", p("h7").toString());
        verdadeiro("a letra I não existe", rejeita("I5"));
        verdadeiro("linha 0 não existe", rejeita("A0"));
        verdadeiro("linha 10 não existe", rejeita("A10"));
        verdadeiro("texto vazio é inválido", rejeita(""));
    }

    private static boolean rejeita(String texto) {
        try {
            Posicao.analisar(texto);
            return false;
        } catch (IllegalArgumentException esperado) {
            return true;
        }
    }

    private static void captura() {
        secao("Captura");
        Partida partida = new Partida();
        jogarSequencia(partida, "E4", "E5", "D5", "A1", "F5", "A2");
        ResultadoJogada r = partida.jogar(1, p("E6"));
        verdadeiro("jogada que cerca a pedra é válida", r.isValida());
        igual("captura exatamente E5", new HashSet<>(Arrays.asList(p("E5"))), new HashSet<>(r.getCapturadas()));
        igual("E5 ficou vazia", Pedra.VAZIA, partida.getTabuleiro().obter(p("E5")));
        igual("a vez passou para o 2", 2, partida.getVez());

        Partida grupo = new Partida();
        // Branco forma um grupo de duas pedras (A1,B1) que Preto cerca em A2,B2,C1.
        jogarSequencia(grupo, "A2", "A1", "B2", "B1");
        ResultadoJogada capturaGrupo = grupo.jogar(1, p("C1"));
        igual("grupo de 2 pedras é capturado inteiro", 2, capturaGrupo.getCapturadas().size());
    }

    private static void suicidio() {
        secao("Suicídio");
        Partida partida = new Partida();
        jogarSequencia(partida, "B1", "J9", "A2");
        ResultadoJogada r = partida.jogar(2, p("A1"));
        verdadeiro("jogar sem liberdades e sem capturar é recusado", !r.isValida());
        igual("motivo SUICIDIO", MotivoErro.SUICIDIO, r.getErro());
        igual("a vez continua com o Branco", 2, partida.getVez());
        igual("a posição continua vazia", Pedra.VAZIA, partida.getTabuleiro().obter(p("A1")));

        // Exceção: sem liberdades, mas capturando, a jogada vale.
        Partida excecao = new Partida();
        jogarSequencia(excecao, "A2", "A1", "J9", "J8");
        // Branco A1 tem a liberdade B1; Preto joga B1 e captura A1 mesmo ficando sem espaço.
        ResultadoJogada captura = excecao.jogar(1, p("B1"));
        verdadeiro("capturar valida a jogada", captura.isValida());
        igual("A1 foi capturada", 1, captura.getCapturadas().size());
    }

    private static void casosBasicos() {
        secao("Ocupada e vez");
        Partida partida = new Partida();
        verdadeiro("Branco não pode abrir a partida",
                partida.jogar(2, p("E5")).getErro() == MotivoErro.NAO_E_SUA_VEZ);
        partida.jogar(1, p("E5"));
        igual("posição ocupada é recusada", MotivoErro.OCUPADA, partida.jogar(2, p("E5")).getErro());
        igual("jogador 3 não existe", MotivoErro.JOGADOR_INVALIDO, partida.jogar(3, p("A1")).getErro());
    }

    private static void ko() {
        secao("Ko");
        Partida partida = new Partida();
        //     C D E F
        //  6  . X O .
        //  5  X O . O
        //  4  . X O .
        jogarSequencia(partida, "D6", "E6", "C5", "D5", "D4", "F5", "A1", "E4");
        ResultadoJogada captura = partida.jogar(1, p("E5"));
        verdadeiro("Preto captura D5 em E5", captura.isValida() && captura.getCapturadas().contains(p("D5")));

        ResultadoJogada retomada = partida.jogar(2, p("D5"));
        igual("retomar o ko na hora é proibido", MotivoErro.KO, retomada.getErro());
        igual("a vez continua com o Branco", 2, partida.getVez());

        jogarSequencia(partida, "J9", "J1");
        ResultadoJogada depois = partida.jogar(2, p("D5"));
        verdadeiro("depois de uma ameaça em outro lugar o ko pode ser retomado", depois.isValida());
        igual("capturando E5", 1, depois.getCapturadas().size());
    }

    private static void passesEDesistencia() {
        secao("Passes e desistência");
        Partida partida = new Partida();
        partida.passar(1);
        verdadeiro("um passe não encerra", !partida.isEncerrada());
        partida.passar(2);
        verdadeiro("dois passes seguidos encerram", partida.isEncerrada());
        igual("depois do fim nada é aceito", MotivoErro.PARTIDA_ENCERRADA, partida.jogar(1, p("E5")).getErro());

        Partida intercalada = new Partida();
        intercalada.passar(1);
        intercalada.jogar(2, p("E5"));
        intercalada.passar(1);
        verdadeiro("jogada entre passes zera a contagem", !intercalada.isEncerrada());

        Partida desistencia = new Partida();
        desistencia.desistir(1);
        verdadeiro("desistir encerra", desistencia.isEncerrada());
        ResultadoFinal r = desistencia.getResultadoFinal();
        igual("quem desiste perde: vence o Branco", 2, r.getVencedor());
        verdadeiro("marcado como desistência", r.isPorDesistencia());
    }

    private static void pontuacao() {
        secao("Pontuação (área + komi 5.5)");
        ResultadoFinal vazio = Pontuacao.calcular(new Tabuleiro());
        igual("tabuleiro vazio: Preto 0", 0.0, vazio.getPontosPreto());
        igual("tabuleiro vazio: Branco só o komi", 5.5, vazio.getPontosBranco());
        igual("tabuleiro vazio: Branco vence", 2, vazio.getVencedor());

        Tabuleiro sozinha = new Tabuleiro();
        sozinha.colocar(p("E5"), Pedra.PRETA);
        ResultadoFinal umaPedra = Pontuacao.calcular(sozinha);
        igual("uma pedra domina o tabuleiro: 81", 81.0, umaPedra.getPontosPreto());
        igual("vencedor Preto", 1, umaPedra.getVencedor());

        Tabuleiro dividido = new Tabuleiro();
        for (int linha = 1; linha <= 9; linha++) {
            dividido.colocar(p("E" + linha), Pedra.PRETA);
            dividido.colocar(p("F" + linha), Pedra.BRANCA);
        }
        ResultadoFinal r = Pontuacao.calcular(dividido);
        igual("Preto: 9 pedras + 36 de território = 45", 45.0, r.getPontosPreto());
        igual("Branco: 9 pedras + 27 de território + komi = 41.5", 41.5, r.getPontosBranco());
        igual("Preto vence por 3.5", 1, r.getVencedor());

        Tabuleiro neutro = new Tabuleiro();
        neutro.colocar(p("A1"), Pedra.PRETA);
        neutro.colocar(p("J9"), Pedra.BRANCA);
        ResultadoFinal comNeutro = Pontuacao.calcular(neutro);
        igual("região tocada pelas duas cores não conta: Preto 1", 1.0, comNeutro.getPontosPreto());
        igual("região neutra: Branco 1 + komi", 6.5, comNeutro.getPontosBranco());
    }
}
