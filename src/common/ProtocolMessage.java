package common;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class ProtocolMessage {
    @SerializedName("op")
    private String operationCode; // Código da operação (ex: "000", "001", "010")
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
    @SerializedName("topics")
    private List<Map<String, String>> topics;
    @SerializedName("users")
    private List<Map<String, String>> users;

    public ProtocolMessage(String operationCode) {
        this.operationCode = operationCode;
    }
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

    public List<Map<String, String>> getTopics() { return topics; }
    public void setTopics(List<Map<String, String>> topics) { this.topics = topics; }

    public List<Map<String, String>> getUsers() { return users; }
    public void setUsers(List<Map<String, String>> users) { this.users = users; }

    public static ProtocolMessage createErrorMessage(String opCode, String msg) {
        ProtocolMessage errorMsg = new ProtocolMessage(opCode);
        errorMsg.setMessageContent(msg);
        return errorMsg;
    }
}