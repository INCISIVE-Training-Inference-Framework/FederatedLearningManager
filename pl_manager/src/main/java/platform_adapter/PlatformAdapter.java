package platform_adapter;

import config.EnvironmentVariable;
import exceptions.PlatformException;

import java.util.ArrayList;
import java.util.List;

public interface PlatformAdapter {

    static List<EnvironmentVariable> getEnvironmentVariables() {
        return new ArrayList<>();
    }

    void communicateExecutionEnd(boolean isOk) throws PlatformException;

    void uploadModel(byte[] model) throws PlatformException;
}
