package com.segovia;

import com.jasongoodwin.monads.Try;
import com.segovia.controller.PaymentController;
import com.segovia.model.DetailedPaymentResponse;
import com.segovia.model.PaymentRequest;
import com.segovia.repository.PaymentRepository;
import com.segovia.service.AsyncPaymentServiceImpl;
import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IntegrationTests {

    @Autowired
    private AsyncPaymentServiceImpl paymentService;
    @Value("${application.callback-url}")
    String callback;

    @Test
    public void successAndDuplicateIntegration() throws IOException, NoSuchFieldException, IllegalAccessException, InterruptedException {

        //atomic reference to get around lambda restrictions for test
        AtomicReference<DetailedPaymentResponse> outcome = new AtomicReference<>();

        PaymentRequest request = processCsv("test/sample-inputs/success.csv", outcome);

        Thread.sleep(5000);

        DetailedPaymentResponse paymentResponse = outcome.get();

        Assertions.assertNotNull(paymentResponse);
        Assertions.assertEquals(request.reference(), paymentResponse.getCustomerReference());
        Assertions.assertEquals(0, paymentResponse.getStatus());

        processCsv("test/sample-inputs/success.csv", outcome);

        Thread.sleep(5000);

        paymentResponse = outcome.get();

        Assertions.assertNotNull(paymentResponse);
        Assertions.assertTrue(paymentResponse.getStatus() > 0);

    }

    @Test
    public void successNoCallbackIntegration() throws IOException, NoSuchFieldException, IllegalAccessException, InterruptedException {

        //atomic reference to get around lambda restrictions for test
        AtomicReference<DetailedPaymentResponse> outcome = new AtomicReference<>();

        PaymentRequest request = processCsv("test/sample-inputs/success-no-callback.csv", outcome);

        Thread.sleep(5000);

        DetailedPaymentResponse paymentResponse = outcome.get();

        Assertions.assertNotNull(paymentResponse);
        Assertions.assertEquals(request.reference(), paymentResponse.getCustomerReference());
        Assertions.assertEquals(0, paymentResponse.getStatus());

    }


    @Test
    public void closeConnectionIntegration() throws IOException, NoSuchFieldException, IllegalAccessException, InterruptedException {
        //atomic reference to get around lambda restrictions for test
        AtomicReference<DetailedPaymentResponse> outcome = new AtomicReference<>();

        PaymentRequest request = processCsv("test/sample-inputs/close-connection.csv", outcome);

        Thread.sleep(5000);

        DetailedPaymentResponse paymentResponse = outcome.get();

        Assertions.assertNotNull(paymentResponse);
        Assertions.assertEquals(-1, paymentResponse.getStatus());
        Assertions.assertEquals(request.reference(), paymentResponse.getCustomerReference());
    }

    @Test
    public void delayedSuccessIntegration() throws IOException, NoSuchFieldException, IllegalAccessException, InterruptedException {

        //atomic reference to get around lambda restrictions for test
        AtomicReference<DetailedPaymentResponse> outcome = new AtomicReference<>();

        PaymentRequest paymentRequest = processCsv("test/sample-inputs/delayed-success.csv", outcome);

        Thread.sleep(500_000L);

        DetailedPaymentResponse output = outcome.get();

        Assertions.assertNotNull(output);
        Assertions.assertEquals(paymentRequest.reference(), output.getCustomerReference());
        Assertions.assertEquals(0, output.getStatus());

    }

    @Test
    public void internalErrorIntegration() throws IOException, NoSuchFieldException, IllegalAccessException, InterruptedException {

        //atomic reference to get around lambda restrictions for test
        AtomicReference<DetailedPaymentResponse> outcome = new AtomicReference<>();

        PaymentRequest request = processCsv("test/sample-inputs/internal-error.csv", outcome);

        Thread.sleep(5000);

        DetailedPaymentResponse detailedPaymentResponse = outcome.get();

        Assertions.assertNotNull(detailedPaymentResponse);

        Assertions.assertEquals(detailedPaymentResponse.getCustomerReference(), request.reference());
        Assertions.assertTrue(detailedPaymentResponse.getStatus() < 0);

    }


    @Test
    public void malformedResponseIntegration() throws IOException, NoSuchFieldException, IllegalAccessException, InterruptedException {
        //atomic reference to get around lambda restrictions for test
        AtomicReference<DetailedPaymentResponse> outcome = new AtomicReference<>();

        PaymentRequest request = processCsv("test/sample-inputs/malformed.csv", outcome);

        Thread.sleep(5000);

        DetailedPaymentResponse detailedPaymentResponse = outcome.get();

        Assertions.assertNotNull(detailedPaymentResponse);

        Assertions.assertEquals(detailedPaymentResponse.getCustomerReference(), request.reference());
        Assertions.assertTrue(detailedPaymentResponse.getStatus() < 0);

    }

    @Test
    public void walletFullIntegration() throws IOException, NoSuchFieldException, IllegalAccessException, InterruptedException {

        //atomic reference to get around lambda restrictions for test
        AtomicReference<DetailedPaymentResponse> outcome = new AtomicReference<>();

        PaymentRequest request = processCsv("test/sample-inputs/wallet-full.csv", outcome);

        Thread.sleep(5000);

        DetailedPaymentResponse detailedPaymentResponse = outcome.get();

        Assertions.assertNotNull(detailedPaymentResponse);

        Assertions.assertEquals(detailedPaymentResponse.getCustomerReference(), request.reference());
        Assertions.assertTrue(detailedPaymentResponse.getStatus() > 0);

    }

    private PaymentRequest processCsv(String filename, AtomicReference<DetailedPaymentResponse> pendingOutcome) throws IOException, NoSuchFieldException, IllegalAccessException {
        PaymentRequest request = csvToRequest(filename);
        //atomic reference to get around lambda restrictions for test

        PaymentRepository testPaymentRepo = pendingOutcome::set;

        Field repositoryField = AsyncPaymentServiceImpl.class.getDeclaredField("paymentRepository");
        repositoryField.setAccessible(true);
        repositoryField.set(paymentService, testPaymentRepo);

        new Thread(() -> paymentService.process(request)).start();

        return request;
    }


    private PaymentRequest csvToRequest(String filename) throws IOException {
        return CSVFormat
                .DEFAULT
                .builder()
                .setHeader("ID","Recipient","Amount","Currency")
                .build()
                .parse(new FileReader(filename, StandardCharsets.UTF_8))
                .stream()
                .skip(1)
                .map((csvRow) -> Try.ofFailable(
                                () -> new PaymentRequest(
                                        csvRow.get("Recipient"),
                                        new BigDecimal(csvRow.get("Amount")),
                                        csvRow.get("Currency"),
                                        csvRow.get("ID"),
                                        callback
                                )
                        )
                )
                .filter(Try::isSuccess)
                .map(Try::getUnchecked)
                .findFirst()
                .get();

    }



}