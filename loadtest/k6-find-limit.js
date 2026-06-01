import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate    = new Rate('errors');
const catalogLatency = new Trend('catalog_latency', true);
const pdpLatency     = new Trend('pdp_latency', true);
const checkoutLatency = new Trend('checkout_latency', true);

export const options = {
    scenarios: {
        ramping: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: 500 },
                { duration: '30s', target: 500 },
                { duration: '20s', target: 600 },
                { duration: '30s', target: 600 },
                { duration: '20s', target: 700 },
                { duration: '30s', target: 700 },
                { duration: '20s', target: 800 },
                { duration: '30s', target: 800 },
                { duration: '20s', target: 1000 },
                { duration: '30s', target: 1000 },
                { duration: '20s', target: 0    },
            ],
        },
    },
    thresholds: {
        http_req_failed:   ['rate<0.05'],       // hasta 5% de errores tolerados
        http_req_duration: ['p(95)<3000'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:9090';

const SLUGS = ['laptop-pro-15','smartphone-x12','polera-premium','jeans-clasico','lampara-nordica'];

function getCsrf(body) {
    const match = body.match(/name="_csrf"\s+value="([^"]+)"/);
    return match ? match[1] : '';
}

export default function () {
    const rnd = Math.random();

    if (rnd < 0.6) {
        const res = http.get(`${BASE_URL}/`);
        catalogLatency.add(res.timings.duration);
        check(res, { 'catalogo 200': r => r.status === 200 });
        errorRate.add(res.status !== 200);
        sleep(1 + Math.random() * 2);
        return;
    }

    if (rnd < 0.85) {
        const slug = SLUGS[Math.floor(Math.random() * SLUGS.length)];
        const res = http.get(`${BASE_URL}/productos/${slug}`);
        pdpLatency.add(res.timings.duration);
        check(res, { 'pdp 200': r => r.status === 200 });
        errorRate.add(res.status !== 200);
        sleep(1 + Math.random() * 2);
        return;
    }

    // Flujo carrito + checkout
    const slug = SLUGS[Math.floor(Math.random() * SLUGS.length)];
    const pdp = http.get(`${BASE_URL}/productos/${slug}`);
    const csrf1 = getCsrf(pdp.body);
    if (!csrf1) return;

    http.post(`${BASE_URL}/carrito/agregar`,
        { variantId: String(Math.ceil(Math.random() * 11)), quantity: '1', _csrf: csrf1 },
        { redirects: 0 });
    sleep(0.5);

    const cartRes = http.get(`${BASE_URL}/carrito`);
    const csrf2 = getCsrf(cartRes.body);
    if (!csrf2) return;
    sleep(0.5);

    const res = http.post(`${BASE_URL}/carrito/checkout`,
        { shippingName: 'Test', shippingAddress: 'Calle 123', _csrf: csrf2 },
        { redirects: 1 });
    checkoutLatency.add(res.timings.duration);
    check(res, { 'checkout ok': r => r.status === 200 || r.status === 302 });
    errorRate.add(res.status === 500);
}
