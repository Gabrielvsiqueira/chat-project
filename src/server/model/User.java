package server.model;

public class User {
    // --- CAMPO ADICIONADO ---
    private int id; // Campo para armazenar o ID único do usuário

    private String username;
    private String password;
    private String nickname;
    private String role;

    // Seus construtores existentes (não precisam ser alterados)
    public User(String username, String password, String nickname, String role) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
    }

    // --- MÉTODOS GETTER E SETTER ADICIONADOS ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // --- Fim dos métodos adicionados ---


    // Getters e Setters existentes (não precisam ser alterados)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}