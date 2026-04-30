-- User table
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT
);

-- Balance table
CREATE TABLE balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    amount INT
    --version BIGINT NULL
);

-- Concert schedule table (status 컬럼 추가됨)
CREATE TABLE concert_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    concert_id BIGINT,
    concert_date DATE,
    status VARCHAR(30)  -- 추가됨
);

-- Seat table
CREATE TABLE seat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    concert_schedule_id BIGINT,
    seat_num INT,
    price INT,
    status VARCHAR(30),
    version BIGINT NULL
);

-- reservation table
CREATE TABLE reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    price BIGINT,
    status VARCHAR(255),
    expired_at DATETIME
);

-- payment
CREATE TABLE payment (
   id BIGINT PRIMARY KEY AUTO_INCREMENT,
   user_id BIGINT NOT NULL,
   reservation_id BIGINT NOT NULL,
   amount BIGINT NOT NULL,
   created_at TIMESTAMP
);

-- payment history
CREATE TABLE balance_history (
   id BIGINT PRIMARY KEY AUTO_INCREMENT,
   balance_id BIGINT NOT NULL,
   amount BIGINT NOT NULL,
   type VARCHAR(20) NOT NULL,
   created_at TIMESTAMP
);

-- outbox event
CREATE TABLE outbox_event (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type   VARCHAR(50)  NOT NULL,
    payload      TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME     NOT NULL,
    published_at DATETIME,
    fail_reason  TEXT
);