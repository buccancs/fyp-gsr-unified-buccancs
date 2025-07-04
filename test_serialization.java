import com.buccancs.gsr.common.network.CommandProtocol;

public class test_serialization {
    public static void main(String[] args) {
        System.out.println("Testing CommandProtocol serialization and deserialization...");
        
        try {
            // Test CommandMessage
            System.out.println("\n=== Testing CommandMessage ===");
            CommandProtocol.CommandMessage originalCmd = new CommandProtocol.CommandMessage(
                CommandProtocol.CommandType.CMD_START,
                "android_device_001",
                "session_123",
                "param1", "param2"
            );
            
            byte[] serializedCmd = originalCmd.serialize();
            System.out.println("Serialized CommandMessage: " + serializedCmd.length + " bytes");
            
            CommandProtocol.Message deserializedCmd = CommandProtocol.Message.deserialize(serializedCmd);
            System.out.println("Deserialized successfully!");
            System.out.println("Type: " + deserializedCmd.getType());
            System.out.println("Device ID: " + deserializedCmd.getDeviceId());
            
            if (deserializedCmd instanceof CommandProtocol.CommandMessage) {
                CommandProtocol.CommandMessage cmdMsg = (CommandProtocol.CommandMessage) deserializedCmd;
                System.out.println("Session ID: " + cmdMsg.getSessionId());
                System.out.println("Parameters: " + java.util.Arrays.toString(cmdMsg.getParameters()));
            }
            
            // Test ResponseMessage
            System.out.println("\n=== Testing ResponseMessage ===");
            CommandProtocol.ResponseMessage originalResp = new CommandProtocol.ResponseMessage(
                CommandProtocol.CommandType.ACK,
                "server",
                CommandProtocol.StatusCode.OK,
                "Connection accepted",
                "android_device_001"
            );
            
            byte[] serializedResp = originalResp.serialize();
            System.out.println("Serialized ResponseMessage: " + serializedResp.length + " bytes");
            
            CommandProtocol.Message deserializedResp = CommandProtocol.Message.deserialize(serializedResp);
            System.out.println("Deserialized successfully!");
            System.out.println("Type: " + deserializedResp.getType());
            System.out.println("Device ID: " + deserializedResp.getDeviceId());
            
            if (deserializedResp instanceof CommandProtocol.ResponseMessage) {
                CommandProtocol.ResponseMessage respMsg = (CommandProtocol.ResponseMessage) deserializedResp;
                System.out.println("Status: " + respMsg.getStatus());
                System.out.println("Message: " + respMsg.getMessage());
                System.out.println("Data: " + java.util.Arrays.toString(respMsg.getData()));
            }
            
            // Test SyncMessage
            System.out.println("\n=== Testing SyncMessage ===");
            CommandProtocol.SyncMessage originalSync = new CommandProtocol.SyncMessage(
                CommandProtocol.CommandType.SYNC_PING,
                "android_device_001",
                System.currentTimeMillis() - 1000,
                System.currentTimeMillis() - 500
            );
            
            byte[] serializedSync = originalSync.serialize();
            System.out.println("Serialized SyncMessage: " + serializedSync.length + " bytes");
            
            CommandProtocol.Message deserializedSync = CommandProtocol.Message.deserialize(serializedSync);
            System.out.println("Deserialized successfully!");
            System.out.println("Type: " + deserializedSync.getType());
            System.out.println("Device ID: " + deserializedSync.getDeviceId());
            
            if (deserializedSync instanceof CommandProtocol.SyncMessage) {
                CommandProtocol.SyncMessage syncMsg = (CommandProtocol.SyncMessage) deserializedSync;
                System.out.println("Origin Timestamp: " + syncMsg.getOriginTimestamp());
                System.out.println("Receive Timestamp: " + syncMsg.getReceiveTimestamp());
                System.out.println("Transmit Timestamp: " + syncMsg.getTransmitTimestamp());
            }
            
            System.out.println("\n=== All tests passed! ===");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}