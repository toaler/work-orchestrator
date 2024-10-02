package work.orchestrator.registration;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class UserRegistrationServiceNonBlockingConcurrentTest {

    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationServiceNonBlockingConcurrentTest.class);
    private static final int NUM_REQUESTS = 1000000; // Number of asynchronous requests

    @RegisterExtension
    static final GrpcCleanupExtension grpcCleanup = new GrpcCleanupExtension(); // Register the extension

    private work.orchestrator.registration.UserRegistrationServiceGrpc.UserRegistrationServiceStub asyncStub;

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
        asyncStub = work.orchestrator.registration.UserRegistrationServiceGrpc.newStub(channel);
    }

    @Test
    public void testRegisterUsersInBulkAsync() throws Exception {
        logger.info("testRegisterUsersInBulkAsync");

        // CountDownLatch to wait for all responses
        CountDownLatch latch = new CountDownLatch(NUM_REQUESTS);

        // Thread pool for dispatching calls asynchronously
        ExecutorService executor = Executors.newFixedThreadPool(10);

        IntStream.range(0, NUM_REQUESTS).forEach(i -> executor.submit(() -> {
            // Create the request object
            work.orchestrator.registration.UserRegistrationProto.RegisterUserRequest request = work.orchestrator.registration.UserRegistrationProto.RegisterUserRequest.newBuilder()
                    .setUsername("user" + i)
                    .setEmail("user" + i + "@example.com")
                    .setPassword("SecureP@ssword" + i)
                    .setFirstName("User" + i)
                    .setLastName("Test" + i)
                    .build();

            // Define the StreamObserver to handle the asynchronous response
            StreamObserver<work.orchestrator.registration.UserRegistrationProto.RegisterUserResponse> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(work.orchestrator.registration.UserRegistrationProto.RegisterUserResponse response) {
                    logger.info("onNext for user{}", i);
                    // Verify the response in onNext
                    assertEquals("User registration successful.", response.getMessage());
                }

                @Override
                public void onError(Throwable t) {
                    logger.error("onError for user{}", i, t);
                    fail("Failed due to: " + t.getMessage());
                    latch.countDown(); // Unblock the main thread on error
                }

                @Override
                public void onCompleted() {
                    logger.info("onCompleted for user{}", i);
                    latch.countDown(); // Unblock the main thread when the response is completed
                }
            };

            // Make the asynchronous call using the asyncStub
            asyncStub.registerUser(request, responseObserver);
        }));

        // Wait for all responses to complete
        if (!latch.await(30, TimeUnit.SECONDS)) {
            fail("Test timed out while waiting for all responses.");
        }

        // Shutdown the executor
        executor.shutdown();
    }
}
