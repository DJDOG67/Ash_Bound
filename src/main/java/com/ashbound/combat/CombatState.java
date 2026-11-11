package com.ashbound.combat;

/**
 * Server-only, per-player combat timers. Not saved, not synced:
 * everything here resets harmlessly on relog.
 */
public class CombatState {
    /** Ticks before stamina starts regenerating again. */
    public int regenDelay;
    /** Remaining invulnerability ticks from a dodge. */
    public int iframes;
    /** Ticks before another dodge is accepted. */
    public int dodgeCooldown;
    /** Dodges done inside the current chain window. */
    public int chainCount;
    /** Ticks left in the chain window. */
    public int chainTimer;
    /** Current hit in the weapon combo (1-based, 0 = no combo running). */
    public int comboStep;
    /** Ticks left before the combo resets to the first hit. */
    public int comboTimer;
    /** Damage multiplier for the attack that is about to land (set on swing, used on damage). */
    public float pendingDamageMultiplier = 1f;
    /** True if the attack that is about to land is a combo finisher. */
    public boolean pendingFinisher;
}
