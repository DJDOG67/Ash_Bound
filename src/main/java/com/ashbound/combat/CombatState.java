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
}
