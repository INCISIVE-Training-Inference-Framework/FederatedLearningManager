package ai_engine_adapter.types;

import ai_engine_adapter.AIEngineAdapter;
import config.EnvironmentVariable;
import config.EnvironmentVariableType;
import exceptions.AIEngineException;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class KubernetesApi extends AIEngineAdapter {

    private static final String AI_ENGINE_CONTAINER_NAME = "ai-engine";
    private static final Logger logger = LogManager.getLogger(KubernetesApi.class);
    public static List<EnvironmentVariable> getEnvironmentVariables() {
        List<EnvironmentVariable> abstractClassVariables = new ArrayList<>();
        abstractClassVariables.add(new EnvironmentVariable("KUBERNETES_NAMESPACE", EnvironmentVariableType.STRING));
        abstractClassVariables.add(new EnvironmentVariable("KUBERNETES_MAX_ITERATION_TIME", EnvironmentVariableType.LONG));  // seconds
        abstractClassVariables.add(new EnvironmentVariable("KUBERNETES_MAX_AI_ENGINE_INITIALIZATION_TIME", EnvironmentVariableType.LONG));  // seconds
        return abstractClassVariables;
    }

    private KubernetesClient k8s;
    private final String jobName;
    private String podName;
    private final String namespace;
    private final long maxIterationTime;
    private final long maxAIEngineInitializationTime;
    private static CountDownLatch execLatch;
    private static int exitStatus = 0;

    public KubernetesApi(Map<String, Object> config, String jobName) {
        this.namespace = (String) config.get("KUBERNETES_NAMESPACE");
        this.maxIterationTime = (long) config.get("KUBERNETES_MAX_ITERATION_TIME");
        this.maxAIEngineInitializationTime = (long) config.get("KUBERNETES_MAX_AI_ENGINE_INITIALIZATION_TIME") * 1000;
        this.jobName = jobName;
    }

    @Override
    public void initialize() throws AIEngineException {
        super.initialize();
        this.k8s = new DefaultKubernetesClient();
        this.podName = getPodName(this.jobName, this.namespace);
        logger.debug("Pod name: " + this.podName);
        logger.debug("Container name: " + AI_ENGINE_CONTAINER_NAME);
    }

    @Override
    public void waitAIEngineToBeReady() throws AIEngineException {
        logger.debug("waitAIEngineToBeReady method called");

        Timestamp startTime = Timestamp.from(Instant.now());
        Timestamp currentTime = startTime;
        boolean AIEngineStarted = false;

        while (!AIEngineStarted && (currentTime.getTime() < (startTime.getTime() + this.maxAIEngineInitializationTime))) {
            // sleep 10 seconds between the requests
            try {
                Thread.sleep(10000);  // TODO implement better
            } catch (InterruptedException e) {
                throw new AIEngineException("Error while waiting for the AI Engine to be ready (during the thread sleep)", e);
            }

            // get pod
            Pod pod;
            try {
                pod = this.k8s.pods().inNamespace(namespace).withName(this.podName).get();
            } catch (Exception e) {
                throw new AIEngineException("Error while retrieving the AI Engine pod name", e);
            }

            // get AI Engine container status
            boolean found = false;
            for (ContainerStatus containerStatus: pod.getStatus().getContainerStatuses()) {
                if (containerStatus.getName().equals(AI_ENGINE_CONTAINER_NAME)) {
                    found = true;
                    AIEngineStarted = containerStatus.getStarted();
                    break;
                }
            }
            if (!found) throw new AIEngineException("Error while waiting for the AI Engine to be ready. Unable to find the container", null);

            currentTime = Timestamp.from(Instant.now());
        }

        if (!AIEngineStarted) throw new AIEngineException("Error while waiting for the AI Engine to be ready. It did not start before the timeout", null);
    }

    @Override
    public void run(String useCase) throws AIEngineException {
        String[] command = new String[]{"/bin/sh", "-c", "cd /usr/application && bash " + useCase + ".sh"};
        runCommandInsideContainer(command, "Error while running the AI Engine");
    }

    @Override
    public void endExecution() throws AIEngineException {
        String[] command = new String[]{"/bin/sh", "-c", "kill -15 1"};
        runCommandInsideContainer(command, "Error while ending AI Engine execution");
    }

    private String getPodName(String jobName, String namespace) throws AIEngineException {
        List<Pod> pods;
        try {
            pods = this.k8s.pods()
                    .inNamespace(namespace)
                    .withLabel("job-name", jobName)
                    .list()
                    .getItems();
        } catch (Exception e) {
            throw new AIEngineException("Error while retrieving the AI Engine pod name", e);
        }

        if (pods.size() != 1) {
            throw new AIEngineException("Error while retrieving the AI Engine pod name. More than one pod or none with the same name: " + pods.size(), null);
        }

        Pod pod = pods.get(0);
        return pod.getMetadata().getName();
    }

    private void runCommandInsideContainer(String[] command, String errorMessage) throws AIEngineException {
        logger.debug(String.join(",", command));
        execLatch = new CountDownLatch(1);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayOutputStream error = new ByteArrayOutputStream();
             ByteArrayOutputStream errorChannel = new ByteArrayOutputStream();
             ExecWatch ignored = this.k8s.pods()
                     .inNamespace(this.namespace)
                     .withName(this.podName)
                     .inContainer(AI_ENGINE_CONTAINER_NAME)
                     .writingOutput(out)
                     .writingError(error)
                     .writingErrorChannel(errorChannel)
                     .usingListener(new MyPodExecListener())
                     .exec(command)) {
            boolean latchTerminationStatus = execLatch.await(this.maxIterationTime, TimeUnit.SECONDS);
            if (!latchTerminationStatus) {
                throw new AIEngineException("The AI Engine did not terminate before the max permitted time", null);
            }
            logger.info("AI Engine output: {} ", out);
            logger.info("AI Engine error: {} ", error);
            logger.info("AI Engine error channel: {} ", errorChannel);
            if (exitStatus != 1000) {
                throw new AIEngineException(errorMessage + ". Exit status not equal to 1000 -> " + exitStatus + ". Reason: " + error, null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AIEngineException(errorMessage, e);
        } catch (KubernetesClientException e) {
            throw new AIEngineException(errorMessage + "Status code: " + e.getCode() + ". Reason: " + e.getStatus(), e);
        } catch (IOException e) {
            throw new AIEngineException(errorMessage + ". Error closing the streams", e);
        } catch (IllegalArgumentException e) {
            throw new AIEngineException(errorMessage + ". Illegal argument", e);
        }
    }

    private static class MyPodExecListener implements ExecListener {
        @Override
        public void onOpen() {
            logger.debug("Shell was opened");
        }

        @Override
        public void onFailure(Throwable t, Response failureResponse) {
            logger.debug("Some error encountered");
            execLatch.countDown();
        }

        @Override
        public void onClose(int i, String s) {
            logger.debug("Shell Closing");
            exitStatus = i;
            execLatch.countDown();
        }
    }
}
