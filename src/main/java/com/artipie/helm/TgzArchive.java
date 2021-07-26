/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.helm;

import com.artipie.asto.ArtipieIOException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/**
 * A .tgz archive file.
 * @since 0.2
 */
@SuppressWarnings({
    "PMD.ArrayIsStoredDirectly",
    "PMD.AssignmentInOperand"
})
public final class TgzArchive {

    /**
     * The archive content.
     */
    private final byte[] content;

    /**
     * Chart yaml file.
     */
    private final ChartYaml chart;

    /**
     * Ctor.
     * @param content The archive content.
     */
    public TgzArchive(final byte[] content) {
        this.content = content;
        this.chart = new ChartYaml(this.file("Chart.yaml"));
    }

    /**
     * Obtain archive name.
     * @return How the archive should be named on the file system
     */
    public String name() {
        return String.format("%s-%s.tgz", this.chart.name(), this.chart.version());
    }

    /**
     * Metadata of archive.
     *
     * @param baseurl Base url.
     * @return Metadata of archive.
     */
    public Map<String, Object> metadata(final Optional<String> baseurl) {
        final Map<String, Object> meta = new HashMap<>();
        meta.put(
            "urls",
            new ArrayList<>(
                Collections.singletonList(
                    String.format(
                        "%s%s",
                        baseurl.orElse(""),
                        this.name()
                    )
                )
            )
        );
        meta.put("digest", DigestUtils.sha256Hex(this.content));
        meta.putAll(this.chart.fields());
        return meta;
    }

    /**
     * Find a Chart.yaml file inside.
     * @return The Chart.yaml file.
     */
    public ChartYaml chartYaml() {
        return this.chart;
    }

    /**
     * Obtains binary content of archive.
     * @return Byte array with content of archive.
     */
    public byte[] bytes() {
        return Arrays.copyOf(this.content, this.content.length);
    }

    /**
     * Obtain file by name.
     *
     * @param name The name of a file.
     * @return The file content.
     */
    private String file(final String name) {
        try {
            final TarArchiveInputStream taris = new TarArchiveInputStream(
                new GzipCompressorInputStream(new ByteArrayInputStream(this.content))
            );
            TarArchiveEntry entry;
            while ((entry = taris.getNextTarEntry()) != null) {
                if (entry.getName().endsWith(name)) {
                    return new BufferedReader(new InputStreamReader(taris))
                        .lines()
                        .collect(Collectors.joining("\n"));
                }
            }
            throw new IllegalStateException(String.format("'%s' file wasn't found", name));
        } catch (final IOException exc) {
            throw new ArtipieIOException(exc);
        }
    }
}
