# Ash_Bound

엘든링 · 디아블로 · 붉은사막 · 검은신화: 오공 · 패스 오브 엑자일의 메커니즘을 섞은
**자유도 높은 소울라이크 ARPG 오버홀 모드** (Minecraft Java Edition).

- 게임 버전: Minecraft 26.3
- 로더: NeoForge 26.3.x
- 언어: Java 25
- 기획서: claude.ai 문서 「마인크래프트 소울라이크 ARPG 모드 기획서」

## 폴더 구조

```
Ash_Bound/
├ docs/        결정 기록, 시스템별 1페이지 기획
├ art/         아트 원본 (게임에 직접 들어가지 않음)
│  ├ sketches/      A3 볼펜 스케치 스캔
│  ├ concept/       AI 컨셉·Vizcom 렌더
│  ├ models/        Blockbench .bbmodel 원본
│  └ textures_src/  텍스처 원본 (.psd 등)
├ tools/       텍스처 변형 자동 생성 등 보조 스크립트
└ src/main/
   ├ java/        코드
   └ resources/
      ├ assets/   게임에 들어가는 텍스처·모델·애니메이션·번역
      └ data/     밸런스 JSON (접사, 루트, 패시브 트리, 보스, 작물, 세력)
```

## 작업 규칙

- `art/` 는 원본, `src/main/resources/assets/` 는 게임용 결과물. 둘을 섞지 않는다.
- 밸런스 수치는 코드에 하드코딩하지 않고 `data/` 의 JSON으로 뺀다.
- `.psd`, `.blend`, 스케치 스캔 등 큰 파일은 Git LFS로 관리된다 (`.gitattributes`).
- 기능 하나 = 브랜치 하나. 게임에서 테스트 후 `main` 에 합친다.
