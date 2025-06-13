package common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Utilitários para serializar e desserializar mensagens ProtocolMessage
 * usando GSON via ObjectOutputStream e ObjectInputStream.
 * Esta abordagem é adequada para TCP onde se espera um fluxo de objetos.
 *
 * NOTA IMPORTANTE: Para que ObjectInputStream e ObjectOutputStream funcionem corretamente,
 * tanto o cliente quanto o servidor devem usar as mesmas versões das classes
 * ProtocolMessage e GSON. Alterações nessas classes exigem recompilação em ambos os lados.
 */
public class SerializationHelper {
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Escreve um ProtocolMessage no OutputStream.
     * O objeto é serializado para JSON, e a string JSON é escrita no fluxo.
     *
     * @param message O ProtocolMessage a ser enviado.
     * @param out O ObjectOutputStream do socket.
     * @throws IOException Se ocorrer um erro de I/O.
     */
    public static void writeMessage(ProtocolMessage message, ObjectOutputStream out) throws IOException {
        String json = GSON.toJson(message);
        out.writeObject(json); // Escreve a String JSON como um objeto
        out.flush(); // Garante que os dados sejam enviados imediatamente
    }

    /**
     * Lê um ProtocolMessage do InputStream.
     * Lê uma String JSON do fluxo e a desserializa para um ProtocolMessage.
     *
     * @param in O ObjectInputStream do socket.
     * @return O ProtocolMessage lido.
     * @throws IOException Se ocorrer um erro de I/O.
     * @throws ClassNotFoundException Se a classe do objeto lido não for encontrada (ocorre se não for String).
     * @throws JsonSyntaxException Se a string JSON lida for inválida.
     */
    public static ProtocolMessage readMessage(ObjectInputStream in) throws IOException, ClassNotFoundException, JsonSyntaxException {
        String json = (String) in.readObject(); // Lê a String JSON
        return GSON.fromJson(json, ProtocolMessage.class); // Desserializa a String JSON para o objeto
    }
}