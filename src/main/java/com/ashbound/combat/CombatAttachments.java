package com.ashbound.combat;

import java.util.function.Supplier;

import com.ashbound.AshBound;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class CombatAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AshBound.MODID);

    /** Stamina, synced only to the player who owns it. */
    public static final Supplier<AttachmentType<StaminaData>> STAMINA = ATTACHMENT_TYPES.register("stamina",
            () -> AttachmentType.builder(() -> StaminaData.FULL)
                    .sync((holder, to) -> holder == to, StaminaData.STREAM_CODEC)
                    .build());

    /** Server-side timers (i-frames, regen delay, dodge chain). */
    public static final Supplier<AttachmentType<CombatState>> STATE = ATTACHMENT_TYPES.register("combat_state",
            () -> AttachmentType.builder(CombatState::new).build());

    /** Poise as clients see it (lock-on bar). Synced to everyone tracking the entity. */
    public static final Supplier<AttachmentType<PoiseData>> POISE = ATTACHMENT_TYPES.register("poise",
            () -> AttachmentType.builder(() -> PoiseData.NONE)
                    .sync((holder, to) -> true, PoiseData.STREAM_CODEC)
                    .build());

    /** Server-side precise poise, regen delay and stagger timer. Created on first hit. */
    public static final Supplier<AttachmentType<PoiseState>> POISE_STATE = ATTACHMENT_TYPES.register("poise_state",
            () -> AttachmentType.builder(PoiseState::new).build());

    /** Focus gauge + heavy charge timings, synced to the owning player. */
    public static final Supplier<AttachmentType<FocusData>> FOCUS = ATTACHMENT_TYPES.register("focus",
            () -> AttachmentType.builder(() -> FocusData.EMPTY)
                    .sync((holder, to) -> holder == to, FocusData.STREAM_CODEC)
                    .build());

    private CombatAttachments() {
    }
}
