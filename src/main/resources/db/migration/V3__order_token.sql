-- Token público para ver confirmación sin exponer ID secuencial
ALTER TABLE orders ADD COLUMN confirmation_token UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE;
CREATE INDEX idx_orders_token ON orders(confirmation_token);
