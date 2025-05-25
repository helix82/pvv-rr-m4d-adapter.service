package su.opencode.digsigcik.pvv_rr_m4d_adapter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.*;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.AuthResponse;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.M4dResponseInfo;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.M4dFnsDataResponse;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.M4dRevokeResponseInfo;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.util.HashUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealApiService {
    private final RestTemplate restTemplate;
    ObjectMapper objectMapper = new ObjectMapper();
    private final ModelMapper modelMapper;

    @Value("${rr-m4d.get-token.uri}")
    private String GET_TOKEN_URI;
    @Value("${rr-m4d.registry-m4d.uri}")
    private String REGISTRY_M4D_URI;
    @Value("${rr-m4d.registry-revoke.uri}")
    private String REGISTRY_REVOKE_URI;
    @Value("${rr-m4d.user-name}")
    private String USER_NAME;
    @Value("${rr-m4d.user-password}")
    private String USER_PASSWORD;
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
            M4dResponseInfo.Error error = convertClassErrorToClassError(authResponse.getError(), M4dResponseInfo.Error.class);
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
                M4dResponseInfo.Error error = convertStringErrorToClassError(e.getResponseBodyAsString(), M4dResponseInfo.Error.class);
                responseInfo.setError(error);
            } catch (RestClientResponseException e) {
                System.out.println("Ответ сервера: " + e.getResponseBodyAsString());
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
                M4dRevokeResponseInfo error = convertStringErrorToClassError(e.getResponseBodyAsString(), M4dRevokeResponseInfo.class);
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
            M4dFnsDataResponse.Error error = convertClassErrorToClassError(authResponse.getError(), M4dFnsDataResponse.Error.class);
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
                M4dFnsDataResponse.Error error = convertStringErrorToClassError(e.getResponseBodyAsString(), M4dFnsDataResponse.Error.class);
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
            AuthResponse.Error error = convertStringErrorToClassError(e.getResponseBodyAsString(), AuthResponse.Error.class);
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
            throw new RuntimeException("Не удалось создать временную директорию", e);
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
            throw new RuntimeException("Ошибка распаковки ZIP файла: " + e.getMessage(), e);
        }

        if (xmlCount == 0) {
            cleanupDirectory(tempDir);
            throw new RuntimeException("XML-файл не найден в ZIP архиве.");
        } else if (xmlCount > 2) {
            cleanupDirectory(tempDir);
            throw new RuntimeException("В архиве найдено более двух XML-файлов.");
        }
        if (sigCount == 0) {
            cleanupDirectory(tempDir);
            throw new RuntimeException("SIG-файл не найден в ZIP архиве.");
        } else if (sigCount > 2) {
            cleanupDirectory(tempDir);
            throw new RuntimeException("В архиве найдено более двух SIG-файлов.");
        }

        try {
            // Вызов API для создания запроса на регистрацию мчд в рр мчд
            M4dResponseInfo response = createM4dRequest(xmlFile, sigFile, xmlFileKvit, sigFileKvit);
            if (response == null) {
                throw new RuntimeException("Ошибка регистрации МЧД в РР МЧД.");
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
            throw new RuntimeException("Не удалось создать временную директорию", e);
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
            throw new RuntimeException("Ошибка распаковки ZIP файла: " + e.getMessage(), e);
        }

        if (xmlCount == 0) {
            cleanupDirectory(tempDir);
            throw new RuntimeException("XML-файл не найден в ZIP архиве.");
        } else if (xmlCount > 2) {
            cleanupDirectory(tempDir);
            throw new RuntimeException("В архиве найдено более двух XML-файлов.");
        }
        if (sigCount == 0) {
            cleanupDirectory(tempDir);
            throw new RuntimeException("SIG-файл не найден в ZIP архиве.");
        } else if (sigCount > 2) {
            cleanupDirectory(tempDir);
            throw new RuntimeException("В архиве найдено более двух SIG-файлов.");
        }
        try {
            // Вызов API для создания запроса на регистрацию мчд в рр мчд
            M4dRevokeResponseInfo response = createM4dRevokeRequest(xmlFile, sigFile, xmlFileKvit, sigFileKvit);
            if (response == null) {
                throw new RuntimeException("Ошибка регистрации МЧД в РР МЧД.");
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
            log.warn("Ошибка очистки временной директории {}: {}", directory, e.getMessage());
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

    private static void createZip(String zipFilePath, List<String> files) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (String filePath : files) {
                File file = new File(filePath);
                if (!file.exists()) {
                    System.err.println("Файл не найден: " + filePath);
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

    private <T> T convertStringErrorToClassError(String error, Class<T> clazz) throws JsonProcessingException {
        return objectMapper.readValue(error, clazz);
    }

    private  <D> D convertClassErrorToClassError(AuthResponse.Error authError, Class<D> destinationClass) {
        return modelMapper.map(authError, destinationClass);
    }

}
