package server.repository;

import server.model.Topic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TopicRepository {
    private final Map<String, Topic> topicDatabase; // topicId -> Topic object
    private final AtomicInteger nextTopicId;

    public TopicRepository() {
        topicDatabase = new ConcurrentHashMap<>();
        nextTopicId = new AtomicInteger(1);
        topicDatabase.put("1", new Topic("1", "Bem-vindos ao Fórum", "Introdução", "Olá a todos! Este é o primeiro tópico do nosso fórum.", "admin"));
        topicDatabase.put("2", new Topic("2", "Dicas de Programação Java", "Desenvolvimento", "Compartilhe suas melhores dicas e truques de Java aqui!", "user1"));
    }

    public void save(Topic topic) {
        topicDatabase.put(topic.getId(), topic);
    }

    public Topic findById(String id) {
        return topicDatabase.get(id);
    }

    public List<Topic> findAll() {
        return new ArrayList<>(topicDatabase.values());
    }

    public String getNextTopicId() {
        return String.valueOf(nextTopicId.getAndIncrement());
    }
}