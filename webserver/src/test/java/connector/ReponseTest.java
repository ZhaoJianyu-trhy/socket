package connector;

import org.junit.Assert;
import org.junit.Test;
import utils.TestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ReponseTest {


    private static final String validRequest = "Get /index.html HTTP/1.1";
    private static final String invalidRequest = "Get /notfound.html HTTP/1.1";

    private static final String status200 = "HTTP/1.1 200 OK\r\n\r\n";
    private static final String status404 = "HTTP/1.1 404 File Not Found\r\n\r\n";

    @Test
    public void givenValidRequest_thenReturnStaticResource() throws IOException {
        Request request = TestUtils.createRequest(validRequest);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Response response = new Response(out);
        response.setRequest(request);
        response.sendStaticResource();

        String resource = TestUtils.readFileToString(ConnectorUtils.WEB_ROOT + request.getRequestUri());
        Assert.assertEquals(status200 + resource, out.toString());
    }

    @Test
    public void givenValidRequest_thenReturnError() throws IOException {
        Request request = TestUtils.createRequest(invalidRequest);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Response response = new Response(out);
        response.setRequest(request);
        response.sendStaticResource();

        String resource = TestUtils.readFileToString(ConnectorUtils.WEB_ROOT + "/404.html");
        Assert.assertEquals(status404 + resource, out.toString());
    }
}