package platform_adapter;

import config.EnvironmentVariable;
import exceptions.PlatformException;

import java.util.ArrayList;
import java.util.List;

public interface PlatformAdapter {

    static List<EnvironmentVariable> getEnvironmentVariables() {
        return new ArrayList<>();
    }

    void communicateExecutionFailure(String failureEndpoint, String message) throws PlatformException;

}
