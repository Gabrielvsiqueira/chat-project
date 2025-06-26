package common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class SerializationHelper {
    private static final Gson GSON = new GsonBuilder().create();

    public static void writeMessage(ProtocolMessage message, PrintWriter writer) throws IOException {
        String json = GSON.toJson(message);
        writer.println(json);
        writer.flush();
    }

    public static ProtocolMessage readMessage(BufferedReader reader) throws IOException, JsonSyntaxException {
        String json = reader.readLine();
        if (json == null) {
            return null;
        }
        return GSON.fromJson(json, ProtocolMessage.class);
    }
}