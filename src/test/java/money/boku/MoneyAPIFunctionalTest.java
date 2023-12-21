package money.boku;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.testtools.HttpClient;
import io.javalin.testtools.JavalinTest;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoneyAPIFunctionalTest {
    private Javalin app;
    @BeforeEach
    public void setupJavalin() {
        // Setting up javalin before each test ensures account/withdrawal request data is cleaned up between tests.
        app = Main.javalinApp();
    }

    @Test
    public void transferRequestBodyValidation() {
        JavalinTest.test(app, (server, client) -> {
            assertThat(client.post("/transfer", "{}").code()).isEqualTo(400);
            assertThat(client.post("/transfer", "{ \"from\": \"non-uuid\", \"to\": \"non-uuid\", \"amount\": 30.0 }").code()).isEqualTo(400);
            assertThat(client.post("/transfer", "{ \"from\": \"ada05a6a-6a5c-4ce9-8abc-69a502390795\", \"to\": \"ada05a6a-6a5c-4ce9-8abc-69a502390795\" }").code()).isEqualTo(400);
            assertThat(client.post("/transfer", "{ \"to\": \"ada05a6a-6a5c-4ce9-8abc-69a502390795\", \"amount\": 30.0 }").code()).isEqualTo(400);
            assertThat(client.post("/transfer", "{ \"from\": \"ada05a6a-6a5c-4ce9-8abc-69a502390795\", \"amount\": 30.0 }").code()).isEqualTo(400);
            assertThat(client.post("/transfer", "{ \"from\": \"aaaaaaaa-6a5c-4ce9-8abc-69a502390795\", \"to\": \"bbbbbbbb-6a5c-4ce9-8abc-69a502390795\", \"amount\": 0.0 }").code()).isEqualTo(400);
            assertThat(client.post("/transfer", "{ \"from\": \"aaaaaaaa-6a5c-4ce9-8abc-69a502390795\", \"to\": \"bbbbbbbb-6a5c-4ce9-8abc-69a502390795\", \"amount\": -2.0 }").code()).isEqualTo(400);
        });
    }
    @Test
    public void transferNoAccountFound() {
        JavalinTest.test(app, (server, client) -> {
            // both accounts are non-existent
            assertThat(client.post("/transfer", "{ \"from\": \"aaaaaaaa-6a5c-4ce9-8abc-69a502390795\", \"to\": \"bbbbbbbb-6a5c-4ce9-8abc-69a502390795\", \"amount\": 30.0 }").code()).isEqualTo(404);

            // one of the accounts is non-existent
            Response accountResponse1 = client.post("/open-account?initialBalance=400.0");
            assertThat(accountResponse1.code()).isEqualTo(200);
            String accountId = accountResponse1.body().string();
            String fromAccountIdJson = "{ \"from\": \"%s\", \"to\": \"ada05a6a-6a5c-4ce9-8abc-69a502390795\", \"amount\": 30.0 }".formatted(accountId);
            String toAccountIdJson = "{ \"from\": \"ada05a6a-6a5c-4ce9-8abc-69a502390795\", \"to\": \"%s\", \"amount\": 30.0 }".formatted(accountId);
            assertThat(client.post("/transfer", fromAccountIdJson).code()).isEqualTo(404);
            assertThat(client.post("/transfer", toAccountIdJson).code()).isEqualTo(404);
        });
    }

    @Test
    public void transferToTheSameAccountIsNotPossible() {
        JavalinTest.test(app, (server, client) -> {
            Response accountResponse1 = client.post("/open-account?initialBalance=400.0");
            assertThat(accountResponse1.code()).isEqualTo(200);
            String accountId = accountResponse1.body().string();
            String fromToAccountIdJson = "{ \"from\": \"%s\", \"to\": \"%s\", \"amount\": 30.0 }".formatted(accountId, accountId);
            assertThat(client.post("/transfer", fromToAccountIdJson).code()).isEqualTo(400);
        });
    }

    @Test
    public void transferResultsInCorrectBalance() {
        JavalinTest.test(app, (server, client) -> {
            Response senderAccountResponse = client.post("/open-account?initialBalance=400.0");
            assertThat(senderAccountResponse.code()).isEqualTo(200);
            String senderAccount = senderAccountResponse.body().string();

            Response receiverAccountResponse = client.post("/open-account?initialBalance=400.0");
            assertThat(receiverAccountResponse.code()).isEqualTo(200);
            String receiverAccount = receiverAccountResponse.body().string();

            // request to transfer more than originally was in the account -> rejected
            String fromToAccountIdJson = "{ \"from\": \"%s\", \"to\": \"%s\", \"amount\": 3000.0 }".formatted(senderAccount, receiverAccount);
            assertThat(client.post("/transfer", fromToAccountIdJson).code()).isEqualTo(400);

            // request to transfer a little bit less than the balance -> accepted
            fromToAccountIdJson = "{ \"from\": \"%s\", \"to\": \"%s\", \"amount\": 399.9999999999999 }".formatted(senderAccount, receiverAccount);
            assertThat(client.post("/transfer", fromToAccountIdJson).code()).isEqualTo(200);

            // check sender balance
            Response balanceResponse = client.get("/balance/%s".formatted(senderAccount));
            assertThat(balanceResponse.code()).isEqualTo(200);
            BigDecimal balance = new BigDecimal(balanceResponse.body().string());
            assertEquals(balance, new BigDecimal("0.0000000000001"));

            // check receiver balance
            balanceResponse = client.get("/balance/%s".formatted(receiverAccount));
            assertThat(balanceResponse.code()).isEqualTo(200);
            balance = new BigDecimal(balanceResponse.body().string());
            assertEquals(balance, new BigDecimal("799.9999999999999"));

            // try to transfer a little bit more than currently is in account -> rejected
            fromToAccountIdJson = "{ \"from\": \"%s\", \"to\": \"%s\", \"amount\": 0.0000000000002 }".formatted(senderAccount, receiverAccount);
            assertThat(client.post("/transfer", fromToAccountIdJson).code()).isEqualTo(400);

            // try to transfer exact balance -> accepted
            fromToAccountIdJson = "{ \"from\": \"%s\", \"to\": \"%s\", \"amount\": 0.0000000000001 }".formatted(senderAccount, receiverAccount);
            assertThat(client.post("/transfer", fromToAccountIdJson).code()).isEqualTo(200);

            // check sender balance
            balanceResponse = client.get("/balance/%s".formatted(senderAccount));
            assertThat(balanceResponse.code()).isEqualTo(200);
            balance = new BigDecimal(balanceResponse.body().string());
            assertEquals(balance, new BigDecimal("0.0000000000000"));

            // check receiver balance
            balanceResponse = client.get("/balance/%s".formatted(receiverAccount));
            assertThat(balanceResponse.code()).isEqualTo(200);
            balance = new BigDecimal(balanceResponse.body().string());
            assertEquals(balance, new BigDecimal("800.0000000000000"));

            // try to transfer from an empty account -> rejected
            fromToAccountIdJson = "{ \"from\": \"%s\", \"to\": \"%s\", \"amount\": 0.0000000000001 }".formatted(senderAccount, receiverAccount);
            assertThat(client.post("/transfer", fromToAccountIdJson).code()).isEqualTo(400);

            // return some money
            fromToAccountIdJson = "{ \"from\": \"%s\", \"to\": \"%s\", \"amount\": 20 }".formatted(receiverAccount, senderAccount);
            assertThat(client.post("/transfer", fromToAccountIdJson).code()).isEqualTo(200);

            // check sender balance
            balanceResponse = client.get("/balance/%s".formatted(senderAccount));
            assertThat(balanceResponse.code()).isEqualTo(200);
            balance = new BigDecimal(balanceResponse.body().string());
            assertEquals(balance, new BigDecimal("20.0000000000000"));

            // check receiver balance
            balanceResponse = client.get("/balance/%s".formatted(receiverAccount));
            assertThat(balanceResponse.code()).isEqualTo(200);
            balance = new BigDecimal(balanceResponse.body().string());
            assertEquals(balance, new BigDecimal("780.0000000000000"));
        });
    }

    @Test
    public void withdrawRequestBodyValidation() {
        JavalinTest.test(app, (server, client) -> {
            assertThat(client.post("/withdraw", "{}").code()).isEqualTo(400);
            assertThat(client.post("/withdraw", "{ \"accountId\": \"non-uuid\", \"withdrawalAddress\": \"address\", \"amount\": 30.0 }").code()).isEqualTo(400);
            assertThat(client.post("/withdraw", "{ \"accountId\": \"ada05a6a-6a5c-4ce9-8abc-69a502390795\", \"withdrawalAddress\": \"\", \"amount\": 30.0 }").code()).isEqualTo(400);
            assertThat(client.post("/withdraw", "{ \"accountId\": \"ada05a6a-6a5c-4ce9-8abc-69a502390795\", \"amount\": 30.0 }").code()).isEqualTo(400);
            assertThat(client.post("/withdraw", "{ \"withdrawalAddress\": \"address\", \"amount\": 30.0 }").code()).isEqualTo(400);
            assertThat(client.post("/withdraw", "{ \"accountId\": \"ada05a6a-6a5c-4ce9-8abc-69a502390795\", \"withdrawalAddress\": \"address\" }").code()).isEqualTo(400);
            assertThat(client.post("/withdraw", "{ \"accountId\": \"ada05a6a-6a5c-4ce9-8abc-69a502390795\", \"withdrawalAddress\": \"address\", \"amount\": -30.0 }").code()).isEqualTo(400);
        });
    }

    @Test
    public void withdrawNoAccountFound() {
        JavalinTest.test(app, (server, client) -> {
            assertThat(client.post("/withdraw", "{ \"accountId\": \"ada05a6a-6a5c-4ce9-8abc-69a502390795\", \"withdrawalAddress\": \"address\", \"amount\": 30.0 }").code()).isEqualTo(404);
        });
    }

    @Test
    public void withdrawResultsInCorrectBalance() {
        JavalinTest.test(app, (server, client) -> {
            Response accountResponse = client.post("/open-account?initialBalance=400.0");
            assertThat(accountResponse.code()).isEqualTo(200);
            String accountId = accountResponse.body().string();

            // check that can't withdraw more than in the account
            String withdrawRequest = "{ \"accountId\": \"%s\", \"withdrawalAddress\": \"address\", \"amount\": 500.0 }".formatted(accountId);
            Response withdrawResponse = client.post("/withdraw", withdrawRequest);
            assertThat(withdrawResponse.code()).isEqualTo(400);

            // check that successful withdrawal takes money from the account
            while (executeWithdrawRequest(client, accountId, BigDecimal.valueOf(30.0)) != WithdrawalService.WithdrawalState.COMPLETED) {}

            Response balanceResponse = client.get("/balance/%s".formatted(accountId));
            assertThat(balanceResponse.code()).isEqualTo(200);
            BigDecimal newBalance = new BigDecimal(balanceResponse.body().string());
            BigDecimal expectedNewBalance = new BigDecimal("370.0");
            assertEquals(newBalance, expectedNewBalance);

            // check that failed withdrawal does not take money from the account
            BigDecimal withdrawAmount = new BigDecimal("0.00000000001");
            int successfulWithdrawals = 0;
            while (executeWithdrawRequest(client, accountId, withdrawAmount) != WithdrawalService.WithdrawalState.FAILED) {
                successfulWithdrawals++;
            }
            BigDecimal resultingDelta = withdrawAmount.multiply(BigDecimal.valueOf(successfulWithdrawals));

            // Waits for WithdrawalRequestWatcher to run.
            // Alternatively, can add method to WithdrawalRequestWatcher that would force it to go through the queue,
            // but for the sake of one test it looks like an overkill.
            Thread.sleep(100);

            balanceResponse = client.get("/balance/%s".formatted(accountId));
            assertThat(balanceResponse.code()).isEqualTo(200);
            BigDecimal balanceAfterFirstFailure = new BigDecimal(balanceResponse.body().string());
            assertEquals(balanceAfterFirstFailure, newBalance.subtract(resultingDelta));
        });
    }

    @Test
    public void withdrawalStateParamValidation() {
        JavalinTest.test(app, (server, client) -> {
            // javalin routing does not match path param, so it's 404 instead of 400
            assertThat(client.get("/withdraw/%s/state".formatted("")).code()).isEqualTo(404);
            assertThat(client.get("/withdraw/%s/state".formatted("non-uuid")).code()).isEqualTo(400);
        });
    }

    @Test
    public void withdrawalStateNoWithdrawalFound() {
        JavalinTest.test(app, (server, client) -> {
            assertThat(client.get("/withdraw/%s/state".formatted(UUID.randomUUID())).code()).isEqualTo(404);
        });
    }

    private static WithdrawalService.WithdrawalState executeWithdrawRequest(HttpClient client, String accountId, BigDecimal amount) throws IOException {
        String withdrawRequest = "{ \"accountId\": \"%s\", \"withdrawalAddress\": \"address\", \"amount\": %s }".formatted(accountId, amount);
        Response withdrawResponse = client.post("/withdraw", withdrawRequest);
        assertThat(withdrawResponse.code()).isEqualTo(200);
        ObjectMapper objectMapper = new ObjectMapper();
        WithdrawalRequestResponse withdrawRequestResponse = objectMapper.readValue(withdrawResponse.body().string(), WithdrawalRequestResponse.class);
        return waitWithdrawalFinalState(client, withdrawRequestResponse, withdrawResponse, objectMapper);
    }

    private static WithdrawalService.WithdrawalState waitWithdrawalFinalState(HttpClient client, WithdrawalRequestResponse withdrawRequestResponse, Response withdrawResponse, ObjectMapper objectMapper) throws IOException {
        WithdrawalService.WithdrawalState state = WithdrawalService.WithdrawalState.PROCESSING;
        while (state == WithdrawalService.WithdrawalState.PROCESSING) {
            Response withdrawStateResponse = client.get("/withdraw/%s/state".formatted(withdrawRequestResponse.withdrawalId()));
            assertThat(withdrawResponse.code()).isEqualTo(200);
            WithdrawalStateResponse withdrawalStateResponse = objectMapper.readValue(withdrawStateResponse.body().string(), WithdrawalStateResponse.class);
            state = withdrawalStateResponse.state();
        }
        return state;
    }
}
