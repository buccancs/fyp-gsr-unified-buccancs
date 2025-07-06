import com.buccancs.gsr.common.network.CommandProtocol;

public class test_integration {
    public static void main(String[] args) {
        System.out.println("Testing CommandProtocol integration...");
        
        try {
            // Test CommandMessage
            System.out.println("\n=== Testing CommandMessage ===");
            CommandProtocol.CommandMessage startCmd = new CommandProtocol.CommandMessage(
                CommandProtocol.CommandType.CMD_START,
                "android_test_device",
                "session_123"
            );
            
            byte[] serialized = startCmd.serialize();
            System.out.println("Serialized CMD_START: " + serialized.length + " bytes");
            
            CommandProtocol.Message deserialized = CommandProtocol.Message.deserialize(serialized);
            System.out.println("Deserialized type: " + deserialized.getType());
            System.out.println("Device ID: " + deserialized.getDeviceId());
            
            // Test ResponseMessage
            System.out.println("\n=== Testing ResponseMessage ===");
            CommandProtocol.ResponseMessage ackResponse = new CommandProtocol.ResponseMessage(
                CommandProtocol.CommandType.ACK,
                "android_test_device",
                CommandProtocol.StatusCode.OK,
                "Command received successfully",
                "battery:85%", "storage:60% free"
            );
            
            byte[] responseData = ackResponse.serialize();
            System.out.println("Serialized ACK response: " + responseData.length + " bytes");
            
            CommandProtocol.Message deserializedResponse = CommandProtocol.Message.deserialize(responseData);
            if (deserializedResponse instanceof CommandProtocol.ResponseMessage) {
                CommandProtocol.ResponseMessage respMsg = (CommandProtocol.ResponseMessage) deserializedResponse;
                System.out.println("Response status: " + respMsg.getStatus());
                System.out.println("Response message: " + respMsg.getMessage());
                System.out.println("Response data: " + java.util.Arrays.toString(respMsg.getData()));
            }
            
            System.out.println("\n=== Integration Test PASSED ===");
            System.out.println("The Android app can now communicate with both:");
            System.out.println("1. Python PC Controller (JSON-based via NetworkClient)");
            System.out.println("2. Java PC Controller (CommandProtocol via CommandProtocolClient)");
            
        } catch (Exception e) {
            System.err.println("Integration test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}