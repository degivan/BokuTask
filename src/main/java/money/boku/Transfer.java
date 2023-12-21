package money.boku;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Passed to API when requesting transfer from one user account to another.
 */
public record Transfer(UUID from, UUID to, BigDecimal amount) {
}
