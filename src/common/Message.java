package common;

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        CLIENT_CONNECT(1),
        CLIENT_LIST_UPDATE(2),
        SEND_MESSAGE(3),
        MESSAGE_RECEIVED(4),
        CLIENT_DISCONNECT(5);

        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Type fromValue(int value) {
            for (Type type : Type.values()) {
                if (type.getValue() == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid message type: " + value);
        }
    }

    private Type type;
    private String clientName;
    private String content;
    private String targetClient;
    private boolean broadcast;
    private List<ClientInfo> clientList;

    public Message(Type type) {
        this.type = type;
    }

    public Message(Type type, String clientName) {
        this.type = type;
        this.clientName = clientName;
    }

    // Getters and setters
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTargetClient() { return targetClient; }
    public void setTargetClient(String targetClient) { this.targetClient = targetClient; }

    public boolean isBroadcast() { return broadcast; }
    public void setBroadcast(boolean broadcast) { this.broadcast = broadcast; }

    public List<ClientInfo> getClientList() { return clientList; }
    public void setClientList(List<ClientInfo> clientList) { this.clientList = clientList; }
}