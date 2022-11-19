package utils;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class HttpCalls {

    public static class HttpResponse {
        private final int statusCode;
        private String responseContents;

        public HttpResponse(CloseableHttpResponse response) throws IOException {
            this.statusCode = response.getStatusLine().getStatusCode();

            this.responseContents = null;
            if (response.getEntity() != null) {
                try (InputStream inputStream = response.getEntity().getContent()) {
                    this.responseContents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseContents() {
            return responseContents;
        }
    }

    public static HttpResponse jsonUpload(String url, JSONObject jsonObject, String methodName) throws IOException {
        HttpEntityEnclosingRequestBase method;
        if (methodName.equals("PATCH")) method = new HttpPatch(url);
        else method = new HttpPost(url);
        // TODO: change to https
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            StringEntity entity = new StringEntity(jsonObject.toString());
            method.setEntity(entity);
            method.setHeader("Accept", "application/json");
            method.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = client.execute(method);
            return new HttpResponse(response);
        }
    }

}
