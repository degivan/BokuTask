package money.boku.transfers.account;

import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * API for dealing with account data. It is used for testing within the scope of the task.
 */
public class AccountAPI {
    public final static String OPEN_ACCOUNT_PATH = "/open-account";
    public final static String BALANCE_PATH = "/balance/{accountId}";

    private final AccountDatastore accountDatastore;

    public AccountAPI(AccountDatastore accountDatastore) {
        this.accountDatastore = Objects.requireNonNull(accountDatastore);
    }

    /**
     * Handles HTTP request to open new account.
     *
     * @param ctx request context
     */
    public void handleOpenAccountRequest(@NotNull Context ctx) {
        String balanceStr = ctx.queryParam("initialBalance");
        if (balanceStr == null || balanceStr.isEmpty()) {
            throw new IllegalArgumentException("initialBalance parameter is not specified properly");
        }
        Account account = accountDatastore.createNewAccount(new BigDecimal(balanceStr));
        // opted out for returning just value instead of JSON to simplify testing
        ctx.result(account.getId().toString());
    }

    /**
     * Handles HTTP request to look at account balance.
     *
     * @param ctx request context
     * @throws AccountNotFoundException if no account with such id exists
     */
    public void handleBalanceRequest(@NotNull Context ctx) throws AccountNotFoundException {
        String accountIdStr = ctx.pathParam("accountId");
        UUID accountId = UUID.fromString(accountIdStr);
        Account account = accountDatastore.getAccount(accountId);
        // opted out for returning just value instead of JSON to simplify testing
        ctx.result(account.getBalance().toString());
    }
}
