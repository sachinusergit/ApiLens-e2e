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
  // Register a fresh user per run so this does not depend on demo credentials.
  const id = `${Date.now()}${Math.floor(Math.random() * 1e6)}`;
  const email = `k6_upload_${id}@example.com`;
  const password = 'Test@1234';
  const reg = http.post(
    `${BASE}/api/auth/register`,
    JSON.stringify({ email, password, name: 'k6 upload', orgName: 'k6 upload org' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  if (reg.status !== 201) {
    throw new Error(`register failed: ${reg.status} ${reg.body}`);
  }
  return { token: reg.json('accessToken') };
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
