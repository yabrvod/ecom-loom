import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate      = new Rate('errors');
const catalogLatency = new Trend('catalog_latency', true);
const pdpLatency     = new Trend('pdp_latency', true);
const cartLatency    = new Trend('cart_latency', true);
const checkoutLatency = new Trend('checkout_latency', true);

export const options = {
    scenarios: {
        humanos: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 50 },
                { duration: '50s', target: 50 },
                { duration: '5s',  target: 0 },
            ],
        },
    },
    thresholds: {
        http_req_failed:   ['rate<0.01'],
        http_req_duration: ['p(95)<3000'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8090';
const SLUGS    = ['laptop-pro-15','smartphone-x12','polera-premium','jeans-clasico','lampara-nordica'];

function getCsrf(body) {
    const m = body.match(/name="_csrf"\s+value="([^"]+)"/);
    return m ? m[1] : '';
}

export default function () {
    const rnd = Math.random();

    // ── 65% solo navega catálogo ──────────────────────────
    if (rnd < 0.65) {
        const res = http.get(`${BASE_URL}/`);
        catalogLatency.add(res.timings.duration);
        check(res, { 'catalogo 200': r => r.status === 200 });
        errorRate.add(res.status !== 200);
        sleep(3 + Math.random() * 5);
        return;
    }

    // ── 20% ve detalle de producto ─────────────────────────
    if (rnd < 0.85) {
        const slug = SLUGS[Math.floor(Math.random() * SLUGS.length)];
        const res = http.get(`${BASE_URL}/productos/${slug}`);
        pdpLatency.add(res.timings.duration);
        check(res, { 'pdp 200': r => r.status === 200 });
        errorRate.add(res.status !== 200);
        sleep(4 + Math.random() * 6);
        return;
    }

    // ── 10% agrega al carrito pero abandona ───────────────
    if (rnd < 0.95) {
        const slug = SLUGS[Math.floor(Math.random() * SLUGS.length)];
        const pdp  = http.get(`${BASE_URL}/productos/${slug}`);
        const csrf = getCsrf(pdp.body);
        if (!csrf) { errorRate.add(1); return; }

        http.post(`${BASE_URL}/carrito/agregar`,
            { variantId: String(Math.ceil(Math.random() * 11)), quantity: '1', _csrf: csrf },
            { redirects: 0 });

        const cart = http.get(`${BASE_URL}/carrito`);
        cartLatency.add(cart.timings.duration);
        check(cart, { 'carrito 200': r => r.status === 200 });
        errorRate.add(cart.status !== 200);

        sleep(5 + Math.random() * 8); // abandona
        return;
    }

    // ── 5% checkout completo ──────────────────────────────
    const slug = SLUGS[Math.floor(Math.random() * SLUGS.length)];
    const pdp  = http.get(`${BASE_URL}/productos/${slug}`);
    const csrf1 = getCsrf(pdp.body);
    if (!csrf1) { errorRate.add(1); return; }

    http.post(`${BASE_URL}/carrito/agregar`,
        { variantId: String(Math.ceil(Math.random() * 11)), quantity: '1', _csrf: csrf1 },
        { redirects: 0 });
    sleep(1);

    const cart  = http.get(`${BASE_URL}/carrito`);
    const csrf2 = getCsrf(cart.body);
    if (!csrf2) { errorRate.add(1); return; }
    sleep(2 + Math.random() * 3);

    const res = http.post(`${BASE_URL}/carrito/checkout`,
        { shippingName: 'Usuario Test', shippingAddress: 'Av. Principal 123', _csrf: csrf2 },
        { redirects: 1 });
    checkoutLatency.add(res.timings.duration);
    check(res, {
        'checkout ok': r => r.status === 200 || r.status === 302,
        'sin error 500': r => r.status !== 500,
    });
    errorRate.add(res.status === 500);
}
