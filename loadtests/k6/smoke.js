import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1,
  duration: '10s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

// Self-contained: registers a unique user in setup so the smoke test
// does not depend on the seeded demo account.
export function setup() {
  const id = `${Date.now()}${Math.floor(Math.random() * 1e6)}`;
  const email = `k6_smoke_${id}@example.com`;
  const password = 'Test@1234';
  const res = http.post(
    `${BASE}/api/auth/register`,
    JSON.stringify({ email, password, name: 'k6 smoke', orgName: 'k6 smoke org' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  if (res.status !== 201) {
    throw new Error(`register failed: ${res.status} ${res.body}`);
  }
  return { email, password };
}

export default function (data) {
  const health = http.get(`${BASE}/actuator/health`);
  check(health, { 'gateway up': (r) => r.status < 500 });

  const login = http.post(
    `${BASE}/api/auth/login`,
    JSON.stringify({ email: data.email, password: data.password }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(login, {
    'login 200': (r) => r.status === 200,
    'has token': (r) => !!r.json('accessToken'),
  });

  sleep(1);
}
