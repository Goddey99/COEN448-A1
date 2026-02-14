package coen448.computablefuture.test;


import coen448.computablefuture.AsyncProcessor;
import coen448.computablefuture.Microservice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.RepeatedTest;

public class AsyncProcessorTest {
        @Test
        void testFailFast_policy_propagatesException() {
            Microservice ok = new Microservice("OK");
            Microservice fail = new Microservice("FAIL") {
                @Override
                public CompletableFuture<String> retrieveAsync(String message) {
                    CompletableFuture<String> f = new CompletableFuture<>();
                    f.completeExceptionally(new RuntimeException("Service failed"));
                    return f;
                }
            };
            AsyncProcessor processor = new AsyncProcessor();
            List<Microservice> services = List.of(ok, fail);
            List<String> messages = List.of("msg1", "msg2");
            CompletableFuture<String> future = processor.processAsyncFailFast(services, messages);
            assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS));
        }

        @Test
        void testFailPartial_policy_returnsPartialResults() throws Exception {
            Microservice ok = new Microservice("OK");
            Microservice fail = new Microservice("FAIL") {
                @Override
                public CompletableFuture<String> retrieveAsync(String message) {
                    CompletableFuture<String> f = new CompletableFuture<>();
                    f.completeExceptionally(new RuntimeException("Service failed"));
                    return f;
                }
            };
            AsyncProcessor processor = new AsyncProcessor();
            List<Microservice> services = List.of(ok, fail);
            List<String> messages = List.of("msg1", "msg2");
            CompletableFuture<List<String>> future = processor.processAsyncFailPartial(services, messages);
            List<String> result = future.get(1, TimeUnit.SECONDS);
            assertEquals(1, result.size());
            assertTrue(result.get(0).startsWith("OK:"));
        }

        @Test
        void testFailSoft_policy_usesFallback() throws Exception {
            Microservice ok = new Microservice("OK");
            Microservice fail = new Microservice("FAIL") {
                @Override
                public CompletableFuture<String> retrieveAsync(String message) {
                    CompletableFuture<String> f = new CompletableFuture<>();
                    f.completeExceptionally(new RuntimeException("Service failed"));
                    return f;
                }
            };
            AsyncProcessor processor = new AsyncProcessor();
            List<Microservice> services = List.of(ok, fail);
            List<String> messages = List.of("msg1", "msg2");
            String fallback = "FALLBACK";
            CompletableFuture<String> future = processor.processAsyncFailSoft(services, messages, fallback);
            String result = future.get(1, TimeUnit.SECONDS);
            assertTrue(result.contains("OK:"));
            assertTrue(result.contains(fallback));
        }

        @Test
        void testLiveness_noDeadlockOrInfiniteWait() throws Exception {
            Microservice slow = new Microservice("SLOW") {
                @Override
                public CompletableFuture<String> retrieveAsync(String message) {
                    return CompletableFuture.supplyAsync(() -> {
                        try { Thread.sleep(100); } catch (InterruptedException e) {}
                        return "SLOW:" + message.toUpperCase();
                    });
                }
            };
            Microservice ok = new Microservice("OK");
            AsyncProcessor processor = new AsyncProcessor();
            List<Microservice> services = List.of(ok, slow);
            List<String> messages = List.of("msg1", "msg2");
            CompletableFuture<String> fast = processor.processAsyncFailFast(services, messages);
            CompletableFuture<List<String>> partial = processor.processAsyncFailPartial(services, messages);
            CompletableFuture<String> soft = processor.processAsyncFailSoft(services, messages, "FALLBACK");
            assertDoesNotThrow(() -> fast.get(2, TimeUnit.SECONDS));
            assertDoesNotThrow(() -> partial.get(2, TimeUnit.SECONDS));
            assertDoesNotThrow(() -> soft.get(2, TimeUnit.SECONDS));
        }
    @RepeatedTest(5)
    public void testProcessAsyncSuccess() throws ExecutionException, InterruptedException {
        Microservice service1 = new Microservice("Hello") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                return CompletableFuture.completedFuture("Hello");
            }
        };
        Microservice service2 = new Microservice("World") {
            @Override
            public CompletableFuture<String> retrieveAsync(String input) {
                return CompletableFuture.completedFuture("World");
            }
        };
        AsyncProcessor processor = new AsyncProcessor();
        CompletableFuture<String> resultFuture = processor.processAsync(List.of(service1, service2), null);
        String result = resultFuture.get();
        assertEquals("Hello World", result);
    }
	
	
	@ParameterizedTest
    @CsvSource({
        "hi, Hello:HI World:HI",
        "cloud, Hello:CLOUD World:CLOUD",
        "async, Hello:ASYNC World:ASYNC"
    })
    public void testProcessAsync_withDifferentMessages(
            String message,
            String expectedResult)
            throws ExecutionException, InterruptedException, TimeoutException {

        Microservice service1 = new Microservice("Hello");
        Microservice service2 = new Microservice("World");

        AsyncProcessor processor = new AsyncProcessor();

        CompletableFuture<String> resultFuture =
            processor.processAsync(List.of(service1, service2), message);

        String result = resultFuture.get(1, TimeUnit.SECONDS);

        assertEquals(expectedResult, result);
        
    }
	
	
	@RepeatedTest(20)
    void showNondeterminism_completionOrderVaries() throws Exception {

        Microservice s1 = new Microservice("A");
        Microservice s2 = new Microservice("B");
        Microservice s3 = new Microservice("C");

        AsyncProcessor processor = new AsyncProcessor();

        List<String> order = processor
            .processAsyncCompletionOrder(List.of(s1, s2, s3), "msg")
            .get(1, TimeUnit.SECONDS);

        // Not asserting a fixed order (because it is intentionally nondeterministic)
        System.out.println(order);

        // A minimal sanity check: all three must be present
        assertEquals(3, order.size());
   
        assertTrue(order.stream().anyMatch(x -> x.startsWith("A:")));
        assertTrue(order.stream().anyMatch(x -> x.startsWith("B:")));
        assertTrue(order.stream().anyMatch(x -> x.startsWith("C:")));
    }
}
	