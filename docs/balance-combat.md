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
| `attack_cost` | 12 | 콤보 없는 무기(바닐라 검·맨손) 공격 1회 소모량. 0이면 공격 불가 |
| `dodge_cost` | 20 | 구르기 1회 소모량 |
| `dodge_speed` | 1.0 | 구르기 수평 속도. 올리면 더 멀리 감 (1.0 ≈ 2~3블록) |
| `dodge_hop` | 0.1 | 구르기 순간 살짝 뜨는 높이. 0이면 바닥을 미끄러짐 |
| `dodge_iframe_ticks` | 6 | 구르기 무적 시간 (0.3초) |
| `dodge_cooldown_ticks` | 8 | 연속 구르기 최소 간격 (0.4초) |
| `chain_window_ticks` | 20 | 연속 구르기로 묶이는 시간 창 (1초) |
| `chain_limit` | 3 | 창 안에서 이 횟수를 넘기면 무적 시간 절반 (구르기 난사 방지) |
| `combo_window_ticks` | 16 | 이 시간 안에 다음 타를 맞히면 콤보 계속 (0.8초) |
| `combo_damage_multipliers` | [1.0, 1.1, 1.5] | 1·2·3타 피해 배율. 개수가 곧 콤보 타수 |
| `combo_stamina_costs` | [12, 12, 18] | 1·2·3타 스태미나 소모 |
| `combo_finisher_knockback` | 0.8 | 마지막 타 넉백 세기 |

콤보는 `data/ashbound/tags/item/weapon_types/straight_sword.json` 태그에 들어간 무기에만 적용된다.
다른 무기(바닐라 철검 등)를 직검 콤보로 쓰고 싶으면 이 태그 `values` 에 아이템 ID를 추가하면 된다.

## 락온 설정 (밸런스가 아니라 개인 취향 → 게임 안 설정 화면)

Mods → Ash_Bound → Config → Client → 락온: 거리, 카메라 회전 속도, 선택 각도, 대상 체력바 표시.
