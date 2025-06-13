package server.repository;

import server.model.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepository {
    private final Map<String, User> userDatabase; // username -> User object

    public UserRepository() {
        userDatabase = new ConcurrentHashMap<>();
        // Adiciona alguns usuários de teste
        userDatabase.put("admin", new User("admin", "adminpass", "Administrator", "admin"));
        userDatabase.put("user1", new User("user1", "user1pass", "User One", "common"));
        userDatabase.put("user2", new User("user2", "user2pass", "User Two", "common"));
    }

    public User findByUsername(String username) {
        return userDatabase.get(username);
    }

    public boolean existsByUsername(String username) {
        return userDatabase.containsKey(username);
    }

    public void save(User user) {
        userDatabase.put(user.getUsername(), user);
    }

    public void deleteByUsername(String username) {
        userDatabase.remove(username);
    }
}