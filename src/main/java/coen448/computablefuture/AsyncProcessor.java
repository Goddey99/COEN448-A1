package coen448.computablefuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AsyncProcessor {
    public CompletableFuture<String> processAsync(List<Microservice> microservices, String message) {
        List<CompletableFuture<String>> futures = microservices.stream()
            .map(client -> client.retrieveAsync(message))
            .collect(Collectors.toList());
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.joining(" ")));
    }

    public CompletableFuture<List<String>> processAsyncCompletionOrder(
            List<Microservice> microservices, String message) {
        List<String> completionOrder =
            Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = microservices.stream()
            .map(ms -> ms.retrieveAsync(message)
                .thenAccept(completionOrder::add))
            .collect(Collectors.toList());
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> completionOrder);
    }

    // Fail-Fast (Atomic Policy)
    public CompletableFuture<String> processAsyncFailFast(
            List<Microservice> services,
            List<String> messages) {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < services.size(); i++) {
            Microservice ms = services.get(i);
            String msg = messages.get(i);
            futures.add(ms.retrieveAsync(msg));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.joining(", ")));
    }

    // Fail-Partial (Best-Effort Policy)
    public CompletableFuture<List<String>> processAsyncFailPartial(
            List<Microservice> services,
            List<String> messages) {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < services.size(); i++) {
            Microservice ms = services.get(i);
            String msg = messages.get(i);
            futures.add(ms.retrieveAsync(msg)
                .handle((result, ex) -> ex == null ? result : null));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .filter(r -> r != null)
                .collect(Collectors.toList()));
    }

    // Fail-Soft (Fallback Policy)
    public CompletableFuture<String> processAsyncFailSoft(
            List<Microservice> services,
            List<String> messages,
            String fallbackValue) {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < services.size(); i++) {
            Microservice ms = services.get(i);
            String msg = messages.get(i);
            futures.add(ms.retrieveAsync(msg)
                .exceptionally(ex -> fallbackValue));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.joining(", ")));
    }
}
