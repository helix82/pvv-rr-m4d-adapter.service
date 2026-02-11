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
import su.opencode.digsigcik.pvva.PvvResponse;
import su.opencode.digsigcik.pvva.enums.PvvResponseType;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    @Operation(summary = CONTROLLER_UPLOAD_ZIP_AND_GET_STATUS_AUTO,
            description = CONTROLLER_UPLOAD_ZIP_AND_GET_STATUS_AUTO //, hidden = true
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = API_RESPONSE_CODE_200_MESSAGE),
            @ApiResponse(responseCode = "404", description = API_RESPONSE_CODE_404_MESSAGE),
            @ApiResponse(responseCode = "400", description = API_RESPONSE_CODE_400_MESSAGE)
    })
    @PostMapping(value ="/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadM4dZip(@RequestParam("file") MultipartFile zipFile) throws IOException {

        log.debug("ПВВ: запрос на регистрацию");

        String fileName = zipFile.getOriginalFilename();
        if (fileName != null && realApiService.isNotZipFile(ConvertUtils.convertMultipartFileToFile(zipFile))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(NO_ZIP);
        }
        try {
            String res;
            M4dResponseInfo response = realApiService.processM4dZip(zipFile);
            res = pvvApiService.sendResponseRegisterM4dFromRR(response, response.getPoaNumber());

            HttpHeaders headers = new HttpHeaders();
            if (res != null) {
                headers.set("X-Request-ID", res);
            }
            return new ResponseEntity<>(PvvResponse.builder()
                    .responseType(PvvResponseType.OK_SUBSCRIPTION_REQUIRED)
                    .subscriptionData(Map.of(
                            "method", "GET",
                            "uri", String.format("/api/v1/receive/%s", response.getPoaNumber()),
                            "successNamespace", "M4D_REGISTRATION_REQUEST_EXCHANGE_NAMESPACE_JSON"
                    ))
                    .jsonPayload(Map.of(
                            "guid", response.getPoaNumber()
                    ))
                    .build(), headers, HttpStatus.OK);
//            return ResponseEntity.ok(PvvResponse.builder()
//                    .responseType(PvvResponseType.OK_SUBSCRIPTION_REQUIRED)
//                    .subscriptionData(Map.of(
//                            "method", "GET",
//                            "uri", String.format("/api/v1/receive/%s", response.getPoaNumber()),
//                            "successNamespace", "UCFK_ORDER_FK_REQUEST_EXCHANGE_NAMESPACE_JSON"
//                    ))
//                    .saveload(Map.of(
//                            "guid", response.getPoaNumber()
//                    ))
//                    .build());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = CONTROLLER_UPLOAD_ZIP_AND_GET_STATUS,
            description = CONTROLLER_UPLOAD_ZIP_AND_GET_STATUS)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = API_RESPONSE_CODE_200_MESSAGE),
            @ApiResponse(responseCode = "404", description = API_RESPONSE_CODE_404_MESSAGE),
            @ApiResponse(responseCode = "400", description = API_RESPONSE_CODE_400_MESSAGE)
    })
    @PostMapping(value ="/send/manual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadM4dZipManual(@RequestParam("file") MultipartFile zipFile) throws IOException {
        log.debug("ПВВ: запрос на регистрацию");

        String fileName = zipFile.getOriginalFilename();
        if (fileName != null && realApiService.isNotZipFile(ConvertUtils.convertMultipartFileToFile(zipFile))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(NO_ZIP);
        }
        try {
            String res;
            M4dResponseInfo response = realApiService.processM4dZip(zipFile);
            res = pvvApiService.sendResponseRegisterM4dFromRR(response, response.getPoaNumber());

            HttpHeaders headers = new HttpHeaders();
            if (res != null) {
                headers.set("X-Request-ID", res);
            }
            return new ResponseEntity<>(response, headers, HttpStatus.OK);
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
    @PostMapping(value = "/receive", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> getM4dRequestStatus(@RequestBody String json, HttpServletResponse response) throws JsonProcessingException {

        log.debug("ПВВ: статус");

        CheckByGuidRequestDto requestDto = ConvertUtils.convertStringToClass(json, CheckByGuidRequestDto.class);
        String guidXml = requestDto.getGuid();
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
            File resource = realApiService.createArchiveWithM4dRequestStatusToFile(guidXml, report, files);
            MultipartFile zipFile = new CustomMultipartFile(resource);
            String res = pvvApiService.sendResponseStatusM4dFromRR(zipFile, guidXml);
            HttpHeaders headers = new HttpHeaders();
            if (res != null) {
                headers.set("X-Request-ID", res);
            }
            try {
                response.setHeader("Content-Disposition", "attachment; filename=\"" + guidXml + ".zip\"");

                return new ResponseEntity<>(
                        zipFile.getBytes(),
                        headers,
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

    @Operation(summary = CONTROLLER_GET_M4D_BY_STATUS_AUTO,
            description = CONTROLLER_GET_M4D_BY_STATUS_AUTO //, hidden = true
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = API_RESPONSE_CODE_200_MESSAGE),
            @ApiResponse(responseCode = "404", description = API_RESPONSE_CODE_404_MESSAGE),
            @ApiResponse(responseCode = "400", description = API_RESPONSE_CODE_400_MESSAGE)
    })
    @GetMapping(value = "/receive/{guidXml}", produces = "application/zip")
    public ResponseEntity<?> getM4dRequestStatusByGuid(@PathVariable String guidXml) {
        List<String> files;
        try {
            M4dFnsDataResponse report = realApiService.getM4dRequestStatus(guidXml);
            if (report.getError() == null) {
                files = mockApiService.createAndSignedM4dConfirm(report); //файлы подтверждения регистрации мчд в рр мчд
                if (Objects.equals(report.getStatus(), "REVOKED")) {
                    List<String> filesRevoke = mockApiService.createAndSignedM4dRevokeConfirm(report); //файлы подтверждения регистрации заявления на отзыв мчд в рр мчд
                    files.addAll(filesRevoke);
                }
                File resource = realApiService.createArchiveWithM4dRequestStatusToFile(guidXml, report, files);
                MultipartFile zipFile = new CustomMultipartFile(resource);
                return PvvResponse.multipartOkResponse(zipFile.getBytes());
            } else {
                return ResponseEntity.ok(PvvResponse.builder()
                        .responseType(PvvResponseType.IDLE_CONTINUE)
                        .build());
            }
        } catch (Exception e) {
            return ResponseEntity.ok(PvvResponse.builder()
                    .responseType(PvvResponseType.FAILED)
                    .errorDetails(e.getMessage())
                    .build());
        }
        //return ResponseEntity.ok(realApiService.getM4dRequestStatusByGuid(guidXml));
    }

    @Operation(summary = CONTROLLER_GET_M4D_BY_STATUS,
            description = CONTROLLER_GET_M4D_BY_STATUS)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = API_RESPONSE_CODE_200_MESSAGE),
            @ApiResponse(responseCode = "404", description = API_RESPONSE_CODE_404_MESSAGE),
            @ApiResponse(responseCode = "400", description = API_RESPONSE_CODE_400_MESSAGE)
    })
    @GetMapping(value = "/receive/manual/{guidXml}", produces = "application/zip")
    public ResponseEntity<?> getM4dRequestStatusByGuidManual(@PathVariable String guidXml, HttpServletResponse response) {
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
            File resource = realApiService.createArchiveWithM4dRequestStatusToFile(guidXml, report, files);
            MultipartFile zipFile = new CustomMultipartFile(resource);
            String res = pvvApiService.sendResponseStatusM4dFromRR(zipFile, guidXml);
            HttpHeaders headers = new HttpHeaders();
            if (res != null) {
                headers.set("X-Request-ID", res);
            }
            try {
                response.setHeader("Content-Disposition", "attachment; filename=\"" + guidXml + ".zip\"");

                return new ResponseEntity<>(
                        zipFile.getBytes(),
                        headers,
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
    @PostMapping(value = "/receive-full", produces = "application/zip")
    public ResponseEntity<?> getFullM4dRequest(@RequestBody String json, HttpServletResponse response) throws JsonProcessingException {
        CheckByGuidRequestDto requestDto = ConvertUtils.convertStringToClass(json, CheckByGuidRequestDto.class);
        String guidXml = requestDto.getGuid();
        List<String> files = new ArrayList<>();

        try {
            M4dFnsDataResponse report = realApiService.getM4dRequestStatus(guidXml);
            M4dFnsDataResponse reportData = realApiService.getFullM4dRequest(requestDto);
            report.setPrincipalInn(reportData.getPrincipalInn());
            report.setRepresentativeInn(reportData.getRepresentativeInn());
            report.setReqStatus(reportData.getReqStatus());
                ByteArrayResource file = reportData.getFile();
                if (file != null && file.contentLength() > 0) {
                    String tempFileResource = "/tmp/resource_" + file.getFilename();
                    try (FileOutputStream fos = new FileOutputStream(tempFileResource)) {
                        fos.write(file.getByteArray());
                    }
                    files.add(tempFileResource);
                }
            File resource = realApiService.createArchiveWithM4dRequestStatusToFile(guidXml, report, files);
            MultipartFile zipFile = new CustomMultipartFile(resource);
            String res = pvvApiService.sendResponseStatusM4dFromRR(zipFile, guidXml);
            HttpHeaders headers = new HttpHeaders();
            if (res != null) {
                headers.set("X-Request-ID", res);
            }
            try {
                response.setHeader("Content-Disposition", "attachment; filename=\"" + guidXml + ".zip\"");

                return new ResponseEntity<>(
                        zipFile.getBytes(),
                        headers,
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

    @Operation(summary = CONTROLLER_UPLOAD_ZIP_AND_GET_STATUS_REVOKE_AUTO,
            description = CONTROLLER_UPLOAD_ZIP_AND_GET_STATUS_REVOKE_AUTO //, hidden = true
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = API_RESPONSE_CODE_200_MESSAGE),
            @ApiResponse(responseCode = "404", description = API_RESPONSE_CODE_404_MESSAGE),
            @ApiResponse(responseCode = "400", description = API_RESPONSE_CODE_400_MESSAGE)
    })
    @PostMapping(value ="/revoke/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadM4dRevokeZip(@RequestParam("file") MultipartFile zipFile) throws IOException {

        log.debug("ПВВ: отзыв");

        String fileName = zipFile.getOriginalFilename();
        if (fileName != null && realApiService.isNotZipFile(ConvertUtils.convertMultipartFileToFile(zipFile))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(NO_ZIP);
        }
        try {
            String res;
            M4dRevokeResponseInfo response = realApiService.processM4dRevokeZip(zipFile);
            res = pvvApiService.sendResponseRevokeM4dFromRR(response, response.getPoaNumber());
            HttpHeaders headers = new HttpHeaders();
            if (res != null) {
                headers.set("X-Request-ID", res);
            }
            //return new ResponseEntity<>(response, headers, HttpStatus.OK);
            return new ResponseEntity<>(PvvResponse.builder()
                    .responseType(PvvResponseType.OK_SUBSCRIPTION_REQUIRED)
                    .subscriptionData(Map.of(
                            "method", "GET",
                            "uri", String.format("/api/v1/receive/%s", response.getPoaNumber()),
                            "successNamespace", "M4D_REVOCATION_REQUEST_EXCHANGE_NAMESPACE_JSON"
                    ))
                    .jsonPayload(Map.of(
                            "guid", response.getPoaNumber()
                    ))
                    .build(), headers, HttpStatus.OK);
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
    @PostMapping(value ="/revoke/send/manual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadM4dRevokeZipManual(@RequestParam("file") MultipartFile zipFile) throws IOException {
        log.debug("ПВВ: отзыв");

        String fileName = zipFile.getOriginalFilename();
        if (fileName != null && realApiService.isNotZipFile(ConvertUtils.convertMultipartFileToFile(zipFile))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(NO_ZIP);
        }
        try {
            String res;
            M4dRevokeResponseInfo response = realApiService.processM4dRevokeZip(zipFile);
            res = pvvApiService.sendResponseRevokeM4dFromRR(response, response.getPoaNumber());
            HttpHeaders headers = new HttpHeaders();
            if (res != null) {
                headers.set("X-Request-ID", res);
            }
            return new ResponseEntity<>(response, headers, HttpStatus.OK);
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

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
