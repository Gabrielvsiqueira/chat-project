package server.model;

import java.io.Serializable;

public class Topic implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String subject;
    private String content;
    private String authorUserId;

    public Topic(String id, String title, String subject, String content, String authorUserId) {
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.content = content;
        this.authorUserId = authorUserId;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
    public String getAuthorUserId() { return authorUserId; }

    public void markAsDeleted() {
        this.title = "[DELETED]";
        this.subject = "[DELETED]";
        this.content = "Mensagem Apagada";
        this.authorUserId = "[DELETED]";
    }
}