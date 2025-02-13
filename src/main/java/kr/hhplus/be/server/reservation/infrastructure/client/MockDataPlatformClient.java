package kr.hhplus.be.server.reservation.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockDataPlatformClient implements DataPlatformClient {
    @Override
    public void sendReservationData(Long paymentId) {
        log.info("데이터 플랫폼 API 호출 {}", paymentId);
    }
}
