-- Seed data for M3 demo

-- Machine models (services table)
INSERT INTO services (model_name, description, daily_rate) VALUES
    ('John Deere 6M Tractor', 'Versatile utility tractor, 110 HP, ideal for fieldwork', 350.00),
    ('Case IH Axial-Flow Combine', 'High-capacity grain combine, 330 HP', 900.00);

-- Machines
INSERT INTO machines (service_id, serial_number, year_built, condition_notes) VALUES
    (1, 'JD6M-2021-001', 2021, 'Excellent — serviced Jan 2026'),
    (1, 'JD6M-2020-002', 2020, 'Good — minor cab wear'),
    (2, 'CIHAF-2022-001', 2022, 'Excellent — new belts installed');

-- Availability slots: multi-day date ranges for each machine
INSERT INTO availability_slots (machine_id, start_date, end_date, is_available) VALUES
    (1, '2026-06-02', '2026-06-06', TRUE),
    (1, '2026-06-09', '2026-06-13', TRUE),
    (2, '2026-06-02', '2026-06-05', TRUE),
    (2, '2026-06-16', '2026-06-20', TRUE),
    (3, '2026-06-08', '2026-06-12', TRUE),
    (3, '2026-06-22', '2026-06-26', TRUE),
    (1, '2026-07-07', '2026-07-11', TRUE),
    (2, '2026-07-14', '2026-07-18', TRUE);

-- Seed users: admin, technicians, customer
INSERT INTO users (name, email, phone, role, password) VALUES
    ('Admin User',   'admin@beagle.com',      '555-0001', 'ADMIN',      '{bcrypt}$2a$10$08YT1HT/WitZGfAPP5ZVYOjHopwrUZYz5xh9rrzPMalI00FGK/gpm'),
    ('Tech One',     'tech1@beagle.com',       '555-0002', 'TECHNICIAN', '{bcrypt}$2a$10$WqxL8BavicM5QZxTTSDpnOPkYOCKVXpNhTyH/ifKPOuhY/ysV0oSe'),
    ('Tech Two',     'tech2@beagle.com',       '555-0003', 'TECHNICIAN', '{bcrypt}$2a$10$5CnFWHRVF7x9dz5s./.fse6HBCX318rfr/B1tqZNstWt1jkb18MUW'),
    ('Alice Farmer', 'alice@farm.example.com', '555-0100', 'CUSTOMER',   '{noop}customer123');
