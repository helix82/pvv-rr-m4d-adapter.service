package su.opencode.digsigcik.pvv_rr_m4d_adapter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.service.PvvApiService;

import static su.opencode.digsigcik.pvv_rr_m4d_adapter.util.Constants.*;


@Slf4j
@RestController
@RequestMapping("/api/v1/from-zk")
@CrossOrigin(origins = "*")
@Tag(name = "Получение Данных из ЗК")
@AllArgsConstructor
class PvvApiController {
    private final PvvApiService pvvApiService;

    @Operation(summary = "Correlation id content")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = API_RESPONSE_CODE_200_MESSAGE),
            @ApiResponse(responseCode = "404", description = API_RESPONSE_CODE_404_MESSAGE),
            @ApiResponse(responseCode = "400", description = API_RESPONSE_CODE_400_MESSAGE)
    })
    @PostMapping(value ="/content/{correlationId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getContentFromPvvByCorrelationId(@PathVariable String correlationId) {
        try {

            var res = pvvApiService.getResource(correlationId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", res.getFilename()))
                    .body(res);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}