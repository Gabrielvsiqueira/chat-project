package common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ProtocolMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON_TO_STRING = new GsonBuilder().setPrettyPrinting().create();

    @SerializedName("op")
    private String operationCode;
    @SerializedName("user")
    private String user;
    @SerializedName("pass")
    private String password;
    @SerializedName("token")
    private String token;
    @SerializedName("msg")
    private String messageContent;
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
    @SerializedName("id")
    private String id;
    @SerializedName("msg_list")
    private List<Map<String, String>> messageList;
    @SerializedName("topics")
    private List<Map<String, String>> topics;
    @SerializedName("users")
    private List<Map<String, String>> users;
    @SerializedName("user_list")
    private List<String> userList;

    public ProtocolMessage(String operationCode) {
        this.operationCode = operationCode;
    }

    // --- NOVO CONSTRUTOR: Para mensagens com código de operação e conteúdo ---
    public ProtocolMessage(String operationCode, String messageContent) {
        this.operationCode = operationCode;
        this.messageContent = messageContent;
    }
    // --- FIM DO NOVO CONSTRUTOR ---


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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<Map<String, String>> getMessageList() { return messageList; }
    public void setMessageList(List<Map<String, String>> messageList) { this.messageList = messageList; }

    public List<Map<String, String>> getTopics() { return topics; }
    public void setTopics(List<Map<String, String>> topics) { this.topics = topics; }

    public List<Map<String, String>> getUsers() { return users; }
    public void setUsers(List<Map<String, String>> users) { this.users = users; }

    public List<String> getUserList() { return userList; }
    public void setUserList(List<String> userList) { this.userList = userList; }

    public static ProtocolMessage createErrorMessage(String opCode, String msg) {
        ProtocolMessage errorMsg = new ProtocolMessage(opCode);
        errorMsg.setMessageContent(msg);
        return errorMsg;
    }

    @Override
    public String toString() {
        return GSON_TO_STRING.toJson(this);
    }
}