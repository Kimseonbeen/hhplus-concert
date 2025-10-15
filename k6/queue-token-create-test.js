import http from "k6/http";
import { sleep, check } from "k6";

export let options = {
  scenarios: {
    initial_spike: {
      executor: 'ramping-arrival-rate',
      startRate: 300,     // 시작 시 높은 TPS (300 요청/초)
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 500,
      stages: [
        { duration: '5s', target: 300 },   // 처음 5초 동안 높은 부하 유지
        { duration: '10s', target: 150 },  // 다음 10초 동안 50%로 감소
        { duration: '20s', target: 100 },  // 다음 20초 동안 추가 감소
        { duration: '25s', target: 50 },   // 마지막 25초 동안 낮은 부하
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<750'], // 응답 시간 임계값
    http_req_failed: ['rate<0.05'],   // 최대 5%의 실패율 허용
  },
};
export default function () {
  const response1 = http.post(
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
  let token = response1.json().token;
  check(response1, { "토큰 발급 성공": (r) => r.status === 200 });
}