package common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ProtocolMessage implements Serializable { // Mantendo Serializable por boa prática, embora serializemos JSON String
    private static final long serialVersionUID = 1L;
    private static final Gson GSON_TO_STRING = new GsonBuilder().setPrettyPrinting().create(); // Para formatar o JSON no toString


    @SerializedName("op")
    private String operationCode;

    @SerializedName("user")
    private String user;
    @SerializedName("pass")
    private String password;
    @SerializedName("token")
    private String token;
    @SerializedName("msg")
    private String messageContent; // Usado para mensagens de erro, conteúdo de tópico e conteúdo de resposta
    @SerializedName("nick")
    private String nickname;
    @SerializedName("new_nick")
    private String newNickname;
    @SerializedName("new_pass")
    private String newPassword;
    @SerializedName("title")
    private String title;
    @SerializedName("subject")
    private String subject;

    @SerializedName("topic_id") // Para broadcast de tópicos (055)
    private String topicId;
    @SerializedName("topic_title") // Para broadcast de tópicos (055)
    private String topicTitle;
    @SerializedName("topic_subject") // Para broadcast de tópicos (055)
    private String topicSubject;
    @SerializedName("topic_content") // Para broadcast de tópicos (055)
    private String topicContent;
    @SerializedName("topic_author") // Para broadcast de tópicos (055)
    private String topicAuthor;

    // Para "Enviar Mensagem (Responder mensagem)" (060), "Receber Respostas" (070), "Apagar Mensagem (admin)" (100)
    @SerializedName("id") // ID da mensagem/tópico (para operações de resposta e exclusão)
    private String id; // Definido como String para ser flexível (topicId, messageId)

    // Para "Receber Respostas" (071) e "Receber Tópicos" (076)
    @SerializedName("msg_list") // Lista genérica para mensagens/tópicos
    private List<Map<String, String>> messageList; // Usar um nome mais genérico

    // Os campos 'topics' e 'users' já existem e podem ser usados para 'msg_list' dependendo do contexto.
    // Se "msg_list" é sempre uma lista de tópicos/mensagens, podemos reutilizar ou adicionar um novo.
    // Vou usar 'messageList' para generalizar. Se você quiser campos separados para topics e replies, é só adicionar.
    @SerializedName("topics") // Já existe, usado em 061
    private List<Map<String, String>> topics;
    @SerializedName("users") // Já existe, para listar usuários (embora o protocolo não tenha opcode para isso)
    private List<Map<String, String>> users;


    public ProtocolMessage(String operationCode) {
        this.operationCode = operationCode;
    }

    // --- Getters e Setters (já estão no seu código, só para referência) ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<Map<String, String>> getMessageList() { return messageList; }
    public void setMessageList(List<Map<String, String>> messageList) { this.messageList = messageList; }

    // ... (restante dos getters e setters existentes)
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
    @Override
    public String toString() {
        // Usa GSON para serializar o próprio objeto para uma string JSON legível
        return GSON_TO_STRING.toJson(this);
    }
}