-- V2 가 못 지운 정비 종류 값 목록 CHECK. ddl-auto 가 컬럼 단위로 붙여서 DROP CONSTRAINT 로는 안 지워짐
-- 컬럼을 CHECK 없이 다시 정의. 새 DB 에서는 바뀌는 것 없음
ALTER TABLE service_intervals MODIFY COLUMN `type` VARCHAR(30) NOT NULL;
