/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town;

import java.util.Optional;

/** Pure validation and normalization for player-editable town and resident names. */
public final class TownNamingModel {
    public static final int MAX_TOWN_NAME_LENGTH = 64;
    public static final int MAX_RESIDENT_NAME_PART_LENGTH = 32;

    private TownNamingModel() {
    }

    public static Optional<String> normalizeTownName(String input) {
        return normalizeRequired(input, MAX_TOWN_NAME_LENGTH);
    }

    public static Optional<ResidentName> normalizeResidentName(String firstName, String lastName) {
        Optional<String> normalizedFirst = normalizeRequired(firstName, MAX_RESIDENT_NAME_PART_LENGTH);
        if (normalizedFirst.isEmpty()) return Optional.empty();
        String normalizedLast = normalizeOptional(lastName, MAX_RESIDENT_NAME_PART_LENGTH);
        return Optional.of(new ResidentName(normalizedFirst.get(), normalizedLast));
    }

    private static Optional<String> normalizeRequired(String input, int maximumLength) {
        String normalized = normalizeOptional(input, maximumLength);
        return normalized.isBlank() ? Optional.empty() : Optional.of(normalized);
    }

    private static String normalizeOptional(String input, int maximumLength) {
        if (input == null) return "";
        StringBuilder safe = new StringBuilder(Math.min(input.length(), maximumLength));
        boolean skipFormattingCode = false;
        for (int offset = 0; offset < input.length();) {
            int codePoint = input.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (skipFormattingCode) {
                skipFormattingCode = false;
                continue;
            }
            if (codePoint == '\u00a7') {
                skipFormattingCode = true;
                continue;
            }
            if (Character.isISOControl(codePoint)) continue;
            if (safe.length() + Character.charCount(codePoint) > maximumLength) break;
            safe.appendCodePoint(codePoint);
        }
        return safe.toString().strip();
    }

    public record ResidentName(String firstName, String lastName) {
    }
}
