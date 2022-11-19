package utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
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

    public static HttpResponse multipartJsonAndFileUpload(String url, JSONObject jsonObject, String fileName, byte[] fileContents) throws IOException {
        // TODO: change to https
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addTextBody("data", jsonObject.toString(), ContentType.APPLICATION_JSON);
            builder.addBinaryBody(fileName, fileContents, ContentType.DEFAULT_BINARY, fileName);
            HttpEntity entity = builder.build();
            post.setEntity(entity);
            CloseableHttpResponse response = client.execute(post);
            return new HttpResponse(response);
        }
    }
}
