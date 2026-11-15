import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1,
  duration: '10s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const health = http.get(`${BASE}/actuator/health`);
  check(health, { 'gateway up': (r) => r.status < 500 });

  const login = http.post(
    `${BASE}/api/auth/login`,
    JSON.stringify({ email: 'ravi@example.com', password: 'Test@1234' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(login, {
    'login 200': (r) => r.status === 200,
    'has token': (r) => !!r.json('accessToken'),
  });

  sleep(1);
}
