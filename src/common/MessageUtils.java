package common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

/**
 * Utilitários para serializar e desserializar mensagens usando GSON.
 * Esta classe substitui a serialização de objetos Java padrão por JSON,
 * o que é mais robusto e interoperável para comunicação em rede.
 */
public class MessageUtils {
    // Instância de Gson para serialização/desserialização.
    // GsonBuilder().create() é usado para uma configuração padrão.
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Serializa um objeto ProtocolMessage para um array de bytes (formato JSON).
     *
     * @param message O objeto ProtocolMessage a ser serializado.
     * @return Um array de bytes representando a mensagem JSON.
     * @throws IOException Se ocorrer um erro durante a serialização (improvável com GSON para String).
     */
    public static byte[] serializeMessage(ProtocolMessage message) throws IOException {
        String json = GSON.toJson(message); // Converte o objeto Java para uma string JSON
        return json.getBytes(StandardCharsets.UTF_8); // Converte a string JSON para bytes usando UTF-8
    }

    /**
     * Desserializa um DatagramPacket para um objeto ProtocolMessage.
     * Extrai os bytes do pacote, converte-os para uma string JSON e, em seguida,
     * para um objeto ProtocolMessage.
     *
     * @param packet O DatagramPacket contendo os dados da mensagem JSON.
     * @return O objeto ProtocolMessage desserializado.
     */
    public static ProtocolMessage deserializeMessage(DatagramPacket packet) {
        // Converte os bytes do pacote para uma string JSON
        String json = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
        return GSON.fromJson(json, ProtocolMessage.class); // Converte a string JSON para um objeto Java
    }
}