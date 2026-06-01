-- Categorías
INSERT INTO categories (name, slug, description) VALUES
('Electrónica',  'electronica',  'Smartphones, laptops, accesorios'),
('Ropa',         'ropa',         'Prendas para hombre y mujer'),
('Hogar',        'hogar',        'Muebles y decoración');

-- Productos
INSERT INTO products (category_id, name, slug, description, base_price) VALUES
(1, 'Laptop Pro 15"', 'laptop-pro-15',
 'Laptop de alto rendimiento con procesador de última generación.', 899990),
(1, 'Smartphone X12',  'smartphone-x12',
 'Pantalla AMOLED 6.7", cámara 108MP, batería 5000mAh.', 499990),
(2, 'Polera Premium',  'polera-premium',
 'Algodón 100% orgánico, corte slim.', 19990),
(2, 'Jeans Clásico',   'jeans-clasico',
 'Denim resistente, varios colores disponibles.', 34990),
(3, 'Lámpara Nórdica', 'lampara-nordica',
 'Diseño minimalista, luz cálida regulable.', 49990);

-- Variantes
INSERT INTO product_variants (product_id, sku, name, price) VALUES
(1, 'LAPTOP-PRO-8GB',   '8GB RAM / 256GB SSD',  899990),
(1, 'LAPTOP-PRO-16GB',  '16GB RAM / 512GB SSD', 1199990),
(2, 'PHONE-X12-128',    '128GB Negro',           499990),
(2, 'PHONE-X12-256',    '256GB Blanco',          579990),
(3, 'POLERA-S-NEGRO',   'S / Negro',             19990),
(3, 'POLERA-M-NEGRO',   'M / Negro',             19990),
(3, 'POLERA-L-BLANCO',  'L / Blanco',            19990),
(4, 'JEANS-30-AZUL',    '30 / Azul',             34990),
(4, 'JEANS-32-NEGRO',   '32 / Negro',            34990),
(5, 'LAMPARA-BLANCA',   'Blanco Hueso',          49990),
(5, 'LAMPARA-NEGRA',    'Negro Mate',            49990);

-- Inventario
INSERT INTO inventory (variant_id, stock) VALUES
(1, 15), (2, 8), (3, 50), (4, 30),
(5, 100), (6, 100), (7, 80),
(8, 45), (9, 45),
(10, 20), (11, 20);

-- Admin por defecto
INSERT INTO users (username, email, password) VALUES
('admin', 'admin@ecommerce.cl', '$2a$12$8uvkcJFZWRbHjXRRSMM7IuU4UcNcMpFhV7PjOxMlBJjX6nfmOW54G');
-- password: admin123

INSERT INTO user_roles (user_id, role) VALUES (1, 'ROLE_ADMIN'), (1, 'ROLE_USER');
