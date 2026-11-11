package com.ashbound.combat;

/** Server-only poise bookkeeping for any living entity that has been hit. */
public class PoiseState {
    public boolean initialized;
    public float current;
    public float max;
    /** Ticks before poise starts to regenerate. */
    public int regenDelay;
    /** Remaining stagger (groggy) ticks. 0 = not staggered. */
    public int stagger;
    /** Ticks this entity is still held by a grab (frozen in place, can't attack). */
    public int held;
    /** Ticks this entity is still flying from a throw (can't attack, keeps its momentum). */
    public int thrown;
}
