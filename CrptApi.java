package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;
    private final ObjectMapper objectMapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.objectMapper = new ObjectMapper();

        long delay = timeUnit.toMillis(1);
        scheduler.scheduleAtFixedRate(() -> semaphore.release(requestLimit - semaphore.availablePermits()),
                delay, delay, TimeUnit.MILLISECONDS);
    }

    public MessageSendingResult createDocument(Document document, String signature) throws InterruptedException, IOException {
        semaphore.acquire();
        try {
            return sendPostRequest(document, signature);
        } finally {
            semaphore.release();
        }
    }

    private MessageSendingResult sendPostRequest(Document document, String signature) throws IOException {
        URL url = new URL("https://ismp.crpt.ru/api/v3/lk/documents/create");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("signature", signature);
        connection.setDoOutput(true);

        String jsonInputString = objectMapper.writeValueAsString(document);
        try (var os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int result = connection.getResponseCode();
        try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = reader.readLine()) != null) {
                response.append(responseLine);
            }
            return new MessageSendingResult(result, response.toString());
        }
    }

    public static class MessageSendingResult {

        public MessageSendingResult(int result, String response) {
            this.result = result;
            this.response = response;
        }

        private int result;

        private String response;
    }

    // В примере тела запроса в тестовом задании ошибка. Для корректности исключил значение 109
    public static class Document {
        private String description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private List<Product> products = new ArrayList<>();
        private String regDate;
        private String regNumber;

        // getters and setters

    }

    public static class Product {
        private String certificateDocument;
        private String certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;

        // getters and setters
    }
}

