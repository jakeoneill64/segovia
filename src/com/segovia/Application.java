package com.segovia;

import com.jasongoodwin.monads.Try;
import com.segovia.model.PaymentRequest;
import com.segovia.service.AsyncPaymentService;
import org.apache.commons.csv.CSVFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

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
    @Profile("production")
    public CommandLineRunner commandLineRunner(
                                @Value("${application.input-file}") String inputFile,
                                @Value("${application.callback-url}") String callbackUrl
    ) {
        return args -> CSVFormat
            .DEFAULT
            .builder()
            .setHeader("ID","Recipient","Amount","Currency")
            .build()
            .parse(new FileReader(inputFile, StandardCharsets.UTF_8))
            .stream()
            .skip(1)
            .map((csvRow) -> Try.ofFailable(
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
            .forEach((paymentRequest -> paymentService.process(paymentRequest)));
    }
}
