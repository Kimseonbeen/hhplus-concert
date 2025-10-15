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
        { duration: '10s', target: 100 },    // 워밍업: 초당 10개 요청으로 서서히 증가
        { duration: '10s', target: 200 },    // 급증: 빠르게 초당 100개 요청으로 증가
      ],
      timeUnit: '1s',            // 시간 단위 설정
      startRate: 0,              // 시작 요청 비율
      gracefulStop: '30s'        // 종료 시 유예 시간
    }
  }
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

  const response2 = http.get(
    "http://localhost:8080/api/concert/1/schedules",
    {
      headers: {
        'TOKEN': token,  // TOKEN 헤더 추가
        'Content-Type': 'application/json'
      }
    }
  );
  check(response2, { "스케쥴 조회 성공": (r) => r.status === 200 });

  //const responseBody = response2.json();
  //const firstConcertScheduleId = responseBody[0].concertScheduleId;

  const response3 = http.get(
    "http://localhost:8080/api/concert/1/seats",
    {
      headers: {
        'TOKEN': token,  // TOKEN 헤더 추가
        'Content-Type': 'application/json'
      }
    }
  );
  check(response3, { "좌석 조회 성공": (r) => r.status === 200 });

  sleep(1);
}