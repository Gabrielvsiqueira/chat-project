package server.repository;

import server.model.User;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class UserRepository {

    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final Map<Integer, User> usersById = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(0);

    public UserRepository() {
        User adminUser = new User("admin123", "admin123", "admin123", "admin");
        save(adminUser);
    }

    public void save(User user) {
        if (user.getId() == 0) {
            int newId = idCounter.incrementAndGet();
            user.setId(newId);
        }
        usersByUsername.put(user.getUsername(), user);
        usersById.put(user.getId(), user);
    }

    public User findByUsername(String username) {
        return usersByUsername.get(username);
    }

    public User findById(int id) {
        return usersById.get(id);
    }

    public boolean existsByUsername(String username) {
        return usersByUsername.containsKey(username);
    }

    public void deleteByUsername(String username) {
        User user = usersByUsername.remove(username);
        if (user != null) {
            usersById.remove(user.getId());
        }
    }

    /**
     * Lista os NOMES de todos os usuários.
     * @return Uma lista de strings com os nomes de usuário.
     */
    public List<String> listAllUsernames() {
        // --- CORREÇÃO (Protocolo 111) ---
        // Retorna apenas a lista de usernames, não a string formatada.
        return usersByUsername.values().stream()
                .map(User::getUsername)
                .collect(Collectors.toList());
    }
}