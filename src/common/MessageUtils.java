package common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;

public class MessageUtils {
    private static final Gson GSON = new GsonBuilder().create();

    public static String serializeMessageToJson(ProtocolMessage message) {
        return GSON.toJson(message);
    }

    public static ProtocolMessage deserializeMessageFromJson(String jsonString) throws JsonSyntaxException {
        return GSON.fromJson(jsonString, ProtocolMessage.class);
    }
}