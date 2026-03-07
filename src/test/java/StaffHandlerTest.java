
import com.hotel.handlers.StaffHandler;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class StaffHandlerTest {

    private final StaffHandler handler = new StaffHandler();

    @Test
    void shouldHandleCorsPreflight() throws IOException {
        FakeHttpExchange ex = new FakeHttpExchange(
                "OPTIONS",
                "/api/staff",
                ""
        );

        handler.handle(ex);

        assertEquals(204, ex.getSentStatusCode());
        assertEquals("*", ex.getResponseHeaders().getFirst("Access-Control-Allow-Origin"));
        assertEquals("GET,POST,PUT,DELETE,OPTIONS",
                ex.getResponseHeaders().getFirst("Access-Control-Allow-Methods"));
        assertEquals("Content-Type,Authorization",
                ex.getResponseHeaders().getFirst("Access-Control-Allow-Headers"));
    }

    @Test
    void shouldReturn400WhenCreateStaffMissingRequiredFields() throws IOException {
        FakeHttpExchange ex = new FakeHttpExchange(
                "POST",
                "/api/staff",
                "{\"username\":\"\",\"password\":\"\",\"fullName\":\"\"}"
        );

        handler.handle(ex);

        assertEquals(400, ex.getSentStatusCode());
        assertTrue(ex.getResponseBodyAsString()
                .contains("username, password, and fullName are required."));
        assertJsonHeaders(ex);
    }

    @Test
    void shouldReturn400WhenCreateStaffFieldsAreBlank() throws IOException {
        FakeHttpExchange ex = new FakeHttpExchange(
                "POST",
                "/api/staff",
                "{\"username\":\"   \",\"password\":\"   \",\"fullName\":\"   \"}"
        );

        handler.handle(ex);

        assertEquals(400, ex.getSentStatusCode());
        assertTrue(ex.getResponseBodyAsString()
                .contains("username, password, and fullName are required."));
        assertJsonHeaders(ex);
    }

    @Test
    void shouldReturn400WhenCreateStaffRoleIsInvalid() throws IOException {
        FakeHttpExchange ex = new FakeHttpExchange(
                "POST",
                "/api/staff",
                "{\"username\":\"john\",\"password\":\"1234\",\"fullName\":\"John Doe\",\"role\":\"manager\"}"
        );

        handler.handle(ex);

        assertEquals(400, ex.getSentStatusCode());
        assertTrue(ex.getResponseBodyAsString()
                .contains("role must be 'admin' or 'staff'."));
        assertJsonHeaders(ex);
    }

    @Test
    void shouldReturn400WhenUpdateMissingStaffId() throws IOException {
        FakeHttpExchange ex = new FakeHttpExchange(
                "PUT",
                "/api/staff",
                "{\"fullName\":\"New Name\"}"
        );

        handler.handle(ex);

        assertEquals(400, ex.getSentStatusCode());
        assertTrue(ex.getResponseBodyAsString().contains("Staff ID required."));
        assertJsonHeaders(ex);
    }

    @Test
    void shouldReturn400WhenUpdateHasNoFields() throws IOException {
        FakeHttpExchange ex = new FakeHttpExchange(
                "PUT",
                "/api/staff/5",
                "{}"
        );

        handler.handle(ex);

        assertEquals(400, ex.getSentStatusCode());
        assertTrue(ex.getResponseBodyAsString().contains("No fields to update."));
        assertJsonHeaders(ex);
    }

    @Test
    void shouldReturn400WhenUpdateOnlyBlankPassword() throws IOException {
        FakeHttpExchange ex = new FakeHttpExchange(
                "PUT",
                "/api/staff/5",
                "{\"password\":\"   \"}"
        );

        handler.handle(ex);

        assertEquals(400, ex.getSentStatusCode());
        assertTrue(ex.getResponseBodyAsString().contains("No fields to update."));
        assertJsonHeaders(ex);
    }

    @Test
    void shouldReturn400WhenDeleteMissingStaffId() throws IOException {
        FakeHttpExchange ex = new FakeHttpExchange(
                "DELETE",
                "/api/staff",
                ""
        );

        handler.handle(ex);

        assertEquals(400, ex.getSentStatusCode());
        assertTrue(ex.getResponseBodyAsString().contains("Staff ID required."));
        assertJsonHeaders(ex);
    }

    @Test
    void shouldReturn405ForUnsupportedMethod() throws IOException {
        FakeHttpExchange ex = new FakeHttpExchange(
                "PATCH",
                "/api/staff",
                ""
        );

        handler.handle(ex);

        assertEquals(405, ex.getSentStatusCode());
        assertTrue(ex.getResponseBodyAsString().contains("Method not allowed"));
        assertJsonHeaders(ex);
    }

    private void assertJsonHeaders(FakeHttpExchange ex) {
        assertEquals("application/json; charset=utf-8",
                ex.getResponseHeaders().getFirst("Content-Type"));
        assertEquals("*",
                ex.getResponseHeaders().getFirst("Access-Control-Allow-Origin"));
        assertEquals("GET,POST,PUT,DELETE,OPTIONS",
                ex.getResponseHeaders().getFirst("Access-Control-Allow-Methods"));
        assertEquals("Content-Type,Authorization",
                ex.getResponseHeaders().getFirst("Access-Control-Allow-Headers"));
    }

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

        int getSentStatusCode() {
            return sentStatusCode;
        }

        String getResponseBodyAsString() {
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
        }

        @Override
        public void setStreams(InputStream i, OutputStream o) {
        }

        @Override
        public HttpPrincipal getPrincipal() {
            return null;
        }
    }
}