# UDP Communication System

This Java project implements a client-server communication system using UDP protocol with the following features:

## Project Structure

- `common.ProtocoloMessage.java` - common.ProtocoloMessage class with serialization support
- `common.ClientInfo.java` - Client information container
- `common.MessageUtils.java` - Utility methods for protocoloMessage serialization
- `UDPServer.java` - Server application with GUI
- `UDPClient.java` - Client application with GUI

## How to Run
- To run this application, you'll need to install the GSON library. 
- We recommend installing the library using this link: [Download da biblioteca GSON](https://search.maven.org/artifact/com.google.code.gson/gson/2.13.1/jar?eh=)
- After downloading, you'll need to add the library as a dependency to this project.

## Install GSON in Project Dependencies
- Go to File > Project Structure... `(or press Ctrl+Alt+Shift+S)`.
- In the "Project Structure" window, select Modules from the left-hand menu. Then, select your project's module.
- Go to the Dependencies tab.
- Click the + (plus) button on the right side and select JARs or directories....
- Navigate to your project's lib (or libs) folder, select the GSON JAR file you downloaded, and click OK.
- Ensure that the dependency's "Scope" is set to Compile (or Runtime, depending on your needs, but Compile is most common for libraries).
- Click Apply and then OK.

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

1. Start the server and specify a port or use default port (default: 12345)
2. Run client applications
3. In each client, enter server details and your name
4. Click "Connect" to join the messaging system
5. Select target client or check "Broadcast to all"
6. Type and send messages
7. Use "Disconnect" to leave the system

The system supports multiple concurrent clients and real-time protocoloMessage exchange.
