package ai_engine.linkage;

import ai_engine_adapter.linkage.AIEngineLinkageAdapter;
import ai_engine_adapter.linkage.types.async_rest_api.AsyncRestAPI;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import exceptions.AIEngineException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.*;
import org.wiremock.webhooks.Webhooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.http.RequestMethod.DELETE;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.wiremock.webhooks.Webhooks.webhook;

public class TestAsyncRestAPI {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(8001).extensions(Webhooks.class), false);

    private final static String pingUrl = "/api/client/ping";
    private final static String runUrl = "/api/client/run";
    private final static String endUrl = "/api/client/end";
    private final static String callbackUrl = "/api/server/callback";

    private AIEngineLinkageAdapter aiEngineLinkageAdapter;

    @Before
    public void beforeEach() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_ITERATION_TIME", 5L);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_INITIALIZATION_TIME", 2L);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_FINALIZATION_TIME", 2L);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_FINALIZATION_RETRIES", 2);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_CLIENT_HOST", String.format("127.0.0.1:%d", 8001));
        config.put("AI_ENGINE_LINKAGE_ADAPTER_SERVER_HOST", String.format("127.0.0.1:%d", 8000));
        config.put("AI_ENGINE_LINKAGE_ADAPTER_PING_URL", pingUrl);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_RUN_URL", runUrl);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_END_URL", endUrl);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_CALLBACK_URL", callbackUrl);
        aiEngineLinkageAdapter = new AsyncRestAPI(config);
    }

    @After
    public void afterEach() throws Exception {
        aiEngineLinkageAdapter.clean();
    }

    @Test
    public void pingSuccess() throws Exception {
        stubFor(get(urlEqualTo(pingUrl))
                .willReturn(aResponse().withStatus(200))
        );
        aiEngineLinkageAdapter.waitAIEngineToBeReady();
    }

    @Test
    public void pingFailureNoResponse() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_ITERATION_TIME", 2L);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_INITIALIZATION_TIME", 2L);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_FINALIZATION_TIME", 2L);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_FINALIZATION_RETRIES", 2);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_CLIENT_HOST", String.format("127.0.0.1:%d", 8002));
        config.put("AI_ENGINE_LINKAGE_ADAPTER_SERVER_HOST", String.format("127.0.0.1:%d", 8000));
        config.put("AI_ENGINE_LINKAGE_ADAPTER_PING_URL", pingUrl);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_RUN_URL", runUrl);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_END_URL", endUrl);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_CALLBACK_URL", callbackUrl);
        aiEngineLinkageAdapter = new AsyncRestAPI(config);

        Exception exception = assertThrows(AIEngineException.class, () -> {
            aiEngineLinkageAdapter.waitAIEngineToBeReady();
        });

        String expectedMessage = "Internal exception: AI Engine exception: Internal exception: Error while waiting for the AI Engine to be ready. It did not start before the timeout";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void pingFailureIncorrect() throws Exception {
        stubFor(get(pingUrl).willReturn(serverError()));
        Exception exception = assertThrows(AIEngineException.class, () -> {
            aiEngineLinkageAdapter.waitAIEngineToBeReady();
        });

        String expectedMessage = "Internal exception: AI Engine exception: Internal exception: Error while waiting for the AI Engine to be ready (during the query). Incorrect initialization with status code 500";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void runSuccess() throws Exception {
        String use_case = "training_from_scratch";
        String ack_url = String.format("http://127.0.0.1:%d%s", 8000, callbackUrl);
        stubFor(post(String.format("%s?use_case=%s&callback_url=%s", runUrl, use_case, ack_url)).willReturn(ok()));
        aiEngineLinkageAdapter.initialize();
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
        String ack_url = String.format("http://127.0.0.1:%d%s", 8000, callbackUrl);
        stubFor(post(String.format("%s?use_case=%s&callback_url=%s", runUrl, use_case, ack_url)).willReturn(ok()));
        aiEngineLinkageAdapter.initialize();
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

        String expectedMessage = "Internal exception: AI Engine exception: Internal exception: Error while running use case. Error while parsing returning error message";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void runFailureNoACK() throws Exception {
        String use_case = "training_from_scratch";
        String ack_url = String.format("http://127.0.0.1:%d%s", 8000, callbackUrl);
        stubFor(post(String.format("%s?use_case=%s&callback_url=%s", runUrl, use_case, ack_url)).willReturn(ok()));
        aiEngineLinkageAdapter.initialize();

        Exception exception = assertThrows(AIEngineException.class, () -> {
            aiEngineLinkageAdapter.run(use_case);
        });

        String expectedMessage = "Internal exception: AI Engine exception: Internal exception: Error while running use case. The end of the iteration was not notified on time";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void runFailureConnectionRefused() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_ITERATION_TIME", 2L);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_INITIALIZATION_TIME", 2L);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_FINALIZATION_TIME", 2L);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_MAX_FINALIZATION_RETRIES", 2);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_CLIENT_HOST", String.format("127.0.0.1:%d", 8002));
        config.put("AI_ENGINE_LINKAGE_ADAPTER_SERVER_HOST", String.format("127.0.0.1:%d", 8000));
        config.put("AI_ENGINE_LINKAGE_ADAPTER_PING_URL", pingUrl);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_RUN_URL", runUrl);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_END_URL", endUrl);
        config.put("AI_ENGINE_LINKAGE_ADAPTER_CALLBACK_URL", callbackUrl);
        aiEngineLinkageAdapter = new AsyncRestAPI(config);
        aiEngineLinkageAdapter.initialize();

        String use_case = "training_from_scratch";

        Exception exception = assertThrows(AIEngineException.class, () -> {
            aiEngineLinkageAdapter.run(use_case);
        });

        String expectedMessage = "Internal exception: AI Engine exception: Internal exception: Error while running use case (during the query)";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void endSuccess() throws Exception {
        stubFor(get(urlEqualTo(pingUrl))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
        );
        UUID stubId1 = UUID.randomUUID();
        stubFor(get(urlEqualTo(pingUrl))
                .withId(stubId1)
                .willReturn(aResponse().withStatus(200))
        );
        stubFor(post(urlEqualTo(endUrl))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
        );
        UUID stubId2 = UUID.randomUUID();
        stubFor(post(urlEqualTo(endUrl))
                .withId(stubId2)
                .willReturn(aResponse().withStatus(200))
                .withPostServeAction("webhook", webhook()
                        .withMethod(DELETE)
                        .withUrl(wireMockRule.url("/__admin/mappings/" + stubId1))
                )
                .withPostServeAction("webhook", webhook()
                        .withMethod(DELETE)
                        .withUrl(wireMockRule.url("/__admin/mappings/" + stubId2))
                )
        );

        aiEngineLinkageAdapter.end();
    }

}
