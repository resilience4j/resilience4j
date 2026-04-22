# JDK 25 Spring Bump Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `resilience4j-spring6` / `resilience4j-spring-boot3` 모듈의 JDK 25 테스트 실패(ASM "Unsupported class file major version 69")를 Spring 6 / Spring Boot 3 / Spring Cloud 3 라이브러리 버전 bump으로 해결한다.

**Architecture:** `gradle/libs.versions.toml`의 3개 버전 변수(`spring6`, `spring-boot3`, `spring-cloud3`)를 JDK 25를 지원하는 현행 최신 GA 버전으로 올린 뒤, 버전 점프 과정에서 발생하는 테스트 코드 fallout을 최소 범위로 수정한다. 프로덕션 코드는 수정하지 않는다.

**Tech Stack:** Gradle 9.2.1, Java 25, Spring Framework 6.2.18, Spring Boot 3.5.13, Spring Cloud 4.3.2 (Northfields 2025.0 release train), JUnit 5 (Jupiter 5.x via JUnit BOM 6.0.3)

**Working directory:** `/Users/al02628768/private/resilience4j` (branch `feature/2343-bump-up-jdk-to-25`, worktree 아님 - 기존 feature branch 직접 작업)

**Design doc:** `docs/superpowers/specs/2026-04-22-jdk25-spring-bump-design.md`

---

## File Structure

Primary change:
- **Modify:** `gradle/libs.versions.toml` (versions block, 3 lines)

Potentially touched during fallout fixes (실제 실패 발생 시에만):
- `resilience4j-spring6/src/test/java/**/*.java` (테스트 코드)
- `resilience4j-spring-boot3/src/test/java/**/*.java` (테스트 코드)
- `resilience4j-spring-boot3/src/main/java/**/*.java` (autoconfigure spring.factories 경로 변경 등 필요시)
- `resilience4j-spring6/src/main/java/**/*.java` (불가피할 경우만)
- `resilience4j-spring-boot3/src/test/resources/application*.yml` 또는 `.properties` (property key deprecation)

프로덕션 코어 코드(`resilience4j-core` 등)는 건드리지 않는다.

---

## Task 1: Record baseline failures

`spring6`와 `spring-boot3` 모듈의 현재 실패 상태를 기록한다. 이후 수정이 의도한 에러만 해결하는지 검증 기준이 된다.

**Files:**
- Read-only. 결과는 터미널 출력으로만 기록.

- [ ] **Step 1: Run spring6 tests, capture failure summary**

Run:
```bash
./gradlew :resilience4j-spring6:test --no-daemon --continue 2>&1 | tail -40
```

Expected: FAIL. 실패 메시지에 다음 문자열이 반드시 포함되어야 한다:
```
java.lang.IllegalArgumentException: Unsupported class file major version 69
```

검증: `grep -c 'Unsupported class file major version 69' resilience4j-spring6/build/test-results/test/*.xml` 결과 ≥ 1.

- [ ] **Step 2: Run spring-boot3 tests, capture failure summary**

Run:
```bash
./gradlew :resilience4j-spring-boot3:test --no-daemon --continue 2>&1 | tail -60
```

Expected: FAIL. "80 failed" 수준의 대량 실패, root cause는 동일하게 version 69.

검증: `grep -c 'Unsupported class file major version 69' resilience4j-spring-boot3/build/test-results/test/*.xml` 결과 ≥ 1.

- [ ] **Step 3: Record baseline snapshot**

기록만 하고 커밋하지 않는다. 이후 Task 5에서 비교 대상으로 사용.

```bash
echo "Baseline recorded: spring6 failing, spring-boot3 failing with class version 69"
```

---

## Task 2: Apply the version bump

`libs.versions.toml`의 3개 버전을 JDK 25 지원 최신으로 동시 변경한다. 3개가 한 릴리즈 트레인으로 묶여 있어 한 커밋에서 함께 바꾼다.

**Files:**
- Modify: `gradle/libs.versions.toml` (versions block)

- [ ] **Step 1: Apply the diff**

`gradle/libs.versions.toml`의 다음 3줄을 수정:

```diff
-spring6 = "6.1.1"
-spring-boot3 = "3.2.0"
-spring-cloud3 = "3.1.5"
+spring6 = "6.2.18"
+spring-boot3 = "3.5.13"
+spring-cloud3 = "4.3.2"
```

최종 3줄 상태(파일의 현재 위치에서):

```toml
spring6 = "6.2.18"
spring-boot3 = "3.5.13"
spring-cloud3 = "4.3.2"
```

다른 라인(예: `spring7 = "7.0.2"`)은 건드리지 않는다.

- [ ] **Step 2: Verify compilation succeeds across all modules**

Run:
```bash
./gradlew compileJava compileTestJava --no-daemon 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`. 컴파일 에러가 나면 에러를 읽고 minimal 수정 후 재시도. 단, 이 Task에서는 테스트 실행까지 가지 않는다.

- [ ] **Step 3: Do NOT commit yet**

테스트가 통과하는지 확인 전에는 커밋하지 않는다. Task 3~5 완료 후 전체를 한 커밋으로 묶는다.

---

## Task 3: Get `resilience4j-spring6` tests green

spring6 모듈 테스트를 실행하고, 발생하는 실패를 하나씩 최소 범위로 수정하는 루프.

**Files:**
- Test: `resilience4j-spring6/src/test/java/**/*.java` (수정 대상)
- Test resources: `resilience4j-spring6/src/test/resources/**` (수정 대상)

- [ ] **Step 1: Run spring6 tests**

Run:
```bash
./gradlew :resilience4j-spring6:test --no-daemon --continue 2>&1 | tail -60
```

Expected 결과 2가지 중 하나:
- (A) `BUILD SUCCESSFUL` → Step 5로 건너뛰기.
- (B) `FAILED` → Step 2로.

- [ ] **Step 2: Confirm version-69 errors are GONE**

```bash
grep -c 'Unsupported class file major version 69' resilience4j-spring6/build/test-results/test/*.xml
```

Expected: `0`. 여전히 > 0이면 버전 bump가 제대로 반영되지 않은 것. `./gradlew --stop` 후 `./gradlew clean :resilience4j-spring6:test` 재실행.

- [ ] **Step 3: Enumerate remaining failures**

```bash
grep -l 'failures="[1-9]\|errors="[1-9]' resilience4j-spring6/build/test-results/test/*.xml | xargs -I{} basename {} .xml
grep -h 'Caused by' resilience4j-spring6/build/test-results/test/*.xml | sort -u
```

실패가 없으면 Step 5로. 있으면 Step 4로.

- [ ] **Step 4: Fix failures using the playbook below, then re-run Step 1**

다음 유형별 minimal 수정을 적용하고 다시 Step 1로 돌아간다. 수정 범위는 **이 실행에서 관찰된 실패만**으로 제한한다. 사전에 "예방적으로" 바꾸지 않는다.

**Playbook — 흔한 fallout 유형:**

| 실패 증상 (테스트 XML의 `<failure>` 메시지) | 원인 | 최소 수정 |
|---|---|---|
| `NoSuchMethodError: org.springframework.test.context...` | Spring 6.1→6.2에서 시그니처가 바뀐 내부 API 사용 | 테스트 유틸을 6.2 API로 교체 |
| `MockitoException: ... MockMaker cannot mock final class` | Mockito 5.16→5.23에서 inline mock 동작 변경 | 테스트에서 `@MockitoSettings(strictness=LENIENT)` 또는 mock 방식 수정 |
| `Cannot resolve property ...` in `@ConfigurationProperties` | Spring Boot 3.2→3.5 프로퍼티 키 이름 변경 | `application*.yml`/`.properties`의 키를 새 이름으로 |
| `FeignClientBuilder ...` / `Contract ...` API 변경 | Spring Cloud OpenFeign 3.1→4.3 breaking change | 새 API로 설정 업데이트 (Spring Cloud 2025.0 가이드 참조) |
| `BeanDefinitionStoreException: Failed to import autoconfiguration ...` | `spring.factories` 경로 deprecation (Spring Boot 2.7에서 이미 deprecated, 3.x에서도 유지됨) → 2.7 이후 권장: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | main 모듈의 `META-INF/spring.factories` 가 있으면 `AutoConfiguration.imports` 로 마이그레이션 |
| `@MockBean` deprecation warning은 무시 (WARN, not ERROR) | Spring Boot 3.4에서 deprecated (`@MockitoBean` 권장) | 테스트가 통과하면 수정 안 함 — scope 제한 |

**Fix의 scope 원칙:**
- 실제로 실패한 테스트가 있는 경우에만 해당 테스트 파일 / 해당 property 파일을 수정.
- 한 번에 한 유형의 fallout만 수정하고 Step 1로 돌아가 재실행. 여러 종류를 batch로 바꾸지 않는다.
- 프로덕션 Java 코드(`src/main/java/...`)는 최후의 수단. 대부분 테스트/리소스 수정으로 해결된다.

- [ ] **Step 5: Verify spring6 all green**

Run:
```bash
./gradlew :resilience4j-spring6:test --no-daemon 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`. `n tests completed, 0 failed`.

검증: `grep -l 'failures="[1-9]\|errors="[1-9]' resilience4j-spring6/build/test-results/test/*.xml` 결과 empty.

- [ ] **Step 6: Do NOT commit yet**

다음 Task에서 spring-boot3까지 마친 후 일괄 커밋.

---

## Task 4: Get `resilience4j-spring-boot3` tests green

spring-boot3 모듈에 대해 Task 3과 동일한 루프를 적용. spring-boot3는 Spring Boot 3.2→3.5와 Spring Cloud 3.1→4.3 동시 bump라 fallout이 spring6보다 많을 수 있다.

**Files:**
- Test: `resilience4j-spring-boot3/src/test/java/**/*.java`
- Test resources: `resilience4j-spring-boot3/src/test/resources/**`
- 필요 시: `resilience4j-spring-boot3/src/main/resources/META-INF/spring/*.imports`

- [ ] **Step 1: Run spring-boot3 tests**

Run:
```bash
./gradlew :resilience4j-spring-boot3:test --no-daemon --continue 2>&1 | tail -100
```

Expected 결과 2가지:
- (A) `BUILD SUCCESSFUL` → Step 5로.
- (B) `FAILED` → Step 2로.

- [ ] **Step 2: Confirm version-69 errors are GONE**

```bash
grep -c 'Unsupported class file major version 69' resilience4j-spring-boot3/build/test-results/test/*.xml
```

Expected: `0`.

- [ ] **Step 3: Enumerate remaining failures and identify the FIRST (root) failure**

ApplicationContext failure threshold 패턴은 연쇄 실패이므로, 가장 먼저 발생한 실제 원인을 찾아야 한다.

```bash
# Find tests with real root causes, not "threshold exceeded" cascades
grep -l 'Caused by' resilience4j-spring-boot3/build/test-results/test/*.xml | head -3

# Print the first Caused by chain from each
for f in $(grep -l 'Caused by' resilience4j-spring-boot3/build/test-results/test/*.xml | head -3); do
  echo "=== $f ==="
  grep -A 2 'Caused by' "$f" | head -20
done
```

- [ ] **Step 4: Fix failures using Task 3 Step 4 playbook**

Task 3 Step 4의 playbook을 그대로 적용. spring-boot3에서 추가로 흔한 유형:

| 추가 증상 | 원인 | 최소 수정 |
|---|---|---|
| `Failed to load ApplicationContext` with `ClassNotFoundException: jakarta.servlet...` | Tomcat 10.1→11 API 변경 | 보통 직접 수정 불필요, dependency 정리로 해결됨. `./gradlew --stop && ./gradlew :resilience4j-spring-boot3:clean test` |
| `application.yml` 의 `management.endpoints.web.exposure.*` 키 인식 불가 | Actuator property 경로는 안 바뀜, 하지만 일부 observability 키는 변경됨 | 실패 메시지의 제안 키로 교체 |
| `CircuitBreakerFeignTest`의 FeignClient 설정 에러 | Spring Cloud OpenFeign 3.1→4.3 | `FeignClientsConfiguration` import / `Contract` 변경 |
| `TestApplication` 로드 실패, `WebSecurityConfigurerAdapter` 관련 | Spring Security 6.x 완전 제거된 API | 영향 없어야 함 (resilience4j는 security 미사용), 만약 transitively 등장하면 의존성 확인 |
| `ConfigurationPropertySource ... not bound` | `@ConstructorBinding` 위치 변경 (Spring Boot 3에서 class 레벨만 허용) | 이미 적용되어 있을 것. 아니라면 class 레벨로 이동 |

한 번에 한 유형만 수정 → Step 1 재실행 반복.

- [ ] **Step 5: Verify spring-boot3 all green**

Run:
```bash
./gradlew :resilience4j-spring-boot3:test --no-daemon 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`. `101 tests completed, 0 failed, 6 skipped` 또는 유사 (테스트 개수는 실제와 일치하면 OK).

검증: `grep -l 'failures="[1-9]\|errors="[1-9]' resilience4j-spring-boot3/build/test-results/test/*.xml` 결과 empty.

- [ ] **Step 6: Do NOT commit yet**

Task 5에서 전체 회귀 검증 후 일괄 커밋.

---

## Task 5: Full-build regression check

이전에 통과하던 모든 모듈이 여전히 통과하는지 확인. Spring Boot 3.5가 다른 모듈로 누수되진 않지만, `resilience4j-all` / `resilience4j-consumer` 등 cross-module 경계에서 transitives 영향이 있을 수 있음.

**Files:**
- Read-only (추가 수정 없음이 목표)

- [ ] **Step 1: Clean incremental test cache**

Run:
```bash
./gradlew --stop
```

Expected: `Stopping Daemon(s)` 또는 daemon이 없다는 메시지. 에러 없이 종료.

- [ ] **Step 2: Run full build with tests**

Run:
```bash
./gradlew build --continue 2>&1 | tail -40
```

Expected: `BUILD SUCCESSFUL`. 전체 task 중 실패한 테스트 task 없음.

- [ ] **Step 3: Verify no regression in previously passing modules**

```bash
# List any test result files with failures in modules OTHER than spring6/spring-boot3
for dir in resilience4j-*/build/test-results/test; do
  mod=$(echo "$dir" | cut -d/ -f1)
  if [ "$mod" = "resilience4j-spring6" ] || [ "$mod" = "resilience4j-spring-boot3" ]; then
    continue
  fi
  fails=$(grep -l 'failures="[1-9]\|errors="[1-9]' "$dir"/*.xml 2>/dev/null)
  if [ -n "$fails" ]; then
    echo "REGRESSION in $mod:"
    echo "$fails"
  fi
done
```

Expected: 출력 empty. 다른 모듈에서 실패 발생 시 해당 모듈 failure 로그 분석 후 minimal 수정 (단, 본 plan의 scope는 spring6/spring-boot3 이므로 타 모듈 실패는 "의외의 회귀"로 취급 — 원인 파악 후 user와 상의할지 판단).

- [ ] **Step 4: Final confirmation**

```bash
echo "spring6:"
./gradlew :resilience4j-spring6:test --no-daemon 2>&1 | grep -E '(BUILD|tests completed)'
echo "spring-boot3:"
./gradlew :resilience4j-spring-boot3:test --no-daemon 2>&1 | grep -E '(BUILD|tests completed)'
```

Expected: 양쪽 모두 `BUILD SUCCESSFUL`, `0 failed`.

---

## Task 6: Commit the bump + any fallout fixes

**Files:**
- Staged: `gradle/libs.versions.toml` + Task 3/4에서 수정한 모든 파일

- [ ] **Step 1: Review the diff**

```bash
git status
git diff --stat
git diff gradle/libs.versions.toml
```

Expected:
- `gradle/libs.versions.toml` 정확히 3줄 변경 (spring6/spring-boot3/spring-cloud3).
- 기타 수정 파일들이 있다면 각각 Task 3/4에서 의도한 fallout 수정인지 재확인.

- [ ] **Step 2: Stage explicitly (no `git add -A`)**

```bash
git add gradle/libs.versions.toml
# Task 3/4에서 수정한 개별 파일들을 명시적으로 add
# 예: git add resilience4j-spring-boot3/src/test/resources/application.yml
```

민감 파일이나 비관련 파일이 들어가지 않도록 명시적 add.

- [ ] **Step 3: Commit with a descriptive message**

```bash
git commit -m "$(cat <<'EOF'
bump Spring 6 / Boot 3 / Cloud 3 baselines for JDK 25

- spring6: 6.1.1 -> 6.2.18 (final 6.x LTS, JDK 25 supported)
- spring-boot3: 3.2.0 -> 3.5.13 (official JDK 25 support since 3.5.5)
- spring-cloud3: 3.1.5 -> 4.3.2 (Northfields 2025.0 train, Boot 3.5 compat)

Fixes "Unsupported class file major version 69" errors in
resilience4j-spring6 and resilience4j-spring-boot3 test suites
caused by older Spring-bundled ASM not supporting JDK 25 class files.

Design: docs/superpowers/specs/2026-04-22-jdk25-spring-bump-design.md
EOF
)"
```

- [ ] **Step 4: Verify commit**

```bash
git log -1 --stat
```

Expected: 방금 만든 커밋이 HEAD. 변경 파일 목록이 의도와 일치.

---

## Done Criteria

- [ ] `./gradlew :resilience4j-spring6:test` — BUILD SUCCESSFUL, 0 failed
- [ ] `./gradlew :resilience4j-spring-boot3:test` — BUILD SUCCESSFUL, 0 failed
- [ ] `./gradlew build` — BUILD SUCCESSFUL, 전체 회귀 없음
- [ ] `libs.versions.toml` 3줄만 의도대로 변경
- [ ] 프로덕션 core 코드(`resilience4j-core/src/main/...` 등) 건드리지 않음
- [ ] 하나의 커밋에 bump + fallout fix 묶임
