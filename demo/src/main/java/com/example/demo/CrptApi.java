package com.example.demo;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class CrptApi {
    private final AtomicInteger requestCounter;
    private final Lock lock;
    private volatile long lastRequestTime;
    @Value("${crpt.timeUnit}")
    private TimeUnit timeUnit;

    @Value("${crpt.requestLimit}")
    private int requestLimit;

    public CrptApi() {
        this.requestCounter = new AtomicInteger(0);
        this.lock = new ReentrantLock();
        this.lastRequestTime = System.currentTimeMillis();
    }

    public boolean makeApiRequest() {
        lock.lock();
        try {
            if (requestLimit <= 0)
                throw new IllegalStateException("requestLimit should be positive");
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastRequestTime;
            long timeLimitMillis = timeUnit.toMillis(1);

            if (elapsedTime >= timeLimitMillis) {
                requestCounter.set(0);
                lastRequestTime = currentTime;
            }
            if (requestCounter.get() >= requestLimit) {
                return false;
            }
            requestCounter.incrementAndGet();
            return true;
        } finally {
            lock.unlock();
        }
    }
}

class Product {
    @JsonProperty("certificate_document")
    private String certificateDocument;

    @JsonProperty("certificate_document_date")
    private String certificateDocumentDate;

    @JsonProperty("certificate_document_number")
    private String certificateDocumentNumber;

    @JsonProperty("owner_inn")
    private String ownerInn;

    @JsonProperty("producer_inn")
    private String producerInn;

    @JsonProperty("production_date")
    private String productionDate;

    @JsonProperty("tnved_code")
    private String tnvedCode;

    @JsonProperty("uit_code")
    private String uitCode;

    @JsonProperty("uitu_code")
    private String uituCode;
}

class Document {
    @JsonProperty("doc_id")
    private String docId;

    @JsonProperty("doc_status")
    private String docStatus;

    @JsonProperty("doc_type")
    private String docType;

    @JsonProperty("importRequest")
    private boolean importRequest;

    @JsonProperty("owner_inn")
    private String ownerInn;

    @JsonProperty("participant_inn")
    private String participantInn;

    @JsonProperty("producer_inn")
    private String producerInn;

    @JsonProperty("production_date")
    private String productionDate;

    @JsonProperty("production_type")
    private String productionType;

    @JsonProperty("products")
    private List<Product> products;

    @JsonProperty("reg_date")
    private String regDate;

    @JsonProperty("reg_number")
    private String regNumber;

    public Document() {}
}

@RestController
class DocumentController {
    private final CrptApi crptApi;
    DocumentController(CrptApi crptApi) {
        this.crptApi = crptApi;
    }

    @PostMapping("/api/v3/lk/documents/create")
    public ResponseEntity<String> createDocument(@RequestBody Document document) {
         if (crptApi.makeApiRequest()) {
            return ResponseEntity.ok("Document created successfully" + document);
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("API request limit exceeded");
    }
}
