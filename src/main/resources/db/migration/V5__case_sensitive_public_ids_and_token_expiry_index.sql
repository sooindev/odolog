-- 공개 id 를 대소문자 구분(utf8mb4_bin). unicode_ci 면 대소문자만 바꾼 주소가 같은 자원을 열고,
-- 무작위로 만든 id 가 대소문자만 다른 기존 id 와 유니크에서 부딪힘. 구분하게 바꾸는 쪽은 기존 행과 충돌하지 않음
ALTER TABLE vehicles MODIFY COLUMN public_id VARCHAR(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
ALTER TABLE maintenance_records MODIFY COLUMN public_id VARCHAR(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
ALTER TABLE fuel_records MODIFY COLUMN public_id VARCHAR(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;

-- 만료 토큰 정리(DELETE ... WHERE expires_at < ?)가 표 전체를 훑지 않게
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens (expires_at);
