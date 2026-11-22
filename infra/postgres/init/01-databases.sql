-- Catalog
CREATE USER catalog_user WITH PASSWORD 'catalog_local_password';
CREATE DATABASE catalog OWNER catalog_user;

-- Reservation
CREATE USER reservation_user WITH PASSWORD 'reservation_local_password';
CREATE DATABASE reservation OWNER reservation_user;

-- Orders
CREATE USER orders_user WITH PASSWORD 'orders_local_password';
CREATE DATABASE orders OWNER orders_user;

-- Payments
CREATE USER payments_user WITH PASSWORD 'payments_local_password';
CREATE DATABASE payments OWNER payments_user;