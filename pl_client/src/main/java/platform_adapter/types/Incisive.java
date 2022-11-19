package platform_adapter.types;

import config.EnvironmentVariable;
import config.EnvironmentVariableType;
import exceptions.PlatformException;
import org.json.JSONObject;
import platform_adapter.PlatformAdapter;
import utils.HttpCalls;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Incisive implements PlatformAdapter {

    public static List<EnvironmentVariable> getEnvironmentVariables() {
        List<EnvironmentVariable> abstractClassVariables = PlatformAdapter.getEnvironmentVariables();
        abstractClassVariables.add(new EnvironmentVariable("INCISIVE_ORCHESTRATOR_SERVICE_HOSTNAME", EnvironmentVariableType.STRING));
        return abstractClassVariables;
    }

    private final String orchestratorServiceHostname;
    private final String jobId;

    public Incisive(Map<String, Object> config, String jobId) {
        this.orchestratorServiceHostname = (String) config.get("INCISIVE_ORCHESTRATOR_SERVICE_HOSTNAME");
        this.jobId = jobId;
    }

    @Override
    public void communicateExecutionEnd(boolean isOk) throws PlatformException {
        JSONObject metadata = new JSONObject();

        if (isOk) {
            throw new PlatformException("Success finish execution not implemented", null);
        } else metadata.put("status", "Failed");

        try {
            String url = "http://" + this.orchestratorServiceHostname + "/api/jobs/" + this.jobId + "/ended_job_execution/";
            HttpCalls.HttpResponse httpResponse = HttpCalls.jsonUpload(url, metadata, "PATCH");

            if (httpResponse.getStatusCode() != 200) {
                throw new PlatformException("Error while updating the end of the execution to the Orchestrator. Wrong status code: " + httpResponse.getStatusCode() + " " + httpResponse.getResponseContents(), null);
            }

        } catch (IOException e) {
            throw new PlatformException("Error while updating the end of the execution to the Orchestrator", e);
        }
    }
}
