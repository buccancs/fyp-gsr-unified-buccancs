package com.buccancs.gsr.common.network;

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
            // Implementation would depend on the serialization format chosen
            // (e.g., JSON, Protocol Buffers, custom binary format)
            // This is a placeholder for the actual implementation
            throw new UnsupportedOperationException("Deserialization not implemented");
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
            // Implementation would depend on the serialization format chosen
            // This is a placeholder for the actual implementation
            throw new UnsupportedOperationException("Serialization not implemented");
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
            // Implementation would depend on the serialization format chosen
            // This is a placeholder for the actual implementation
            throw new UnsupportedOperationException("Serialization not implemented");
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
            // Implementation would depend on the serialization format chosen
            // This is a placeholder for the actual implementation
            throw new UnsupportedOperationException("Serialization not implemented");
        }
    }
}