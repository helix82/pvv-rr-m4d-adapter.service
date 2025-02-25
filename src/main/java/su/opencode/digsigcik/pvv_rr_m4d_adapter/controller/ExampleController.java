package su.opencode.digsigcik.pvv_rr_m4d_adapter.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.ResponseInfo;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.service.ExampleService;

import static su.opencode.digsigcik.pvv_rr_m4d_adapter.util.Constants.*;

@Slf4j
@su.opencode.digsigcik.pvv_rr_m4d_adapter.annotation.PvvRrM4dExceptionHandler
@RestController
@RequestMapping("/api/v1/pvv-rr-m4d-adapter")
@CrossOrigin(origins = "*")
@Tag(name = "pvv-rr-m4d-adapter Controller")
public class ExampleController {

    private final ExampleService exampleService;

    public ExampleController(ExampleService exampleService) {
        this.exampleService = exampleService;
    }
    @Operation(summary = CONTROLLER_UPLOAD_ZIP_AND_GET_STATUS_OF_SMTH,
            description = CONTROLLER_UPLOAD_ZIP_AND_GET_STATUS_OF_SMTH)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = API_RESPONSE_CODE_200_MESSAGE),
            @ApiResponse(responseCode = "404", description = API_RESPONSE_CODE_404_MESSAGE),
            @ApiResponse(responseCode = "400", description = API_RESPONSE_CODE_400_MESSAGE)
    })
    @PostMapping(value ="/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseInfo> uploadCertificateZip(@RequestParam("file") MultipartFile zipFile) {
        ResponseInfo response = exampleService.processCertificateZip(zipFile);
        return ResponseEntity.ok(response);
    }
    @Operation(summary = CONTROLLER_GET_SMTH_BY_STATUS,
            description = CONTROLLER_GET_SMTH_BY_STATUS)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = API_RESPONSE_CODE_200_MESSAGE),
            @ApiResponse(responseCode = "404", description = API_RESPONSE_CODE_404_MESSAGE),
            @ApiResponse(responseCode = "400", description = API_RESPONSE_CODE_400_MESSAGE)
    })
    @GetMapping("/receive/{requestId}")
    public ResponseEntity<ResponseInfo> getCertificateRequestStatus(@PathVariable String requestId) {
        ResponseInfo response = exampleService.getStatus(requestId);
        return ResponseEntity.ok(response);
    }
}
