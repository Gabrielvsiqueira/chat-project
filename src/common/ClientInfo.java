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
 */
public class ClientInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name; // Nome de exibição/Apelido do cliente
    private String userId; // ID de usuário único (do protocolo), definido após login/registro
    private String token; // Token de autenticação, definido após login
    private InetAddress address; // Endereço IP do cliente
    private int port; // Porta do cliente

    /**
     * Construtor para criar uma ClientInfo inicial (antes do login).
     *
     * @param name    Nome de exibição inicial (ex: "Convidado").
     * @param address Endereço IP do cliente.
     * @param port    Porta do cliente.
     */
    public ClientInfo(String name, InetAddress address, int port) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.userId = null; // Inicialmente nulo, definido após login/registro
        this.token = null; // Inicialmente nulo, definido após login
    }

    /**
     * Construtor completo para ClientInfo, usado pelo servidor após autenticação.
     *
     * @param name    Nome de exibição/Apelido.
     * @param userId  ID de usuário.
     * @param token   Token de autenticação.
     * @param address Endereço IP.
     * @param port    Porta.
     */
    public ClientInfo(String name, String userId, String token, InetAddress address, int port) {
        this.name = name;
        this.userId = userId;
        this.token = token;
        this.address = address;
        this.port = port;
    }

    // Getters
    public String getName() { return name; }
    public String getUserId() { return userId; }
    public String getToken() { return token; }
    public InetAddress getAddress() { return address; }
    public int getPort() { return port; }

    // Setters (usados pelo servidor para atualizar informações do cliente)
    public void setName(String name) { this.name = name; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setToken(String token) { this.token = token; }

    /**
     * Compara dois objetos ClientInfo.
     * A igualdade é baseada no endereço IP e na porta, pois eles identificam
     * unicamente o endpoint de rede de um cliente UDP.
     *
     * @param o O objeto a ser comparado.
     * @return true se os objetos forem iguais, false caso contrário.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientInfo that = (ClientInfo) o;
        return port == that.port &&
                Objects.equals(address, that.address);
    }

    /**
     * Gera o código hash para o objeto ClientInfo.
     * Baseado no endereço IP e na porta.
     *
     * @return O código hash.
     */
    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }

    /**
     * Retorna uma representação em string do objeto ClientInfo.
     *
     * @return String formatada com nome, ID de usuário, IP e porta.
     */
    @Override
    public String toString() {
        return name + " (" + (userId != null ? userId : "N/A") + ") - " + address.getHostAddress() + ":" + port;
    }
}