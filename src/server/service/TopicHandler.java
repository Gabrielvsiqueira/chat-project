package server.service;

import common.ClientInfo;
import common.ProtocolMessage;
import common.SerializationHelper;
import server.model.MessageReply;
import server.model.Topic;
import server.model.User;
import server.repository.ReplyRepository;
import server.repository.TopicRepository;

import java.io.IOException;
import java.io.PrintWriter; // Corrigido: Importa PrintWriter
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Handles forum topics (create, list, broadcast) and replies to topics.
 */
public class TopicHandler {
    private final TopicRepository topicRepository;
    private final ReplyRepository replyRepository;
    private final AuthHandler authHandler;
    private final Consumer<String> logConsumer;
    private final Map<String, PrintWriter> activeClientOutputs; // Corrigido: O tipo do mapa é PrintWriter

    public TopicHandler(TopicRepository topicRepository, ReplyRepository replyRepository, AuthHandler authHandler, Consumer<String> logConsumer, Map<String, PrintWriter> activeClientOutputs) {
        this.topicRepository = topicRepository;
        this.replyRepository = replyRepository;
        this.authHandler = authHandler;
        this.logConsumer = logConsumer;
        this.activeClientOutputs = activeClientOutputs;
    }

    /**
     * Handles operation 050 (Create Topic).
     */
    public ProtocolMessage handleCreateTopic(ProtocolMessage request, ClientInfo clientInfo) {
        String token = request.getToken();
        String title = request.getTitle();
        String subject = request.getSubject();
        String msgContent = request.getMessageContent();

        logConsumer.accept("Attempting to create topic by client: '" + clientInfo.getName() + "'. Title: '" + title + "'");

        if (token == null || token.isEmpty() || title == null || title.isEmpty() ||
                subject == null || subject.isEmpty() || msgContent == null || msgContent.isEmpty()) {
            return ProtocolMessage.createErrorMessage("052", "Token, title, subject, or message cannot be null/empty.");
        }

        ClientInfo authClient = authHandler.getAuthenticatedClientInfo(token);
        if (authClient == null || !authClient.getUserId().equals(clientInfo.getUserId())) {
            return ProtocolMessage.createErrorMessage("052", "Invalid or expired token.");
        }

        String topicId = topicRepository.getNextTopicId();
        Topic newTopic = new Topic(topicId, title, subject, msgContent, authClient.getUserId());
        topicRepository.save(newTopic);

        logConsumer.accept("New topic created by " + authClient.getUserId() + ": '" + title + "' (ID: " + topicId + ")");

        // Não há necessidade de broadcast aqui, a menos que o protocolo exija

        return new ProtocolMessage("051", "Topic created successfully!");
    }

    /**
     * Handles operation 060 (Send Reply).
     */
    public ProtocolMessage handleReplyMessage(ProtocolMessage request, ClientInfo clientInfo) {
        String token = request.getToken();
        String topicId = request.getId();
        String msgContent = request.getMessageContent();

        if (token == null || token.isEmpty() || topicId == null || topicId.isEmpty() || msgContent == null || msgContent.isEmpty()) {
            return ProtocolMessage.createErrorMessage("062", "Token, Topic ID, or message cannot be null/empty.");
        }

        ClientInfo authClient = authHandler.getAuthenticatedClientInfo(token);
        if (authClient == null || !authClient.getUserId().equals(clientInfo.getUserId())) {
            return ProtocolMessage.createErrorMessage("062", "Invalid or expired token.");
        }

        if (topicRepository.findById(topicId) == null) {
            return ProtocolMessage.createErrorMessage("062", "Topic not found.");
        }

        String replyId = replyRepository.getNextReplyId();
        MessageReply newReply = new MessageReply(replyId, topicId, authClient.getUserId(), msgContent);
        replyRepository.save(newReply);

        logConsumer.accept("New reply created by " + authClient.getUserId() + " to topic '" + topicId + "' (Reply ID: " + replyId + ")");

        return new ProtocolMessage("061", "Reply sent successfully!");
    }

    /**
     * Handles operation 070 (Get Replies).
     */
    public ProtocolMessage handleGetReplies(ProtocolMessage request, ClientInfo clientInfo) {
        String topicId = request.getId();

        if (topicId == null || topicId.isEmpty()) {
            return ProtocolMessage.createErrorMessage("072", "Topic ID cannot be null/empty.");
        }

        if (topicRepository.findById(topicId) == null) {
            return ProtocolMessage.createErrorMessage("072", "Topic not found.");
        }

        List<MessageReply> replies = replyRepository.findByTopicId(topicId);
        replies.sort(Comparator.comparingLong(MessageReply::getTimestamp));

        List<Map<String, String>> replyData = replies.stream()
                .map(reply -> {
                    Map<String, String> replyMap = new ConcurrentHashMap<>();
                    replyMap.put("id", reply.getId());
                    User authorUser = authHandler.getUserByUsername(reply.getAuthorUserId());
                    replyMap.put("nick", authorUser != null ? authorUser.getNickname() : "Unknown");
                    replyMap.put("msg", reply.getContent());
                    return replyMap;
                })
                .collect(Collectors.toList());

        ProtocolMessage response = new ProtocolMessage("071");
        response.setMessageList(replyData);
        return response;
    }

    /**
     * Handles operation 075 (Get Topics).
     */
    public ProtocolMessage handleGetTopics(ProtocolMessage request, ClientInfo clientInfo) {
        List<Map<String, String>> topicsData = topicRepository.findAll().stream()
                .map(topic -> {
                    Map<String, String> topicMap = new ConcurrentHashMap<>();
                    topicMap.put("id", topic.getId());
                    topicMap.put("title", topic.getTitle());
                    topicMap.put("subject", topic.getSubject());
                    User authorUser = authHandler.getUserByUsername(topic.getAuthorUserId());
                    topicMap.put("nick", authorUser != null ? authorUser.getNickname() : "Unknown");
                    topicMap.put("msg", topic.getContent());
                    return topicMap;
                })
                .collect(Collectors.toList());

        ProtocolMessage response = new ProtocolMessage("076");
        response.setMessageList(topicsData);
        logConsumer.accept("Sent 076 response with " + topicsData.size() + " topics.");
        return response;
    }

    /**
     * Broadcasts a message to all or specific clients.
     * This is a generic broadcast method now.
     */
    private void broadcastMessage(ProtocolMessage message) {
        logConsumer.accept("Broadcasting message op: " + message.getOperationCode() + " to all authenticated clients.");

        for (Map.Entry<String, PrintWriter> entry : new ConcurrentHashMap<>(activeClientOutputs).entrySet()) {
            try {
                // *** CORREÇÃO APLICADA AQUI ***
                // O método agora usa o PrintWriter do mapa, que é o tipo correto.
                SerializationHelper.writeMessage(message, entry.getValue());
            } catch (IOException e) {
                logConsumer.accept("Error broadcasting to client " + entry.getKey() + ": " + e.getMessage() + ". Removing client output.");
                activeClientOutputs.remove(entry.getKey());
            }
        }
    }
}