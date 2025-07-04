package com.buccancs.gsr.android.network;

import com.buccancs.gsr.common.network.CommandProtocol;
import com.buccancs.gsr.common.network.NetworkTransport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TcpClientTransport implements the NetworkTransport interface using TCP/IP sockets
 * for the Android client. It connects to a PC server and maintains a bi-directional
 * communication channel.
 */
public class TcpClientTransport implements NetworkTransport {

    private static final int DEFAULT_PORT = 8080;
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    private static final int RECONNECT_DELAY = 5000; // 5 seconds

    private String deviceId;
    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Listener listener;
    private boolean running;
    private boolean connected;
    private Thread receiveThread;
    private Thread reconnectThread;
    private ExecutorService executorService;

    /**
     * Creates a new TcpClientTransport with the default port.
     * 
     * @param serverAddress The server's IP address or hostname
     */
    public TcpClientTransport(String serverAddress) {
        this(serverAddress, DEFAULT_PORT);
    }

    /**
     * Creates a new TcpClientTransport with the specified port.
     * 
     * @param serverAddress The server's IP address or hostname
     * @param serverPort The server's port
     */
    public TcpClientTransport(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void initialize(String deviceId, Listener listener) throws IOException {
        this.deviceId = deviceId;
        this.listener = listener;
    }

    @Override
    public void start() throws IOException {
        if (running) {
            return;
        }

        running = true;

        // Connect to the server
        connect();

        // Start the reconnect thread
        reconnectThread = new Thread(this::reconnectLoop);
        reconnectThread.setDaemon(true);
        reconnectThread.start();

        System.out.println("TCP client started, connecting to " + serverAddress + ":" + serverPort);
    }

    @Override
    public void stop() {
        running = false;

        // Close the connection
        disconnect();

        // Shutdown the executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        System.out.println("TCP client stopped");
    }

    @Override
    public boolean sendMessage(CommandProtocol.Message message, String targetDeviceId) throws IOException {
        // In the client, we only send messages to the server
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
            System.err.println("Error sending message to server: " + e.getMessage());

            // Notify the listener
            if (listener != null) {
                listener.onError(deviceId, "Error sending message", e);
            }

            // Disconnect and attempt to reconnect
            handleDisconnection();

            throw e;
        }
    }

    @Override
    public int broadcastMessage(CommandProtocol.Message message) throws IOException {
        // In the client, broadcasting is the same as sending to the server
        return sendMessage(message, null) ? 1 : 0;
    }

    @Override
    public boolean isConnected(String deviceId) {
        // In the client, we only connect to the server
        return connected;
    }

    @Override
    public String[] getConnectedDevices() {
        // In the client, we only connect to the server
        return connected ? new String[]{"server"} : new String[0];
    }

    @Override
    public String getTransportType() {
        return "TCP";
    }

    /**
     * Connects to the server.
     * 
     * @return true if the connection was successful, false otherwise
     */
    private boolean connect() {
        if (connected) {
            return true;
        }

        try {
            // Create a new socket
            socket = new Socket();
            socket.connect(new InetSocketAddress(serverAddress, serverPort), CONNECTION_TIMEOUT);

            // Get the input and output streams
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            // Perform the handshake
            if (performHandshake()) {
                connected = true;

                // Start the receive thread
                receiveThread = new Thread(this::receiveMessages);
                receiveThread.setDaemon(true);
                receiveThread.start();

                // Notify the listener
                if (listener != null) {
                    listener.onConnected("server");
                }

                System.out.println("Connected to server: " + serverAddress + ":" + serverPort);

                return true;
            } else {
                // Handshake failed, close the socket
                socket.close();
                return false;
            }
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());

            // Notify the listener
            if (listener != null) {
                listener.onError(deviceId, "Error connecting to server", e);
            }

            // Close the socket if it was created
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    // Ignore
                }
            }

            return false;
        }
    }

    /**
     * Disconnects from the server.
     */
    private void disconnect() {
        if (!connected) {
            return;
        }

        connected = false;

        // Close the socket
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }

        // Notify the listener
        if (listener != null) {
            listener.onDisconnected("server", "Disconnected");
        }

        System.out.println("Disconnected from server");
    }

    /**
     * Handles a disconnection event.
     */
    private void handleDisconnection() {
        disconnect();

        // Attempt to reconnect if still running
        if (running) {
            executorService.submit(() -> {
                try {
                    Thread.sleep(RECONNECT_DELAY);
                    if (running && !connected) {
                        connect();
                    }
                } catch (InterruptedException e) {
                    // Ignore
                }
            });
        }
    }

    /**
     * Performs the initial handshake with the server.
     * 
     * @return true if the handshake was successful, false otherwise
     */
    private boolean performHandshake() throws IOException {
        try {
            // Send the CONNECT message using CommandProtocol
            CommandProtocol.CommandMessage connectMessage = new CommandProtocol.CommandMessage(
                CommandProtocol.CommandType.CONNECT, 
                deviceId, 
                "handshake"
            );

            byte[] data = connectMessage.serialize();
            outputStream.write(data);
            outputStream.flush();

            // Wait for ACK/NACK response from server
            byte[] buffer = new byte[4096];
            int bytesRead = inputStream.read(buffer);

            if (bytesRead > 0) {
                byte[] responseData = new byte[bytesRead];
                System.arraycopy(buffer, 0, responseData, 0, bytesRead);

                CommandProtocol.Message response = CommandProtocol.Message.deserialize(responseData);

                // Check if it's an ACK response
                if (response instanceof CommandProtocol.ResponseMessage) {
                    CommandProtocol.ResponseMessage responseMsg = (CommandProtocol.ResponseMessage) response;
                    return responseMsg.getType() == CommandProtocol.CommandType.ACK && 
                           responseMsg.getStatus() == CommandProtocol.StatusCode.OK;
                }
            }

            return false;
        } catch (Exception e) {
            System.err.println("Error during handshake: " + e.getMessage());
            return false;
        }
    }

    /**
     * Continuously attempts to reconnect to the server if disconnected.
     */
    private void reconnectLoop() {
        while (running) {
            if (!connected) {
                connect();
            }

            try {
                Thread.sleep(RECONNECT_DELAY);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    /**
     * Receives messages from the server.
     */
    private void receiveMessages() {
        byte[] buffer = new byte[4096];

        while (connected) {
            try {
                int bytesRead = inputStream.read(buffer);

                if (bytesRead == -1) {
                    // End of stream, server disconnected
                    break;
                }

                if (bytesRead > 0) {
                    // Process the received data
                    processReceivedData(buffer, bytesRead);
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("Error receiving message from server: " + e.getMessage());

                    // Notify the listener
                    if (listener != null) {
                        listener.onError(deviceId, "Error receiving message", e);
                    }

                    // Disconnect and attempt to reconnect
                    break;
                }
            }
        }

        // Handle disconnection if still connected
        if (connected) {
            handleDisconnection();
        }
    }

    /**
     * Processes received data from the server.
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
            System.err.println("Error processing received data from server: " + e.getMessage());

            // Notify the listener
            if (listener != null) {
                listener.onError(deviceId, "Error processing received data", e);
            }
        }
    }
}