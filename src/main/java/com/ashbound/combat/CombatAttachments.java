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

    private CombatAttachments() {
    }
}
