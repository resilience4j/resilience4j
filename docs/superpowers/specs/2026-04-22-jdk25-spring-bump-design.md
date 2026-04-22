# JDK 25 호환을 위한 Spring / Spring Boot / Spring Cloud 3.x 버전 bump

- Date: 2026-04-22
- Branch: `feature/2343-bump-up-jdk-to-25`
- Related commit: `784c33c3 bump up jdk to 25`

## Problem

`bump up jdk to 25` 커밋 이후 다음 두 모듈의 테스트가 대량으로 실패한다.

- `resilience4j-spring6`
- `resilience4j-spring-boot3` (101 tests, 80 failed, 6 skipped)

다른 모든 모듈(`core`, `bulkhead`, `kotlin`, `metrics`, `ratelimiter`, `micrometer`, `micronaut`, `spring-boot4`, `feign`, `cache`, `retry`, `circuitbreaker`, `timelimiter`, `reactor`, `rxjava2/3`, `circularbuffer`, `commons-configuration`, `consumer`, `framework-common`, `hedge`, `vavr`, `all`)은 정상 통과한다.

## Root Cause

테스트 로그의 핵심 예외:

```
java.io.IOException: ASM ClassReader failed to parse class file - probably
  due to a new Java class file version that isn't supported yet
Caused by: java.lang.IllegalArgumentException: Unsupported class file major version 69
  at org.springframework.asm.ClassReader.<init>(ClassReader.java:199)
```

- Class file major version **69 = JDK 25** 바이트코드 포맷.
- 두 모듈이 의존하는 `spring6 = "6.1.1"` 및 `spring-boot3 = "3.2.0"` (내부 Spring 6.1.x)에 repackage 된 `org.springframework.asm`이 JDK 25 class 포맷을 파싱하지 못함.
- ASM 9.8부터 version 69가 지원되며, Spring Framework 6.2.x LTS 후기 패치 / Spring Boot 3.5.5+에서 해당 ASM을 repackage하여 동봉.
- `resilience4j-spring-boot4`(Spring Boot 4.0.0 → Spring 7.x)는 이미 신규 ASM이 포함되어 있어 통과한다.

## Scope

변경은 `gradle/libs.versions.toml`의 버전 변수 3개에 국한한다. 프로덕션 Java 코드는 수정하지 않는다. 단, 버전 점프로 인한 Spring/Spring Boot/Spring Cloud의 API/프로퍼티 변경에 대응하는 최소한의 테스트 코드 수정은 scope 내에 있다.

Out of scope:
- 다른 모듈의 테스트/코드 수정
- `spring7`/`spring-boot4`/`spring-cloud5` 버전 변경
- Kotlin, Micronaut, Mockito, JUnit 등 기타 의존성 변경

## Design

### 버전 매트릭스

| 변수 | 현재 | 변경 후 | 근거 |
|---|---|---|---|
| `spring6` | `6.1.1` | `6.2.18` | Spring Framework 6.x 세대 최종 feature branch. JDK 25를 LTS로 공식 지원. 6.2.x 후기 패치는 ASM 9.8 repackage 포함 |
| `spring-boot3` | `3.2.0` | `3.5.13` | Spring Boot 3.5.5+가 JDK 25 공식 지원. 3.5.13은 최신 GA 패치 |
| `spring-cloud3` | `3.1.5` | `4.3.2` | Spring Boot 3.5.x와 pair인 Spring Cloud 2025.0 (Northfields) 릴리즈 트레인의 최신 패치. `spring-cloud-context`, `spring-cloud-openfeign-core`, `spring-cloud-starter-openfeign` 공통 |

### 변경 대상 파일

`gradle/libs.versions.toml` 상단 versions 블록의 아래 3줄만 수정:

```diff
-spring6 = "6.1.1"
-spring-boot3 = "3.2.0"
-spring-cloud3 = "3.1.5"
+spring6 = "6.2.18"
+spring-boot3 = "3.5.13"
+spring-cloud3 = "4.3.2"
```

### 예상 추가 수정(실제 테스트 실행 후 필요시)

버전 점프 폭이 크므로 다음 유형의 후속 수정이 발생할 수 있다. 실제 실패가 관찰될 때만 최소 범위로 수정한다.

1. **Spring Boot 3.2 → 3.5 프로퍼티/어노테이션 변경**
   - `spring.factories` / `META-INF/spring/*.imports` 경로의 autoconfigure 등록
   - `@MockBean`의 deprecation (`@MockitoBean`으로 대체 권장)
   - actuator / observation 프로퍼티 키 변경
2. **Spring Cloud 2022.0 → 2025.0 OpenFeign API 변경**
   - `CircuitBreakerFeignTest` 주변의 FeignClient 설정
3. **Spring Framework 6.1 → 6.2 minor API deprecation**
   - `TestExecutionListener` / `ApplicationContextFailureProcessor` 관련
4. **번들 라이브러리 변경**
   - Jackson 2.17 → 2.19, Tomcat 10.1.x → 11, Netty 등

이 수정들은 본 설계의 부수 작업이며, 테스트 실행 → 실패 메시지 확인 → 최소 수정 → 재실행의 반복으로 처리한다.

## Verification

성공 기준:
1. `./gradlew :resilience4j-spring6:test` — 100% 통과, 회귀 없음
2. `./gradlew :resilience4j-spring-boot3:test` — 100% 통과, 회귀 없음
3. `./gradlew build` — 전체 회귀 없이 통과

검증 순서:
1. `libs.versions.toml` 3줄 수정 후 compile 확인 (`./gradlew compileTestJava`)
2. `spring6`, `spring-boot3` 단위 테스트 실행
3. 실패가 있으면 로그 분석 후 최소 수정, 2번으로 복귀
4. 두 모듈 통과 시 전체 빌드로 회귀 검증

## Rollback

변경이 단일 파일 3줄이므로 `git revert` 또는 해당 라인을 이전 값(`6.1.1` / `3.2.0` / `3.1.5`)으로 원복한다. 추가 테스트 코드 수정이 발생한 경우 같은 커밋 범위 내에서 함께 revert.

## Risks

- **낮음:** `libs.versions.toml` 외 프로덕션 코드 변경 없음
- **중간:** 버전 점프 폭이 커서 테스트 코드 일부 조정이 필요할 가능성이 있음 (추가 수정 범위는 실제 실패 로그로 결정)
- **낮음:** `spring-boot4`/`spring-cloud5` 모듈은 독립 변수를 쓰므로 간섭 없음

## References

- Spring Framework Versions wiki — https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-Versions
- Spring Boot Java 25 지원 issue (#47245) — https://github.com/spring-projects/spring-boot/issues/47245
- Spring Boot 3.5.7 release note — https://spring.io/blog/2025/10/23/spring-boot-3-5-7-available-now/
- Spring Cloud 2025.0.0 Northfields GA — https://spring.io/blog/2025/05/29/spring-cloud-2025-0-0-is-abvailable/
- Spring Cloud Supported Versions — https://github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions
