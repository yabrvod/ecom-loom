-- ============================================================
-- CATÁLOGO — leído principalmente con jOOQ
-- ============================================================
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    slug        VARCHAR(120) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    name        VARCHAR(200) NOT NULL,
    slug        VARCHAR(220) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    base_price  NUMERIC(12,2) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_active    ON products(active, created_at DESC);
CREATE INDEX idx_products_name      ON products(LOWER(name));

CREATE TABLE product_variants (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku        VARCHAR(100) NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,  -- ej: "Talla M / Azul"
    price      NUMERIC(12,2) NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_variants_product ON product_variants(product_id);

-- ============================================================
-- INVENTARIO — leído con jOOQ, escrito en checkout con Hibernate
-- ============================================================
CREATE TABLE inventory (
    variant_id BIGINT PRIMARY KEY REFERENCES product_variants(id) ON DELETE CASCADE,
    stock      INT NOT NULL DEFAULT 0 CHECK (stock >= 0),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- USUARIOS — gestionados con Hibernate (Spring Security)
-- ============================================================
CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(50) NOT NULL UNIQUE,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- ============================================================
-- CARRITO — gestionado con Hibernate (estado mutable)
-- ============================================================
CREATE TABLE carts (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT REFERENCES users(id) ON DELETE SET NULL,
    session_id VARCHAR(100),   -- carrito anónimo
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_carts_user    ON carts(user_id);
CREATE INDEX idx_carts_session ON carts(session_id);

CREATE TABLE cart_items (
    id         BIGSERIAL PRIMARY KEY,
    cart_id    BIGINT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id),
    quantity   INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    UNIQUE (cart_id, variant_id)
);

-- ============================================================
-- ÓRDENES — gestionadas con Hibernate (transacción compleja)
-- ============================================================
CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total           NUMERIC(12,2) NOT NULL,
    shipping_name   VARCHAR(150),
    shipping_address TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_user   ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status, created_at DESC);

CREATE TABLE order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    variant_id  BIGINT NOT NULL REFERENCES product_variants(id),
    product_name VARCHAR(200) NOT NULL,  -- snapshot al momento de compra
    variant_name VARCHAR(100) NOT NULL,
    unit_price  NUMERIC(12,2) NOT NULL,
    quantity    INT NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order ON order_items(order_id);

CREATE TABLE payments (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT NOT NULL UNIQUE REFERENCES orders(id),
    status       VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    method       VARCHAR(30) NOT NULL DEFAULT 'SIMULATED',
    amount       NUMERIC(12,2) NOT NULL,
    processed_at TIMESTAMP
);
