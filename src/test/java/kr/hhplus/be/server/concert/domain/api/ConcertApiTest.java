package kr.hhplus.be.server.concert.domain.api;

import kr.hhplus.be.server.concert.presentation.dto.response.ConcertScheduleResponse;
import kr.hhplus.be.server.concert.presentation.dto.response.ConcertSeatAvailableResponse;
import kr.hhplus.be.server.queueToken.presentation.dto.request.QueueTokenRequest;
import kr.hhplus.be.server.queueToken.presentation.dto.response.QueueTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;

public class ConcertApiTest {
    RestClient restClient = RestClient.create("http://localhost:8080");
    String token;

    @BeforeEach
    void setUp() {
        // 토큰 생성
        QueueTokenRequest request = new QueueTokenRequest(1L);

        QueueTokenResponse queueTokenResponse = restClient.post()
                .uri("/api/queue/token")
                .body(request)
                .retrieve()
                .body(QueueTokenResponse.class);

        token = queueTokenResponse.token();
    }

    @Test
    void ReservedDateReadTest() {
        List<ConcertScheduleResponse> responses = ReservedDateRead(1L);
        System.out.println("responses " + responses);
    }

    List<ConcertScheduleResponse> ReservedDateRead(Long concertId) {
        return restClient.get()
                .uri("/api/concert/{concertId}/schedules", concertId)
                .header("TOKEN",token)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ConcertScheduleResponse>>() {
                });
    }

    @Test
    void ReservedSeatsReadTest() {
        ConcertSeatAvailableResponse response = ReservedSeatsRead(557029L);
        System.out.println("response = " + response);
    }

    ConcertSeatAvailableResponse ReservedSeatsRead(Long concertScheduleId) {
        return restClient.get()
                .uri("/api/concert/{concertScheduleId}/seats", concertScheduleId)
                .header("TOKEN", token)
                .retrieve()
                .body(ConcertSeatAvailableResponse.class);
    }
}
