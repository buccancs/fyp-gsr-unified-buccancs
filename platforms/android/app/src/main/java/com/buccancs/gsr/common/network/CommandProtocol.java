package com.buccancs.gsr.common.network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * CommandProtocol defines the set of commands and their formats for control and synchronization
 * between the PC controller and Android devices.
 */
public class CommandProtocol {

    /**
     * Command types used in the communication protocol.
     */
    public enum CommandType {
        // Control commands
        CMD_START(1),       // Start recording on device
        CMD_STOP(2),        // Stop recording on device
        CMD_STATUS(3),      // Query device status

        // Synchronization commands
        SYNC_PING(101),     // Time synchronization ping
        SYNC_PONG(102),     // Time synchronization response
        SYNC_MARKER(103),   // Synchronization event marker

        // Connection management
        CONNECT(201),       // Initial connection request
        DISCONNECT(202),    // Graceful disconnection
        HEARTBEAT(203),     // Keep-alive message

        // Error and acknowledgment
        ACK(301),           // Acknowledge receipt of command
        NACK(302),          // Negative acknowledgment
        ERROR(303);         // Error notification

        private final int code;

        CommandType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static CommandType fromCode(int code) {
            for (CommandType type : CommandType.values()) {
                if (type.getCode() == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown command code: " + code);
        }
    }

    /**
     * Status codes for device responses.
     */
    public enum StatusCode {
        OK(0),                  // Operation successful
        ERROR_GENERAL(1),       // General error
        ERROR_NOT_CONNECTED(2), // Device not connected
        ERROR_BUSY(3),          // Device busy
        ERROR_TIMEOUT(4),       // Operation timed out
        ERROR_INVALID_CMD(5);   // Invalid command

        private final int code;

        StatusCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static StatusCode fromCode(int code) {
            for (StatusCode status : StatusCode.values()) {
                if (status.getCode() == code) {
                    return status;
                }
            }
            return ERROR_GENERAL;
        }
    }

    /**
     * Base class for all command messages.
     */
    public static abstract class Message {
        private final CommandType type;
        private final long timestamp;
        private final String deviceId;

        public Message(CommandType type, String deviceId) {
            this.type = type;
            this.timestamp = System.currentTimeMillis();
            this.deviceId = deviceId;
        }

        public CommandType getType() {
            return type;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getDeviceId() {
            return deviceId;
        }

        /**
         * Serialize the message to a byte array for transmission.
         */
        public abstract byte[] serialize();

        /**
         * Deserialize a message from a byte array.
         */
        public static Message deserialize(byte[] data) {
            if (data == null || data.length < 16) {
                throw new IllegalArgumentException("Invalid message data");
            }

            ByteBuffer buffer = ByteBuffer.wrap(data);

            // Read header
            int typeCode = buffer.getInt();
            int messageClass = buffer.getInt();
            long timestamp = buffer.getLong();

            // Read deviceId
            int deviceIdLength = buffer.getInt();
            if (deviceIdLength < 0 || deviceIdLength > 1024) {
                throw new IllegalArgumentException("Invalid deviceId length");
            }

            byte[] deviceIdBytes = new byte[deviceIdLength];
            buffer.get(deviceIdBytes);
            String deviceId = new String(deviceIdBytes, StandardCharsets.UTF_8);

            CommandType type = CommandType.fromCode(typeCode);

            // Deserialize based on message class
            switch (messageClass) {
                case 1: // CommandMessage
                    return deserializeCommandMessage(buffer, type, deviceId, timestamp);
                case 2: // ResponseMessage
                    return deserializeResponseMessage(buffer, type, deviceId, timestamp);
                case 3: // SyncMessage
                    return deserializeSyncMessage(buffer, type, deviceId, timestamp);
                default:
                    throw new IllegalArgumentException("Unknown message class: " + messageClass);
            }
        }

        private static CommandMessage deserializeCommandMessage(ByteBuffer buffer, CommandType type, String deviceId, long timestamp) {
            // Read sessionId
            int sessionIdLength = buffer.getInt();
            byte[] sessionIdBytes = new byte[sessionIdLength];
            buffer.get(sessionIdBytes);
            String sessionId = new String(sessionIdBytes, StandardCharsets.UTF_8);

            // Read parameters
            int paramCount = buffer.getInt();
            String[] parameters = new String[paramCount];
            for (int i = 0; i < paramCount; i++) {
                int paramLength = buffer.getInt();
                byte[] paramBytes = new byte[paramLength];
                buffer.get(paramBytes);
                parameters[i] = new String(paramBytes, StandardCharsets.UTF_8);
            }

            CommandMessage msg = new CommandMessage(type, deviceId, sessionId, parameters);
            // Set the timestamp to the deserialized value
            setTimestamp(msg, timestamp);
            return msg;
        }

        private static ResponseMessage deserializeResponseMessage(ByteBuffer buffer, CommandType type, String deviceId, long timestamp) {
            // Read status
            int statusCode = buffer.getInt();
            StatusCode status = StatusCode.fromCode(statusCode);

            // Read message
            int messageLength = buffer.getInt();
            byte[] messageBytes = new byte[messageLength];
            buffer.get(messageBytes);
            String message = new String(messageBytes, StandardCharsets.UTF_8);

            // Read data array
            int dataCount = buffer.getInt();
            String[] data = new String[dataCount];
            for (int i = 0; i < dataCount; i++) {
                int dataLength = buffer.getInt();
                byte[] dataBytes = new byte[dataLength];
                buffer.get(dataBytes);
                data[i] = new String(dataBytes, StandardCharsets.UTF_8);
            }

            ResponseMessage msg = new ResponseMessage(type, deviceId, status, message, data);
            setTimestamp(msg, timestamp);
            return msg;
        }

        private static SyncMessage deserializeSyncMessage(ByteBuffer buffer, CommandType type, String deviceId, long timestamp) {
            long originTimestamp = buffer.getLong();
            long receiveTimestamp = buffer.getLong();

            SyncMessage msg = new SyncMessage(type, deviceId, originTimestamp, receiveTimestamp);
            setTimestamp(msg, timestamp);
            return msg;
        }

        // Helper method to set timestamp via reflection (since timestamp is final)
        private static void setTimestamp(Message message, long timestamp) {
            try {
                java.lang.reflect.Field timestampField = Message.class.getDeclaredField("timestamp");
                timestampField.setAccessible(true);
                timestampField.set(message, timestamp);
            } catch (Exception e) {
                // If reflection fails, we'll keep the current timestamp
                // This is not ideal but won't break functionality
            }
        }
    }

    /**
     * Command message for control operations.
     */
    public static class CommandMessage extends Message {
        private final String sessionId;
        private final String[] parameters;

        public CommandMessage(CommandType type, String deviceId, String sessionId, String... parameters) {
            super(type, deviceId);
            this.sessionId = sessionId;
            this.parameters = parameters;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String[] getParameters() {
            return parameters;
        }

        @Override
        public byte[] serialize() {
            byte[] deviceIdBytes = getDeviceId().getBytes(StandardCharsets.UTF_8);
            byte[] sessionIdBytes = sessionId.getBytes(StandardCharsets.UTF_8);

            // Calculate total size
            int totalSize = 4 + 4 + 8 + 4 + deviceIdBytes.length + 4 + sessionIdBytes.length + 4;
            for (String param : parameters) {
                totalSize += 4 + param.getBytes(StandardCharsets.UTF_8).length;
            }

            ByteBuffer buffer = ByteBuffer.allocate(totalSize);

            // Write header
            buffer.putInt(getType().getCode());
            buffer.putInt(1); // CommandMessage class identifier
            buffer.putLong(getTimestamp());

            // Write deviceId
            buffer.putInt(deviceIdBytes.length);
            buffer.put(deviceIdBytes);

            // Write sessionId
            buffer.putInt(sessionIdBytes.length);
            buffer.put(sessionIdBytes);

            // Write parameters
            buffer.putInt(parameters.length);
            for (String param : parameters) {
                byte[] paramBytes = param.getBytes(StandardCharsets.UTF_8);
                buffer.putInt(paramBytes.length);
                buffer.put(paramBytes);
            }

            return buffer.array();
        }
    }

    /**
     * Response message for command acknowledgments and status reports.
     */
    public static class ResponseMessage extends Message {
        private final StatusCode status;
        private final String message;
        private final String[] data;

        public ResponseMessage(CommandType type, String deviceId, StatusCode status, String message, String... data) {
            super(type, deviceId);
            this.status = status;
            this.message = message;
            this.data = data;
        }

        public StatusCode getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public String[] getData() {
            return data;
        }

        @Override
        public byte[] serialize() {
            byte[] deviceIdBytes = getDeviceId().getBytes(StandardCharsets.UTF_8);
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

            // Calculate total size
            int totalSize = 4 + 4 + 8 + 4 + deviceIdBytes.length + 4 + 4 + messageBytes.length + 4;
            for (String dataItem : data) {
                totalSize += 4 + dataItem.getBytes(StandardCharsets.UTF_8).length;
            }

            ByteBuffer buffer = ByteBuffer.allocate(totalSize);

            // Write header
            buffer.putInt(getType().getCode());
            buffer.putInt(2); // ResponseMessage class identifier
            buffer.putLong(getTimestamp());

            // Write deviceId
            buffer.putInt(deviceIdBytes.length);
            buffer.put(deviceIdBytes);

            // Write status
            buffer.putInt(status.getCode());

            // Write message
            buffer.putInt(messageBytes.length);
            buffer.put(messageBytes);

            // Write data array
            buffer.putInt(data.length);
            for (String dataItem : data) {
                byte[] dataBytes = dataItem.getBytes(StandardCharsets.UTF_8);
                buffer.putInt(dataBytes.length);
                buffer.put(dataBytes);
            }

            return buffer.array();
        }
    }

    /**
     * Synchronization message for time alignment.
     */
    public static class SyncMessage extends Message {
        private final long originTimestamp;
        private final long receiveTimestamp;
        private final long transmitTimestamp;

        public SyncMessage(CommandType type, String deviceId, long originTimestamp, long receiveTimestamp) {
            super(type, deviceId);
            this.originTimestamp = originTimestamp;
            this.receiveTimestamp = receiveTimestamp;
            this.transmitTimestamp = System.currentTimeMillis();
        }

        public long getOriginTimestamp() {
            return originTimestamp;
        }

        public long getReceiveTimestamp() {
            return receiveTimestamp;
        }

        public long getTransmitTimestamp() {
            return transmitTimestamp;
        }

        @Override
        public byte[] serialize() {
            byte[] deviceIdBytes = getDeviceId().getBytes(StandardCharsets.UTF_8);

            // Calculate total size: header + deviceId + 3 timestamps
            int totalSize = 4 + 4 + 8 + 4 + deviceIdBytes.length + 8 + 8 + 8;

            ByteBuffer buffer = ByteBuffer.allocate(totalSize);

            // Write header
            buffer.putInt(getType().getCode());
            buffer.putInt(3); // SyncMessage class identifier
            buffer.putLong(getTimestamp());

            // Write deviceId
            buffer.putInt(deviceIdBytes.length);
            buffer.put(deviceIdBytes);

            // Write timestamps
            buffer.putLong(originTimestamp);
            buffer.putLong(receiveTimestamp);
            buffer.putLong(transmitTimestamp);

            return buffer.array();
        }
    }
}