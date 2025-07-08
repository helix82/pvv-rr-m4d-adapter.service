package su.opencode.digsigcik.pvv_rr_m4d_adapter.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.*;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.service.MockApiService;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.service.PvvApiService;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.service.RealApiService;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.util.ConvertUtils;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.util.CustomMultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

import static su.opencode.digsigcik.pvv_rr_m4d_adapter.util.Constants.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/pvv-rr-m4d-adapter")
@CrossOrigin(origins = "*")
@Tag(name = "pvv-rr-m4d-adapter Controller")
public class M4dRequestController {

    private final RealApiService realApiService;
    private final MockApiService mockApiService;
    private final PvvApiService pvvApiService;

    public M4dRequestController(RealApiService realApiService, MockApiService mockApiService, PvvApiService pvvApiService) {
        this.realApiService = realApiService;
        this.mockApiService = mockApiService;
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
        String fileName = zipFile.getOriginalFilename();
        if (fileName != null && !fileName.toLowerCase().endsWith(".zip")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(NO_ZIP);
        }
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
    @GetMapping(value = "/receive/{guidXml}", produces = "application/zip")
    public ResponseEntity<?> getM4dRequestStatus(@PathVariable String guidXml, HttpServletResponse response) {
        List<String> files = List.of();
        try {
            M4dFnsDataResponse report = realApiService.getM4dRequestStatus(guidXml);
            if (report.getError() == null) {
                files = mockApiService.createAndSignedM4dConfirm(report); //файлы подтверждения регистрации мчд в рр мчд
                if (Objects.equals(report.getStatus(), "REVOKED")) {
                    List<String> filesRevoke = mockApiService.createAndSignedM4dRevokeConfirm(report); //файлы подтверждения регистрации заявления на отзыв мчд в рр мчд
                    files.addAll(filesRevoke);
                }
            }
            ByteArrayResource resource = realApiService.createArchiveWithM4dRequestStatus(guidXml, report, files);
            try {
                response.setHeader("Content-Disposition", "attachment; filename=\"" + guidXml + ".zip\"");

                return new ResponseEntity<>(
                        resource,
                        HttpStatus.OK
                );
            } catch (Exception e) {
                return ResponseEntity.noContent().build();
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = CONTROLLER_GET_FULL_M4D_BY_GUID_AND_INNS,
            description = CONTROLLER_GET_FULL_M4D_BY_GUID_AND_INNS)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = API_RESPONSE_CODE_200_MESSAGE),
            @ApiResponse(responseCode = "404", description = API_RESPONSE_CODE_404_MESSAGE),
            @ApiResponse(responseCode = "400", description = API_RESPONSE_CODE_400_MESSAGE)
    })
    @PostMapping(value = "/receive", produces = "application/zip")
    public ResponseEntity<?> getFullM4dRequest(@RequestBody String json, HttpServletResponse response) throws JsonProcessingException {
        CheckByGuidRequestDto requestDto = ConvertUtils.convertStringToClass(json, CheckByGuidRequestDto.class);
        String guidXml = requestDto.getGuid();
        List<String> files = new ArrayList<>();

        try {
            M4dFullFnsDataResponse report = realApiService.getFullM4dRequest(requestDto);
                ByteArrayResource file = report.getFile();
                if (file != null && file.contentLength() > 0) {
                    String tempFileResource = "/tmp/resource_" + file.getFilename();
                    try (FileOutputStream fos = new FileOutputStream(tempFileResource)) {
                        fos.write(file.getByteArray());
                    }
                    files.add(tempFileResource);
                    report.setFile(null);
                }
            File resource = realApiService.createArchiveWithM4dRequestStatusToFile(guidXml, report, files);
            MultipartFile zipFile = new CustomMultipartFile(resource);
            //String res = pvvApiService.sendResponseStatusM4dFromRR(zipFile);
            //return getResponseEntity(res, SENDING_RESPONSE_GET_FULL_M4D_FROM_RR);

            try {
                response.setHeader("Content-Disposition", "attachment; filename=\"" + guidXml + ".zip\"");

                return new ResponseEntity<>(
                        zipFile.getBytes(),
                        HttpStatus.OK
                );
            } catch (Exception e) {
                return ResponseEntity.noContent().build();
            }
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
        String fileName = zipFile.getOriginalFilename();
        if (fileName != null && !fileName.toLowerCase().endsWith(".zip")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(NO_ZIP);
        }
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

    @Operation(summary = CONTROLLER_SEND_M4D_TO_RR,
            description = CONTROLLER_SEND_M4D_TO_RR)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = API_RESPONSE_CODE_200_MESSAGE),
            @ApiResponse(responseCode = "404", description = API_RESPONSE_CODE_404_MESSAGE),
            @ApiResponse(responseCode = "400", description = API_RESPONSE_CODE_400_MESSAGE)
    })
    @PostMapping(value ="/send/{messageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sendM4dToRR(@PathVariable String messageId) {
        MultipartFile zipFile;
        File tempZipFile;
        try {
            ByteArrayResource res = pvvApiService.receiveResponseM4dToRR(messageId);
            tempZipFile = ConvertUtils.byteArrayResourceToTempFile(res, messageId, ".zip");
            zipFile = new CustomMultipartFile(tempZipFile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        if (!realApiService.isZipFile(tempZipFile)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(NO_ZIP);
        }
        try {
            M4dResponseInfo response = realApiService.processM4dZip(zipFile);
            String res = pvvApiService.sendResponseRegisterM4dFromRR(response);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Request-ID", res);
            Map<String, String> body = new HashMap<>();
            body.put("result", SENDING_RESPONSE_M4D_TO_RR);
            return new ResponseEntity<>(body, headers, HttpStatus.OK);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = CONTROLLER_SEND_M4D_REVOKE_TO_RR,
            description = CONTROLLER_SEND_M4D_REVOKE_TO_RR)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = API_RESPONSE_CODE_200_MESSAGE),
            @ApiResponse(responseCode = "404", description = API_RESPONSE_CODE_404_MESSAGE),
            @ApiResponse(responseCode = "400", description = API_RESPONSE_CODE_400_MESSAGE)
    })
    @PostMapping(value ="/revoke/send/{messageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sendM4dRevokeToRR(@PathVariable String messageId) {
        MultipartFile zipFile;
        File tempZipFile;
        try {
            ByteArrayResource res = pvvApiService.receiveResponseM4dToRR(messageId);
            tempZipFile = ConvertUtils.byteArrayResourceToTempFile(res, messageId, ".zip");
            zipFile = new CustomMultipartFile(tempZipFile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        if (!realApiService.isZipFile(tempZipFile)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(NO_ZIP);
        }
        try {
            M4dRevokeResponseInfo response = realApiService.processM4dRevokeZip(zipFile);
            String res = pvvApiService.sendResponseRevokeM4dFromRR(response);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Request-ID", res);
            Map<String, String> body = new HashMap<>();
            body.put("result", SENDING_RESPONSE_M4D_REVOKE_TO_RR);
            return new ResponseEntity<>(body, headers, HttpStatus.OK);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = CONTROLLER_GET_M4D_STATUS_FROM_RR,
            description = CONTROLLER_GET_M4D_STATUS_FROM_RR)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = API_RESPONSE_CODE_200_MESSAGE),
            @ApiResponse(responseCode = "404", description = API_RESPONSE_CODE_404_MESSAGE),
            @ApiResponse(responseCode = "400", description = API_RESPONSE_CODE_400_MESSAGE)
    })
    @GetMapping(value = "/receive/{messageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getM4dRequestStatusFromRR(@PathVariable String messageId, HttpServletResponse response) {
        M4dStatusRequestDto m4dStatusRequestDto;
        try {
            String res = pvvApiService.receiveJsonResponseM4dToRR(messageId);
            m4dStatusRequestDto = ConvertUtils.convertStringToClass(res, M4dStatusRequestDto.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        List<String> files = List.of();
        try {
            M4dFnsDataResponse report = realApiService.getM4dRequestStatus(m4dStatusRequestDto.getGuid());
            if (report.getError() == null) {
                files = mockApiService.createAndSignedM4dConfirm(report); //файлы подтверждения регистрации мчд в рр мчд
                if (Objects.equals(report.getStatus(), "REVOKED")) {
                    List<String> filesRevoke = mockApiService.createAndSignedM4dRevokeConfirm(report); //файлы подтверждения регистрации заявления на отзыв мчд в рр мчд
                    files.addAll(filesRevoke);
                }
            }
            File resource = realApiService.createArchiveWithM4dRequestStatusToFile(m4dStatusRequestDto.getGuid(), report, files);
            MultipartFile zipFile = new CustomMultipartFile(resource);
            String res = pvvApiService.sendResponseStatusM4dFromRR(zipFile);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Request-ID", res);
            Map<String, String> body = new HashMap<>();
            body.put("result", SENDING_RESPONSE_GET_M4D_STATUS_FROM_RR);
            return new ResponseEntity<>(body, headers, HttpStatus.OK);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    private ResponseEntity<?> getResponseEntity(String res, String responseString) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-ID", res);
        Map<String, String> body = new HashMap<>();
        body.put("result", responseString);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

}
