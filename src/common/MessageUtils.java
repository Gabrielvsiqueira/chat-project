package common;

import java.io.*;
import java.net.DatagramPacket;

public class MessageUtils {

    public static byte[] serializeMessage(Message message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        oos.close();
        return baos.toByteArray();
    }

    public static Message deserializeMessage(DatagramPacket packet) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Message message = (Message) ois.readObject();
        ois.close();
        return message;
    }
}
