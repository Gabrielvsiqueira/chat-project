package server.model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password; // <-- Este campo precisa de getter/setter
    private String nickname; // <-- Este campo precisa de getter/setter
    private String role;     // <-- Este campo precisa de getter

    public User(String username, String password, String nickname, String role) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
    }

    // --- Getters ---
    public String getUsername() { return username; }

    // Adicione ou verifique se este método está presente
    public String getPassword() { return password; }

    // Adicione ou verifique se este método está presente
    public String getNickname() { return nickname; }

    // Adicione ou verifique se este método está presente
    public String getRole() { return role; } // Para verificar se é admin/common

    // --- Setters (se você precisa alterá-los após a criação) ---

    // Adicione ou verifique se este método está presente
    public void setPassword(String password) { this.password = password; }

    // Adicione ou verifique se este método está presente
    public void setNickname(String nickname) { this.nickname = nickname; }

    // Note: Geralmente, o role não é alterado após a criação, então um setter pode não ser necessário.
    // public void setRole(String role) { this.role = role; }
}