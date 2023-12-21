package money.boku.account;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Encapsulates information about user account. Provides functions to increase/decrease balance, can handle
 * concurrent requests.
 */
public class Account {
    // Serves as unique identifier and "address" of the account at the same time.
    private final UUID id;
    private final AtomicReference<BigDecimal> balance;

    public Account(UUID id, BigDecimal balance) {
        this.id = Objects.requireNonNull(id);
        this.balance = new AtomicReference<>(balance);
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance.get();
    }

    /**
     * Adds specified amount to the account balance.
     *
     * @param amount amount to add
     */
    public void add(BigDecimal amount) {
        this.balance.accumulateAndGet(amount, BigDecimal::add);
    }

    /**
     * Subtracts specified amount from the account balance.
     *
     * @param amount amount to subtract
     * @throws NegativeBalanceRequestException if subtracting specified amount would result in negative account balance
     */
    public void subtract(BigDecimal amount) throws NegativeBalanceRequestException {
        while (true) {
            BigDecimal oldVal = balance.get();
            BigDecimal newVal = oldVal.subtract(amount);
            // In case of negative balance we can try to optimize for the number of successful operations by waiting for
            // other operations on the same account to complete.
            // However, it seems to be perfectly reasonable behaviour to fail to go into negative despite potential
            // concurrent additions.
            if (newVal.signum() == -1) {
                throw new NegativeBalanceRequestException("Rejected attempt to transfer/withdraw money that would make balance negative.");
            }
            if (balance.compareAndSet(oldVal, newVal)) {
                return;
            }
        }
    }
}
