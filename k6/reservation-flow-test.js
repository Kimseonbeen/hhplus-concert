import http from "k6/http";
import { sleep, check } from "k6";
import { Trend } from "k6/metrics";

/**
 * 예약 전체 플로우 성능 테스트
 *
 * 목적: 단계별 병목 구간을 탐지한다.
 * 흐름: 토큰 발급 → 스케줄 조회 → 좌석 조회 → 좌석 예약 → 결제
 *
 * 각 VU(가상 유저)는 독립적으로 위 흐름을 순차 실행한다.
 * 여러 VU가 동시에 실행되므로 서버 입장에서는 동시 요청이 발생한다.
 *
 * [실행 방법]
 * ./k6/run-test.sh  (Redis 초기화 후 자동 실행)
 */

// ── 커스텀 메트릭 ──────────────────────────────────────────────────────────
// 각 단계별 응답시간을 개별 Trend로 기록 → Grafana에서 단계별 비교 가능
// duration_01_token, duration_02_schedule ... 순서대로 Grafana 패널에 표시됨
const tokenTrend    = new Trend("duration_01_token");
const scheduleTrend = new Trend("duration_02_schedule");
const seatTrend     = new Trend("duration_03_seat");
const reserveTrend  = new Trend("duration_04_reserve");
const paymentTrend  = new Trend("duration_05_payment");

// ── 테스트 설정 ────────────────────────────────────────────────────────────
export let options = {
  scenarios: {
    reservation_flow: {
      /**
       * executor: ramping-arrival-rate
       * VU 수가 아닌 RPS(초당 요청 수) 기준으로 부하를 제어한다.
       *
       * ramping-vus 방식은 서버가 느려지면 RPS도 자동으로 줄어드는 문제가 있다.
       * ramping-arrival-rate는 서버 응답 여부와 관계없이 목표 RPS를 유지한다.
       * 실제 트래픽(사용자는 서버 상태에 무관하게 요청)을 더 현실적으로 재현한다.
       */
      executor: 'ramping-arrival-rate',
      startRate: 10,        // 시작 시 초당 10 요청
      timeUnit: '1s',       // startRate 단위 = 1초
      preAllocatedVUs: 2000,  // 미리 준비해둘 VU 수 (부하 시작 시 즉시 투입)
      maxVUs: 8000,           // VU 최대 한도
      stages: [
        { duration: '5s',  target: 4000 }, // 즉시 스파이크: 콘서트 오픈 순간 재현
        { duration: '50s', target: 4000 }, // 지속 부하: 피크 구간 실제 성능 측정
        { duration: '5s',  target: 0    }, // 종료
      ],
    },
  },

  /**
   * 임계값 설정 기준
   * p(95): 전체 요청의 95%가 이 시간 안에 응답 — 대부분의 사용자 경험을 대표
   *
   * - 토큰 발급 (200ms): Redis 메모리 작업, 가장 많은 트래픽 몰리는 구간 → 엄격하게
   * - 스케줄/좌석 조회 (500ms): DB 읽기 + 인덱스 → 일반적인 웹 API 기준
   * - 예약/결제 (1000ms): DB 트랜잭션 + 분산 락 대기 시간 포함 → 여유롭게
   */
  thresholds: {
    // 응답시간 기준 — 단계별 특성에 맞게 설정
    // 예약/결제 실패는 동시성 보호(락)에 의한 정상 동작이므로 실패율 임계값은 설정하지 않음
    // 실패율은 하단 check() 결과(단계별 성공률)로 확인
    'duration_01_token':    ['p(95)<200'],
    'duration_02_schedule': ['p(95)<500'],
    'duration_03_seat':     ['p(95)<500'],
    'duration_04_reserve':  ['p(95)<1000'],
    'duration_05_payment':  ['p(95)<1000'],
  },
};

const BASE_URL = "http://localhost:8080";
const HEADERS  = { 'Content-Type': 'application/json' };

/**
 * 예약 가능한 좌석 목록 (scheduleId + seatId + seatNum)
 *
 * 좌석 API(/api/concert/{scheduleId}/seats)는 seatNum(좌석 번호)만 반환하고
 * 예약 API는 seatId(DB PK)가 필요하므로 사전에 매핑 정보를 구성한다.
 *
 * 좌석 수가 많을수록 동시 요청 충돌(중복 예약 시도)이 줄어 실패율이 낮아진다.
 */
const SEATS = [
  { scheduleId: 393447, seatId: 1360733, seatNum: 4   },
  { scheduleId: 393447, seatId: 1360763, seatNum: 5   },
  { scheduleId: 393447, seatId: 1360793, seatNum: 6   },
  { scheduleId: 393447, seatId: 1360973, seatNum: 12  },
  { scheduleId: 393447, seatId: 1361003, seatNum: 13  },
  { scheduleId: 393447, seatId: 1361033, seatNum: 14  },
  { scheduleId: 393447, seatId: 1361063, seatNum: 15  },
  { scheduleId: 393447, seatId: 1361093, seatNum: 16  },
  { scheduleId: 393447, seatId: 1361123, seatNum: 17  },
  { scheduleId: 393447, seatId: 1361213, seatNum: 20  },
  { scheduleId: 393447, seatId: 1361243, seatNum: 21  },
  { scheduleId: 393447, seatId: 1361303, seatNum: 23  },
  { scheduleId: 393447, seatId: 1361363, seatNum: 25  },
  { scheduleId: 393447, seatId: 1361483, seatNum: 29  },
  { scheduleId: 393447, seatId: 1361513, seatNum: 30  },
  { scheduleId: 393447, seatId: 1361573, seatNum: 32  },
  { scheduleId: 393447, seatId: 1361603, seatNum: 33  },
  { scheduleId: 393447, seatId: 1361633, seatNum: 34  },
  { scheduleId: 393447, seatId: 1361663, seatNum: 35  },
  { scheduleId: 393447, seatId: 1361723, seatNum: 37  },
  { scheduleId: 393447, seatId: 1361753, seatNum: 38  },
  { scheduleId: 393447, seatId: 1361813, seatNum: 40  },
  { scheduleId: 393447, seatId: 1361843, seatNum: 41  },
  { scheduleId: 393447, seatId: 1361873, seatNum: 42  },
  { scheduleId: 393447, seatId: 1361903, seatNum: 43  },
  { scheduleId: 393447, seatId: 1362023, seatNum: 47  },
  { scheduleId: 393447, seatId: 1362083, seatNum: 49  },
  { scheduleId: 393447, seatId: 1362113, seatNum: 50  },
  { scheduleId: 393447, seatId: 1362143, seatNum: 51  },
  { scheduleId: 393447, seatId: 1362173, seatNum: 52  },
  { scheduleId: 393447, seatId: 1362203, seatNum: 53  },
  { scheduleId: 393447, seatId: 1362233, seatNum: 54  },
  { scheduleId: 393447, seatId: 1362263, seatNum: 55  },
  { scheduleId: 393447, seatId: 1362323, seatNum: 57  },
  { scheduleId: 393447, seatId: 1362383, seatNum: 59  },
  { scheduleId: 393447, seatId: 1362413, seatNum: 60  },
  { scheduleId: 393447, seatId: 1362503, seatNum: 63  },
  { scheduleId: 393447, seatId: 1362533, seatNum: 64  },
  { scheduleId: 393447, seatId: 1362563, seatNum: 65  },
  { scheduleId: 393447, seatId: 1362653, seatNum: 68  },
  { scheduleId: 393447, seatId: 1362773, seatNum: 72  },
  { scheduleId: 393447, seatId: 1362803, seatNum: 73  },
  { scheduleId: 393447, seatId: 1362833, seatNum: 74  },
  { scheduleId: 393447, seatId: 1362863, seatNum: 75  },
  { scheduleId: 393447, seatId: 1362893, seatNum: 76  },
  { scheduleId: 393447, seatId: 1362923, seatNum: 77  },
  { scheduleId: 393447, seatId: 1362953, seatNum: 78  },
  { scheduleId: 393447, seatId: 1363043, seatNum: 81  },
  { scheduleId: 393447, seatId: 1363103, seatNum: 83  },
  { scheduleId: 393447, seatId: 1363133, seatNum: 84  },
  { scheduleId: 393447, seatId: 1363163, seatNum: 85  },
  { scheduleId: 393447, seatId: 1363223, seatNum: 87  },
  { scheduleId: 393447, seatId: 1363253, seatNum: 88  },
  { scheduleId: 393447, seatId: 1363343, seatNum: 91  },
  { scheduleId: 393447, seatId: 1363373, seatNum: 92  },
  { scheduleId: 393447, seatId: 1363403, seatNum: 93  },
  { scheduleId: 393447, seatId: 1363433, seatNum: 94  },
  { scheduleId: 393447, seatId: 1363463, seatNum: 95  },
  { scheduleId: 393447, seatId: 1363523, seatNum: 97  },
  { scheduleId: 393447, seatId: 1363583, seatNum: 99  },
  { scheduleId: 393447, seatId: 1363613, seatNum: 100 },
  { scheduleId: 393447, seatId: 1363643, seatNum: 101 },
  { scheduleId: 393447, seatId: 1363733, seatNum: 104 },
  { scheduleId: 393447, seatId: 1363763, seatNum: 105 },
  { scheduleId: 393447, seatId: 1363823, seatNum: 107 },
  { scheduleId: 393447, seatId: 1363913, seatNum: 110 },
  { scheduleId: 393447, seatId: 1364003, seatNum: 113 },
  { scheduleId: 393447, seatId: 1364033, seatNum: 114 },
  { scheduleId: 393447, seatId: 1364123, seatNum: 117 },
  { scheduleId: 393447, seatId: 1364153, seatNum: 118 },
  { scheduleId: 393447, seatId: 1364213, seatNum: 120 },
  { scheduleId: 393447, seatId: 1364243, seatNum: 121 },
  { scheduleId: 393447, seatId: 1364363, seatNum: 125 },
  { scheduleId: 393447, seatId: 1364423, seatNum: 127 },
  { scheduleId: 393447, seatId: 1364453, seatNum: 128 },
  { scheduleId: 393447, seatId: 1364483, seatNum: 129 },
  { scheduleId: 393447, seatId: 1364513, seatNum: 130 },
  { scheduleId: 393447, seatId: 1364543, seatNum: 131 },
  { scheduleId: 393447, seatId: 1364573, seatNum: 132 },
  { scheduleId: 393447, seatId: 1364603, seatNum: 133 },
  { scheduleId: 393447, seatId: 1364633, seatNum: 134 },
  { scheduleId: 393447, seatId: 1364663, seatNum: 135 },
  { scheduleId: 393447, seatId: 1364723, seatNum: 137 },
  { scheduleId: 393447, seatId: 1364783, seatNum: 139 },
  { scheduleId: 393447, seatId: 1364813, seatNum: 140 },
  { scheduleId: 393447, seatId: 1364843, seatNum: 141 },
  { scheduleId: 393447, seatId: 1364873, seatNum: 142 },
  { scheduleId: 393447, seatId: 1364903, seatNum: 143 },
  { scheduleId: 393447, seatId: 1364933, seatNum: 144 },
  { scheduleId: 393447, seatId: 1364963, seatNum: 145 },
  { scheduleId: 393447, seatId: 1365083, seatNum: 149 },
  { scheduleId: 393447, seatId: 1365113, seatNum: 150 },
  { scheduleId: 393447, seatId: 1365173, seatNum: 152 },
  { scheduleId: 393447, seatId: 1365233, seatNum: 154 },
  { scheduleId: 393447, seatId: 1365263, seatNum: 155 },
  { scheduleId: 393447, seatId: 1365293, seatNum: 156 },
  { scheduleId: 393447, seatId: 1365323, seatNum: 157 },
  { scheduleId: 393447, seatId: 1365353, seatNum: 158 },
  { scheduleId: 393447, seatId: 1365383, seatNum: 159 },
  { scheduleId: 393447, seatId: 1365443, seatNum: 161 },
];

/**
 * default 함수
 *
 * VU마다 독립적으로 반복 실행된다. (Spring @Test와 유사)
 * 각 VU는 자신만의 userId, token, authHeaders를 가지므로 VU 간 데이터 공유 없음.
 * 여러 VU가 동시에 실행 → 서버 입장에서는 동시 HTTP 요청 발생.
 */
export default function () {
  // userId 1~200 중 랜덤 선택 (200명의 유저가 동시에 예약하는 상황 재현)
  // 유저 풀을 넓혀 동일 userId 동시 결제 충돌(분산 락) 빈도를 낮춤
  const userId = Math.floor(Math.random() * 200) + 1;

  // ── 1단계: 토큰 발급 ──────────────────────────────────────────────────────
  // Redis 기반. MAX_ACTIVE_USERS(150) 슬롯이 남아있으면 ACTIVE, 아니면 WAITING 반환.
  // 대기열의 핵심: 100만 명이 동시 접속해도 이 요청만 Redis가 처리하고
  // 실제 예약 로직(DB)에는 150명만 진입시켜 서버를 보호한다.
  const tokenRes = http.post(
    `${BASE_URL}/api/queue/token`,
    JSON.stringify({ userId }),
    { headers: HEADERS }
  );
  tokenTrend.add(tokenRes.timings.duration);
  const tokenOk = check(tokenRes, { "1. 토큰 발급 성공": (r) => r.status === 200 });
  if (!tokenOk) return;

  const token  = tokenRes.json().token;
  const status = tokenRes.json().status;

  // WAITING 토큰은 QueueTokenInterceptor에서 거부되므로 플로우 진행 불가 — skip
  // 실제 서비스에서도 대기 중인 사용자는 예약 화면에 진입할 수 없다
  if (status !== "ACTIVE") return;

  // 이후 모든 요청에 TOKEN 헤더 포함 (QueueTokenInterceptor 통과용)
  const authHeaders = { ...HEADERS, 'TOKEN': token };

  // ── 2단계: 스케줄 조회 ────────────────────────────────────────────────────
  // DB 조회 + 복합 인덱스(concert_id, status, concert_date) 적용
  // page=0&size=20 — 페이지네이션 적용 (전체 반환 시 대용량 데이터 전송 문제)
  const scheduleRes = http.get(
    `${BASE_URL}/api/concert/1/schedules?page=0&size=20`,
    { headers: authHeaders }
  );
  scheduleTrend.add(scheduleRes.timings.duration);
  const scheduleOk = check(scheduleRes, { "2. 스케줄 조회 성공": (r) => r.status === 200 });
  if (!scheduleOk) return;

  // ── 3단계: 좌석 조회 ──────────────────────────────────────────────────────
  // SEATS 배열에서 랜덤 좌석 선택
  // 같은 좌석을 여러 VU가 선택하면 예약 단계에서 충돌 발생 (동시성 보호 동작)
  const seat = SEATS[Math.floor(Math.random() * SEATS.length)];

  const seatRes = http.get(
    `${BASE_URL}/api/concert/${seat.scheduleId}/seats`,
    { headers: authHeaders }
  );
  seatTrend.add(seatRes.timings.duration);
  const seatOk = check(seatRes, { "3. 좌석 조회 성공": (r) => r.status === 200 });
  if (!seatOk) return;

  // ── 4단계: 좌석 예약 ──────────────────────────────────────────────────────
  // DB 트랜잭션 + 낙관적 락(version) 적용
  // 동시에 같은 좌석 예약 시도 시 한 명만 성공, 나머지는 실패 → 정상 동작
  const reserveRes = http.post(
    `${BASE_URL}/api/reservation/reserve`,
    JSON.stringify({
      scheduleId: seat.scheduleId,
      seatId:     seat.seatId,
      userId:     userId,
      seatNum:    seat.seatNum,
    }),
    { headers: authHeaders }
  );
  reserveTrend.add(reserveRes.timings.duration);
  const reserveOk = check(reserveRes, { "4. 좌석 예약 성공": (r) => r.status === 200 });
  if (!reserveOk) return; // 예약 실패 시 결제 단계 skip

  const reservationId = reserveRes.json().reservationId;

  // ── 5단계: 결제 ───────────────────────────────────────────────────────────
  // 잔액 차감(분산 락) + 결제 기록 + 아웃박스 이벤트 저장
  // 같은 userId가 동시에 결제 시도하면 분산 락 타임아웃으로 일부 실패 → 정상 동작
  // 결제 완료 후 토큰 만료 처리 (active-token:{token} Redis 키 삭제)
  const paymentRes = http.post(
    `${BASE_URL}/api/reservation/payment`,
    JSON.stringify({ reservationId, userId }),
    { headers: authHeaders }
  );
  paymentTrend.add(paymentRes.timings.duration);
  check(paymentRes, { "5. 결제 성공": (r) => r.status === 200 });
}
