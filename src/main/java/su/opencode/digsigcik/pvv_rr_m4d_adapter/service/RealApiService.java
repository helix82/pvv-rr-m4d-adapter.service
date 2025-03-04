package su.opencode.digsigcik.pvv_rr_m4d_adapter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.AuthResponse;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.M4dResponseInfo;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.M4dFnsDataResponse;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.M4dRevokeResponseInfo;

import java.io.*;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class RealApiService {
    private final RestTemplate restTemplate;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    public RealApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public M4dResponseInfo createM4dRequest(File xmlFile, File sigFile) {
        AuthResponse authResponse;
        authResponse = getBearerRequest();
        M4dResponseInfo responseInfo = null;

        if (authResponse != null) {
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
                            apiBaseUrl + "/poar-webapp/integration/poa/",
                            HttpMethod.POST,
                            requestEntity,
                            M4dResponseInfo.class
                    );
                    responseInfo = response.getBody();
                } catch (HttpClientErrorException e) {
                    throw new RuntimeException("Server Error: " + e.getResponseBodyAsString());
                } catch (Exception e) {
                    throw new RuntimeException("Error: " + e.getMessage());
                }
            } catch (Exception e) {
                throw new RuntimeException("Error: " + e.getMessage());
            }
        }
        return responseInfo;
    }

    public M4dRevokeResponseInfo createM4dRevokeRequest(File xmlFile, File sigFile) {
        AuthResponse authResponse;
        authResponse = getBearerRequest();
        M4dRevokeResponseInfo responseInfo = null;

        if (authResponse != null) {
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
                            apiBaseUrl + "/poar-webapp/integration/poa/revoke",
                            HttpMethod.POST,
                            requestEntity,
                            M4dRevokeResponseInfo.class
                    );
                    responseInfo = response.getBody();
                } catch (HttpClientErrorException e) {
                    throw new RuntimeException("Server Error: " + e.getResponseBodyAsString());
                } catch (Exception e) {
                    throw new RuntimeException("Error: " + e.getMessage());
                }
            } catch (Exception e) {
                throw new RuntimeException("Error: " + e.getMessage());
            }
        }
        return responseInfo;
    }

    public M4dFnsDataResponse getM4dRequestStatus(String guidXml) {
        M4dFnsDataResponse m4dFnsDataResponse = null;
        AuthResponse authResponse;
        authResponse = getBearerRequest();

        if (authResponse != null) {
            HttpHeaders headers = createHeaders();
            try {
                headers.setAccept(Collections.singletonList(MediaType.ALL));
                headers.set("Authorization", "Bearer " + authResponse.getAccessToken());
                HttpEntity<String> requestEntity = new HttpEntity<>(headers);

                try {
                    ResponseEntity<M4dFnsDataResponse> response = restTemplate.exchange(
                            apiBaseUrl + "/poar-webapp/integration/poa/" + guidXml + "/public",
                            HttpMethod.GET,
                            requestEntity,
                            M4dFnsDataResponse.class
                    );
                    m4dFnsDataResponse = response.getBody();
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode().value() == 404) {
                        System.out.println("Ресурс не найден (404): " + e.getResponseBodyAsString());
                        System.out.println("МЧД с GUID=" + guidXml + " не найдена на РР МЧД ФНС");
                    } else {
                        System.out.println("Ошибка: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
                    }
                } catch (ResourceAccessException e) {
                    System.out.println("Ошибка доступа к ресурсу: " + e.getMessage());
                    if (e.getCause() instanceof UnknownHostException) {
                        System.out.println("Неизвестный хост: " + e.getCause().getMessage());
                        System.out.println("Сервис РР МЧД ФНС недоступен");
                    }
                } catch (Exception e) {
                    System.out.println("Произошла ошибка: " + e.getMessage());
                }
            } catch (HttpClientErrorException e) {
                throw new RuntimeException("Server Error: " + e.getResponseBodyAsString());
            } catch (Exception e) {
                throw new RuntimeException("Error: " + e.getMessage());
            }
        } else throw new RuntimeException("Ошибка авторизации");
        if (m4dFnsDataResponse == null) {
            System.out.println("Ошибка получения данных по МЧД с сайта РР МЧД ФНС");
        }
        return m4dFnsDataResponse;
    }

    private AuthResponse getBearerRequest() {
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        String username = "demo-client";
        String password = "secret";
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        String requestBody = "{ \"username\": \"operator\", \"password\": \"operator\", \"grant_type\": \"password\" }";
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    apiBaseUrl + "/vst-oauth2/oauth/token",
                    HttpMethod.POST,
                    requestEntity,
                    AuthResponse.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                log.error("Ресурс не найден (404): " + e.getResponseBodyAsString());
            } else {
                log.error("Ошибка: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            }
        } catch (ResourceAccessException e) {
            log.error("Ошибка доступа к ресурсу: " + e.getMessage());
            if (e.getCause() instanceof UnknownHostException) {
                System.out.println("Неизвестный хост: " + e.getCause().getMessage());
            }
        } catch (Exception e) {
            System.out.println("Произошла ошибка: " + e.getMessage());
        }
        return null;
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
        File sigFile = null;
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
                        xmlFile = outFile;
                    }
                    // Если расширение sig – увеличиваем счетчик и запоминаем файл
                    if (entry.getName()
                            .toLowerCase()
                            .endsWith(".sig")) {
                        sigCount++;
                        sigFile = outFile;
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
        } else if (xmlCount > 1) {
            cleanupDirectory(tempDir);
            throw new RuntimeException("В архиве найдено более одного XML-файла.");
        }
        if (sigCount == 0) {
            cleanupDirectory(tempDir);
            throw new RuntimeException("SIG-файл не найден в ZIP архиве.");
        } else if (sigCount > 1) {
            cleanupDirectory(tempDir);
            throw new RuntimeException("В архиве найдено более одного SIG-файла.");
        }

        try {
            // Вызов API для создания запроса на регистрацию мчд в рр мчд
            M4dResponseInfo response = createM4dRequest(xmlFile, sigFile);
            if (response == null) {
                throw new RuntimeException("Ошибка регистрации МЧД в РР МЧД.");
            }
            return response;
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
        File sigFile = null;
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
                        xmlFile = outFile;
                    }
                    // Если расширение sig – увеличиваем счетчик и запоминаем файл
                    if (entry.getName()
                            .toLowerCase()
                            .endsWith(".sig")) {
                        sigCount++;
                        sigFile = outFile;
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
        } else if (xmlCount > 1) {
            cleanupDirectory(tempDir);
            throw new RuntimeException("В архиве найдено более одного XML-файла.");
        }
        if (sigCount == 0) {
            cleanupDirectory(tempDir);
            throw new RuntimeException("SIG-файл не найден в ZIP архиве.");
        } else if (sigCount > 1) {
            cleanupDirectory(tempDir);
            throw new RuntimeException("В архиве найдено более одного SIG-файла.");
        }

        try {
            // Вызов API для создания запроса на регистрацию мчд в рр мчд
            M4dRevokeResponseInfo response = createM4dRevokeRequest(xmlFile, sigFile);
            if (response == null) {
                throw new RuntimeException("Ошибка регистрации МЧД в РР МЧД.");
            }
            return response;
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

}
