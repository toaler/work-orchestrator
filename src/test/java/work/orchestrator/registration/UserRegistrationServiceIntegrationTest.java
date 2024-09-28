package work.orchestrator.registration;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserRegistrationServiceIntegrationTest {

    @RegisterExtension
    static final GrpcCleanupExtension grpcCleanup = new GrpcCleanupExtension(); // Register the extension

    private ManagedChannel channel;
    private UserRegistrationServiceGrpc.UserRegistrationServiceBlockingStub blockingStub;

    @BeforeEach
    public void setup() throws Exception {
        // Create an in-process server with the real service
        String serverName = InProcessServerBuilder.generateName();
        Server server = InProcessServerBuilder.forName(serverName)
                .directExecutor() // Use direct executor for simplicity
                .addService(new UserRegistrationServiceImpl()) // Real service implementation
                .build()
                .start();

        // Register the server to be shut down after the test
        grpcCleanup.register(server);

        // Create an in-process channel to talk to the in-process server
        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();

        // Register the channel to be closed after the test
        grpcCleanup.register(channel);

        // Create the blocking stub for synchronous calls
        blockingStub = UserRegistrationServiceGrpc.newBlockingStub(channel);
    }

    @Test
    public void testRegisterUser() {
        // Create the request object
        UserRegistrationProto.RegisterUserRequest request = UserRegistrationProto.RegisterUserRequest.newBuilder()
                .setUsername("johndoe")
                .setEmail("john.doe@example.com")
                .setPassword("SecureP@ssword")
                .setFirstName("John")
                .setLastName("Doe")
                .build();

        // Call the gRPC method and get the response
        UserRegistrationProto.RegisterUserResponse response = blockingStub.registerUser(request);

        // Verify the response
        assertEquals("User registration successful.", response.getMessage());
        assertEquals("abc123", response.getUserId());
        assertEquals("/register/status?userId=abc123", response.getStatusUrl());
    }

    @Test
    public void testGetRegistrationStatus() {
        // Create the request object
        UserRegistrationProto.GetRegistrationStatusRequest request = UserRegistrationProto.GetRegistrationStatusRequest.newBuilder()
                .setUserId("abc123")
                .build();

        // Call the gRPC method and get the response
        UserRegistrationProto.GetRegistrationStatusResponse response = blockingStub.getRegistrationStatus(request);

        // Verify the response
        assertEquals("abc123", response.getUserId());
        assertEquals("pending", response.getStatus());
        assertEquals("Registration is pending", response.getMessage());
    }
}
