# UDP Communication System

This Java project implements a client-server communication system using UDP protocol with the following features:

## Project Structure

- `common.ProtocoloMessage.java` - common.ProtocoloMessage class with serialization support
- `common.ClientInfo.java` - Client information container
- `common.MessageUtils.java` - Utility methods for protocoloMessage serialization
- `UDPServer.java` - Server application with GUI
- `UDPClient.java` - Client application with GUI

## How to Run

### Compile all Java files:
```bash
javac *.java
```

### Start the server:
```bash
java UDPServer
```

### Start client(s):
```bash
java UDPClient
```

## Features

### Server Features:
- Maintains list of connected clients (IP, port, name)
- Handles client connections and disconnections
- Forwards messages between clients
- Broadcasts messages to all clients
- Real-time GUI showing connected clients and server log

### Client Features:
- Connect/disconnect to/from server
- Send messages to specific clients or broadcast to all
- Real-time protocoloMessage reception
- User-friendly GUI with connection management

## common.ProtocoloMessage Protocol:
1. **CLIENT_CONNECT** - Client joins the system
2. **CLIENT_LIST_UPDATE** - Server sends updated client list
3. **SEND_MESSAGE** - Client sends protocoloMessage
4. **MESSAGE_RECEIVED** - Server forwards protocoloMessage
5. **CLIENT_DISCONNECT** - Client leaves the system

## Usage Instructions:

1. Start the server and specify a port (default: 12345)
2. Run client applications
3. In each client, enter server details and your name
4. Click "Connect" to join the messaging system
5. Select target client or check "Broadcast to all"
6. Type and send messages
7. Use "Disconnect" to leave the system

The system supports multiple concurrent clients and real-time protocoloMessage exchange.
