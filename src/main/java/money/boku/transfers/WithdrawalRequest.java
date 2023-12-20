package money.boku.transfers;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Passed to API when requesting transfer from one user account to another.
 */
public record WithdrawalRequest(UUID accountId, String withdrawalAddress, BigDecimal amount) {
}
