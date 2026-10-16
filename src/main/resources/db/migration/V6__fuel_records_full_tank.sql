-- 가득 채웠는지. 덜 채운 주유는 다음 가득 주유의 구간에 합쳐 연비를 냄
-- 기존 기록은 전부 가득으로(지금까지의 계산과 같은 뜻)
ALTER TABLE fuel_records ADD COLUMN full_tank BIT(1) NOT NULL DEFAULT b'1';
