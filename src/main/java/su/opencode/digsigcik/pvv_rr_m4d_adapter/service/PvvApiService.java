package su.opencode.digsigcik.pvv_rr_m4d_adapter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;

@Slf4j
@Service
public class PvvApiService {
    private final RestTemplate restTemplate;

    @Value("${pvv.get-message-content}")
    private URI PVV_GET_CONTENT_URI;

    public PvvApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ByteArrayResource getResource(String correlationId) throws IOException {

        ResponseEntity<ByteArrayResource> response = restTemplate.exchange(
                PVV_GET_CONTENT_URI.resolve(correlationId),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                ByteArrayResource.class
        );
        var body = response.getBody();
        return body;
    }
}
