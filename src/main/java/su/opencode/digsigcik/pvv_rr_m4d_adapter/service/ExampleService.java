package su.opencode.digsigcik.pvv_rr_m4d_adapter.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.RequestInfo;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.ResponseInfo;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.model.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class ExampleService {
    private final RestTemplate restTemplate;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    @Value("${api.user-id}")
    private String userId;


    public ExampleService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseInfo processCertificateZip(MultipartFile zipFile) {
        // Создаем временную директорию для распаковки ZIP
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("certZip");
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать временную директорию", e);
        }

        File xmlFile = null;
        int xmlCount = 0;
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

        try {
            // JAXB-разбор XML файла
            JAXBContext jaxbContext = JAXBContext.newInstance(Document.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Document document = (Document) unmarshaller.unmarshal(xmlFile);

            if (document == null ||
                    document.getTest() == null) {
                throw new RuntimeException("Неверная структура XML файла.");
            }


            // Маппинг Document в CertificateRequestInfo (без обработки вложенных файлов)
            RequestInfo certRequestInfo = su.opencode.digsigcik.pvv_rr_m4d_adapter.mapper.Mapper.INSTANCE
                    .documentToRequestInfo(document);

            // Вызов API .....
            ResponseInfo response = createRequest(certRequestInfo);
            return response;

        } catch (JAXBException e) {
            throw new RuntimeException("Ошибка при разборе XML: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения XML файла: " + e.getMessage(), e);
        } catch (Exception e) {
            return new ResponseInfo("fake answer on error");
        } finally {
            cleanupDirectory(tempDir);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("UserId", userId);
        return headers;
    }

    public ResponseInfo createRequest(RequestInfo requestDto) throws JsonProcessingException {

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Используем ObjectMapper для правильной сериализации
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String jsonBody = mapper.writeValueAsString(requestDto);

        // Логируем тело запроса
        System.out.println("Headers: " + headers);
        System.out.println("Serialized JSON Body: " + jsonBody);

        // Создаём HttpEntity с JSON строкой вместо объекта
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

        try {
            ResponseEntity<ResponseInfo> response = restTemplate.exchange(
                    apiBaseUrl + "/v1/requests", HttpMethod.POST, requestEntity, ResponseInfo.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Server Error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public ResponseInfo getStatus(String status) {

        // Вызов API ....
        return new ResponseInfo();
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
