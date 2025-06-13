package common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;

/**
 * Utilitários para serializar e desserializar mensagens JSON.
 * Esta classe é agora puramente para manipulação de String JSON e não de I/O de rede.
 */
public class MessageUtils {
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Serializa um objeto ProtocolMessage para uma String JSON.
     *
     * @param message O objeto ProtocolMessage a ser serializado.
     * @return Uma String JSON representando a mensagem.
     */
    public static String serializeMessageToJson(ProtocolMessage message) {
        return GSON.toJson(message);
    }

    /**
     * Desserializa uma String JSON para um objeto ProtocolMessage.
     *
     * @param jsonString A String JSON a ser desserializada.
     * @return O objeto ProtocolMessage desserializado.
     * @throws JsonSyntaxException Se a string JSON for inválida.
     */
    public static ProtocolMessage deserializeMessageFromJson(String jsonString) throws JsonSyntaxException {
        return GSON.fromJson(jsonString, ProtocolMessage.class);
    }
}