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
  // 여러 사용자 ID 중 하나를 무작위로 선택 (더 현실적인 시나리오)
  const userIds = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
  const userId = userIds[Math.floor(Math.random() * userIds.length)];

  const amount = 1000;
  const chargeResponse = http.post(
    `http://localhost:8080/api/balance/${userId}/charge`,
    JSON.stringify(amount),
    {
      headers: {
        'Content-Type': 'application/json'
      }
    }
  );

  // 성공 응답(200)과 락으로 인한 충돌 응답(예: 409) 모두 허용
  check(chargeResponse, {
    "잔액 충전 처리됨": (r) => r.status === 200 || r.status === 409
  });

  sleep(Math.random() * 3); // 0-3초 사이의 랜덤 대기 시간
}