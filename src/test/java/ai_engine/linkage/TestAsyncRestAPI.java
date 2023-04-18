package ai_engine.linkage;

import ai_engine_adapter.linkage.AIEngineLinkageAdapter;
import ai_engine_adapter.linkage.types.AsyncRestAPI;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import exceptions.AIEngineException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertThrows;

@WireMockTest
public class TestAsyncRestAPI {

    private final static String pingUrl = "/api/client/ping";
    private final static String runUrl = "/api/client/run";
    private final static String callbackUrl = "/api/server/callback";
    private static int clientPort;
    private static int serverPortAdd = 0;  // http server does not free address on time

    private AIEngineLinkageAdapter aiEngineLinkageAdapter;

    @BeforeEach
    public void beforeEach(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        clientPort = wmRuntimeInfo.getHttpPort();
        serverPortAdd += 1;

        Map<String, Object> config = new HashMap<>();
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_ITERATION_TIME", 5L);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_INITIALIZATION_TIME", 2L);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_CLIENT_HOST", String.format("127.0.0.1:%d", clientPort));
        config.put("AI_ENGINE_LINKAGE_ADAPTER_SERVER_HOST", String.format("127.0.0.1:%d", clientPort + serverPortAdd));
        config.put("AI_ENGINE_LINKAGE_ADAPTER_PING_URL", pingUrl);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_RUN_URL", runUrl);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_CALLBACK_URL", callbackUrl);
        aiEngineLinkageAdapter = new AsyncRestAPI(config);
        aiEngineLinkageAdapter.initialize();
    }

    @Test
    public void pingSuccess() throws Exception {
        stubFor(get("/api/client/ping").willReturn(ok()));
        aiEngineLinkageAdapter.waitAIEngineToBeReady();
    }

    @Test
    public void pingFailureNoResponse(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        // change port, so wiremock will not respond to the call
        clientPort = wmRuntimeInfo.getHttpPort() + 100;
        Map<String, Object> config = new HashMap<>();
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_ITERATION_TIME", 2L);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_INITIALIZATION_TIME", 2L);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_CLIENT_HOST", String.format("127.0.0.1:%d", clientPort));
        config.put("AI_ENGINE_LINKAGE_ADAPTER_SERVER_HOST", String.format("127.0.0.1:%d", clientPort + 1));
        config.put("AI_ENGINE_LINKAGE_ADAPTER_PING_URL", pingUrl);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_RUN_URL", runUrl);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_CALLBACK_URL", callbackUrl);
        aiEngineLinkageAdapter = new AsyncRestAPI(config);
        aiEngineLinkageAdapter.initialize();

        Exception exception = assertThrows(AIEngineException.class, () -> {
            aiEngineLinkageAdapter.waitAIEngineToBeReady();
        });

        String expectedMessage = "Internal exception: AI Engine exception: Error while waiting for the AI Engine to be ready. It did not start before the timeout";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void pingFailureIncorrect() throws Exception {
        stubFor(get("/api/client/ping").willReturn(serverError()));
        Exception exception = assertThrows(AIEngineException.class, () -> {
            aiEngineLinkageAdapter.waitAIEngineToBeReady();
        });

        String expectedMessage = "Internal exception: AI Engine exception: Error while waiting for the AI Engine to be ready (during the query). Incorrect initialization with status code 500";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void runSuccess() throws Exception {
        String use_case = "training_from_scratch";
        String ack_url = String.format("http://127.0.0.1:%d%s", clientPort + serverPortAdd, callbackUrl);
        stubFor(post(String.format("%s?use_case=%s&callback_url=%s", runUrl, use_case, ack_url)).willReturn(ok()));
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(2000);
                CloseableHttpClient client = HttpClients.createDefault();
                HttpPost httpPost = new HttpPost(ack_url);
                String json = "{\"SUCCESS\": true}";
                StringEntity entity = new StringEntity(json);
                httpPost.setEntity(entity);
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");
                client.execute(httpPost);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
        aiEngineLinkageAdapter.run(use_case);
    }

    @Test
    public void runFailureIncorrect() throws Exception {
        String use_case = "training_from_scratch";
        String ack_url = String.format("http://127.0.0.1:%d%s", clientPort + serverPortAdd, callbackUrl);
        stubFor(post(String.format("%s?use_case=%s&callback_url=%s", runUrl, use_case, ack_url)).willReturn(ok()));
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(2000);
                CloseableHttpClient client = HttpClients.createDefault();
                HttpPost httpPost = new HttpPost(ack_url);
                String json = "{\"SUCCESS\": false}";
                StringEntity entity = new StringEntity(json);
                httpPost.setEntity(entity);
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");
                client.execute(httpPost);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();

        Exception exception = assertThrows(AIEngineException.class, () -> {
            aiEngineLinkageAdapter.run(use_case);
        });

        String expectedMessage = "Internal exception: AI Engine exception: Error while running use case. {\"SUCCESS\":false}";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void runFailureNoACK() throws Exception {
        String use_case = "training_from_scratch";
        String ack_url = String.format("http://127.0.0.1:%d%s", clientPort + serverPortAdd, callbackUrl);
        stubFor(post(String.format("%s?use_case=%s&callback_url=%s", runUrl, use_case, ack_url)).willReturn(ok()));

        Exception exception = assertThrows(AIEngineException.class, () -> {
            aiEngineLinkageAdapter.run(use_case);
        });

        String expectedMessage = "Internal exception: AI Engine exception: Error while running use case. The end of the iteration was not notified on time";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void runFailureConnectionRefused(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        // change port, so wiremock will not respond to the call
        clientPort = wmRuntimeInfo.getHttpPort() + 100;
        Map<String, Object> config = new HashMap<>();
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_ITERATION_TIME", 2L);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_INITIALIZATION_TIME", 2L);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_CLIENT_HOST", String.format("127.0.0.1:%d", clientPort));
        config.put("AI_ENGINE_LINKAGE_ADAPTER_SERVER_HOST", String.format("127.0.0.1:%d", clientPort + 1));
        config.put("AI_ENGINE_LINKAGE_ADAPTER_PING_URL", pingUrl);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_RUN_URL", runUrl);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_CALLBACK_URL", callbackUrl);
        aiEngineLinkageAdapter = new AsyncRestAPI(config);
        aiEngineLinkageAdapter.initialize();

        String use_case = "training_from_scratch";

        Exception exception = assertThrows(AIEngineException.class, () -> {
            aiEngineLinkageAdapter.run(use_case);
        });

        String expectedMessage = "Internal exception: AI Engine exception: Error while running use case (during the query)";
        String actualMessage = exception.getMessage();
        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @AfterEach
    public void afterEach() throws Exception {
        aiEngineLinkageAdapter.clean();
    }

}
