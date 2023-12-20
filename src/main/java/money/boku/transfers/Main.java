package money.boku.transfers;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import money.boku.transfers.account.*;

/**
 * Starts up the server.
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        try (Javalin javalin = javalinApp()) {
            // Can be configurable
            javalin.start(7070);
            Thread.currentThread().join();
        }
    }

    /**
     * Creates {@code Javalin} app.
     *
     * @return Javalin app
     */
    public static Javalin javalinApp() {
        AccountDatastore accountDatastore = new InMemoryAccountDatastore();
        WithdrawalServiceStub withdrawalService = new WithdrawalServiceStub();
        WithdrawalRequestWatcher withdrawalRequestWatcher = new WithdrawalRequestWatcher(accountDatastore, withdrawalService);
        withdrawalRequestWatcher.startWatching();
        MoneyService moneyService = new MoneyServiceImpl(
                accountDatastore,
                withdrawalService,
                withdrawalRequestWatcher
        );
        MoneyAPI moneyAPI = new MoneyAPI(moneyService);
        // Not part of the task, rather helps with testing.
        AccountAPI accountAPI = new AccountAPI(accountDatastore);
        return Javalin.create(config -> {
                    config.http.prefer405over404 = true;
                })
                .post(AccountAPI.OPEN_ACCOUNT_PATH, accountAPI::handleOpenAccountRequest)
                .get(AccountAPI.BALANCE_PATH, accountAPI::handleBalanceRequest)
                .post(MoneyAPI.TRANSFER_PATH, moneyAPI::handleTransferRequest)
                .post(MoneyAPI.WITHDRAW_PATH, moneyAPI::handleWithdrawRequest)
                .get(MoneyAPI.WITHDRAWAL_STATE_PATH, moneyAPI::handleWithdrawalStateRequest)
                .exception(AccountNotFoundException.class, ((exception, ctx) -> {
                    ctx.status(HttpStatus.NOT_FOUND);
                }))
                .exception(NegativeBalanceRequestException.class, ((exception, ctx) -> {
                    ctx.status(HttpStatus.BAD_REQUEST);
                }))
                .exception(IllegalArgumentException.class, ((exception, ctx) -> {
                    ctx.status(HttpStatus.BAD_REQUEST);
                }))
                .exception(NumberFormatException.class, ((exception, ctx) -> {
                    ctx.status(HttpStatus.BAD_REQUEST);
                }))
                .exception(WithdrawalNotFoundException.class, ((exception, ctx) -> {
                    ctx.status(HttpStatus.NOT_FOUND);
                }));
    }
}
