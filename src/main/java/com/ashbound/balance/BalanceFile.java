package com.ashbound.balance;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;

/**
 * One balance JSON file: data/ashbound/balance/&lt;name&gt;.json.
 * Holds the parsed value; falls back to the default when the file is missing or broken.
 */
public final class BalanceFile<T> {
    private final Identifier id;
    private final Codec<T> codec;
    private final T defaultValue;
    private volatile T value;

    BalanceFile(Identifier id, Codec<T> codec, T defaultValue) {
        this.id = id;
        this.codec = codec;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public T get() {
        return value;
    }

    Identifier id() {
        return id;
    }

    Codec<T> codec() {
        return codec;
    }

    T defaultValue() {
        return defaultValue;
    }

    void set(T value) {
        this.value = value;
    }
}
