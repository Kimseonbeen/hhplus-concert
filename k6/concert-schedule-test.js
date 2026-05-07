import http from "k6/http";
import { sleep, check } from "k6";

export function setup() {
  // 토큰 발급
  const res = http.post(
    "http://localhost:8080/api/queue/token",
    JSON.stringify({ userId: 1 }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(res, { "토큰 발급 성공": (r) => r.status === 200 });
  const token = res.json().token;

  // ACTIVE 될 때까지 polling (최대 60초)
  for (let i = 0; i < 12; i++) {
    sleep(5);
    const statusRes = http.get("http://localhost:8080/api/queue/status", {
      headers: { 'TOKEN': token }
    });
    const status = statusRes.json().status;
    console.log(`토큰 상태: ${status} (${(i + 1) * 5}초 경과)`);
    if (status === "ACTIVE") break;
  }

  return token;
}

export let options = {
  scenarios: {
    schedule_load: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 200,
      stages: [
        { duration: '10s', target: 50 },
        { duration: '10s', target: 100 },
        { duration: '10s', target: 50 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  }
};

export default function (token) {
  const response = http.get(
    "http://localhost:8080/api/concert/1/schedules?page=0&size=20",
    {
      headers: {
        'TOKEN': token,
        'Content-Type': 'application/json'
      }
    }
  );
  check(response, { "스케쥴 조회 성공": (r) => r.status === 200 });
}
