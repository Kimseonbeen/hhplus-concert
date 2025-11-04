package kr.hhplus.be.server.queueToken.domain.api;

import kr.hhplus.be.server.queueToken.presentation.dto.request.QueueTokenRequest;
import kr.hhplus.be.server.queueToken.presentation.dto.response.QueueTokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QueueTokenApiTest {

    RestClient restClient = RestClient.create("http://localhost:8080/api/queue");

    final static long USER_ID = 1L;

    @Test
    void createTest() {
        QueueTokenResponse queueTokenResponse = create(new QueueTokenRequest(USER_ID));
        System.out.println("queueTokenResponse = " + queueTokenResponse);
    }

    @Test
    void createMaxTest() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(1000);

        for (int i = 0; i < 1000; i++) {
            executorService.submit(() -> {
                create(new QueueTokenRequest(USER_ID));
                latch.countDown();
            });
        }
        latch.await();
    }

    QueueTokenResponse create(QueueTokenRequest request) {
        return restClient.post()
                .uri("/token")
                .body(request)
                .retrieve()
                .body(QueueTokenResponse.class);
    }
}
