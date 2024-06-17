package money.boku.operations.withdrawal;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Response body for withdrawal request.
 *
 * @param withdrawalId id of withdrawal request
 */
public record WithdrawalRequestResponse(UUID withdrawalId) {
    public WithdrawalRequestResponse(@JsonProperty("withdrawalId") String withdrawalId) {
        this(UUID.fromString(withdrawalId));
    }
}
