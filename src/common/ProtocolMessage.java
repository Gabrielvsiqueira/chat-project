package common;

import com.google.gson.annotations.SerializedName;

/**
 * Representa uma mensagem do protocolo de comunicação, serializável via GSON.
 * Contém campos que mapeiam diretamente as chaves JSON definidas no protocolo.
 * O uso de @SerializedName garante que os nomes das chaves JSON correspondam
 * aos nomes dos campos Java, mesmo que sejam diferentes.
 */
public class ProtocolMessage {
    @SerializedName("op")
    private String operationCode; // Código da operação (ex: "000", "001", "010")

    // Campos comuns que podem aparecer em várias mensagens do protocolo
    @SerializedName("user")
    private String user; // Nome de usuário
    @SerializedName("pass")
    private String password; // Senha
    @SerializedName("token")
    private String token; // Token de autenticação
    @SerializedName("msg")
    private String messageContent; // Usado para mensagens de erro e conteúdo de tópico de fórum
    @SerializedName("nick")
    private String nickname; // Apelido (para registro)
    @SerializedName("new_nick")
    private String newNickname; // Novo apelido (para alteração de cadastro)
    @SerializedName("new_pass")
    private String newPassword; // Nova senha (para alteração de cadastro)
    @SerializedName("title")
    private String title; // Título do tópico (para criar tópico)
    @SerializedName("subject")
    private String subject; // Assunto do tópico (para criar tópico)

    // Novos campos para mensagens de broadcast de tópico (op: "055")
    @SerializedName("topic_id")
    private String topicId;
    @SerializedName("topic_title")
    private String topicTitle;
    @SerializedName("topic_subject")
    private String topicSubject;
    @SerializedName("topic_content")
    private String topicContent;
    @SerializedName("topic_author")
    private String topicAuthor;

    /**
     * Construtor para criar uma ProtocolMessage com um código de operação.
     *
     * @param operationCode O código da operação (ex: "000", "001").
     */
    public ProtocolMessage(String operationCode) {
        this.operationCode = operationCode;
    }

    // Getters e Setters para todos os campos
    public String getOperationCode() { return operationCode; }
    public void setOperationCode(String operationCode) { this.operationCode = operationCode; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getMessageContent() { return messageContent; }
    public void setMessageContent(String messageContent) { this.messageContent = messageContent; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getNewNickname() { return newNickname; }
    public void setNewNickname(String newNickname) { this.newNickname = newNickname; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    // Getters e Setters para os novos campos de tópico
    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }

    public String getTopicTitle() { return topicTitle; }
    public void setTopicTitle(String topicTitle) { this.topicTitle = topicTitle; }

    public String getTopicSubject() { return topicSubject; }
    public void setTopicSubject(String topicSubject) { this.topicSubject = topicSubject; }

    public String getTopicContent() { return topicContent; }
    public void setTopicContent(String topicContent) { this.topicContent = topicContent; }

    public String getTopicAuthor() { return topicAuthor; }
    public void setTopicAuthor(String topicAuthor) { this.topicAuthor = topicAuthor; }

    /**
     * Método auxiliar para criar uma mensagem de erro padronizada.
     *
     * @param opCode O código de operação do erro (ex: "002", "012").
     * @param msg    A mensagem de erro.
     * @return Uma nova instância de ProtocolMessage configurada como mensagem de erro.
     */
    public static ProtocolMessage createErrorMessage(String opCode, String msg) {
        ProtocolMessage errorMsg = new ProtocolMessage(opCode);
        errorMsg.setMessageContent(msg);
        return errorMsg;
    }
}