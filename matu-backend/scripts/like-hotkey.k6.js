import http from 'k6/http';
import { check } from 'k6';
import { sleep } from 'k6';

// 示例：k6 run -e BASE_URL=http://127.0.0.1:8080 -e POST_ID=1 -e USER_ID_START=1000 scripts/like-hotkey.k6.js
export const options = {
  scenarios: {
    hotkey: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 50),
      duration: __ENV.DURATION || '60s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const hotPostId = __ENV.POST_ID || '1';
const userStart = Number(__ENV.USER_ID_START || 1000);
const userCount = Math.max(Number(__ENV.USER_COUNT || 10000), 1);
const postCount = Math.max(Number(__ENV.POST_COUNT || 100), 1);
const cancelRate = Math.min(Math.max(Number(__ENV.CANCEL_RATE || 0.1), 0), 1);
const token = __ENV.TOKEN || '';

export default function () {
  const userId = userStart + ((__VU - 1) % userCount);
  const postId = __ENV.RANDOM_POSTS === 'true'
    ? String(1 + Math.floor(Math.random() * postCount))
    : hotPostId;
  const cancel = Math.random() < cancelRate;
  const url = `${baseUrl}/posts/${postId}/like`;
  const headers = {
    'X-User-Id': String(userId),
    'Content-Type': 'application/json',
  };
  if (token) headers.Authorization = token;
  const params = { headers };
  const response = cancel ? http.del(url, null, params) : http.post(url, null, params);
  check(response, {
    'status is successful or conflict/rate-limited': (r) => [200, 409, 429].includes(r.status),
  });
  sleep(Number(__ENV.THINK_TIME || 0.05));
}
