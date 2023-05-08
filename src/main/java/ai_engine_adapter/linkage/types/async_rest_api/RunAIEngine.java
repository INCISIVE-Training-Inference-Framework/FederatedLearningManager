package ai_engine_adapter.linkage.types.async_rest_api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import exceptions.InternalException;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import utils.FileMethods;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class RunAIEngine {

    private static final int PING_TIMEOUT = 3;  // seconds
    private static final int RUN_TIMEOUT = 3;  // seconds
    private static CountDownLatch countDownLatch = null;
    private static ServerHandlingOutput serverHandlingOutput = null;
    private static final Logger logger = LogManager.getLogger(RunAIEngine.class);

    private final long maxIterationTime;
    private final long maxInitializationTime;
    private final String clientHost;
    private final String serverHost;
    private final String pingUrl;
    private final String runUrl;
    private final String callbackUrl;

    private HttpServer server;

    public RunAIEngine(
            long maxIterationTime,
            long maxInitializationTime,
            String clientHost,
            String serverHost,
            String pingUrl,
            String runUrl,
            String callbackUrl
    ) {
        this.maxIterationTime =maxIterationTime;
        this.maxInitializationTime = maxInitializationTime;
        this.clientHost = clientHost;
        this.serverHost = serverHost;
        this.pingUrl = pingUrl;
        this.runUrl = runUrl;
        this.callbackUrl = callbackUrl;
    }

    private final class ServerHandlingOutput {

        private final boolean goodAck;
        private final String message;

        private ServerHandlingOutput(boolean goodAck, String message) {
            this.goodAck = goodAck;
            this.message = message;
        }

        private boolean isGoodAck() {
            return this.goodAck;
        }

        private String getMessage() {
            return this.message;
        }
    }

    public void initialize() throws InternalException {
        String serverIp = serverHost.split(":")[0];
        int serverPort = Integer.parseInt(serverHost.split(":")[1]);

        try {
            this.server = HttpServer.create(new InetSocketAddress(serverIp, serverPort), 0);
        } catch (IOException e) {
            throw new InternalException("Error while initializing server", e);
        }
        this.server.createContext(this.callbackUrl, new ServerHandler());
        this.server.setExecutor(null); // creates a default executor
        this.server.start();
    }

    public void waitAIEngineToBeReady() throws InternalException {
        logger.debug("Waiting for AI Engine to be ready");

        Timestamp startTime = Timestamp.from(Instant.now());
        Timestamp currentTime = startTime;
        boolean AIEngineStarted = false;

        // set config
        RequestConfig config = RequestConfig.custom().setConnectTimeout(PING_TIMEOUT * 1000).build();

        // create client
        try(CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {

            // iterate until AIEngine is ready
            while (!AIEngineStarted && (currentTime.getTime() < (startTime.getTime() + this.maxInitializationTime  * 1000))) {

                // query AI Engine
                try(CloseableHttpResponse response = client.execute(new HttpGet(String.format("http://%s%s", this.clientHost, this.pingUrl)))) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK) AIEngineStarted = true;
                    else throw new InternalException(String.format("Error while waiting for the AI Engine to be ready (during the query). Incorrect initialization with status code %d", statusCode), null);
                } catch (ConnectException | NoHttpResponseException | ConnectTimeoutException e) {
                    // do nothing
                } catch (IOException e) {
                    throw new InternalException("Error while waiting for the AI Engine to be ready (during the query)", e);
                }

                // sleep 3 seconds
                if (!AIEngineStarted) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        throw new InternalException("Error while waiting for the AI Engine to be ready (during the thread sleep)", e);
                    }
                }

                currentTime = Timestamp.from(Instant.now());
            }

        } catch (IOException e) {
            throw new InternalException("Error while waiting for the AI Engine to be ready (during the client instantiation)", e);
        }

        if (!AIEngineStarted) throw new InternalException("Error while waiting for the AI Engine to be ready. It did not start before the timeout", null);
    }

    public void run(String useCase) throws InternalException {
        logger.debug(String.format("Running the AI Engine. Use case: %s", useCase));

        RequestConfig config = RequestConfig.custom().setConnectTimeout(RUN_TIMEOUT * 1000).build();
        try(CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {

            // create post
            HttpPost httpPost = new HttpPost(String.format(
                    "http://%s%s?use_case=%s&callback_url=%s",
                    this.clientHost,
                    this.runUrl,
                    useCase,
                    String.format("http://%s%s", this.serverHost, this.callbackUrl)
            ));
            StringEntity entity = new StringEntity("{}");
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            // send post
            countDownLatch = new CountDownLatch(1);
            serverHandlingOutput = null;
            try(CloseableHttpResponse response = client.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) throw new InternalException("Error while running use case. Status code equal to " + statusCode + ". " + response.getStatusLine().getReasonPhrase(), null);
            } catch (IOException e) {
                throw new InternalException("Error while running use case (during the query)", e);
            }

            // wait for ack
            boolean received;
            try {
                received = countDownLatch.await(this.maxIterationTime, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new InternalException("Error while running use case. While waiting the ack", e);
            }

            // check ack
            if (!received) throw new InternalException("Error while running use case. The end of the iteration was not notified on time", null);
            if (serverHandlingOutput == null) throw new InternalException("Error while running use case. The server did not save the response information", null);
            if (!serverHandlingOutput.isGoodAck()) throw new InternalException(String.format("Error while running use case. %s", serverHandlingOutput.getMessage()), null);

        } catch (IOException e) {
            throw new InternalException("Error while running use case (during the query creation)", e);
        }
    }

    public void clean() throws InternalException {
        // stop own server
        try {
            if (this.server != null) this.server.stop(0);
        } catch (Exception e) {
            throw new InternalException("Error while shutting down own server", e);
        }
    }

    private class ServerHandler implements HttpHandler {

        // ACK response handler
        @Override
        public void handle(HttpExchange httpExchange) {
            logger.debug("Received callback");
            boolean goodAck = true;
            int responseStatus = 200;
            String message = null;

            if (!"POST".equals(httpExchange.getRequestMethod())) {
                goodAck = false;
                responseStatus = 405;
                message = "Bad request. Only POST method allowed";
            } else {
                try (InputStream inputStream = httpExchange.getRequestBody()) {
                    try {
                        JSONObject jsonResponse = FileMethods.readJson(inputStream);
                        boolean success = (boolean) jsonResponse.get("SUCCESS");
                        if (!success) {
                            goodAck = false;
                            message = jsonResponse.toString();
                        }
                    } catch (IOException e) {
                        goodAck = false;
                        responseStatus = 400;
                        message = String.format("Bad request: incorrect JSON format. %s", e.getMessage());
                    }
                } catch (IOException e) {
                    goodAck = false;
                    responseStatus = 500;
                    message = String.format("Server error: while reading response. %s", e.getMessage());
                }
            }

            try(OutputStream outputStream = httpExchange.getResponseBody()) {
                // encode response
                JSONObject response = new JSONObject();
                if (message != null) {
                    response.put("message", message);
                }

                // send response
                httpExchange.sendResponseHeaders(responseStatus, response.toString().length());
                outputStream.write(response.toString().getBytes());
                outputStream.flush();
            } catch (IOException e) {
                goodAck = false;
                message = String.format("Server error: while writing response. %s", e.getMessage());
            }

            serverHandlingOutput = new ServerHandlingOutput(goodAck, message);
            countDownLatch.countDown();
            logger.debug(goodAck + " " + responseStatus + " " + message);
        }

    }


}