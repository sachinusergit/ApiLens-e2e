import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    login_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 20 },
        { duration: '40s', target: 50 },
        { duration: '20s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const LOAD_KEY = __ENV.LOAD_TEST_KEY || '';
// Static account is intentional for load generation; override when the
// seeded demo credentials have drifted (e.g. email changed via UI):
//   k6 run -e DEMO_EMAIL=you@example.com -e DEMO_PASSWORD=... loadtests/k6/auth-load.js
const EMAIL = __ENV.DEMO_EMAIL || 'ravi@example.com';
const PASSWORD = __ENV.DEMO_PASSWORD || 'Test@1234';

export default function () {
  const headers = { 'Content-Type': 'application/json' };
  if (LOAD_KEY) headers['X-Load-Test-Key'] = LOAD_KEY;

  const res = http.post(
    `${BASE}/api/auth/token`,
    JSON.stringify({ email: EMAIL, password: PASSWORD }),
    { headers }
  );

  check(res, {
    'status 200': (r) => r.status === 200,
    'has token': (r) => !!r.json('accessToken'),
  });

  sleep(0.2);
}
