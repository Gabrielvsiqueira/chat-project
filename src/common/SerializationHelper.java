package common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utilitários para serializar e desserializar mensagens ProtocolMessage
 * usando um protocolo baseado em texto (JSON por linha).
 * Esta abordagem é mais robusta e compatível com diferentes linguagens.
 */
public class SerializationHelper {
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Escreve um ProtocolMessage no PrintWriter como uma única linha de texto JSON.
     *
     * @param message O ProtocolMessage a ser enviado.
     * @param writer  O PrintWriter do socket.
     * @throws IOException Se ocorrer um erro de I/O.
     */
    public static void writeMessage(ProtocolMessage message, PrintWriter writer) throws IOException {
        String json = GSON.toJson(message);
        writer.println(json); // Envia a string JSON seguida por uma nova linha
        writer.flush();       // Garante que os dados sejam enviados imediatamente
    }

    /**
     * Lê uma linha de texto JSON do BufferedReader e a converte em um ProtocolMessage.
     *
     * @param reader O BufferedReader do socket.
     * @return O ProtocolMessage lido. Retorna null se o fluxo terminar.
     * @throws IOException         Se ocorrer um erro de I/O.
     * @throws JsonSyntaxException Se a string JSON lida for inválida.
     */
    public static ProtocolMessage readMessage(BufferedReader reader) throws IOException, JsonSyntaxException {
        String json = reader.readLine(); // Lê uma linha inteira
        if (json == null) {
            return null; // Fim do fluxo (cliente desconectou)
        }
        return GSON.fromJson(json, ProtocolMessage.class); // Desserializa a String JSON para o objeto
    }
}