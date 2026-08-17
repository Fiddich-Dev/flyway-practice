# flyway-practice

Spring Boot + Flyway 학습용 연습장. 개념을 읽는 게 아니라 **직접 깨뜨려보고 복구하는** 것이 목적이다.

- Spring Boot 3.5.15 / Java 21 / Gradle
- PostgreSQL (로컬 `flyway_practice` DB)
- `ddl-auto: validate` + Flyway 조합 — 실무에서 쓰는 그 구성

## 사전 준비

```bash
# Postgres 가 떠 있어야 한다
pg_isready

# 연습용 DB 생성 (이미 만들었다면 생략)
createdb flyway_practice
```

## 실행

```bash
./gradlew bootRun
```

웹 의존성이 없으므로 **마이그레이션을 수행하고 바로 종료**된다. 실습 사이클을 짧게 하려는 의도다.
`BUILD SUCCESSFUL` = 마이그레이션 성공 + Hibernate 검증 통과.

## 상태 확인용 명령

```bash
# 마이그레이션 이력 (가장 자주 쓰게 될 명령)
psql -d flyway_practice -c "SELECT installed_rank, version, description, type, checksum, success FROM flyway_schema_history ORDER BY installed_rank;"

# 현재 테이블 목록
psql -d flyway_practice -c "\dt"

# 특정 테이블 구조
psql -d flyway_practice -c "\d member"
```

## 초기화 (실습하다 꼬였을 때)

```bash
dropdb flyway_practice && createdb flyway_practice
```

언제든 이 두 줄로 백지로 돌아갈 수 있다. **마음껏 망가뜨려도 된다.**

---

# 실습 시나리오

## 실습 0 — 첫 실행 (완료)

`./gradlew bootRun` 을 실행하면 Flyway 가:

1. `flyway_schema_history` 테이블을 만들고
2. `V1__create_member.sql` 을 실행하고
3. 이력을 한 줄 기록한다

```
installed_rank | 1
version        | 1
description    | create member
script         | V1__create_member.sql
checksum       | -2044951807     <-- 파일 내용의 해시
success        | t
```

## 실습 1 — 마이그레이션 추가하기 (증분 실행)

`V2__add_member_phone.sql` 을 만들고 다시 실행한다.
Flyway 가 **V1 은 건너뛰고 V2 만** 실행하는 것을 로그로 확인할 것.

주의: 엔티티(`Member.java`)에 필드를 추가하지 않으면 `validate` 는 통과한다
(DB 에만 있고 엔티티에 없는 컬럼은 검증 대상이 아니다). 반대로 엔티티에만 추가하면 앱이 죽는다.

## 실습 2 — checksum mismatch 재현 ⭐

**Flyway 를 쓰다 가장 먼저 만나는 에러.**

1. 이미 적용된 `V1__create_member.sql` 을 아무렇게나 수정한다 (공백 하나라도)
2. `./gradlew bootRun`
3. `FlywayValidateException: Migration checksum mismatch for migration version 1` 로 앱이 시작조차 안 됨

복구 방법 두 가지:
- **파일을 원래대로 되돌린다** ← 실무의 정답
- `./gradlew flywayRepair` 로 history 의 checksum 을 현재 파일 기준으로 갱신
  (단, 이미 실행된 SQL 이 되돌아가는 건 아니다. DB 상태와 파일 내용이 어긋난 채 봉합될 뿐)

## 실습 3 — 실패한 마이그레이션 복구 ⭐

1. 문법이 틀린 `V3__broken.sql` 을 만든다 (예: `CREAT TABLE ...`)
2. 실행 → 실패
3. `flyway_schema_history` 를 보면 `success = f` 인 행이 남는다 (Postgres 는 DDL 트랜잭션을 지원해 롤백되지만, 이력은 남는다)
4. 이 상태에서는 다음 실행도 계속 막힌다
5. `./gradlew flywayRepair` 로 실패 기록을 지우고, SQL 을 고쳐서 재실행

## 실습 4 — Java 마이그레이션

SQL 로는 불가능한 **데이터 변환**을 할 때 쓴다.
`src/main/java/db/migration/V4__Xxx.java` 에 `BaseJavaMigration` 을 상속해 작성한다.

주의할 점:
- 파일 위치가 `db/migration` 패키지여야 한다 (`locations` 설정과 연동)
- 클래스명이 곧 버전이다 — `V4__Normalize_something`
- 여기서 JPA 리포지토리를 쓰면 안 된다. **순수 JDBC 로만** 작성한다
  (마이그레이션 시점엔 엔티티가 현재 스키마와 안 맞을 수 있기 때문)

acttub 프로젝트의 `V4__Normalize_coaching_focus_axes.java`, `V7__Update_coaching_input_contract.java` 가 실제 사례.

## 실습 5 — 팀 협업 시 버전 충돌

브랜치 A 와 B 가 동시에 `V5__...` 를 만들면 머지 후 터진다.
Flyway 는 같은 버전 번호를 두 개 허용하지 않는다.

대응:
- 버전을 타임스탬프로 (`V20260817203000__...`)
- 또는 머지하는 쪽이 번호를 밀어서 리네임 (**아직 배포 안 된 마이그레이션에 한해서만** 가능)

---

# 기억할 원칙

1. **적용된 파일은 수정 금지** — 고치려면 새 버전을 추가한다
2. **버전은 증가만** — 뒤늦게 낮은 번호를 끼워넣지 않는다
3. **롤백은 없다** — 되돌리려면 되돌리는 마이그레이션을 새로 쓴다 (Undo 는 유료 기능)
4. **엔티티 변경과 마이그레이션은 한 커밋에** — 따로 가면 `validate` 가 앱 시작을 막는다
5. **운영 DB 를 손으로 고치지 않는다** — 손으로 고치는 순간 history 와 실제가 어긋난다
