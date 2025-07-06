package com.buccancs.gsr.windows.network;

import com.buccancs.gsr.common.network.CommandProtocol;
import com.buccancs.gsr.common.network.NetworkTransport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TcpServerTransport implements the NetworkTransport interface using TCP/IP sockets
 * for the Windows PC server. It can handle multiple client connections simultaneously.
 */
public class TcpServerTransport implements NetworkTransport {

    private static final int DEFAULT_PORT = 8080;
    private static final int THREAD_POOL_SIZE = 10;

    private String serverId;
    private int port;
    private ServerSocket serverSocket;
    private boolean running;
    private Listener listener;
    private ExecutorService executorService;

    // Map of connected clients (deviceId -> ClientHandler)
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    /**
     * Creates a new TcpServerTransport with the default port.
     */
    public TcpServerTransport() {
        this(DEFAULT_PORT);
    }

    /**
     * Creates a new TcpServerTransport with the specified port.
     * 
     * @param port The port to listen on
     */
    public TcpServerTransport(int port) {
        this.port = port;
    }

    @Override
    public void initialize(String deviceId, Listener listener) throws IOException {
        this.serverId = deviceId;
        this.listener = listener;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    @Override
    public void start() throws IOException {
        if (running) {
            return;
        }

        serverSocket = new ServerSocket(port);
        running = true;

        // Start the connection acceptor thread
        Thread acceptorThread = new Thread(this::acceptConnections);
        acceptorThread.setDaemon(true);
        acceptorThread.start();

        System.out.println("TCP server started on port " + port);
    }

    @Override
    public void stop() {
        running = false;

        // Close all client connections
        for (ClientHandler handler : clients.values()) {
            handler.close();
        }
        clients.clear();

        // Close the server socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }

        // Shutdown the executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        System.out.println("TCP server stopped");
    }

    @Override
    public boolean sendMessage(CommandProtocol.Message message, String targetDeviceId) throws IOException {
        ClientHandler handler = clients.get(targetDeviceId);
        if (handler != null && handler.isConnected()) {
            return handler.sendMessage(message);
        }
        return false;
    }

    @Override
    public int broadcastMessage(CommandProtocol.Message message) throws IOException {
        int count = 0;
        for (ClientHandler handler : clients.values()) {
            if (handler.isConnected() && handler.sendMessage(message)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean isConnected(String deviceId) {
        ClientHandler handler = clients.get(deviceId);
        return handler != null && handler.isConnected();
    }

    @Override
    public String[] getConnectedDevices() {
        return clients.keySet().toArray(new String[0]);
    }

    @Override
    public String getTransportType() {
        return "TCP";
    }

    /**
     * Accepts incoming client connections.
     */
    private void acceptConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleNewConnection(clientSocket));
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                    if (listener != null) {
                        listener.onError(serverId, "Error accepting connection", e);
                    }
                }
            }
        }
    }

    /**
     * Handles a new client connection.
     * 
     * @param clientSocket The client socket
     */
    private void handleNewConnection(Socket clientSocket) {
        try {
            // Perform initial handshake to get the client's device ID
            String deviceId = performHandshake(clientSocket);

            if (deviceId != null) {
                // Create a new client handler
                ClientHandler handler = new ClientHandler(deviceId, clientSocket);
                clients.put(deviceId, handler);

                // Start the client handler
                handler.start();

                // Notify the listener
                if (listener != null) {
                    listener.onConnected(deviceId);
                }

                System.out.println("Client connected: " + deviceId);
            } else {
                // Failed handshake, close the socket
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error handling new connection: " + e.getMessage());
            try {
                clientSocket.close();
            } catch (IOException ex) {
                // Ignore
            }
        }
    }

    /**
     * Performs the initial handshake with a client to get its device ID.
     * 
     * @param clientSocket The client socket
     * @return The client's device ID, or null if the handshake failed
     */
    private String performHandshake(Socket clientSocket) throws IOException {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();

            // Read the CONNECT message from the client
            byte[] buffer = new byte[4096];
            int bytesRead = inputStream.read(buffer);

            if (bytesRead > 0) {
                // Create a copy of the received data
                byte[] data = new byte[bytesRead];
                System.arraycopy(buffer, 0, data, 0, bytesRead);

                // Deserialize the message
                CommandProtocol.Message message = CommandProtocol.Message.deserialize(data);

                // Check if it's a CONNECT command
                if (message instanceof CommandProtocol.CommandMessage) {
                    CommandProtocol.CommandMessage connectMsg = (CommandProtocol.CommandMessage) message;

                    if (connectMsg.getType() == CommandProtocol.CommandType.CONNECT) {
                        String deviceId = connectMsg.getDeviceId();

                        // Validate the device ID (basic validation)
                        if (deviceId != null && !deviceId.trim().isEmpty()) {
                            // Send ACK response
                            CommandProtocol.ResponseMessage ackResponse = new CommandProtocol.ResponseMessage(
                                CommandProtocol.CommandType.ACK,
                                "server",
                                CommandProtocol.StatusCode.OK,
                                "Connection accepted",
                                deviceId
                            );

                            byte[] responseData = ackResponse.serialize();
                            outputStream.write(responseData);
                            outputStream.flush();

                            return deviceId;
                        } else {
                            // Send NACK response for invalid device ID
                            CommandProtocol.ResponseMessage nackResponse = new CommandProtocol.ResponseMessage(
                                CommandProtocol.CommandType.NACK,
                                "server",
                                CommandProtocol.StatusCode.ERROR_INVALID_CMD,
                                "Invalid device ID"
                            );

                            byte[] responseData = nackResponse.serialize();
                            outputStream.write(responseData);
                            outputStream.flush();
                        }
                    } else {
                        // Send NACK response for wrong command type
                        CommandProtocol.ResponseMessage nackResponse = new CommandProtocol.ResponseMessage(
                            CommandProtocol.CommandType.NACK,
                            "server",
                            CommandProtocol.StatusCode.ERROR_INVALID_CMD,
                            "Expected CONNECT command"
                        );

                        byte[] responseData = nackResponse.serialize();
                        outputStream.write(responseData);
                        outputStream.flush();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error during handshake: " + e.getMessage());

            // Try to send NACK response if possible
            try {
                OutputStream outputStream = clientSocket.getOutputStream();
                CommandProtocol.ResponseMessage nackResponse = new CommandProtocol.ResponseMessage(
                    CommandProtocol.CommandType.NACK,
                    "server",
                    CommandProtocol.StatusCode.ERROR_GENERAL,
                    "Handshake error: " + e.getMessage()
                );

                byte[] responseData = nackResponse.serialize();
                outputStream.write(responseData);
                outputStream.flush();
            } catch (Exception ex) {
                // Ignore if we can't send the error response
            }
        }

        return null;
    }

    /**
     * Handles communication with a connected client.
     */
    private class ClientHandler {
        private final String deviceId;
        private final Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;
        private boolean connected;
        private Thread receiveThread;

        /**
         * Creates a new ClientHandler.
         * 
         * @param deviceId The client's device ID
         * @param socket The client socket
         * @throws IOException If an I/O error occurs
         */
        public ClientHandler(String deviceId, Socket socket) throws IOException {
            this.deviceId = deviceId;
            this.socket = socket;
            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();
            this.connected = true;
        }

        /**
         * Starts the client handler.
         */
        public void start() {
            receiveThread = new Thread(this::receiveMessages);
            receiveThread.setDaemon(true);
            receiveThread.start();
        }

        /**
         * Closes the client handler.
         */
        public void close() {
            connected = false;

            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }

            // Remove from the clients map
            clients.remove(deviceId);

            // Notify the listener
            if (listener != null) {
                listener.onDisconnected(deviceId, "Connection closed");
            }
        }

        /**
         * Sends a message to the client.
         * 
         * @param message The message to send
         * @return true if the message was sent successfully, false otherwise
         */
        public boolean sendMessage(CommandProtocol.Message message) {
            if (!connected) {
                return false;
            }

            try {
                // Serialize the message
                byte[] data = message.serialize();

                // Send the message
                synchronized (outputStream) {
                    outputStream.write(data);
                    outputStream.flush();
                }

                return true;
            } catch (IOException e) {
                System.err.println("Error sending message to client " + deviceId + ": " + e.getMessage());

                // Notify the listener
                if (listener != null) {
                    listener.onError(deviceId, "Error sending message", e);
                }

                // Close the connection
                close();

                return false;
            }
        }

        /**
         * Receives messages from the client.
         */
        private void receiveMessages() {
            byte[] buffer = new byte[4096];

            while (connected) {
                try {
                    int bytesRead = inputStream.read(buffer);

                    if (bytesRead == -1) {
                        // End of stream, client disconnected
                        break;
                    }

                    if (bytesRead > 0) {
                        // Process the received data
                        processReceivedData(buffer, bytesRead);
                    }
                } catch (IOException e) {
                    if (connected) {
                        System.err.println("Error receiving message from client " + deviceId + ": " + e.getMessage());

                        // Notify the listener
                        if (listener != null) {
                            listener.onError(deviceId, "Error receiving message", e);
                        }

                        // Close the connection
                        break;
                    }
                }
            }

            // Close the connection if not already closed
            if (connected) {
                close();
            }
        }

        /**
         * Processes received data from the client.
         * 
         * @param buffer The data buffer
         * @param bytesRead The number of bytes read
         */
        private void processReceivedData(byte[] buffer, int bytesRead) {
            try {
                // Create a copy of the received data
                byte[] data = new byte[bytesRead];
                System.arraycopy(buffer, 0, data, 0, bytesRead);

                // Deserialize the message
                CommandProtocol.Message message = CommandProtocol.Message.deserialize(data);

                // Notify the listener
                if (listener != null) {
                    listener.onMessageReceived(message);
                }
            } catch (Exception e) {
                System.err.println("Error processing received data from client " + deviceId + ": " + e.getMessage());

                // Notify the listener
                if (listener != null) {
                    listener.onError(deviceId, "Error processing received data", e);
                }
            }
        }

        /**
         * Checks if the client is connected.
         * 
         * @return true if connected, false otherwise
         */
        public boolean isConnected() {
            return connected && socket != null && !socket.isClosed() && socket.isConnected();
        }
    }
}
