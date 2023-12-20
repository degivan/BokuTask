package money.boku.transfers;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record WithdrawalRequestResponse(UUID withdrawalId) {
    public WithdrawalRequestResponse(@JsonProperty("withdrawalId") String withdrawalId) {
        this(UUID.fromString(withdrawalId));
    }
}
