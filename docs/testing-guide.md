# 테스트 가이드 — 재시작이 필요한가?

게임은 `src/` 를 직접 읽지 않고 **빌드된 복사본**을 읽는다. 그래서 수정 후에는 항상 **빌드(Ctrl+F9) → 게임 안 새로고침** 순서.

## 재시작 판단표

| 바뀐 것 | 재시작 | 적용 방법 |
|---|---|---|
| 밸런스 JSON (`data/ashbound/...`) | ❌ | Ctrl+F9 → 게임에서 `/reload` |
| 텍스처·모델·애니메이션·번역 (`assets/ashbound/...`) | ❌ | Ctrl+F9 → 게임에서 `F3 + T` |
| 설정 화면 값 (Mods → Ash_Bound → Config) | ❌ | 바로 적용 |
| 자바: 기존 메서드 안의 로직만 수정 | ❌ (🐞 디버그 실행일 때) | Ctrl+F9 → 알림에서 **Reload changed classes** |
| 자바: 새 아이템·블록·엔티티·키·첨부데이터·패킷 등록 | ⭕ | 게임 종료 → 🐞 Client 다시 실행 |
| 자바: 새 클래스·필드·메서드 추가, 메서드 시그니처 변경 | ⭕ | 게임 종료 → 🐞 Client 다시 실행 |
| `build.gradle`, `gradle.properties`, 라이브러리 추가 | ⭕ + Gradle 동기화 | 코끼리 알림 → 'Gradle 변경 사항 로드' → 🐞 Client |

## 실행 방법

- 상단 실행 목록에서 **Client** 선택 → **🐞 디버그** 버튼으로 실행 (▶ 보다 디버그가 코드 즉시 반영에 유리).
- Gradle 패널의 `runClient` 로 실행했다면 Ctrl+F9 대신 Gradle 패널의 `processResources` 를 더블클릭한 뒤 `/reload`.

## 코드를 받아오는 방법

- Claude가 로컬 폴더에 직접 반영했다고 하면 → 할 일 없음. IntelliJ가 바뀐 파일을 자동으로 읽는다.
- GitHub에만 올렸다고 하면 → GitHub Desktop에서 **Fetch origin → Pull**.

## 자주 쓰는 테스트 명령

| 명령 | 용도 |
|---|---|
| `/gamemode survival` | 스태미나 등은 서바이벌에서만 동작 |
| `/summon zombie` | 근접 전투 테스트 상대 |
| `/reload` | 데이터 JSON 다시 읽기 |
| `/time set day`, `/weather clear` | 테스트 환경 정리 |
| `F3 + T` | 텍스처·모델 다시 읽기 |
