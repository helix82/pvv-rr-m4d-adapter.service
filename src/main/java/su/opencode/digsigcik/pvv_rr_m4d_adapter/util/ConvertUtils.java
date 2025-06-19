package su.opencode.digsigcik.pvv_rr_m4d_adapter.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.modelmapper.ModelMapper;
import org.springframework.core.io.ByteArrayResource;
import su.opencode.digsigcik.pvv_rr_m4d_adapter.dto.AuthResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@UtilityClass
public class ConvertUtils {
    static ObjectMapper objectMapper = new ObjectMapper();
    static ModelMapper modelMapper;

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
}
