package eci.edu.dosw.notification;

import eci.edu.dosw.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpNotificationServiceTest {

    private JwtService jwtService;
    private MockRestServiceServer mockServer;
    private HttpNotificationService notificationService;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        when(jwtService.generateServiceToken()).thenReturn("service-token-123");

        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8088");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        notificationService = new HttpNotificationService(builder.build(), jwtService);
    }

    @Test
    void constructorPorUrlCreaElClienteCorrectamente() {
        HttpNotificationService svc = new HttpNotificationService("http://localhost:8088", jwtService);
        assertNotNull(svc);
    }

    @Test
    void sendPasswordResetCodeEnviaPostAlEndpointDeEmail() {
        mockServer.expect(requestTo("http://localhost:8088/api/v1/notification/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer service-token-123"))
                .andExpect(jsonPath("$.to").value("user@oficioya.com"))
                .andExpect(jsonPath("$.type").value("RESET_CODE"))
                .andRespond(withSuccess());

        assertDoesNotThrow(() -> notificationService.sendPasswordResetCode("user@oficioya.com", "654321", 15));
        mockServer.verify();
    }

    @Test
    void sendPasswordChangedNoticeEnviaPostAlEndpointDeEmail() {
        mockServer.expect(requestTo("http://localhost:8088/api/v1/notification/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer service-token-123"))
                .andExpect(jsonPath("$.to").value("user@oficioya.com"))
                .andExpect(jsonPath("$.type").value("PASSWORD_CHANGED"))
                .andRespond(withSuccess());

        assertDoesNotThrow(() -> notificationService.sendPasswordChangedNotice("user@oficioya.com"));
        mockServer.verify();
    }

    @Test
    void sendEmailNoLanzaExcepcionSiNotificationFallaCon500OSinJwtService() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8088");
        MockRestServiceServer serverSinJwt = MockRestServiceServer.bindTo(builder).build();
        HttpNotificationService svcSinJwt = new HttpNotificationService(builder.build(), null);

        serverSinJwt.expect(requestTo("http://localhost:8088/api/v1/notification/email"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertDoesNotThrow(() -> svcSinJwt.sendPasswordChangedNotice("user@oficioya.com"));
        serverSinJwt.verify();
    }
}
