package ai_engine_adapter.linkage.types.async_rest_api;

import exceptions.InternalException;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;


public class EndAIEngine {

    private static final int PING_TIMEOUT = 3;  // seconds
    private static final int END_TIMEOUT = 3;  // seconds
    private static final Logger logger = LogManager.getLogger(EndAIEngine.class);

    private final long maxFinalizationTime;
    private final int maxFinalizationRetries;
    private final String clientHost;
    private final String pingUrl;
    private final String endUrl;

    public EndAIEngine(
            long maxFinalizationTime,
            int maxFinalizationRetries,
            String clientHost,
            String pingUrl,
            String endUrl
    ) {
        this.maxFinalizationTime = maxFinalizationTime;
        this.maxFinalizationRetries = maxFinalizationRetries;
        this.clientHost = clientHost;
        this.pingUrl = pingUrl;
        this.endUrl = endUrl;
    }

    public void end() throws InternalException {
        boolean AIEngineFinished = false;
        int finalizationRetries = 0;

        while (!AIEngineFinished && finalizationRetries < this.maxFinalizationRetries) {
            logger.debug(String.format("Finishing the AI Engine. Retries: %d", finalizationRetries));

            // send finish signal
            RequestConfig config = RequestConfig.custom().setConnectTimeout(END_TIMEOUT * 1000).build();
            try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {

                // create post
                HttpPost httpPost = new HttpPost(String.format("http://%s%s", this.clientHost, this.endUrl));
                StringEntity entity = new StringEntity("{}");
                httpPost.setEntity(entity);
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                // send post
                try (CloseableHttpResponse response = client.execute(httpPost)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK && finalizationRetries == 0) throw new InternalException("Error while ending AI Engine. Status code equal to " + statusCode + ". " + response.getStatusLine().getReasonPhrase(), null);
                } catch (IOException e) {
                    // do nothing
                }

            } catch (IOException e) {
                throw new InternalException("Error while running use case (during the query creation)", e);
            }

            // assure AI Engine is down
            Timestamp startTime = Timestamp.from(Instant.now());
            Timestamp currentTime = startTime;
            config = RequestConfig.custom().setConnectTimeout(PING_TIMEOUT * 1000).build();
            try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {

                // iterate until AIEngine finishes
                while (!AIEngineFinished && (currentTime.getTime() < (startTime.getTime() + this.maxFinalizationTime * 1000))) {

                    // query AI Engine
                    try (CloseableHttpResponse response = client.execute(new HttpGet(String.format("http://%s%s", this.clientHost, this.pingUrl)))) {
                        // empty
                    } catch (IOException e) {
                        AIEngineFinished = true;
                    }

                    // sleep 3 seconds
                    if (!AIEngineFinished) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            throw new InternalException("Error while waiting for the AI Engine to finish (during the thread sleep in the ping)", e);
                        }
                    }

                    currentTime = Timestamp.from(Instant.now());
                }

            } catch (IOException e) {
                throw new InternalException("Error while waiting for the AI Engine to finish (during the ping client instantiation)", e);
            }

            finalizationRetries += 1;
        }

        if (!AIEngineFinished) throw new InternalException("Error while waiting for the AI Engine to finish. It did not end before the timeout", null);
    }

}