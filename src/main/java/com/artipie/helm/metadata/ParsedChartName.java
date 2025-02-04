/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/helm-adapter/LICENSE.txt
 */
package com.artipie.helm.metadata;

/**
 * Encapsulates parsed chart name for validation.
 * @since 0.3
 */
public final class ParsedChartName {
    /**
     * Entries.
     */
    private static final String ENTRS = "entries:";

    /**
     * Chart name.
     */
    private final String name;

    /**
     * Ctor.
     * @param name Parsed from file with breaks chart name
     */
    public ParsedChartName(final String name) {
        this.name = name;
    }

    /**
     * Validates chart name.
     * @return True if parsed chart name is valid, false otherwise.
     */
    public boolean valid() {
        final String trimmed = this.name.trim();
        return trimmed.endsWith(":")
            && !trimmed.equals(ParsedChartName.ENTRS)
            && trimmed.charAt(0) != '-';
    }
}
