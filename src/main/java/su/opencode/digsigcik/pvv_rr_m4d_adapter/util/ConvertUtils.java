package su.opencode.digsigcik.pvv_rr_m4d_adapter.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.modelmapper.ModelMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;
import su.opencode.digsigcik.common.exception.custom.BadRequestException;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.AuthResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static su.opencode.digsigcik.pvv_rr_m4d_adapter.util.Constants.CREATE_TEMP_FOLDER_ERROR;

@UtilityClass
public class ConvertUtils {
    static ObjectMapper objectMapper = new ObjectMapper();
    static ModelMapper modelMapper = new ModelMapper();

    public static <T> T convertStringToClass(String response, Class<T> clazz) throws JsonProcessingException {
        return objectMapper.readValue(response, clazz);
    }

    public  <T> T convertStringErrorToClassError(String error, Class<T> clazz) throws JsonProcessingException {
        return objectMapper.readValue(error, clazz);
    }

    public <D> D convertClassErrorToClassError(AuthResponse.Error authError, Class<D> destinationClass) {
        return modelMapper.map(authError, destinationClass);
    }

    public File byteArrayResourceToTempFile(ByteArrayResource resource, String prefix, String suffix) throws IOException {
        File tempFile = File.createTempFile(prefix, suffix);
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            out.write(resource.getByteArray());
        }
        return tempFile;
    }

    public static void createDirectoryIfSheNotExist(String directoryPathName) {
        Path directoryPath = Paths.get(directoryPathName);
        if (!Files.exists(directoryPath)) {
            File file = new File(directoryPathName);
            boolean res = file.mkdirs();
            if (!res) {
                throw new BadRequestException(CREATE_TEMP_FOLDER_ERROR);
            }
        }
    }

    public static File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        String formattedDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String tempPath = "/tmp/" + formattedDateTime;
        createDirectoryIfSheNotExist(tempPath);
        String tempSourceFile = tempPath + "/" + multipartFile.getOriginalFilename();
        FileOutputStream fo1;
        fo1 = new FileOutputStream(tempSourceFile);
        fo1.write(multipartFile.getBytes());
        fo1.close();
        return new File(tempSourceFile);
    }
}
