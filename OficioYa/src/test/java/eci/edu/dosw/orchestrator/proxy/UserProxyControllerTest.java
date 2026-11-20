package eci.edu.dosw.orchestrator.proxy;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserProxyControllerTest {

    @Test
    void proxyDevuelveBadGatewaySiElServicioEstaCaido() {
        UserProxyController controller = new UserProxyController("http://localhost:59999");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.addHeader("Authorization", "Bearer test-token");
        request.addHeader("Host", "localhost:8081");

        ResponseEntity<byte[]> response = controller.proxy(request, null);

        assertEquals(502, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void proxyPreservaQueryYMetodo() {
        UserProxyController controller = new UserProxyController("http://localhost:59999/");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users");
        request.setQueryString("filter=active");
        request.addHeader("Authorization", "Bearer token-123");
        request.setContent("{\"test\": true}".getBytes());

        ResponseEntity<byte[]> response = controller.proxy(request, "{\"test\": true}".getBytes());

        assertEquals(502, response.getStatusCode().value());
    }

    @Test
    void proxyEnrutaImagenesYExtraeBodyDesdeInputStreamCuandoBodyEsNull() {
        UserProxyController controller = new UserProxyController("http://localhost:59998/", "http://localhost:59999/");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/images");
        request.setContentType("multipart/form-data; boundary=---abc");
        request.setContent("binary-image-data".getBytes());

        ResponseEntity<byte[]> response = controller.proxy(request, null);

        assertEquals(502, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void proxyEnrutaWorkersTradesYTradeCategoriesHaciaWorkersService() {
        UserProxyController controller = new UserProxyController(
                "http://localhost:59997/",
                "http://localhost:59998/",
                "http://localhost:59999/");

        for (String uri : new String[]{"/api/v1/workers/me", "/api/workers/trabajadores", "/api/v1/trades", "/api/v1/trade-categories/1"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
            request.addHeader("Authorization", "Bearer worker-token");
            ResponseEntity<byte[]> response = controller.proxy(request, null);
            assertEquals(502, response.getStatusCode().value());
        }
    }

    @Test
    void proxyEnrutaSearchReputationYReporting() {
        eci.edu.dosw.security.JwtService jwtService = new eci.edu.dosw.security.JwtService("ittFuD1AgrBmd01gZC5U1sO+Ube03p4RRhx6mFsSzbxvvgGIvSkHbCgL3vhrlmD4");
        UserProxyController controller = new UserProxyController(
                "http://localhost:59991",
                "http://localhost:59992",
                "http://localhost:59993",
                "http://localhost:59994",
                "http://localhost:59995",
                "http://localhost:59996",
                "http://localhost:59997",
                jwtService);

        for (String uri : new String[]{"/api/v1/search/workers", "/api/v1/reputation/reputaciones", "/api/v1/reporting/kpis"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
            request.addHeader("Authorization", "Bearer token-123");
            ResponseEntity<byte[]> response = controller.proxy(request, null);
            assertEquals(502, response.getStatusCode().value());
        }
    }

    @Test
    void proxyEnrutaLocalidadesHaciaUsersYRequestsHaciaServices() {
        UserProxyController controller = new UserProxyController(
                "http://localhost:59991",
                "http://localhost:59992",
                "http://localhost:59993",
                "http://localhost:59994",
                "http://localhost:59995",
                "http://localhost:59996",
                "http://localhost:59997",
                null);

        for (String uri : new String[]{"/api/v1/localities", "/api/v1/localities/123/workers"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
            ResponseEntity<byte[]> response = controller.proxy(request, null);
            assertEquals(502, response.getStatusCode().value());
        }

        for (String uri : new String[]{"/api/v1/requests/active", "/api/v1/services/workers/abc/stats", "/api/v1/notification/user/abc"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
            request.addHeader("Authorization", "Bearer token-123");
            ResponseEntity<byte[]> response = controller.proxy(request, null);
            assertEquals(502, response.getStatusCode().value());
        }
    }

    @Test
    void proxyInyectaTokenSistemaEnCambioDeEstadoYEventosReputacion() {
        eci.edu.dosw.security.JwtService jwtService = new eci.edu.dosw.security.JwtService("ittFuD1AgrBmd01gZC5U1sO+Ube03p4RRhx6mFsSzbxvvgGIvSkHbCgL3vhrlmD4");
        UserProxyController controller = new UserProxyController(
                "http://localhost:59991",
                "http://localhost:59992",
                "http://localhost:59993",
                "http://localhost:59994",
                "http://localhost:59995",
                "http://localhost:59996",
                "http://localhost:59997",
                jwtService);

        MockHttpServletRequest req1 = new MockHttpServletRequest("PUT", "/api/v1/workers/123e4567-e89b-12d3-a456-426614174000/status");
        req1.setContent("{\"status\": \"PAUSED\"}".getBytes());
        ResponseEntity<byte[]> resp1 = controller.proxy(req1, "{\"status\": \"PAUSED\"}".getBytes());
        assertEquals(502, resp1.getStatusCode().value());

        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/v1/reputation/eventos/servicios-cumplidos");
        req2.setContent("{\"servicioId\": \"abc\"}".getBytes());
        ResponseEntity<byte[]> resp2 = controller.proxy(req2, "{\"servicioId\": \"abc\"}".getBytes());
        assertEquals(502, resp2.getStatusCode().value());

        MockHttpServletRequest req3 = new MockHttpServletRequest("POST", "/api/v1/notification");
        req3.addHeader("Origin", "http://localhost:5174");
        req3.setContent("{\"type\": \"REVIEW_REPORTED\"}".getBytes());
        ResponseEntity<byte[]> resp3 = controller.proxy(req3, "{\"type\": \"REVIEW_REPORTED\"}".getBytes());
        assertEquals(502, resp3.getStatusCode().value());
    }
}
