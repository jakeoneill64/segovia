package com.segovia.repository;

import com.jasongoodwin.monads.Try;
import com.segovia.model.DetailedPaymentResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Repository
public class FSPaymentRepository implements PaymentRepository{

    private final CSVPrinter printer;
    private final Logger logger = LoggerFactory.getLogger(FSPaymentRepository.class);
    private final Set<String> processedIds = new HashSet<>();

    private FSPaymentRepository(@Value("${application.output-file}") String outputFile){

        printer = Try.ofFailable(() -> CSVFormat
                .DEFAULT
                .builder()
                .setHeader("ID","Server-generated ID", "Status", "Fee","Details")
                .build()
                .print(new File(outputFile), StandardCharsets.UTF_8)
        )
        .getUnchecked(); // if this fails we can't recover so no point to catch.

    }


    @Override
    public void insert(DetailedPaymentResponse response) {

        if(processedIds.contains(response.getCustomerReference()))
            return;

        String status;
        if(response.getStatus() > 0)
            status = "failed";
        else if(response.getStatus() == 0)
            status = "succeeded";
        else
            status = "unknown";


        try {

            synchronized (printer) {

                printer.printRecord(
                        response.getCustomerReference(),
                        Optional.ofNullable(response.getReference()).orElse(""),
                        status,
                        Optional.ofNullable(response.getFee()).map(BigDecimal::toString).orElse(""),
                        response.getMessage()
                );

                printer.flush();

            }

        } catch (IOException e) {
            logger.error("could not write record with id ".concat(response.getCustomerReference()));
            return;
        }

    }

    @PreDestroy
    private void releaseResources() throws IOException {
        printer.flush();
        printer.close();
    }

}
