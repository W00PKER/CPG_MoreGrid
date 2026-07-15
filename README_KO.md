# MoreGrid

Create: Power Grid 0.5.5.1용 NeoForge 1.21.1 회로 부품 애드온입니다.

회로판에 설치할 수 있는 다음 네 부품을 추가합니다.

- 회전 가능한 4단자 회로용 변압기
- I²t 동작을 근사한 일회용 카트리지 퓨즈
- 게이트 트리거와 유지전류를 갖는 래칭 SCR 사이리스터
- 방전·내부저항·역충전 손상이 구현된 아연-탄소 건전지

아이템은 전용 **MoreGrid 회로 부품** 탭과 바닐라 **레드스톤 블록** 탭에 모두 표시됩니다.

## 대상 환경

- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21
- Create 6.0.10
- Create: Power Grid 0.5.5.1

## 개발 환경 실행

1. `libs/` 폴더에 현행 Power Grid JAR를 넣습니다.

   ```text
   libs/powergrid-mc1.21.1-0.5.5.1.jar
   ```

   파일이 없으면 Gradle이 CurseMaven 의존성을 사용합니다.

2. IntelliJ IDEA에서 프로젝트 루트 폴더를 엽니다.
3. Gradle 프로젝트 불러오기가 끝날 때까지 기다립니다.
4. 실행 구성에서 `client`를 실행합니다.
5. 크리에이티브 탭에서 MoreGrid 부품을 찾거나 아래 명령을 사용합니다.

   ```mcfunction
   /give @s moregrid:transformer
   /give @s moregrid:fuse
   /give @s moregrid:scr
   /give @s moregrid:dry_cell
   ```

## 빌드

Windows:

```bat
gradlew.bat build
```

Linux/macOS:

```bash
./gradlew build
```

결과물:

```text
build/libs/moregrid-1.0.0.jar
```

## 부품 사양

### 회로용 변압기

단자:

- `P1`, `P2`: 1차 권선
- `S1`, `S2`: 2차 권선

속성 창에서 **전체 권선 수**와 **1차 권선 수**를 정수로 입력하거나 숫자 필드를 드래그합니다. 전체 권선은 2~30턴이고 1차 권선은 자동으로 `전체-1` 이하로 제한됩니다.

```text
2 ≤ Ntotal ≤ 30
1 ≤ Np < Ntotal
Ns = Ntotal - Np
Vs / Vp = Ns / Np
```

기본값은 `25:5`, 즉 120 V를 약 24 V로 변환하는 비율입니다.

Power Grid의 변압기 모델을 회로판 크기로 옮긴 집중정수 등가회로를 사용합니다.

```text
Lp = Np² × Al
Ls = Ns² × Al
Lm = K × Lp
Rp,stray = Lp - Lm
Rs,stray = Ls - (Ns/Np)² × Lm
Rmag = Lm × 10
```

상수:

- `Al = 1.5`
- `K = 0.9999`
- 총 권선 수 `2~30` (기본 30)

1차 누설저항, 2차 누설저항, 자화·철손 병렬저항에서 발생하는 `I²R` 손실이 모두 발열에 반영됩니다. 고글에는 전체 권선 전류와 총 손실이 표시됩니다. 입력전력 자체를 인위적으로 잘라내지 않고, 전류와 등가저항에서 발생하는 열이 실질적인 한계가 됩니다.

### 카트리지 퓨즈

- 정격 전류: `0.25–32 A`
- 정격 2배 전류에서의 차단시간: `0.05–30 s`
- 냉간저항: `clamp(0.04 / Irated, 0.001, 0.25) Ω`
- 일회용: 단선 후 자동 복구되지 않음

정격 초과 시 다음 손상이 누적됩니다.

```text
damage += ((I/Irated)² - 1) × dt / (3 × t2x)
```

정격 2배에서는 설정한 `t2x` 후에 단선됩니다. 정격 이하에서는 천천히 냉각되어 짧은 돌입전류를 어느 정도 견딥니다.

### SCR 사이리스터

단자:

- `A`: 애노드
- `K`: 캐소드
- `G`: 게이트

기본 사양:

- 게이트 트리거 전류: `20 mA`
- 유지전류: `40 mA`
- 도통저항: `0.15 Ω`
- 도통 전압강하: `1.20 V`
- 정격 애노드 전류 기준: `8 A`
- OFF 저항: `10 MΩ`

`VAK > 0.5 V`이며 게이트 전류가 트리거 전류 이상이면 켜집니다. 한 번 켜진 뒤에는 게이트가 사라져도 래치되고, 애노드 전류가 유지전류 아래로 떨어지거나 역바이어스되면 꺼집니다.

ON 상태는 다음의 구간선형 모델입니다.

```text
VAK = Vf + IA × Ron
```

### 아연-탄소 건전지

- 직렬 셀 수: `1–12`
- 셀당 완충 무부하 전압: `1.60 V`
- 셀당 방전 종료 전압: `0.90 V`
- 기본 용량: `2 Ah`
- 용량 설정 범위: `0.1–10 Ah`
- 셀당 내부저항: 완충 `0.15 Ω`, 방전 말기 `1.20 Ω`
- 비충전식: 역방향 전류는 정상 방전의 5배 속도로 손상
- 게임 시간 배율: `60×`

전압과 내부저항:

```text
Vcell = 0.9 + 0.7 × sqrt(SOC)
Rcell = 0.15 + 1.05 × (1-SOC)²
```

배터리 회로는 이상전압원과 직렬저항의 정확한 DC 등가인 **Norton 모델**로 구현되어, Power Grid 내부 전압원 생성자에 의존하지 않습니다.

## 회전

네 부품 모두 `VerticallyOrientableComponent`를 사용합니다. 회전해도 단자 번호와 의미는 유지됩니다.

- 변압기: `0=P1`, `1=P2`, `2=S1`, `3=S2`
- 퓨즈: `0`, `1`
- SCR: `0=A`, `1=K`, `2=G`
- 건전지: `0=+`, `1=-`

## 소스 구조

```text
src/main/java/com/feb/moregrid/
├── MoreGrid.java
├── component/
│   ├── TransformerComponent.java
│   ├── FuseComponent.java
│   ├── SCRComponent.java
│   └── DryCellComponent.java
├── registry/
│   ├── ModComponents.java
│   └── ModItems.java
├── sim/
│   ├── SCRWire.java
│   └── DryCellWire.java
└── util/
    └── MoreGridMath.java
```

## 확인 사항

이 소스 묶음은 다음 검사를 통과했습니다.

- 모든 Java 소스의 Java 21 구문 및 API 형태를 로컬 스텁으로 컴파일
- 모든 JSON 파일 파싱
- 변압기 권선비·등가저항 계산
- 퓨즈의 2배 정격 차단시간 계산
- 건전지 개방전압·내부저항 곡선
- 건전지 Norton 전류 방향과 SCR 래칭/소호 동작 단위 검사

현재 생성 환경은 외부 Gradle 서버 DNS 접속이 차단되어 실제 NeoForge 전체 의존성을 내려받는 `gradlew build`는 수행하지 못했습니다. 사용자의 IntelliJ 환경에서 첫 빌드를 실행해 실제 Power Grid 0.5.5.1 바이너리와 최종 연동을 확인하세요.
