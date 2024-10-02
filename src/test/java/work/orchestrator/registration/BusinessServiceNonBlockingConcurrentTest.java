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
import work.orchestrator.BusinessServiceGrpc;
import work.orchestrator.BusinessServiceGrpcImpl;
import work.orchestrator.BusinessServiceProto;
import work.orchestrator.util.NamedThreadFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class BusinessServiceNonBlockingConcurrentTest {

    private static final Logger logger = LoggerFactory.getLogger(BusinessServiceNonBlockingConcurrentTest.class);
    private static final int NUM_REQUESTS = 1000000; // Number of asynchronous requests

    @RegisterExtension
    static final GrpcCleanupExtension grpcCleanup = new GrpcCleanupExtension(); // Register the extension

    private BusinessServiceGrpc.BusinessServiceStub asyncStub;

    @BeforeEach
    public void setup() throws Exception {
        // Create an in-process server with the real service
        String serverName = InProcessServerBuilder.generateName();
        Server server = InProcessServerBuilder.forName(serverName)
                .addService(new BusinessServiceGrpcImpl()) // Real service implementation
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
        asyncStub = work.orchestrator.BusinessServiceGrpc.newStub(channel);
    }

    @Test
    public void testRegisterUsersInBulkAsync() throws Exception {
        logger.info("testRegisterUsersInBulkAsync");

        // CountDownLatch to wait for all responses
        CountDownLatch latch = new CountDownLatch(NUM_REQUESTS);

        // Thread pool for dispatching calls asynchronously
        ExecutorService executor = Executors.newFixedThreadPool(10, new NamedThreadFactory("producer"));

        IntStream.range(0, NUM_REQUESTS).forEach(i -> executor.submit(() -> {

            // Create the request object
            BusinessServiceProto.Request request = BusinessServiceProto.Request.newBuilder()
                    .setAction("jump")
                    .setPayload("high")
                    .build();

            // Define the StreamObserver to handle the asynchronous response
            StreamObserver<BusinessServiceProto.Response> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(BusinessServiceProto.Response response) {
                    logger.info("onNext for BusinessService{}", i);
                    // Verify the response in onNext
                    assertEquals("foo", response.getResult());
                }

                @Override
                public void onError(Throwable t) {
                    logger.error("onError for BusinessService{}", i, t);
                    latch.countDown(); // Unblock the main thread on error
                }

                @Override
                public void onCompleted() {
                    logger.info("onCompleted for BusinessService{}", i);
                    latch.countDown(); // Unblock the main thread when the response is completed
                }
            };

            logger.info("dispatching request");
            // Make the asynchronous call using the asyncStub
            asyncStub.dispatch(request, responseObserver);
        }));

        // Wait for all responses to complete
        if (!latch.await(300, TimeUnit.SECONDS)) {
            fail("Test timed out while waiting for all responses.");
        }

        // Shutdown the executor
        executor.shutdown();
    }
}
