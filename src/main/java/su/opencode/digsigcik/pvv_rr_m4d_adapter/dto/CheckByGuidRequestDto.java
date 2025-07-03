package su.opencode.digsigcik.pvv_rr_m4d_adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CheckByGuidRequestDto {
    @JsonProperty("guid")
    private String guid;
    @JsonProperty("principalInn")
    private String principalInn;
    @JsonProperty("representativeInn")
    private String representativeInn;

    @Override
    public String toString() {
        return "{guid='" + guid + "', principalInn='" + principalInn + "', representativeInn='" + representativeInn + "'}";
    }
}
