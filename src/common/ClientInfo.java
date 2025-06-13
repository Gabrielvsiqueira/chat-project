package common;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Objects;

/**
 * Representa as informações de um cliente conectado ao servidor.
 * Inclui o nome de exibição, ID de usuário (após login), token de autenticação,
 * endereço IP e porta.
 * A implementação de equals e hashCode é crucial para identificar clientes
 * de forma única pelo seu endpoint de rede (endereço IP e porta).
 *
 * Implementa Serializable para permitir a passagem através de ObjectOutputStream/InputStream.
 */
public class ClientInfo implements Serializable {
    private static final long serialVersionUID = 1L; // Adicionar serialVersionUID

    private String name;
    private String userId;
    private String token;
    private InetAddress address;
    private int port;

    // ... (restante dos construtores e getters/setters permanecem os mesmos)

    public ClientInfo(String name, InetAddress address, int port) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.userId = null;
        this.token = null;
    }

    public ClientInfo(String name, String userId, String token, InetAddress address, int port) {
        this.name = name;
        this.userId = userId;
        this.token = token;
        this.address = address;
        this.port = port;
    }

    public String getName() { return name; }
    public String getUserId() { return userId; }
    public String getToken() { return token; }
    public InetAddress getAddress() { return address; }
    public int getPort() { return port; }

    public void setName(String name) { this.name = name; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setToken(String token) { this.token = token; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientInfo that = (ClientInfo) o;
        return port == that.port && Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }

    @Override
    public String toString() {
        return name + " (" + (userId != null ? userId : "N/A") + ") - " + address.getHostAddress() + ":" + port;
    }
}