package su.opencode.digsigcik.pvv_rr_m4d_adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class M4dResponseInfo {
    @JsonProperty("poaId")
    private String poaId; // хэш доверенности
    @JsonProperty("poaNumber")
    private String poaNumber; // guid доверенности
    @JsonProperty("issuerInn")
    private String issuerInn; // инн доверителя
    @JsonProperty("txId")
    private String txId; // номер транзакции
}
