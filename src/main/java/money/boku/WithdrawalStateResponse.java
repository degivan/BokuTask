package money.boku;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body for withdrawal state request.
 *
 * @param state withdrawal state
 */
public record WithdrawalStateResponse(WithdrawalService.WithdrawalState state) {
    public WithdrawalStateResponse(@JsonProperty("state") String stateStr) {
        this(WithdrawalService.WithdrawalState.valueOf(stateStr));
    }
}
