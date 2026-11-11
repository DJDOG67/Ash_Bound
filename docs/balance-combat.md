# 전투 밸런스 값 (combat.json)

파일: `src/main/resources/data/ashbound/balance/combat.json`
수정 후 게임 안에서 `/reload` 를 치면 바로 적용된다. (게임 재시작 불필요)
시간 단위: 1초 = 20틱.

| 키 | 기본값 | 의미 |
|---|---|---|
| `max_stamina` | 100 | 최대 스태미나 |
| `regen_per_second` | 20 | 초당 회복량 (0에서 가득 차는 데 5초) |
| `regen_delay_ticks` | 12 | 행동 후 회복이 다시 시작될 때까지 (0.6초) |
| `exhausted_delay_ticks` | 30 | 스태미나를 0까지 다 쓰면 회복 대기 (1.5초) — 탈진 페널티 |
| `attack_cost` | 12 | 근접 공격 1회 소모량. 0이면 공격 불가 |
| `dodge_cost` | 20 | 구르기 1회 소모량 |
| `dodge_speed` | 1.0 | 구르기 수평 속도. 올리면 더 멀리 감 (1.0 ≈ 2~3블록) |
| `dodge_hop` | 0.1 | 구르기 순간 살짝 뜨는 높이. 0이면 바닥을 미끄러짐 |
| `dodge_iframe_ticks` | 6 | 구르기 무적 시간 (0.3초) |
| `dodge_cooldown_ticks` | 8 | 연속 구르기 최소 간격 (0.4초) |
| `chain_window_ticks` | 20 | 연속 구르기로 묶이는 시간 창 (1초) |
| `chain_limit` | 3 | 창 안에서 이 횟수를 넘기면 무적 시간 절반 (구르기 난사 방지) |
