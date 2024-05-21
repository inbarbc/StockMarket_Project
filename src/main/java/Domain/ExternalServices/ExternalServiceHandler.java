package Domain.ExternalServices;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents an external service handler.
 * It manages a list of external services.
 */
public class ExternalServiceHandler {

    private List<ExternalService> externalServices;

    public ExternalServiceHandler() {
        this.externalServices = new ArrayList<ExternalService>();
    }

    /**
     * Connects to all external services.
     * @return true if all services are successfully connected, false otherwise.
     */
    public boolean ConnectToServices() {
        for (ExternalService externalService : externalServices) {
            if (!externalService.ConnectToService()) {
                return false;
            }
        }
        return true;
    }
}
