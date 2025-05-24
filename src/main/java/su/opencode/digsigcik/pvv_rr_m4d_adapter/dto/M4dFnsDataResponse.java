package su.opencode.digsigcik.pvv_rr_m4d_adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class M4dFnsDataResponse {
    @JsonProperty("poaNumber")
    private String guid;
    @JsonProperty("id")
    private String id;
    @JsonProperty("createdDate")
    private String createdDate;
    @JsonProperty("createdDateTime")
    private String createdDateTime;
    @JsonProperty("startDate")
    private String startDate;
    @JsonProperty("startDateTime")
    private String startDateTime;
    @JsonProperty("endDate")
    private String endDate;
    @JsonProperty("endDateTime")
    private String endDateTime;
    @JsonProperty("statusDate")
    private String statusDate;
    @JsonProperty("statusDateTime")
    private String statusDateTime;
    @JsonProperty("status")
    private String status;
    @JsonProperty("issuerPublicKey")
    private String issuerPublicKey;
    @JsonProperty("issuerSnilsHash")
    private String issuerSnilsHash;
    @JsonProperty("issuerOgrnHash")
    private String issuerOgrnHash;
    @JsonProperty("error")
    private Error error;

    @Data
    public static class Error {
        @JsonProperty("detail")
        private String detail; // текст ошибки
        @JsonProperty("instance")
        private String instance; // URL, идентифицирующий ошибку
        @JsonProperty("meta")
        private String meta; // Метаданные ошибки
        @JsonProperty("status")
        private Integer status; // HTTP статус
        @JsonProperty("title")
        private String title; // заголовок ошибки
        @JsonProperty("type")
        private String type; // код ошибки
        @JsonProperty("dictionary_code")
        private String dictionary_code; // Код ошибки в справочнике ERROR-кодов
    }
}
