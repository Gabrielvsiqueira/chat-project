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
import java.io.ObjectOutputStream;
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
    private final Map<String, ObjectOutputStream> activeClientOutputs;

    public TopicHandler(TopicRepository topicRepository, ReplyRepository replyRepository, AuthHandler authHandler, Consumer<String> logConsumer, Map<String, ObjectOutputStream> activeClientOutputs) {
        this.topicRepository = topicRepository;
        this.replyRepository = replyRepository;
        this.authHandler = authHandler;
        this.logConsumer = logConsumer;
        this.activeClientOutputs = activeClientOutputs;
    }

    /**
     * Handles operation 050 (Create Topic).
     * @param request The topic creation request.
     * @param clientInfo The ClientInfo of the sender.
     * @return ProtocolMessage for success (051) or error (052).
     */
    public ProtocolMessage handleCreateTopic(ProtocolMessage request, ClientInfo clientInfo) {
        String token = request.getToken();
        String title = request.getTitle();
        String subject = request.getSubject();
        String msgContent = request.getMessageContent();

        logConsumer.accept("Attempting to create topic by client: '" + clientInfo.getName() + "' (ID: " + clientInfo.getUserId() + ") from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort() + ". Title: '" + title + "'");

        if (token == null || token.isEmpty() || title == null || title.isEmpty() ||
                subject == null || subject.isEmpty() || msgContent == null || msgContent.isEmpty()) {
            logConsumer.accept("Topic creation failed: Missing token, title, subject, or message content.");
            return ProtocolMessage.createErrorMessage("052", "Token, title, subject, or message cannot be null/empty.");
        }

        ClientInfo authClient = authHandler.getAuthenticatedClientInfo(token);
        if (authClient == null || !authClient.getUserId().equals(clientInfo.getUserId())) {
            logConsumer.accept("Topic creation failed: Invalid or expired token for user '" + clientInfo.getUserId() + "'.");
            return ProtocolMessage.createErrorMessage("052", "Invalid or expired token.");
        }

        // Length validations as per protocol: [6:???] for title, subject, msg
        // Assuming '???' as 255 for title/subject, and a larger value (e.g., 8192) for msg content
        if (title.length() < 6 || title.length() > 255) {
            logConsumer.accept("Topic creation failed: Title length out of range [6-255].");
            return ProtocolMessage.createErrorMessage("052", "Title must be 6-255 characters.");
        }
        if (subject.length() < 6 || subject.length() > 255) {
            logConsumer.accept("Topic creation failed: Subject length out of range [6-255].");
            return ProtocolMessage.createErrorMessage("052", "Subject must be 6-255 characters.");
        }
        if (msgContent.length() < 6 || msgContent.length() > 8192) { // Max 8KB for message content
            logConsumer.accept("Topic creation failed: Message content length out of range [6-8192].");
            return ProtocolMessage.createErrorMessage("052", "Message content must be 6-8192 characters.");
        }

        String topicId = topicRepository.getNextTopicId();
        Topic newTopic = new Topic(topicId, title, subject, msgContent, authClient.getUserId());
        topicRepository.save(newTopic);

        logConsumer.accept("New topic created by " + authClient.getUserId() + ": '" + title + "' (ID: " + topicId + ") from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        broadcastTopic(newTopic);

        return new ProtocolMessage("051", "Topic created successfully!");
    }

    /**
     * Handles operation 060 (Send Reply).
     * @param request The reply message request.
     * @param clientInfo The ClientInfo of the sender.
     * @return ProtocolMessage for success (061) or error (062).
     */
    public ProtocolMessage handleReplyMessage(ProtocolMessage request, ClientInfo clientInfo) {
        String token = request.getToken();
        String topicId = request.getId(); // 'id' refers to the topic ID
        String msgContent = request.getMessageContent();

        logConsumer.accept("Attempting to reply to topic '" + topicId + "' by client: '" + clientInfo.getName() + "' (ID: " + clientInfo.getUserId() + ") from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        if (token == null || token.isEmpty() || topicId == null || topicId.isEmpty() || msgContent == null || msgContent.isEmpty()) {
            logConsumer.accept("Reply failed: Missing token, topic ID, or message content.");
            return ProtocolMessage.createErrorMessage("062", "Token, Topic ID, or message cannot be null/empty.");
        }

        ClientInfo authClient = authHandler.getAuthenticatedClientInfo(token);
        if (authClient == null || !authClient.getUserId().equals(clientInfo.getUserId())) {
            logConsumer.accept("Reply failed: Invalid or expired token for user '" + clientInfo.getUserId() + "'.");
            return ProtocolMessage.createErrorMessage("062", "Invalid or expired token.");
        }

        Topic existingTopic = topicRepository.findById(topicId);
        if (existingTopic == null) {
            logConsumer.accept("Reply failed: Topic with ID '" + topicId + "' not found.");
            return ProtocolMessage.createErrorMessage("062", "Topic not found.");
        }

        if (msgContent.length() < 6 || msgContent.length() > 8192) { // Max 8KB for message content
            logConsumer.accept("Reply failed: Message content length out of range [6-8192].");
            return ProtocolMessage.createErrorMessage("062", "Message content must be 6-8192 characters.");
        }

        String replyId = replyRepository.getNextReplyId();
        MessageReply newReply = new MessageReply(replyId, topicId, authClient.getUserId(), msgContent);
        replyRepository.save(newReply);

        logConsumer.accept("New reply created by " + authClient.getUserId() + " to topic '" + topicId + "' (Reply ID: " + replyId + ")");

        // Optional: Broadcast new reply if protocol desires (e.g., opcode 065)

        return new ProtocolMessage("061", "Reply sent successfully!");
    }

    /**
     * Handles operation 070 (Get Replies).
     * @param request The request message.
     * @param clientInfo The ClientInfo of the sender.
     * @return ProtocolMessage with reply list (071) or error (072).
     */
    public ProtocolMessage handleGetReplies(ProtocolMessage request, ClientInfo clientInfo) {
        String topicId = request.getId();

        logConsumer.accept("Attempting to get replies for topic '" + topicId + "' by client: '" + clientInfo.getName() + "' from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

        if (topicId == null || topicId.isEmpty()) {
            logConsumer.accept("Get replies failed: Topic ID cannot be null/empty.");
            return ProtocolMessage.createErrorMessage("072", "Topic ID cannot be null/empty.");
        }

        Topic existingTopic = topicRepository.findById(topicId);
        if (existingTopic == null) {
            logConsumer.accept("Get replies failed: Topic with ID '" + topicId + "' not found.");
            return ProtocolMessage.createErrorMessage("072", "Topic not found.");
        }

        List<MessageReply> replies = replyRepository.findByTopicId(topicId);
        replies.sort(Comparator.comparingLong(MessageReply::getTimestamp)); // Sort by timestamp

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
        logConsumer.accept("Sent 071 response with " + replyData.size() + " replies for topic '" + topicId + "'.");
        return response;
    }

    /**
     * Handles operation 075 (Get Topics).
     * @param request The request message.
     * @param clientInfo The ClientInfo of the sender.
     * @return ProtocolMessage with topic list (076) or error (077).
     */
    public ProtocolMessage handleGetTopics(ProtocolMessage request, ClientInfo clientInfo) {
        logConsumer.accept("Attempting to get all topics by client: '" + clientInfo.getName() + "' from " + clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort());

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
     * Broadcasts a new/updated topic (opcode 055) to all authenticated clients.
     * @param topic The Topic object to broadcast.
     */
    private void broadcastTopic(Topic topic) {
        ProtocolMessage broadcastTopicMsg = new ProtocolMessage("055");
        broadcastTopicMsg.setTopicId(topic.getId());
        broadcastTopicMsg.setTopicTitle(topic.getTitle());
        broadcastTopicMsg.setTopicSubject(topic.getSubject());
        broadcastTopicMsg.setTopicContent(topic.getContent());

        User authorUser = authHandler.getUserByUsername(topic.getAuthorUserId());
        broadcastTopicMsg.setTopicAuthor(authorUser != null ? authorUser.getNickname() : "Unknown");

        logConsumer.accept("Broadcasting new topic '" + topic.getTitle() + "' (ID: " + topic.getId() + ") to all authenticated clients.");

        for (Map.Entry<String, ObjectOutputStream> entry : new ConcurrentHashMap<>(activeClientOutputs).entrySet()) {
            try {
                SerializationHelper.writeMessage(broadcastTopicMsg, entry.getValue());
                logConsumer.accept("  - Broadcasted (attempted) to client with token: " + entry.getKey());
            } catch (IOException e) {
                logConsumer.accept("Error broadcasting to client " + entry.getKey() + ": " + e.getMessage() + ". Removing client output.");
                activeClientOutputs.remove(entry.getKey());
            }
        }
    }
}