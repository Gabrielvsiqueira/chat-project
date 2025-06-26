package server.model;

import java.io.Serializable;

public class MessageReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String topicId;
    private String authorUserId;
    private String content;
    private long timestamp;

    public MessageReply(String id, String topicId, String authorUserId, String content) {
        this.id = id;
        this.topicId = topicId;
        this.authorUserId = authorUserId;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getTopicId() { return topicId; }
    public String getAuthorUserId() { return authorUserId; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }

    public void markAsDeleted() {
        this.content = "Mensagem Apagada";
    }
}