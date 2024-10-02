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


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class BusinessServiceGrpcImplTest {
    private static final Logger logger = LoggerFactory.getLogger(BusinessServiceGrpcImplTest.class);

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
        asyncStub = BusinessServiceGrpc.newStub(channel);
    }

    @Test
    public void testBusinessServiceAsync() throws Exception {
        logger.info("testBusinessServiceAsync");
        // Create the request object
        var request = BusinessServiceProto.Request.newBuilder()
                .setAction("jump")
                .setPayload("{'foo' : 'bar'}")
                .build();

        // Use a CountDownLatch to wait for the async response
        CountDownLatch latch = new CountDownLatch(1);

        // Define the StreamObserver to handle the asynchronous response
        StreamObserver<BusinessServiceProto.Response> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(BusinessServiceProto.Response response) {
                logger.info("onNext");
                // Verify the response in onNext
                assertEquals("foo", response.getResult());
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
        logger.info("calling BusinessServiceGrpcImpl.dispatch asynchronously");
        asyncStub.dispatch(request, responseObserver);

        // Wait for the response to be received
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Test timed out while waiting for response.");
        }
    }
}