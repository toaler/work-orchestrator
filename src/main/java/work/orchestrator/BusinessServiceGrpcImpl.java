package work.orchestrator;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.orchestrator.BusinessServiceGrpc.BusinessServiceImplBase;
import work.orchestrator.BusinessServiceProto.Request;
import work.orchestrator.BusinessServiceProto.Response;
import work.orchestrator.registration.UserRegistrationServiceImpl;

public class BusinessServiceGrpcImpl extends BusinessServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(BusinessServiceGrpcImpl.class);

    private final ExecutorService businessLogicExecutor = Executors.newFixedThreadPool(10); // Business logic thread pool

    @Override
    public void dispatch(Request request, StreamObserver<Response> responseObserver) {
        logger.info("Received request: {}", request);
        // Handle the incoming gRPC request asynchronously
        CompletableFuture
                .supplyAsync(() -> invokeBusinessLogic(request), businessLogicExecutor)  // Use separate thread pool
                .thenAccept(result -> {
                    // Once business logic is done, send the response back to gRPC client
                    Response response = Response.newBuilder().setResult(result).build();
                    logger.info("Response received: {}", response);
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                })
                .exceptionally(ex -> {
                    // Handle exceptions
                    responseObserver.onError(ex);
                    return null;
                });
    }

    private String invokeBusinessLogic(Request request) {
        try {
            // Use reflection or MethodHandles to dynamically invoke business logic
            logger.info("Invoking business logic...");
            String action = request.getAction();
            String payload = request.getPayload();

            return "foo";

            // MethodHandles alternative (more performant but complex)
            // MethodHandles.Lookup lookup = MethodHandles.lookup();
            // MethodType methodType = MethodType.methodType(String.class, String.class);
            // MethodHandle methodHandle = lookup.findVirtual(businessLogicInstance.getClass(), action, methodType);
            // return (String) methodHandle.invoke(businessLogicInstance, payload);

        } catch (Exception e) {
            e.printStackTrace();
            return "Error invoking business logic: " + e.getMessage();
        }
    }
}
