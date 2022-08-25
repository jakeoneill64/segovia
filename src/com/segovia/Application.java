package com.segovia;

import com.jasongoodwin.monads.Try;
import com.segovia.model.ApiCredentials;
import com.segovia.model.PaymentRequest;
import com.segovia.service.AsyncPaymentService;
import org.apache.commons.csv.CSVFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.io.FileReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;


@SpringBootApplication
public class Application {

    @Autowired
    private AsyncPaymentService paymentService;

    public static void main(String[] args){
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(
                                @Value("${application.input-file}") String inputFile,
                                @Value("${application.callback-url}") String callbackUrl,
                                @Value("${credentials.account-id}") String accountId,
                                @Value("${credentials.key}") String key
    ) {
        return args -> CSVFormat.DEFAULT.parse(new FileReader(inputFile, StandardCharsets.UTF_8))
            .stream()
            .map((csvRow) -> Try.ofFailable( // try monad which allows us to discard any invalid rows
                                () -> new PaymentRequest(
                                    csvRow.get("Recipient"),
                                    new BigDecimal(csvRow.get("Amount")),
                                    csvRow.get("Currency"),
                                    csvRow.get("ID"),
                                    callbackUrl
                                )
                             )
            )
            .filter(Try::isSuccess)
            .map(Try::getUnchecked)
            .forEach((paymentRequest -> paymentService.process(paymentRequest, new ApiCredentials(accountId, key))));
    }
}
