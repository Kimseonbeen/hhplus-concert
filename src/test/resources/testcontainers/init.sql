-- User table
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    version BIGINT
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