package money.boku.transfers.account;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory implementation of {@link AccountDatastore}.
 */
public class InMemoryAccountDatastore implements AccountDatastore {
    // Could've been ConcurrentMap<UUID, BigInteger>, but having an account class makes it more extendable.
    private final ConcurrentMap<UUID, Account> idToAccount = new ConcurrentHashMap<>();

    @Override
    public Account createNewAccount(BigDecimal initialBalance) {
        // Ensure account ID is unique.
        UUID accountId = UUID.randomUUID();
        Account account = new Account(accountId, initialBalance);
        while (idToAccount.putIfAbsent(accountId, account) != null) {
            accountId = UUID.randomUUID();
            account = new Account(accountId, initialBalance);
        }
        return account;
    }

    @Override
    public Account getAccount(UUID accountId) throws AccountNotFoundException {
        Account account = idToAccount.get(accountId);
        if (account == null) {
            throw new AccountNotFoundException("Account %s is not found".formatted(accountId));
        }
        return account;
    }

    @Override
    public void increaseBalance(UUID accountId, BigDecimal amount) throws AccountNotFoundException {
        Account account = idToAccount.get(accountId);
        if (account == null) {
            throw new AccountNotFoundException("Account %s is not found".formatted(accountId));
        }
        account.add(amount);
    }

    @Override
    public void decreaseBalance(UUID accountId, BigDecimal amount) throws AccountNotFoundException, NegativeBalanceRequestException {
        Account account = idToAccount.get(accountId);
        if (account == null) {
            throw new AccountNotFoundException("Account %s is not found".formatted(accountId));
        }
        account.subtract(amount);
    }
}
