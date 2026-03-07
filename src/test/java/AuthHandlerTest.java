

import com.hotel.handlers.AuthHandler;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AuthHandlerTest {

    private final AuthHandler handler = new AuthHandler();

    @Test
    void shouldReturn404ForUnknownPath() throws IOException {
        FakeHttpExchange ex = new FakeHttpExchange(
                "POST",
                "/auth/unknown",
                "{}"
        );

        handler.handle(ex);

        assertEquals(404, ex.getSentStatusCode());
        assertTrue(ex.getResponseBodyAsString().contains("Not found"));
    }

    @Test
    void shouldReturn404ForWrongMethod() throws IOException {
        FakeHttpExchange ex = new FakeHttpExchange(
                "GET",
                "/auth/login",
                ""
        );

        handler.handle(ex);

        assertEquals(404, ex.getSentStatusCode());
        assertTrue(ex.getResponseBodyAsString().contains("Not found"));
    }

    @Test
    void shouldReturn400WhenUsernameAndPasswordAreMissing() throws IOException {
        FakeHttpExchange ex = new FakeHttpExchange(
                "POST",
                "/auth/login",
                "{\"username\":\"\",\"password\":\"\"}"
        );

        handler.handle(ex);

        assertEquals(400, ex.getSentStatusCode());
        assertTrue(ex.getResponseBodyAsString().contains("Username and password are required."));
    }

    @Test
    void shouldReturn400WhenUsernameAndPasswordAreOnlySpaces() throws IOException {
        FakeHttpExchange ex = new FakeHttpExchange(
                "POST",
                "/auth/login",
                "{\"username\":\"   \",\"password\":\"   \"}"
        );

        handler.handle(ex);

        assertEquals(400, ex.getSentStatusCode());
        assertTrue(ex.getResponseBodyAsString().contains("Username and password are required."));
    }

    /**
     * Minimal fake HttpExchange for unit testing without Mockito.
     */
    static class FakeHttpExchange extends HttpExchange {
        private final String requestMethod;
        private final URI requestUri;
        private final Headers requestHeaders = new Headers();
        private final Headers responseHeaders = new Headers();
        private final InputStream requestBody;
        private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();

        private int sentStatusCode = -1;
        private long sentResponseLength = -1;

        FakeHttpExchange(String method, String path, String body) {
            this.requestMethod = method;
            this.requestUri = URI.create(path);
            this.requestBody = new ByteArrayInputStream(
                    body.getBytes(StandardCharsets.UTF_8)
            );
            requestHeaders.add("Content-Type", "application/json");
        }

        public int getSentStatusCode() {
            return sentStatusCode;
        }

        public long getSentResponseLength() {
            return sentResponseLength;
        }

        public String getResponseBodyAsString() {
            return responseBody.toString(StandardCharsets.UTF_8);
        }

        @Override
        public Headers getRequestHeaders() {
            return requestHeaders;
        }

        @Override
        public Headers getResponseHeaders() {
            return responseHeaders;
        }

        @Override
        public URI getRequestURI() {
            return requestUri;
        }

        @Override
        public String getRequestMethod() {
            return requestMethod;
        }

        @Override
        public HttpContext getHttpContext() {
            return null;
        }

        @Override
        public void close() {
            try {
                requestBody.close();
                responseBody.close();
            } catch (IOException ignored) {
            }
        }

        @Override
        public InputStream getRequestBody() {
            return requestBody;
        }

        @Override
        public OutputStream getResponseBody() {
            return responseBody;
        }

        @Override
        public void sendResponseHeaders(int rCode, long responseLength) {
            this.sentStatusCode = rCode;
            this.sentResponseLength = responseLength;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return new InetSocketAddress("localhost", 12345);
        }

        @Override
        public int getResponseCode() {
            return sentStatusCode;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return new InetSocketAddress("localhost", 8080);
        }

        @Override
        public String getProtocol() {
            return "HTTP/1.1";
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public void setAttribute(String name, Object value) {
            // no-op
        }

        @Override
        public void setStreams(InputStream i, OutputStream o) {
            // no-op
        }

        @Override
        public HttpPrincipal getPrincipal() {
            return null;
        }
    }
}