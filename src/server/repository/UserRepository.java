package server.repository;

import server.model.User;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList; // For listAllUsernames

public class UserRepository {
    private final Map<String, User> userDatabase; // username -> User object

    public UserRepository() {
        userDatabase = new ConcurrentHashMap<>();
        // Add test users as specified in the protocol
        userDatabase.put("admin123", new User("admin123", "admin123", "admin123", "admin")); // Specified admin user
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

    public List<String> listAllUsernames() { // New method for opcode 110
        return new ArrayList<>(userDatabase.keySet());
    }
}