package su.opencode.digsigcik.pvv_rr_m4d_adapter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.annotation.PvvRrM4dExceptionHandler;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.M4dFnsDataResponse;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.service.PvvApiService;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.service.RealApiService;

import static su.opencode.digsigcik.pvv_rr_m4d_adapter.util.Constants.*;

@Slf4j
@PvvRrM4dExceptionHandler
@RestController
@RequestMapping("/api/v1/pvv-rr-m4d-adapter")
@CrossOrigin(origins = "*")
@Tag(name = "pvv-rr-m4d-adapter Controller")
public class M4dRequestController {

    private final RealApiService realApiService;
    private final PvvApiService pvvApiService;

    public M4dRequestController(RealApiService realApiService, PvvApiService pvvApiService) {
        this.realApiService = realApiService;
        this.pvvApiService = pvvApiService;
    }

    @Operation(summary = CONTROLLER_UPLOAD_ZIP_AND_GET_STATUS,
            description = CONTROLLER_UPLOAD_ZIP_AND_GET_STATUS)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = API_RESPONSE_CODE_200_MESSAGE),
            @ApiResponse(responseCode = "404", description = API_RESPONSE_CODE_404_MESSAGE),
            @ApiResponse(responseCode = "400", description = API_RESPONSE_CODE_400_MESSAGE)
    })
    @PostMapping(value ="/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadM4dZip(@RequestParam("file") MultipartFile zipFile) {
        try {
            return ResponseEntity.ok(realApiService.processM4dZip(zipFile));
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = CONTROLLER_GET_M4D_BY_STATUS,
            description = CONTROLLER_GET_M4D_BY_STATUS)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = API_RESPONSE_CODE_200_MESSAGE),
            @ApiResponse(responseCode = "404", description = API_RESPONSE_CODE_404_MESSAGE),
            @ApiResponse(responseCode = "400", description = API_RESPONSE_CODE_400_MESSAGE)
    })
    @GetMapping("/receive/{guidXml}")
    public ResponseEntity<?> getM4dRequestStatus(@PathVariable String guidXml) {
        try {
            M4dFnsDataResponse response = realApiService.getM4dRequestStatus(guidXml);
            if (response == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("МЧД не найдена");
            }
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = CONTROLLER_UPLOAD_ZIP_AND_GET_STATUS_REVOKE,
            description = CONTROLLER_UPLOAD_ZIP_AND_GET_STATUS_REVOKE)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = API_RESPONSE_CODE_200_MESSAGE),
            @ApiResponse(responseCode = "404", description = API_RESPONSE_CODE_404_MESSAGE),
            @ApiResponse(responseCode = "400", description = API_RESPONSE_CODE_400_MESSAGE)
    })
    @PostMapping(value ="/revoke/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadM4dRevokeZip(@RequestParam("file") MultipartFile zipFile) {
        try {
            return ResponseEntity.ok(realApiService.processM4dRevokeZip(zipFile));
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

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
