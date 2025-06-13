package server.model;

import java.io.Serializable;

public class MessageReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id; // ID da resposta (sequencial dentro do tópico, ou global?)
    private String topicId; // ID do tópico ao qual esta resposta pertence
    private String authorUserId; // ID do usuário que postou a resposta
    private String content; // Conteúdo da resposta
    private long timestamp; // Para ordenar respostas (opcional, mas bom para fóruns)

    public MessageReply(String id, String topicId, String authorUserId, String content) {
        this.id = id;
        this.topicId = topicId;
        this.authorUserId = authorUserId;
        this.content = content;
        this.timestamp = System.currentTimeMillis(); // Define o timestamp no momento da criação
    }

    public String getId() { return id; }
    public String getTopicId() { return topicId; }
    public String getAuthorUserId() { return authorUserId; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }

    // Métodos para "apagar" uma mensagem (opção 101)
    public void markAsDeleted() {
        this.content = "Mensagem Apagada";
        // Opcionalmente, pode-se também mudar o autor ou adicionar uma flag de "apagado"
    }
}