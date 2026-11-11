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
    /** Combo step / length of the attack that is about to land (0 = not a combo weapon). */
    public int pendingStep;
    public int pendingLength;
    /** Ticks left in which a hit is parried (opened when the shield is raised). */
    public int parryWindow;
    /** Ticks before raising the shield opens a new parry window. */
    public int parryCooldown;
    /** Focus gauge in pips (1.0 = one pip). */
    public float focus;
    /** Ticks since focus was last gained (for decay). */
    public int focusIdle;
    /** Ticks before another heavy attack is accepted. */
    public int heavyCooldown;
    /** Poise multiplier for the heavy hit that is being dealt right now (1 = none). */
    public float pendingHeavyPoise = 1f;
}
