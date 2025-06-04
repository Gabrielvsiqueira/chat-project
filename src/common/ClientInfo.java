package common;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Objects;

public class ClientInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private InetAddress address;
    private int port;

    public ClientInfo(String name, InetAddress address, int port) {
        this.name = name;
        this.address = address;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return name + " (" + address.getHostAddress() + ":" + port + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientInfo)) return false;
        ClientInfo that = (ClientInfo) o;
        return port == that.port &&
                Objects.equals(name, that.name) &&
                Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, address, port);
    }
}
