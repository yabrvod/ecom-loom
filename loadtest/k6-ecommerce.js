import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate    = new Rate('errors');
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
            gracefulRampDown: '5s',
        },
    },
    thresholds: {
        http_req_failed:   ['rate<0.01'],
        http_req_duration: ['p(95)<2000'],
        catalog_latency:   ['p(95)<500'],
        pdp_latency:       ['p(95)<800'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8090';

const SLUGS = [
    'laptop-pro-15',
    'smartphone-x12',
    'polera-premium',
    'jeans-clasico',
    'lampara-nordica',
];

const VARIANT_IDS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];

function getCsrf(body) {
    const match = body.match(/name="_csrf"\s+value="([^"]+)"/);
    return match ? match[1] : '';
}

export default function () {
    const rnd = Math.random();

    // 60% — navegar catálogo
    if (rnd < 0.6) {
        const res = http.get(`${BASE_URL}/`);
        catalogLatency.add(res.timings.duration);
        const ok = check(res, { 'catalogo 200': r => r.status === 200 });
        errorRate.add(!ok);
        sleep(2 + Math.random() * 3);
        return;
    }

    // 25% — ver detalle de producto
    if (rnd < 0.85) {
        const slug = SLUGS[Math.floor(Math.random() * SLUGS.length)];
        const res = http.get(`${BASE_URL}/productos/${slug}`);
        pdpLatency.add(res.timings.duration);
        const ok = check(res, { 'pdp 200': r => r.status === 200 });
        errorRate.add(!ok);
        sleep(3 + Math.random() * 4);
        return;
    }

    // 15% — flujo carrito + checkout
    const jar = http.cookieJar();

    // 1. Ir al PDP y obtener CSRF
    const slug = SLUGS[Math.floor(Math.random() * SLUGS.length)];
    const pdp = http.get(`${BASE_URL}/productos/${slug}`);
    check(pdp, { 'pdp para carrito 200': r => r.status === 200 });
    const csrf1 = getCsrf(pdp.body);

    if (!csrf1) { errorRate.add(1); return; }

    // 2. Agregar al carrito
    const variantId = VARIANT_IDS[Math.floor(Math.random() * VARIANT_IDS.length)];
    const addRes = http.post(`${BASE_URL}/carrito/agregar`,
        { variantId: String(variantId), quantity: '1', _csrf: csrf1 },
        { redirects: 0 }
    );
    check(addRes, { 'agregar carrito 302': r => r.status === 302 || r.status === 200 });
    sleep(1 + Math.random() * 2);

    // 3. Ver carrito
    const cartRes = http.get(`${BASE_URL}/carrito`);
    cartLatency.add(cartRes.timings.duration);
    check(cartRes, { 'carrito 200': r => r.status === 200 });
    const csrf2 = getCsrf(cartRes.body);

    if (!csrf2) { errorRate.add(1); return; }

    sleep(2 + Math.random() * 3);

    // 4. Checkout
    const checkoutRes = http.post(`${BASE_URL}/carrito/checkout`,
        {
            shippingName: 'Usuario Test',
            shippingAddress: 'Av. Principal 123, Santiago',
            _csrf: csrf2,
        },
        { redirects: 1 }
    );
    checkoutLatency.add(checkoutRes.timings.duration);
    const ok = check(checkoutRes, {
        'checkout ok': r => r.status === 200 || r.status === 302,
        'sin error 500': r => r.status !== 500,
    });
    errorRate.add(!ok);
}
