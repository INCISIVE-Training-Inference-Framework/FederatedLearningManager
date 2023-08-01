package utils;

import exceptions.InternalException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class HttpMethods {

    public static JSONObject patchMultipartMethod(
            String url,
            JSONObject jsonEntity,
            List<String> fileNameList,
            List<File> fileEntityList,
            Set<Integer> expectedStatusCode,
            String errorMessage
    ) throws InternalException {
        return multipartMethod(new HttpPatch(url), jsonEntity, fileNameList, fileEntityList, expectedStatusCode, errorMessage);
    }

    private static JSONObject multipartMethod(
            HttpEntityEnclosingRequestBase httpMethod,
            JSONObject jsonEntity,
            List<String> fileNameList,
            List<File> fileEntityList,
            Set<Integer> expectedStatusCode,
            String errorMessage
    ) throws InternalException {
        try(CloseableHttpClient client = HttpClients.createDefault()) {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addTextBody("data", jsonEntity.toString(), ContentType.APPLICATION_JSON);
            for (int i = 0; i < fileNameList.size(); ++i) {
                builder.addBinaryBody(
                        fileNameList.get(i),
                        fileEntityList.get(i),
                        ContentType.DEFAULT_BINARY,
                        fileNameList.get(i)
                );
            }
            HttpEntity entity = builder.build();
            httpMethod.setEntity(entity);
            return responseHandling(client, httpMethod, expectedStatusCode, errorMessage);
        } catch (IOException e) {
            throw new InternalException(String.format("%s. Error during post creation", errorMessage), e);
        }
    }

    private static JSONObject responseHandling(
            CloseableHttpClient client,
            HttpEntityEnclosingRequestBase httpMethod,
            Set<Integer> expectedStatusCode,
            String errorMessage
    ) throws InternalException {
        try(CloseableHttpResponse response = client.execute(httpMethod)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (!expectedStatusCode.contains(statusCode)) {
                throw new InternalException(String.format(
                        "%s. Wrong response status code. Expected: %s. Actual: %d. %s",
                        errorMessage,
                        expectedStatusCode,
                        statusCode,
                        response.getStatusLine().getReasonPhrase()),
                        null
                );
            }
            return new JSONObject(new String(
                    response.getEntity().getContent().readAllBytes(),
                    StandardCharsets.UTF_8
            ));
        } catch (IOException | JSONException e) {
            throw new InternalException(String.format("%s. Error during post response", errorMessage), e);
        }
    }

}
