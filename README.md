# fixed-width-codec

KSP 기반 컴파일 타임 코드 생성으로 만든 **zero-reflection 고정길이 레코드 코덱**. GraalVM native-image 호환.

## 왜 필요한가

고정길이(fixed-width / fixed-length) 레코드는 생각보다 많이 쓰입니다. 금융권 전문(電文), COBOL 카피북, 레거시 플랫 파일, EDI, 메인프레임 데이터 덤프 등. JVM 생태계에 기존 라이브러리는 있지만 대부분 짐이 따라옵니다.

| 라이브러리 | 방식 | 단점 |
|---|---|---|
| Apache Camel Bindy | 런타임 리플렉션 | Camel 스택 필요. 리플렉션 오버헤드. GraalVM 호환 어려움 |
| BeanIO | 런타임 리플렉션 + XML 설정 | 외부 설정 파일. 리플렉션 |
| uniVocity-parsers | 런타임 리플렉션 | 빠르지만 여전히 리플렉션 |
| 직접 구현 | 명령형 파싱 | 반복 작업. offset 실수 나기 쉬움 |

`fixed-width-codec`은 다른 트레이드오프를 택합니다 — **어노테이션으로 레이아웃만 선언하면 KSP가 컴파일 타임에 매퍼를 생성**합니다. 리플렉션 없음, 런타임 설정 없음. 생성된 코드는 여러분이 손으로 짰을 법한 모양 그대로입니다.

- **제로 런타임 리플렉션** — 생성된 코드는 일반 Kotlin 코드로 바로 컴파일됨
- **GraalVM native-image 친화적** — 리플렉션 메타데이터 설정 불필요
- **프레임워크 중립** — Spring / Camel / Netty 의존성 없음
- **컴파일 타임 안전성** — 필드 offset, 길이, 타입이 빌드 시점에 검증됨
- **Kotlin 네이티브** — data class, nullable 타입, val 프로퍼티 그대로

## 호환성

| 항목 | 최소 지원 | 빌드 검증 | 비고 |
|---|---|---|---|
| **Kotlin** | `2.0.0` | `2.3.20` | KSP2가 Kotlin 2.0부터 지원 |
| **JVM (바이트코드 타깃)** | `11` | `11`, `17`, `21` (CI) | JVM 11 기준 컴파일, 상위 호환 |
| **KSP** | `2.0.x` | `2.3.6` | KSP2. KSP1은 미지원 |
| **Gradle** | `8.0` | `8.14` | `com.google.devtools.ksp` 플러그인 요구사항과 동일 |
| **Android** | 별도 검증 안 됨 | — | 이론상 JVM 타깃 11+인 Android 프로젝트에서 동작. 미보증 |

> 최소 버전 미만에서의 동작은 보장하지 않습니다. 호환성 이슈 제보 받습니다.

## 설치

> 아직 Maven Central에 게시 전입니다.

```kotlin
plugins {
    kotlin("jvm") version "2.3.20"
    id("com.google.devtools.ksp") version "2.3.6"
}

dependencies {
    implementation("io.github.tax1116:fixed-width-codec-core:0.1.0")
    ksp("io.github.tax1116:fixed-width-codec-processor:0.1.0")
}
```

**아티팩트 구성**:
- **`-core`**: 어노테이션 + 런타임 기저 클래스. 런타임 classpath (의존성: Kotlin stdlib만)
- **`-processor`**: KSP 프로세서. 빌드 타임 classpath (kotlinpoet 의존.)

## 시작하기

레코드를 Kotlin data class + 어노테이션으로 선언합니다.

```kotlin
import io.github.tax1116.fixedwidthcodec.annotations.*

@Record(charset = "US-ASCII")
data class AccountBalance(
    @MetaField(length = 4)
    val totalSize: Long,

    @MetaField(length = 4)
    val messageCode: Long,

    @MetaField(length = 6)
    val requestId: Long,

    @MetaField(length = 15, align = Align.LEFT)
    val accountNumber: String,

    @MetaField(length = 15, align = Align.RIGHT, paddingChar = '0')
    val balanceAmount: String,

    @MetaField(length = 10)
    val reservedArea: String? = null,
)
```

빌드하면 KSP가 `AccountBalanceRecordMapper`를 자동 생성합니다.

```kotlin
// 생성된 코드 — 수정하지 않음
public object AccountBalanceRecordMapper : AbstractRecordMapper() {
    public override val charset: Charset = Charset.forName("US-ASCII")

    public fun serialize(record: AccountBalance): ByteArray { /* ... */ }
    public fun deserialize(bytes: ByteArray): AccountBalance { /* ... */ }
}
```

사용법:

```kotlin
val record = AccountBalance(
    totalSize = 54,
    messageCode = 2000,
    requestId = 123,
    accountNumber = "1234567890",
    balanceAmount = "000000000050000",
)

val bytes: ByteArray = AccountBalanceRecordMapper.serialize(record)
val parsed: AccountBalance = AccountBalanceRecordMapper.deserialize(bytes)
```

리플렉션 없음. 런타임 등록 없음. 그냥 평범한 함수 호출 하나.

## 지원 어노테이션

| 어노테이션 | 대상 | 용도 |
|---|---|---|
| `@Record(charset)` | class | 고정길이 레코드 선언. charset은 바이트↔문자열 변환에 사용됨 |
| `@MetaField(length, paddingChar, align, pattern)` | property | 고정 바이트 길이 스칼라 필드 |
| `@ObjectField(length)` | property | 중첩된 레코드 객체 (다른 `@Record` 클래스) |
| `@ArrayField(type, size, sizeField, elementLength, ...)` | property | 반복 필드. 크기는 고정, 다른 필드 참조, 런타임 파라미터 중 선택 |

지원 타입: `String`, `Int`, `Long`, `Double`, 중첩 `@Record` 클래스, 위 타입들의 `List<T>`.

## 다른 라이브러리 선택 가이드

- **Apache Camel Bindy** — 리플렉션 기반, Camel 생태계가 이미 있는 프로젝트. 리플렉션 싫고 Camel 안 쓸 거면 `fixed-width-codec`
- **BeanIO** — XML 설정 + 리플렉션. **런타임에 스키마가 바뀌는 경우**에 적합. 정적 스키마면 `fixed-width-codec`
- **uniVocity-parsers** — CSV/고정길이 대용량 파싱에 최적화된 리플렉션 기반 파서. 빅데이터 ingest용. **타입 안전한 Kotlin data class 매핑**이 필요하면 `fixed-width-codec`

## 로드맵

- [ ] v0.1 — 내부 코드베이스에서 추출, Maven Central 게시
- [ ] v0.2 — `BigDecimal` + implied decimal 지원
- [ ] v0.2 — `LocalDate` / `LocalDateTime` + pattern 지원
- [ ] v0.3 — `@Record(code = "...")` 기반 레코드 variant dispatch + 컴파일 타임 레지스트리

## 라이선스

Apache License 2.0. [LICENSE](LICENSE) 참고.
