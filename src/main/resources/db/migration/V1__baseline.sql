-- 2026-10-03 운영 스키마 그대로(ddl-auto 가 만들어 온 것). 운영 DB 는 이 버전을 이미 적용된 기준점으로 표시
-- 차이 하나: service_intervals.type 의 CHECK 는 넣지 않음(V2 가 운영에서 지움)

CREATE TABLE users (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  created_at datetime(6) NOT NULL,
  email varchar(100) NOT NULL,
  nickname varchar(30) NOT NULL,
  password varchar(255) NOT NULL,
  updated_at datetime(6) NOT NULL,
  currency varchar(3) NOT NULL DEFAULT 'KRW',
  language varchar(10) NOT NULL DEFAULT 'KO',
  time_zone varchar(64) NOT NULL DEFAULT 'Asia/Seoul',
  unit_system varchar(20) NOT NULL DEFAULT 'KM_PER_L',
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vehicles (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  created_at datetime(6) NOT NULL,
  manufacturer varchar(50) NOT NULL,
  model_name varchar(100) NOT NULL,
  model_year int(11) DEFAULT NULL,
  odometer int(11) NOT NULL,
  plate_number varchar(20) NOT NULL,
  updated_at datetime(6) NOT NULL,
  user_id bigint(20) NOT NULL,
  public_id varchar(12) NOT NULL,
  version bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_vehicles_user_plate_number (user_id, plate_number),
  UNIQUE KEY uk_vehicles_public_id (public_id),
  CONSTRAINT fk_vehicles_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE maintenance_records (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  cost int(11) DEFAULT NULL,
  created_at datetime(6) NOT NULL,
  description varchar(255) DEFAULT NULL,
  service_date date NOT NULL,
  service_odometer int(11) DEFAULT NULL,
  type varchar(30) NOT NULL,
  updated_at datetime(6) NOT NULL,
  vehicle_id bigint(20) NOT NULL,
  public_id varchar(12) NOT NULL,
  currency varchar(3) NOT NULL DEFAULT 'KRW',
  PRIMARY KEY (id),
  UNIQUE KEY uk_maintenance_records_public_id (public_id),
  KEY fk_maintenance_records_vehicle (vehicle_id),
  CONSTRAINT fk_maintenance_records_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE service_intervals (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  created_at datetime(6) NOT NULL,
  updated_at datetime(6) NOT NULL,
  interval_km int(11) DEFAULT NULL,
  interval_months int(11) DEFAULT NULL,
  type varchar(30) NOT NULL,
  vehicle_id bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_service_intervals_vehicle_type (vehicle_id, type),
  CONSTRAINT fk_service_intervals_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fuel_records (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  created_at datetime(6) NOT NULL,
  updated_at datetime(6) NOT NULL,
  fueled_at date NOT NULL,
  liters decimal(6,2) DEFAULT NULL,
  memo varchar(255) DEFAULT NULL,
  odometer int(11) NOT NULL,
  total_cost int(11) DEFAULT NULL,
  vehicle_id bigint(20) NOT NULL,
  reset_point bit(1) NOT NULL DEFAULT b'0',
  public_id varchar(12) NOT NULL,
  currency varchar(3) NOT NULL DEFAULT 'KRW',
  PRIMARY KEY (id),
  UNIQUE KEY uk_fuel_records_public_id (public_id),
  KEY fk_fuel_records_vehicle (vehicle_id),
  CONSTRAINT fk_fuel_records_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  created_at datetime(6) NOT NULL,
  updated_at datetime(6) NOT NULL,
  expires_at datetime(6) NOT NULL,
  token_hash varchar(64) NOT NULL,
  used_at datetime(6) DEFAULT NULL,
  user_id bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_password_reset_tokens_token_hash (token_hash),
  KEY fk_password_reset_tokens_user (user_id),
  CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
