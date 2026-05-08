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
    (1, '2026-03-16', '2026-03-20', TRUE),
    (1, '2026-03-23', '2026-03-27', TRUE),
    (2, '2026-03-16', '2026-03-19', TRUE),
    (2, '2026-03-24', '2026-03-28', TRUE),
    (3, '2026-03-17', '2026-03-21', TRUE),
    (3, '2026-03-25', '2026-03-30', TRUE),
    (1, '2026-04-01', '2026-04-05', TRUE),
    (2, '2026-04-07', '2026-04-11', TRUE);

-- Seed users: admin, technicians, customer
INSERT INTO users (name, email, phone, role, password) VALUES
    ('Admin User',   'admin@beagle.com',      '555-0001', 'ADMIN',      '{noop}admin'),
    ('Tech One',     'tech1@beagle.com',       '555-0002', 'TECHNICIAN', '{noop}tech1'),
    ('Tech Two',     'tech2@beagle.com',       '555-0003', 'TECHNICIAN', '{noop}tech2'),
    ('Alice Farmer', 'alice@farm.example.com', '555-0100', 'CUSTOMER',   null);
