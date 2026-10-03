-- ddl-auto 가 @Enumerated 에 붙인 값 목록 CHECK. 정비 종류를 더하면 운영에서만 저장이 실패한다
-- 새 DB(V1 부터)에는 없으므로 있을 때만 지움
ALTER TABLE service_intervals DROP CONSTRAINT IF EXISTS `type`;
