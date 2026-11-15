package br.go9x9.servidor;

import br.go9x9.modelo.MotivoErro;
import br.go9x9.protocolo.Comando;
import br.go9x9.protocolo.Evento;
import br.go9x9.protocolo.MensagemInvalidaException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

final class ConexaoJogador implements Runnable, Destinatario {
    private final int numero;
    private final Socket socket;
    private final PartidaCompartilhada partida;
    private BufferedWriter saida;

    ConexaoJogador(int numero, Socket socket, PartidaCompartilhada partida) {
        this.numero = numero;
        this.socket = socket;
        this.partida = partida;
    }

    @Override
    public void run() {
        try (Socket conexao = socket;
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(conexao.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter escritor = new BufferedWriter(
                     new OutputStreamWriter(conexao.getOutputStream(), StandardCharsets.UTF_8))) {
            this.saida = escritor;
            partida.registrar(numero, this);

            String linha;
            while ((linha = entrada.readLine()) != null) {
                log("recebido  <- " + linha);
                tratarLinha(linha);
                if (partida.isEncerrada()) {
                    break;
                }
            }
        } catch (IOException e) {
            log("conexão interrompida: " + e.getMessage());
        } finally {
            partida.desconectar(numero);
            log("desconectado");
        }
    }

    private void tratarLinha(String linha) {
        try {
            partida.processar(numero, Comando.analisar(linha));
        } catch (MensagemInvalidaException e) {
            partida.responderErro(numero, MotivoErro.MENSAGEM_INVALIDA);
        }
    }

    @Override
    public synchronized void enviar(Evento evento) {
        if (saida == null) {
            return;
        }
        try {
            saida.write(evento.paraLinha());
            saida.write('\n');
            saida.flush();
            log("enviado   -> " + evento.paraLinha());
        } catch (IOException e) {
            log("falha ao enviar '" + evento.paraLinha() + "': " + e.getMessage());
        }
    }

    private void log(String mensagem) {
        System.out.println("[servidor] jogador " + numero + " | " + mensagem);
    }
}
