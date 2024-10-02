package work.orchestrator.registration;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRegistrationServiceImpl extends work.orchestrator.registration.UserRegistrationServiceGrpc.UserRegistrationServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationServiceImpl.class);

    @Override
    public void registerUser(work.orchestrator.registration.UserRegistrationProto.RegisterUserRequest request, StreamObserver<work.orchestrator.registration.UserRegistrationProto.RegisterUserResponse> responseObserver) {
        logger.info("Registering user");
        // Ensure request fields are correct
        if (request.getUsername().isEmpty() || request.getEmail().isEmpty()) {
            // If the request is invalid, we return an error
            responseObserver.onError(new RuntimeException("Invalid request: missing required fields."));
            return;
        }

        // Create a valid response
        work.orchestrator.registration.UserRegistrationProto.RegisterUserResponse response = work.orchestrator.registration.UserRegistrationProto.RegisterUserResponse.newBuilder()
                .setMessage("User registration successful.")
                .setUserId("abc123")
                .setStatusUrl("/register/status?userId=abc123")
                .build();

        // Send the response and complete the call
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getRegistrationStatus(work.orchestrator.registration.UserRegistrationProto.GetRegistrationStatusRequest request, StreamObserver<work.orchestrator.registration.UserRegistrationProto.GetRegistrationStatusResponse> responseObserver) {
        // Implement your registration status logic here
        work.orchestrator.registration.UserRegistrationProto.GetRegistrationStatusResponse response = work.orchestrator.registration.UserRegistrationProto.GetRegistrationStatusResponse.newBuilder()
                .setUserId(request.getUserId())
                .setStatus("pending")
                .setMessage("Registration is pending")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
