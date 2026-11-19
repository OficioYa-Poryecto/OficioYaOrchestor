package eci.edu.dosw.orchestrator.proxy;

import eci.edu.dosw.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Controlador Proxy / Gateway en el Orquestador.
 * Reenvía las peticiones hacia:
 * - /api/v1/users/**, /api/v1/admin/**, /api/v1/zones/**, /api/v1/localities/** -> microservicio de Usuarios (${users.service.url})
 * - /api/v1/images/** -> microservicio de Imágenes (${images.service.url})
 * - /api/v1/workers/**, /api/workers/**, /api/v1/trades/**, /api/v1/trade-categories/** -> microservicio de Workers (${workers.service.url})
 * - /api/v1/search/** -> microservicio de Search (${search.service.url})
 * - /api/v1/reputation/** -> microservicio de Reputation (${reputation.service.url})
 * - /api/v1/reporting/** -> microservicio de Reporting (${reporting.service.url})
 * - /api/v1/requests/**, /api/v1/services/** -> microservicio de Servicios (${services.service.url})
 * - /api/v1/notification/**, /api/v1/notifications/** -> microservicio de Notificaciones (${notification.service.url})
 * preservando método HTTP, ruta, query params, payload binario/multipart y cabeceras.
 */
@RestController
public class UserProxyController {

    private static final Logger log = LoggerFactory.getLogger(UserProxyController.class);

    private final String usersServiceUrl;
    private final String imagesServiceUrl;
    private final String workersServiceUrl;
    private final String searchServiceUrl;
    private final String reputationServiceUrl;
    private final String reportingServiceUrl;
    private final String servicesServiceUrl;
    private final String notificationServiceUrl;
    private final JwtService jwtService;
    private final RestClient restClient;

    public UserProxyController(String usersServiceUrl) {
        this(usersServiceUrl, "http://localhost:8083", "http://localhost:8082");
    }

    public UserProxyController(String usersServiceUrl, String imagesServiceUrl) {
        this(usersServiceUrl, imagesServiceUrl, "http://localhost:8082");
    }

    public UserProxyController(String usersServiceUrl, String imagesServiceUrl, String workersServiceUrl) {
        this(usersServiceUrl, imagesServiceUrl, workersServiceUrl,
                "http://localhost:8084", "http://localhost:8085", "http://localhost:8086", "http://localhost:8087", "http://localhost:8088", null);
    }

    public UserProxyController(String usersServiceUrl, String imagesServiceUrl, String workersServiceUrl,
            String searchServiceUrl, String reputationServiceUrl, String reportingServiceUrl, JwtService jwtService) {
        this(usersServiceUrl, imagesServiceUrl, workersServiceUrl,
                searchServiceUrl, reputationServiceUrl, reportingServiceUrl, "http://localhost:8087", "http://localhost:8088", jwtService);
    }

    public UserProxyController(String usersServiceUrl, String imagesServiceUrl, String workersServiceUrl,
            String searchServiceUrl, String reputationServiceUrl, String reportingServiceUrl,
            String servicesServiceUrl, JwtService jwtService) {
        this(usersServiceUrl, imagesServiceUrl, workersServiceUrl,
                searchServiceUrl, reputationServiceUrl, reportingServiceUrl, servicesServiceUrl, "http://localhost:8088", jwtService);
    }

    @Autowired
    public UserProxyController(
            @Value("${users.service.url:http://localhost:8080}") String usersServiceUrl,
            @Value("${images.service.url:http://localhost:8083}") String imagesServiceUrl,
            @Value("${workers.service.url:https://oficioya-worker-api.azurewebsites.net}") String workersServiceUrl,
            @Value("${search.service.url:https://oficioya-search-atgjh5gdfcb5b8h3.mexicocentral-01.azurewebsites.net}") String searchServiceUrl,
            @Value("${reputation.service.url:https://oficioya-reputation-bggabfhkdvaqh4bw.mexicocentral-01.azurewebsites.net}") String reputationServiceUrl,
            @Value("${reporting.service.url:https://oficioya-reporting-hvehegasb3hrarc9.mexicocentral-01.azurewebsites.net}") String reportingServiceUrl,
            @Value("${services.service.url:https://oficioya-serviceapi-dye8exf8g6eudfbe.chilecentral-01.azurewebsites.net}") String servicesServiceUrl,
            @Value("${notification.service.url:https://oficioya-notificationapi-apffc2eygzgcbghp.chilecentral-01.azurewebsites.net}") String notificationServiceUrl,
            @Autowired(required = false) JwtService jwtService) {
        this.usersServiceUrl = usersServiceUrl.replaceAll("/+$", "");
        this.imagesServiceUrl = imagesServiceUrl.replaceAll("/+$", "");
        this.workersServiceUrl = workersServiceUrl.replaceAll("/+$", "");
        this.searchServiceUrl = searchServiceUrl.replaceAll("/+$", "");
        this.reputationServiceUrl = reputationServiceUrl.replaceAll("/+$", "");
        this.reportingServiceUrl = reportingServiceUrl.replaceAll("/+$", "");
        this.servicesServiceUrl = servicesServiceUrl.replaceAll("/+$", "");
        this.notificationServiceUrl = notificationServiceUrl.replaceAll("/+$", "");
        this.jwtService = jwtService;
        this.restClient = RestClient.builder().build();
    }

    @RequestMapping(value = {
            "/api/v1/users/**",
            "/api/v1/admin/**",
            "/api/v1/zones/**",
            "/api/v1/localities/**",
            "/api/v1/images/**",
            "/api/v1/workers/**",
            "/api/workers/**",
            "/api/v1/trades/**",
            "/api/v1/trade-categories/**",
            "/api/v1/search/**",
            "/api/v1/reputation/**",
            "/api/v1/reporting/**",
            "/api/v1/requests/**",
            "/api/v1/services/**",
            "/api/v1/notification/**",
            "/api/v1/notifications/**"
    })
    public ResponseEntity<byte[]> proxy(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        String path = request.getRequestURI();
        String query = request.getQueryString();
        String baseUrl = resolveBaseUrl(path);
        String targetUrl = baseUrl + path + (query != null ? "?" + query : "");
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        log.info("Proxy gateway reenvía: {} {} -> {}", method, path, targetUrl);

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (name.equalsIgnoreCase(HttpHeaders.HOST) || name.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(name);
            headers.addAll(name, Collections.list(values));
        }

        // Permisos entre servicios: inyectar token con rol SISTEMA/SERVICE si es cambio de estado de trabajador
        // o si es un evento enviado a Reputation (Service -> Reputation sin usuario detrás)
        boolean requireSystemToken = (method == HttpMethod.PUT && path.matches("^/api/v1/workers/[^/]+/status.*$"))
                || path.startsWith("/api/v1/reputation/eventos");

        if (requireSystemToken && jwtService != null) {
            String systemToken = jwtService.generateServiceToken();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + systemToken);
            log.info("Proxy gateway inyecta token SISTEMA para {}", path);
        }

        try {
            byte[] payload = body;
            if ((payload == null || payload.length == 0) && request.getContentLengthLong() > 0) {
                payload = request.getInputStream().readAllBytes();
            }

            var spec = restClient.method(method)
                    .uri(URI.create(targetUrl))
                    .headers(h -> h.addAll(headers));

            if (payload != null && payload.length > 0) {
                spec.body(payload);
            }

            return spec.exchange((clientRequest, clientResponse) -> {
                HttpHeaders responseHeaders = new HttpHeaders();
                clientResponse.getHeaders().forEach((k, v) -> {
                    if (!k.equalsIgnoreCase(HttpHeaders.TRANSFER_ENCODING)) {
                        responseHeaders.addAll(k, v);
                    }
                });
                byte[] responseBody = clientResponse.getBody().readAllBytes();
                return ResponseEntity.status(clientResponse.getStatusCode())
                        .headers(responseHeaders)
                        .body(responseBody);
            });
        } catch (HttpStatusCodeException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .headers(ex.getResponseHeaders())
                    .body(ex.getResponseBodyAsByteArray());
        } catch (Exception ex) {
            log.error("Error en proxy gateway al contactar {}: {}", targetUrl, ex.getMessage());
            return ResponseEntity.status(502).body(("Bad Gateway: " + ex.getMessage()).getBytes());
        }
    }

    private String resolveBaseUrl(String path) {
        if (path.startsWith("/api/v1/images")) {
            return imagesServiceUrl;
        }
        if (path.startsWith("/api/v1/workers")
                || path.startsWith("/api/workers")
                || path.startsWith("/api/v1/trades")
                || path.startsWith("/api/v1/trade-categories")) {
            return workersServiceUrl;
        }
        if (path.startsWith("/api/v1/search")) {
            return searchServiceUrl;
        }
        if (path.startsWith("/api/v1/reputation")) {
            return reputationServiceUrl;
        }
        if (path.startsWith("/api/v1/reporting")) {
            return reportingServiceUrl;
        }
        if (path.startsWith("/api/v1/requests") || path.startsWith("/api/v1/services")) {
            return servicesServiceUrl;
        }
        if (path.startsWith("/api/v1/notification")) {
            return notificationServiceUrl;
        }
        return usersServiceUrl;
    }
}
