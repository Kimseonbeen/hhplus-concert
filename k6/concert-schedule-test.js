import http from "k6/http";
import { sleep, check } from "k6";

// 전역 변수로 토큰 저장
let token;

// 토큰을 한 번만 생성하는 setup 함수
export function setup() {
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

  check(response, { "토큰 발급 성공": (r) => r.status === 200 });

  return response.json().token;
}

export let options = {
  stages: [
    { duration: '10s', target: 50 },
    { duration: '10s', target: 100 },
    { duration: '10s', target: 50 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  }
};

export default function (data) {
  // setup 함수에서 반환한 토큰 사용
  const token = data;

  const response2 = http.get(
    "http://localhost:8080/api/concert/999/schedules",
    {
      headers: {
        'TOKEN': token,
        'Content-Type': 'application/json'
      }
    }
  );
  check(response2, { "스케쥴 조회 성공": (r) => r.status === 200 });

  sleep(1);
}