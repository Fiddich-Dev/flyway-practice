-- 첫 번째 마이그레이션.
-- 이 파일이 한 번 실행되고 나면 flyway_schema_history 에 checksum 이 박힌다.
-- 그 후로는 이 파일을 절대 수정하면 안 된다. (수정하면 앱이 안 뜬다 — 실습에서 직접 재현해볼 것)

CREATE TABLE member (
    id         BIGSERIAL    PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    nickname   VARCHAR(50)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
