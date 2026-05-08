-- M3 schema: agricultural machinery rental booking flow
-- H2 MySQL-compat mode

CREATE TABLE IF NOT EXISTS services (
    service_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_name   VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    daily_rate   DECIMAL(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS machines (
    machine_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_id    BIGINT NOT NULL,
    serial_number VARCHAR(50) NOT NULL UNIQUE,
    year_built    INT,
    condition_notes VARCHAR(300),
    CONSTRAINT fk_machine_service FOREIGN KEY (service_id) REFERENCES services(service_id)
);

CREATE TABLE IF NOT EXISTS availability_slots (
    slot_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_id   BIGINT NOT NULL,
    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    version      INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_slot_machine FOREIGN KEY (machine_id) REFERENCES machines(machine_id)
);

CREATE TABLE IF NOT EXISTS users (
    user_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL UNIQUE,
    phone        VARCHAR(20),
    role         VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    password     VARCHAR(100),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS appointments (
    appointment_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    slot_id                BIGINT NOT NULL UNIQUE,
    customer_id            BIGINT NOT NULL,
    machine_id             BIGINT NOT NULL,
    status                 VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    customer_notes         VARCHAR(500),
    booked_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_technician_id BIGINT,
    machine_ready          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_appt_slot     FOREIGN KEY (slot_id)                 REFERENCES availability_slots(slot_id),
    CONSTRAINT fk_appt_customer FOREIGN KEY (customer_id)             REFERENCES users(user_id),
    CONSTRAINT fk_appt_machine  FOREIGN KEY (machine_id)              REFERENCES machines(machine_id),
    CONSTRAINT fk_appt_tech     FOREIGN KEY (assigned_technician_id)  REFERENCES users(user_id)
);
