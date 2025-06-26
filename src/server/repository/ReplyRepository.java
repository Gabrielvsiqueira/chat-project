package server.repository;

import server.model.MessageReply;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ReplyRepository {
    private final Map<String, List<MessageReply>> topicReplies;
    private final AtomicInteger nextReplyId;

    public ReplyRepository() {
        topicReplies = new ConcurrentHashMap<>();
        nextReplyId = new AtomicInteger(1);
    }

    public void save(MessageReply reply) {
        topicReplies.computeIfAbsent(reply.getTopicId(), k -> new ArrayList<>()).add(reply);
    }

    public List<MessageReply> findByTopicId(String topicId) {
        return new ArrayList<>(topicReplies.getOrDefault(topicId, new ArrayList<>()));
    }

    public MessageReply findReplyByIdInTopic(String topicId, String replyId) {
        List<MessageReply> replies = topicReplies.get(topicId);
        if (replies != null) {
            return replies.stream()
                    .filter(r -> r.getId().equals(replyId))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public void deleteReply(String topicId, String replyId) {
        List<MessageReply> replies = topicReplies.get(topicId);
        if (replies != null) {
            replies.removeIf(reply -> reply.getId().equals(replyId));
        }
    }

    public String getNextReplyId() {
        return String.valueOf(nextReplyId.getAndIncrement());
    }
}