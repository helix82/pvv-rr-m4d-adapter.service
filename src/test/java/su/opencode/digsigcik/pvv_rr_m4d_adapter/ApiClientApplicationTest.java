package su.opencode.digsigcik.pvv_rr_m4d_adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
class ApiClientApplicationTest {

    @Mock
    private RestTemplate restTemplate;


    @Test
    void testExample() throws Exception {
        assertTrue(true);
    }


}