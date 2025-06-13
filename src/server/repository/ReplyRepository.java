package server.repository;

import server.model.MessageReply;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ReplyRepository {
    // Mapeia topicId para uma lista de respostas associadas a ele
    private final Map<String, List<MessageReply>> topicReplies;
    private final AtomicInteger nextReplyId; // ID global para todas as respostas

    public ReplyRepository() {
        topicReplies = new ConcurrentHashMap<>();
        nextReplyId = new AtomicInteger(1);
    }

    public void save(MessageReply reply) {
        // Garante que a lista de respostas para este tópico exista
        topicReplies.computeIfAbsent(reply.getTopicId(), k -> new ArrayList<>()).add(reply);
    }

    public List<MessageReply> findByTopicId(String topicId) {
        // Retorna uma cópia da lista para evitar ConcurrentModificationException
        // e para que as alterações na lista retornada não afetem o mapa interno.
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
            // Remove a resposta da lista
            replies.removeIf(reply -> reply.getId().equals(replyId));
        }
    }

    public String getNextReplyId() {
        return String.valueOf(nextReplyId.getAndIncrement());
    }
}