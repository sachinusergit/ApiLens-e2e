import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const LOAD_KEY = __ENV.LOAD_TEST_KEY || '';

export function setup() {
  const res = http.post(
    `${BASE}/api/auth/login`,
    JSON.stringify({ email: 'ravi@example.com', password: 'Test@1234' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  return { token: res.json('accessToken') };
}

export default function (data) {
  const content = 'load-test-payload-' + __VU + '-' + __ITER + ' ' + 'x'.repeat(1024);
  const payload = {
    file: http.file(content, `load-${__VU}-${__ITER}.txt`, 'text/plain'),
  };

  const headers = { Authorization: `Bearer ${data.token}` };
  if (LOAD_KEY) headers['X-Load-Test-Key'] = LOAD_KEY;

  const res = http.post(`${BASE}/api/files/upload`, payload, { headers });

  check(res, {
    'upload 201': (r) => r.status === 201,
  });

  sleep(0.5);
}
