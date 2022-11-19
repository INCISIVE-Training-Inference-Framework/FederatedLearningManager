package platform_adapter.types;

import config.EnvironmentVariable;
import config.EnvironmentVariableType;
import exceptions.BadConfigurationException;
import exceptions.PlatformException;
import org.json.JSONException;
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
        abstractClassVariables.add(new EnvironmentVariable("INCISIVE_MAAS_SERVICE_HOSTNAME", EnvironmentVariableType.STRING));
        return abstractClassVariables;
    }

    private final String orchestratorServiceHostname;
    private final String maasServiceHostname;
    private final String jobId;
    private int modelId;

    private final JSONObject modelMetadata;


    public Incisive(Map<String, Object> config, String jobId, JSONObject modelMetadata) throws BadConfigurationException {
        this.orchestratorServiceHostname = (String) config.get("INCISIVE_ORCHESTRATOR_SERVICE_HOSTNAME");
        this.maasServiceHostname = (String) config.get("INCISIVE_MAAS_SERVICE_HOSTNAME");
        this.jobId = jobId;
        this.modelMetadata = modelMetadata;
    }

    @Override
    public void communicateExecutionEnd(boolean isOk) throws PlatformException {
        JSONObject metadata = new JSONObject();

        if (isOk) {
            metadata.put("status", "Succeeded");
            metadata.put("result", "http://" + this.maasServiceHostname + "/api/models/" + modelId + "/download_model_files/");
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

    @Override
    public void uploadModel(byte[] model) throws PlatformException {
        try {
            String url = "http://" + this.maasServiceHostname + "/api/models/update_or_create/";
            HttpCalls.HttpResponse httpResponse = HttpCalls.multipartJsonAndFileUpload(url, this.modelMetadata, "model_files", model);

            if (httpResponse.getStatusCode() != 201 && httpResponse.getStatusCode() != 200) {
                throw new PlatformException("Error while uploading model to the MaaS. Wrong status code: " + httpResponse.getStatusCode() + " " + httpResponse.getResponseContents(), null);
            }

            JSONObject responseJson = new JSONObject(httpResponse.getResponseContents());
            this.modelId = responseJson.getInt("id");

        } catch (JSONException | IOException e) {
            throw new PlatformException("Error while uploading model to the MaaS", e);
        }
    }
}
