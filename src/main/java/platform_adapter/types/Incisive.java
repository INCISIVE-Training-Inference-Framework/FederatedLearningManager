package platform_adapter.types;

import config.EnvironmentVariable;
import exceptions.BadConfigurationException;
import exceptions.InternalException;
import exceptions.PlatformException;
import org.json.JSONObject;
import platform_adapter.PlatformAdapter;

import java.util.*;

import static utils.HttpMethods.patchMultipartMethod;

public class Incisive implements PlatformAdapter {

    public static List<EnvironmentVariable> getEnvironmentVariables() {
        return PlatformAdapter.getEnvironmentVariables();
    }


    public Incisive(Map<String, Object> config) throws BadConfigurationException {}

    @Override
    public void communicateExecutionFailure(String failureEndpoint, String message) throws PlatformException {
        JSONObject entity = new JSONObject();
        entity.put("message", message);
        Set<Integer> expectedStatusCode = new HashSet<>();
        expectedStatusCode.add(200);
        try {
            patchMultipartMethod(
                    failureEndpoint,
                    entity,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    expectedStatusCode,
                    "Error while updating status to failed"
            );
        } catch (InternalException e) {
            throw new PlatformException(String.format("Platform exception: %s", e.getMessage()), e.getException());
        }
    }

}
