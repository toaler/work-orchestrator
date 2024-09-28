package work.orchestrator.grpc.user.registration;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.List;

public class GrpcCleanupExtension implements BeforeEachCallback, AfterEachCallback {

    private final List<AutoCloseable> resources = new ArrayList<>();

    @Override
    public void beforeEach(ExtensionContext context) {
        // No initialization needed here
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // Clean up all registered resources (servers, channels) after each test
        for (AutoCloseable resource : resources) {
            resource.close();
        }
        resources.clear();
    }

    public void register(Server server) {
        resources.add(server::shutdownNow);
    }

    public void register(ManagedChannel channel) {
        resources.add(channel::shutdownNow);
    }
}
