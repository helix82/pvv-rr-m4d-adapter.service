package su.opencode.digsigcik.pvv_rr_m4d_adapter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.*;
import org.springframework.web.multipart.MultipartFile;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.*;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.util.ConvertUtils;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.util.HashUtils;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static su.opencode.digsigcik.pvv_rr_m4d_adapter.util.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealApiService {
    private final RestTemplate restTemplate;

    @Value("${rr-m4d.get-token.uri}")
    private URI GET_TOKEN_URI;
    @Value("${rr-m4d.registry-m4d.uri}")
    private URI REGISTRY_M4D_URI;
    @Value("${rr-m4d.registry-revoke.uri}")
    private String REGISTRY_REVOKE_URI;
    @Value("${rr-m4d.user-name}")
    private URI USER_NAME;
    @Value("${rr-m4d.user-password}")
    private URI USER_PASSWORD;
    @Value("${rr-m4d.soul}")
    private String M4D_SOUL;

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public M4dResponseInfo createM4dRequest(File xmlFile, File sigFile, File xmlFileKvit, File sigFileKvit) throws IOException, NoSuchAlgorithmException {
        //TODO: @helix82 использовать xmlFileKvit и sigFileKvit когда появится функционал в API РР МЧД по приёму подтверждения и подписи о регистрации в ПЭП
        AuthResponse authResponse;
        M4dResponseInfo responseInfo = new M4dResponseInfo();
        responseInfo.setPoaNumber(getGuidFromXml(xmlFile));
        responseInfo.setPoaId(HashUtils.getHash(Files.readAllBytes(Paths.get(xmlFile.getPath())), M4D_SOUL));
        authResponse = getBearerRequest();
        if (authResponse.getAccessToken()==null) {
            M4dResponseInfo.Error error = ConvertUtils.convertClassErrorToClassError(authResponse.getError(), M4dResponseInfo.Error.class);
            responseInfo.setError(error);
            return responseInfo;
        }
        HttpHeaders headers = createHeaders();
        try {
            headers.setAccept(Collections.singletonList(MediaType.ALL));
            headers.set("Authorization", "Bearer " + authResponse.getAccessToken());
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
            map.add("poa", new FileSystemResource(xmlFile));
            map.add("signature", new FileSystemResource(sigFile));
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);

            try {
                ResponseEntity<M4dResponseInfo> response = restTemplate.exchange(
                        REGISTRY_M4D_URI,
                        HttpMethod.POST,
                        requestEntity,
                        M4dResponseInfo.class
                );
                responseInfo = response.getBody();
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                M4dResponseInfo.Error error = ConvertUtils.convertStringErrorToClassError(e.getResponseBodyAsString(), M4dResponseInfo.Error.class);
                responseInfo.setError(error);
            } catch (RestClientResponseException e) {
                System.out.println("Server response: " + e.getResponseBodyAsString());
            } catch (Exception e) {
                throw new RuntimeException("Error: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
        return responseInfo;
    }

    public M4dRevokeResponseInfo createM4dRevokeRequest(File xmlFile, File sigFile, File xmlFileKvit, File sigFileKvit) throws IOException, NoSuchAlgorithmException {
        //TODO: @helix82 использовать xmlFileKvit и sigFileKvit когда появится функционал в API РР МЧД по приёму подтверждения и подписи о регистрации в ПЭП
        AuthResponse authResponse;
        authResponse = getBearerRequest();
        M4dRevokeResponseInfo responseInfo = new M4dRevokeResponseInfo();
        responseInfo.setPoaNumber(getGuidFromXml(xmlFile));
        if (authResponse.getAccessToken()==null) {
            responseInfo.setType(authResponse.getError().getType());
            responseInfo.setTitle(authResponse.getError().getTitle());
            responseInfo.setStatus(authResponse.getError().getStatus());
            responseInfo.setDetail(authResponse.getError().getDetail());
            responseInfo.setInstance(authResponse.getError().getInstance());
            responseInfo.setDictionary_code(authResponse.getError().getDictionary_code());
            responseInfo.setMeta(authResponse.getError().getMeta());
            return responseInfo;
        }
        HttpHeaders headers = createHeaders();
        try {
            headers.setAccept(Collections.singletonList(MediaType.ALL));
            headers.set("Authorization", "Bearer " + authResponse.getAccessToken());
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
            map.add("poaRevoke", new FileSystemResource(xmlFile));
            map.add("signature", new FileSystemResource(sigFile));
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);

            try {
                ResponseEntity<M4dRevokeResponseInfo> response = restTemplate.exchange(
                        REGISTRY_REVOKE_URI,
                        HttpMethod.POST,
                        requestEntity,
                        M4dRevokeResponseInfo.class
                );
                responseInfo = response.getBody();
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                M4dRevokeResponseInfo error = ConvertUtils.convertStringErrorToClassError(e.getResponseBodyAsString(), M4dRevokeResponseInfo.class);
                responseInfo.setType(error.getType());
                responseInfo.setTitle(error.getTitle());
                responseInfo.setStatus(error.getStatus());
                responseInfo.setDetail(error.getDetail());
                responseInfo.setInstance(error.getInstance());
                responseInfo.setDictionary_code(error.getDictionary_code());
                responseInfo.setMeta(error.getMeta());
            } catch (Exception e) {
                throw new RuntimeException("Error: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
        return responseInfo;
    }

    public M4dFnsDataResponse getM4dRequestStatus(String guidXml) throws JsonProcessingException {
        M4dFnsDataResponse m4dFnsDataResponse = new M4dFnsDataResponse();
        m4dFnsDataResponse.setGuid(guidXml);
        AuthResponse authResponse;
        authResponse = getBearerRequest();
        if (authResponse.getAccessToken()==null) {
            M4dFnsDataResponse.Error error = ConvertUtils.convertClassErrorToClassError(authResponse.getError(), M4dFnsDataResponse.Error.class);
            m4dFnsDataResponse.setError(error);
            return m4dFnsDataResponse;
        }
        HttpHeaders headers = createHeaders();
        try {
            headers.setAccept(Collections.singletonList(MediaType.ALL));
            headers.set("Authorization", "Bearer " + authResponse.getAccessToken());
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            try {
                ResponseEntity<M4dFnsDataResponse> response = restTemplate.exchange(
                        REGISTRY_M4D_URI + "/" + guidXml + "/public",
                        HttpMethod.GET,
                        requestEntity,
                        M4dFnsDataResponse.class
                );
                m4dFnsDataResponse = response.getBody();
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                M4dFnsDataResponse.Error error = ConvertUtils.convertStringErrorToClassError(e.getResponseBodyAsString(), M4dFnsDataResponse.Error.class);
                m4dFnsDataResponse.setError(error);
            } catch (Exception e) {
                throw new RuntimeException("Error: " + e.getMessage());
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Server Error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
        return m4dFnsDataResponse;
    }

    public M4dFnsDataResponse getFullM4dRequest(CheckByGuidRequestDto requestDto) throws JsonProcessingException {
        M4dFnsDataResponse m4dFnsDataResponse = new M4dFnsDataResponse();
        m4dFnsDataResponse.setGuid(requestDto.getGuid());
        m4dFnsDataResponse.setPrincipalInn(requestDto.getPrincipalInn());
        m4dFnsDataResponse.setRepresentativeInn(requestDto.getRepresentativeInn());
        AuthResponse authResponse;
        authResponse = getBearerRequest();
        if (authResponse.getAccessToken()==null) {
            M4dFnsDataResponse.Error error = ConvertUtils.convertClassErrorToClassError(authResponse.getError(), M4dFnsDataResponse.Error.class);
            m4dFnsDataResponse.setError(error);
            return m4dFnsDataResponse;
        }
        HttpHeaders headers = createHeaders();
        try {
            headers.setAccept(Collections.singletonList(MediaType.ALL));
            headers.set("Authorization", "Bearer " + authResponse.getAccessToken());
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            String urlTemplate = REGISTRY_M4D_URI + "/zip/?poaNumber={poaNumber}&issuerInn={issuerInn}&representativeInn={representativeInn}";

            Map<String, String> params = Map.of(
                    "poaNumber", requestDto.getGuid(),
                    "issuerInn", requestDto.getPrincipalInn(),
                    "representativeInn", requestDto.getRepresentativeInn()
            );

            try {
                ResponseEntity<ByteArrayResource> response = restTemplate.exchange(
                        urlTemplate, //REGISTRY_M4D_URI + "/zip/",
                        HttpMethod.GET,
                        requestEntity,
                        ByteArrayResource.class,
                        params
                );
                m4dFnsDataResponse.setFile(response.getBody());
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                M4dFnsDataResponse.Error error = ConvertUtils.convertStringErrorToClassError(e.getResponseBodyAsString(), M4dFnsDataResponse.Error.class);
                m4dFnsDataResponse.setError(error);
            } catch (Exception e) {
                throw new RuntimeException("Error: " + e.getMessage());
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Server Error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
        return m4dFnsDataResponse;
    }

    private AuthResponse getBearerRequest() throws JsonProcessingException {
        AuthResponse authResponse = new AuthResponse();
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        String auth = USER_NAME + ":" + USER_PASSWORD;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        String requestBody = "{ \"username\": \"operator\", \"password\": \"operator\", \"grant_type\": \"password\" }";
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    GET_TOKEN_URI,
                    HttpMethod.POST,
                    requestEntity,
                    AuthResponse.class
            );
            authResponse = response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            AuthResponse.Error error = ConvertUtils.convertStringErrorToClassError(e.getResponseBodyAsString(), AuthResponse.Error.class);
            authResponse.setError(error);
        } catch (Exception e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
        return authResponse;
    }

    public M4dResponseInfo processM4dZip(MultipartFile zipFile) {
        // Создаем временную директорию для распаковки ZIP
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("mchdZip");
        } catch (IOException e) {
            throw new RuntimeException(CREATE_TEMP_FOLDER_ERROR, e);
        }

        File xmlFile = null;
        File xmlFileKvit = null;
        File sigFile = null;
        File sigFileKvit = null;
        int xmlCount = 0;
        int sigCount = 0;
        // Распаковка ZIP-архива и сохранение всех файлов
        try (InputStream is = zipFile.getInputStream();
             ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    File outFile = new File(tempDir.toFile(), entry.getName());
                    try (OutputStream os = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                    // Если расширение xml – увеличиваем счетчик и запоминаем файл
                    if (entry.getName()
                            .toLowerCase()
                            .endsWith(".xml")) {
                        xmlCount++;
                        if (entry.getName()
                                .toLowerCase()
                                .startsWith("on_emchd")) {
                            xmlFile = outFile;
                        } else if (entry.getName()
                                .toLowerCase()
                                .startsWith("квит003_фнс_on_emchd")) {
                            xmlFileKvit = outFile;
                        }
                    }
                    // Если расширение sig – увеличиваем счетчик и запоминаем файл
                    if (entry.getName()
                            .toLowerCase()
                            .endsWith(".sig")) {
                        sigCount++;
                        if (entry.getName()
                                .toLowerCase()
                                .startsWith("on_emchd")) {
                            sigFile = outFile;
                        } else if (entry.getName()
                                .toLowerCase()
                                .startsWith("квит003_фнс_on_emchd")) {
                            sigFileKvit = outFile;
                        }
                    }
                }
            }
        } catch (IOException e) {
            cleanupDirectory(tempDir);
            throw new RuntimeException(ZIP_UNPACK_ERROR + ": " + e.getMessage(), e);
        }

        if (xmlCount == 0) {
            cleanupDirectory(tempDir);
            throw new RuntimeException(ZIP_XML_NOT_FOUND);
        } else if (xmlCount > 2) {
            cleanupDirectory(tempDir);
            throw new RuntimeException(ZIP_XML_MORE_TWO);
        }
        if (sigCount == 0) {
            cleanupDirectory(tempDir);
            throw new RuntimeException(ZIP_SIG_NOT_FOUND);
        } else if (sigCount > 2) {
            cleanupDirectory(tempDir);
            throw new RuntimeException(ZIP_SIG_MORE_TWO);
        }

        try {
            // Вызов API для создания запроса на регистрацию мчд в рр мчд
            M4dResponseInfo response = createM4dRequest(xmlFile, sigFile, xmlFileKvit, sigFileKvit);
            if (response == null) {
                throw new RuntimeException(M4D_REGISTRATION_ERROR);
            }
            return response;
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } finally {
            cleanupDirectory(tempDir);
        }
    }

    public M4dRevokeResponseInfo processM4dRevokeZip(MultipartFile zipFile) {
        // Создаем временную директорию для распаковки ZIP
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("mchdRevokeZip");
        } catch (IOException e) {
            throw new RuntimeException(CREATE_TEMP_FOLDER_ERROR, e);
        }

        File xmlFile = null;
        File xmlFileKvit = null;
        File sigFile = null;
        File sigFileKvit = null;
        int xmlCount = 0;
        int sigCount = 0;
        // Распаковка ZIP-архива и сохранение всех файлов
        try (InputStream is = zipFile.getInputStream();
             ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    File outFile = new File(tempDir.toFile(), entry.getName());
                    try (OutputStream os = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                    // Если расширение xml – увеличиваем счетчик и запоминаем файл
                    if (entry.getName()
                            .toLowerCase()
                            .endsWith(".xml")) {
                        xmlCount++;
                        if (entry.getName()
                                .toLowerCase()
                                .startsWith("on_offdover")) {
                            xmlFile = outFile;
                        } else if (entry.getName()
                                .toLowerCase()
                                .startsWith("квит003_фнс_on_offdover")) {
                            xmlFileKvit = outFile;
                        }
                    }
                    // Если расширение sig – увеличиваем счетчик и запоминаем файл
                    if (entry.getName()
                            .toLowerCase()
                            .endsWith(".sig")) {
                        sigCount++;
                        if (entry.getName()
                                .toLowerCase()
                                .startsWith("on_offdover")) {
                            sigFile = outFile;
                        } else if (entry.getName()
                                .toLowerCase()
                                .startsWith("квит003_фнс_on_offdover")) {
                            sigFileKvit = outFile;
                        }
                    }
                }
            }
        } catch (IOException e) {
            cleanupDirectory(tempDir);
            throw new RuntimeException(ZIP_UNPACK_ERROR + ": " + e.getMessage(), e);
        }

        if (xmlCount == 0) {
            cleanupDirectory(tempDir);
            throw new RuntimeException(ZIP_XML_NOT_FOUND);
        } else if (xmlCount > 2) {
            cleanupDirectory(tempDir);
            throw new RuntimeException(ZIP_XML_MORE_TWO);
        }
        if (sigCount == 0) {
            cleanupDirectory(tempDir);
            throw new RuntimeException(ZIP_SIG_NOT_FOUND);
        } else if (sigCount > 2) {
            cleanupDirectory(tempDir);
            throw new RuntimeException(ZIP_SIG_MORE_TWO);
        }
        try {
            // Вызов API для создания запроса на регистрацию мчд в рр мчд
            M4dRevokeResponseInfo response = createM4dRevokeRequest(xmlFile, sigFile, xmlFileKvit, sigFileKvit);
            if (response == null) {
                throw new RuntimeException(M4D_REVOKE_REGISTRATION_ERROR);
            }
            return response;
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } finally {
            cleanupDirectory(tempDir);
        }
    }

    private void cleanupDirectory(Path directory) {
        try {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.warn(CLEAR_TEMP_FOLDER_ERROR + " {}: {}", directory, e.getMessage());
        }
    }

    public ByteArrayResource createArchiveWithM4dRequestStatus(String guidXml, M4dFnsDataResponse response, List<String> files) throws IOException {
        String tempZipFile = "/tmp/ON_EMCHD_" + guidXml + ".zip";
        String tempFileResponse = "/tmp/status_ON_EMCHD_" + guidXml + ".json";
        List<String> filesList = new ArrayList<>();

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Сериализация объекта в JSON и запись в файл
            objectMapper.writeValue(new File(tempFileResponse), response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        filesList.add(tempFileResponse);
        if (!files.isEmpty()) {
            filesList.addAll(files);
        }
        createZip(tempZipFile, filesList);
        File f = new File(tempFileResponse);
        f.delete();
        return new ByteArrayResource(Files.readAllBytes((new File(tempZipFile)).toPath()));
    }

    public File createArchiveWithM4dRequestStatusToFile(String guidXml, Object report, List<String> files) throws IOException {
        String tempZipFile = "/tmp/ON_EMCHD_" + guidXml + ".zip";
        String tempFileResponse = "/tmp/status_ON_EMCHD_" + guidXml + ".json";
        List<String> filesList = new ArrayList<>();

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Сериализация объекта в JSON и запись в файл
            objectMapper.writeValue(new File(tempFileResponse), report);
        } catch (IOException e) {
            e.printStackTrace();
        }
        filesList.add(tempFileResponse);
        if (!files.isEmpty()) {
            filesList.addAll(files);
        }
        createZip(tempZipFile, filesList);
        File f = new File(tempFileResponse);
        f.delete();
        return new File(tempZipFile);
    }

    private static void createZip(String zipFilePath, List<String> files) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (String filePath : files) {
                File file = new File(filePath);
                if (!file.exists()) {
                    System.err.println(FILE_NOT_FOUND + ": " + filePath);
                    continue;
                }

                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zos.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) >= 0) {
                        zos.write(buffer, 0, length);
                    }

                    zos.closeEntry();
                }
            }
        }
    }

    public String getGuidFromXml(File file) {
        String guid = null;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("НомДовер=")) {
                    String key = " НомДовер=\"";
                    int startIndex = line.indexOf(key);
                    if (startIndex != -1) {
                        startIndex += key.length();
                        int endIndex = line.indexOf("\"", startIndex);
                        if (endIndex != -1) {
                            guid = line.substring(startIndex, endIndex);
                        }
                    }
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return guid;
    }

    public boolean isZipFile(File file) {
        try (ZipFile zipFile = new ZipFile(file)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
