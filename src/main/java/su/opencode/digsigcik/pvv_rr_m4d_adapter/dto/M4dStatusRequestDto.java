package su.opencode.digsigcik.pvv_rr_m4d_adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class M4dStatusRequestDto {
    @JsonProperty("guid")
    private String guid;
    @JsonProperty("issuerInn")
    private String issuerInn;
    @JsonProperty("representativeInn")
    private String representativeInn;
}
