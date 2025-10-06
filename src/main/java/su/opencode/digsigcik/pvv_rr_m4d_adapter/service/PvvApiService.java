package su.opencode.digsigcik.pvv_rr_m4d_adapter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.M4dResponseInfo;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.M4dRevokeResponseInfo;

import java.net.URI;

@Slf4j
@Service
public class PvvApiService {
    private final RestTemplate restTemplate;

    @Value("${pvv.get-message-content}")
    private URI PVV_GET_CONTENT_URI;
    @Value("${pvv.fns-send-response-registration-request-m4d}")
    private URI FNS_SEND_RESPONSE_REGISTRATION_REQUEST_M4D_URI;
    @Value("${pvv.fns-send-response-status-request-m4d}")
    private URI FNS_SEND_RESPONSE_STATUS_REQUEST_M4D_URI;
    @Value("${pvv.fns-send-response-revoke-request-m4d}")
    private URI FNS_SEND_RESPONSE_REVOKE_REQUEST_M4D_URI;

    public PvvApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ByteArrayResource getResource(String correlationId) {

        ResponseEntity<ByteArrayResource> response = restTemplate.exchange(
                PVV_GET_CONTENT_URI.resolve(correlationId),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                ByteArrayResource.class
        );
        return response.getBody();
    }

    public String sendResponseRegisterM4dFromRR(M4dResponseInfo response) {
        log.debug("Отправка сообщения REGISTRATION в ПВВ {}: {}", FNS_SEND_RESPONSE_REGISTRATION_REQUEST_M4D_URI, response);
        String res = sendJsonResponseM4dViaPvv(response, FNS_SEND_RESPONSE_REGISTRATION_REQUEST_M4D_URI);
        log.debug("Ответ от ПВВ: {}", res);

        return res;
    }

    public String sendResponseRevokeM4dFromRR(M4dRevokeResponseInfo response) {
        log.debug("Отправка сообщения REVOKE в ПВВ {}: {}", FNS_SEND_RESPONSE_REVOKE_REQUEST_M4D_URI, response);
        String res = sendJsonResponseM4dRevokeViaPvv(response, FNS_SEND_RESPONSE_REVOKE_REQUEST_M4D_URI);
        log.debug("Ответ от ПВВ: {}", res);

        return res;
    }

    public String sendResponseStatusM4dFromRR(MultipartFile file) {
        log.debug("Отправка сообщения STATUS в ПВВ {}: {}", FNS_SEND_RESPONSE_STATUS_REQUEST_M4D_URI, file.getOriginalFilename());
        String res = sendFileResponseM4dViaPvv(file, FNS_SEND_RESPONSE_STATUS_REQUEST_M4D_URI);
        log.debug("Ответ от ПВВ: {}", res);
        return res;
    }

    private String sendJsonResponseM4dViaPvv(M4dResponseInfo response, URI uri) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.setContentType(MediaType.APPLICATION_JSON); // если тело JSON

            HttpEntity<String> entity = new HttpEntity<>(convertM4dResponseInfoToString(response), headers);
            return restTemplate.postForEntity(
                            uri,
                            entity,
                            String.class
                    )
                    .getBody();
        } catch (Exception ex) {
            log.error("Ошибка при отправке json-ответа для межконтурного обновления: {} по адресу '{}'", ex.getMessage(),
                    uri);
        }
        return null;
    }

    private String sendJsonResponseM4dRevokeViaPvv(M4dRevokeResponseInfo response, URI uri) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.setContentType(MediaType.APPLICATION_JSON); // если тело JSON

            HttpEntity<String> entity = new HttpEntity<>(convertM4dRevokeResponseInfoToString(response), headers);
            return restTemplate.postForEntity(
                            uri,
                            entity,
                            String.class
                    )
                    .getBody();
        } catch (Exception ex) {
            log.error("Ошибка при отправке json-ответа для межконтурного обновления: {} по адресу '{}'", ex.getMessage(),
                    uri);
        }
        return null;
    }

    private String sendFileResponseM4dViaPvv(MultipartFile file, URI uri) {
        try {
            return restTemplate.postForEntity(
                            uri,
                            new HttpEntity<>(file.getBytes()),
                            String.class
                    )
                    .getBody();
        } catch (Exception ex) {
            log.error("Ошибка при отправке файла-ответа для межконтурного обновления: {} по адресу '{}'", ex.getMessage(),
                    uri);
        }
        return null;
    }

    public String convertM4dResponseInfoToString(M4dResponseInfo info) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(info);
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public String convertM4dRevokeResponseInfoToString(M4dRevokeResponseInfo info) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(info);
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
