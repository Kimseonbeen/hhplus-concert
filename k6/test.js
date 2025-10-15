// GET
import http from "k6/http";
import { sleep, check } from "k6";

export const options = {
  scenarios: {
    spike_test: {
      executor: 'ramping-arrival-rate',
      preAllocatedVUs: 100,      // 미리 할당할 VU 수
      maxVUs: 500,               // 최대 VU 수
      stages: [
        { duration: '30s', target: 10 },    // 워밍업: 초당 10개 요청으로 서서히 증가
        { duration: '1m', target: 100 },    // 급증: 빠르게 초당 100개 요청으로 증가
        { duration: '2m', target: 500 },    // 피크: 초당 500개 요청으로 급격히 증가
        { duration: '3m', target: 500 },    // 유지: 피크 부하 유지
        { duration: '1m', target: 0 }       // 감소: 서서히 트래픽 감소
      ],
      timeUnit: '1s',            // 시간 단위 설정
      startRate: 0,              // 시작 요청 비율
      gracefulStop: '30s'        // 종료 시 유예 시간
    }
  }
};

export default function () {
  const response = http.post(
      "http://localhost:8080/api/queue/token",
      JSON.stringify({
        userId: 1
      }),
      {
        headers: {
          'Content-Type': 'application/json'
        }
      }
  );
  const token = response.json().token;
  check(response, { "토큰 발급 성공": (r) => r.status === 200 });

  sleep(1);
}