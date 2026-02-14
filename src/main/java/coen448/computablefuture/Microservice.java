package coen448.computablefuture;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Microservice {
    private final String serviceId;

    public Microservice(String serviceId) {
        this.serviceId = serviceId;
    }

    public CompletableFuture<String> retrieveAsync(String input) {
        return CompletableFuture.supplyAsync(() -> {
            int delayMs = ThreadLocalRandom.current().nextInt(0, 31);
            try {
                TimeUnit.MILLISECONDS.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            return serviceId + ":" + input.toUpperCase();
        });
    }
}
