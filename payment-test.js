import http from 'k6/http';
import { check } from 'k6';

export let options = {
  scenarios: {
    constant_load: {
      executor: 'constant-arrival-rate',
      rate: 250, // 250 TPS
      timeUnit: '1s',
      duration: '66m',
      preAllocatedVUs: 100,
      maxVUs: 500,
    },
  },
};

export default function () {
  const payload = JSON.stringify({
    transaction_id: `txn_${Math.random()}`,
    sender_id: "U1004",
    receiver_id: "U1002",
    amount: 123.00,
    currency: "INR"
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  let res = http.post('http://host.docker.internal:8881/v1/payments', payload, params);

  check(res, {
    'status is 200/201': (r) => r.status === 200 || r.status === 201,
  });
}