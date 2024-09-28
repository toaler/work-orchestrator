package work.orchestrator.grpc.user.registration;

import com.example.grpc.user.registration.UserRegistrationProto;
import com.example.grpc.user.registration.UserRegistrationServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class UserRegistrationServiceNonBlockingTest {
    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationServiceNonBlockingTest.class);

    @RegisterExtension
    static final GrpcCleanupExtension grpcCleanup = new GrpcCleanupExtension(); // Register the extension

    private UserRegistrationServiceGrpc.UserRegistrationServiceStub asyncStub;

    @BeforeEach
    public void setup() throws Exception {
        // Create an in-process server with the real service
        String serverName = InProcessServerBuilder.generateName();
        Server server = InProcessServerBuilder.forName(serverName)
                .addService(new UserRegistrationServiceImpl()) // Real service implementation
                .build()
                .start();

        // Register the server to be shut down after the test
        grpcCleanup.register(server);

        // Create an in-process channel to talk to the in-process server
        ManagedChannel channel = InProcessChannelBuilder.forName(serverName)
                .build();

        // Register the channel to be closed after the test
        grpcCleanup.register(channel);

        // Create the non-blocking stub for asynchronous calls
        asyncStub = UserRegistrationServiceGrpc.newStub(channel);
    }

    @Test
    public void testRegisterUserAsync() throws Exception {
        logger.info("testRegisterUserAsync");
        // Create the request object
        UserRegistrationProto.RegisterUserRequest request = UserRegistrationProto.RegisterUserRequest.newBuilder()
                .setUsername("johndoe")
                .setEmail("john.doe@example.com")
                .setPassword("SecureP@ssword")
                .setFirstName("John")
                .setLastName("Doe")
                .build();

        // Use a CountDownLatch to wait for the async response
        CountDownLatch latch = new CountDownLatch(1);

        // Define the StreamObserver to handle the asynchronous response
        StreamObserver<UserRegistrationProto.RegisterUserResponse> responseObserver = new StreamObserver<UserRegistrationProto.RegisterUserResponse>() {
            @Override
            public void onNext(UserRegistrationProto.RegisterUserResponse response) {
                logger.info("onNext");
                // Verify the response in onNext
                assertEquals("User registration successful.", response.getMessage());
                assertEquals("abc123", response.getUserId());
                assertEquals("/register/status?userId=abc123", response.getStatusUrl());
            }

            @Override
            public void onError(Throwable t) {
                logger.error("onError", t);
                fail("Failed due to: " + t.getMessage());
                latch.countDown(); // Unblock the main thread on error
            }

            @Override
            public void onCompleted() {
                logger.info("onCompleted");
                latch.countDown(); // Unblock the main thread when the response is completed
            }
        };

        // Make the asynchronous call using the asyncStub
        logger.info("calling registerUser asynchronously");
        asyncStub.registerUser(request, responseObserver);

        // Wait for the response to be received
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Test timed out while waiting for response.");
        }
    }

    @Test
    public void testGetRegistrationStatusAsync() throws Exception {
        // Create the request object
        UserRegistrationProto.GetRegistrationStatusRequest request = UserRegistrationProto.GetRegistrationStatusRequest.newBuilder()
                .setUserId("abc123")
                .build();

        // Use a CountDownLatch to wait for the async response
        CountDownLatch latch = new CountDownLatch(1);

        // Define the StreamObserver to handle the asynchronous response
        StreamObserver<UserRegistrationProto.GetRegistrationStatusResponse> responseObserver = new StreamObserver<UserRegistrationProto.GetRegistrationStatusResponse>() {
            @Override
            public void onNext(UserRegistrationProto.GetRegistrationStatusResponse response) {
                // Verify the response in onNext
                assertEquals("abc123", response.getUserId());
                assertEquals("pending", response.getStatus());
                assertEquals("Registration is pending", response.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                fail("Failed due to: " + t.getMessage());
                latch.countDown(); // Unblock the main thread on error
            }

            @Override
            public void onCompleted() {
                latch.countDown(); // Unblock the main thread when the response is completed
            }
        };

        // Make the asynchronous call using the asyncStub
        asyncStub.getRegistrationStatus(request, responseObserver);

        // Wait for the response to be received
        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("Test timed out while waiting for response.");
        }
    }
}
