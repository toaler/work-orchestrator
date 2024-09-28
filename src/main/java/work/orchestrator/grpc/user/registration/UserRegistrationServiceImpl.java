package work.orchestrator.grpc.user.registration;

import com.example.grpc.user.registration.UserRegistrationProto;
import com.example.grpc.user.registration.UserRegistrationServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRegistrationServiceImpl extends UserRegistrationServiceGrpc.UserRegistrationServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationServiceImpl.class);


    @Override
    public void registerUser(UserRegistrationProto.RegisterUserRequest request, StreamObserver<UserRegistrationProto.RegisterUserResponse> responseObserver) {
        logger.info("Registering user");
        // Ensure request fields are correct
        if (request.getUsername().isEmpty() || request.getEmail().isEmpty()) {
            // If the request is invalid, we return an error
            responseObserver.onError(new RuntimeException("Invalid request: missing required fields."));
            return;
        }

        // Create a valid response
        UserRegistrationProto.RegisterUserResponse response = UserRegistrationProto.RegisterUserResponse.newBuilder()
                .setMessage("User registration successful.")
                .setUserId("abc123")
                .setStatusUrl("/register/status?userId=abc123")
                .build();

        // Send the response and complete the call
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getRegistrationStatus(UserRegistrationProto.GetRegistrationStatusRequest request, StreamObserver<UserRegistrationProto.GetRegistrationStatusResponse> responseObserver) {
        // Implement your registration status logic here
        UserRegistrationProto.GetRegistrationStatusResponse response = UserRegistrationProto.GetRegistrationStatusResponse.newBuilder()
                .setUserId(request.getUserId())
                .setStatus("pending")
                .setMessage("Registration is pending")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
