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
package com.artipie.helm.metadata;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.helm.ChartYaml;
import com.artipie.helm.TgzArchive;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Index.yaml file. The main file in a chart repo.
 *
 * @since 0.2
 * @checkstyle MethodBodyCommentsCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class IndexYaml {

    /**
     * The `index.yaml` string.
     */
    private static final Key INDEX_YAML = new Key.From("index.yaml");

    /**
     * An example of time this formatter produces: 2016-10-06T16:23:20.499814565-06:00 .
     */
    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnZZZZZ");

    /**
     * The RxStorage.
     */
    private final RxStorage storage;

    /**
     * The base path for urls field.
     */
    private final String base;

    /**
     * Ctor.
     * @param storage The storage.
     * @param base The base path for urls field.
     */
    public IndexYaml(final Storage storage, final String base) {
        this.storage = new RxStorageWrapper(storage);
        this.base = base;
    }

    /**
     * Update the index file.
     * @param arch New archive in a repo for which metadata is missing.
     * @return The operation result
     */
    public Completable update(final TgzArchive arch) {
        return this.indexFromStrg(
            Single.just(IndexYaml.empty())
        ).map(
            idx -> {
                this.update(idx, arch);
                return idx;
            }
        ).flatMapCompletable(this::indexToStorage);
    }

    /**
     * Delete from `index.yaml` file specified chart.
     * If the file `index.yaml` is missing an exception is thrown.
     * @param name Chart name
     * @return The operation result.
     */
    public Completable deleteByName(final String name) {
        return this.indexFromStrg(IndexYaml.notFoundException())
            .map(
                idx -> {
                    new IndexYamlMapping(idx).entries().remove(name);
                    return idx;
                }
            ).flatMapCompletable(this::indexToStorage);
    }

    /**
     * Delete from `index.yaml` file specified chart with given version.
     * If the file `index.yaml` is missing an exception is thrown.
     * @param name Chart name
     * @param version Version of the chart which should be deleted
     * @return The operation result.
     */
    public Completable deleteByNameAndVersion(final String name, final String version) {
        return this.indexFromStrg(IndexYaml.notFoundException())
            .map(
                idx -> {
                    final IndexYamlMapping mapping = new IndexYamlMapping(idx);
                    final List<Map<String, Object>> newvers;
                    newvers = mapping.entriesByChart(name).stream()
                        .filter(entry -> !entry.get("version").equals(version))
                        .collect(Collectors.toList());
                    mapping.entries().remove(name);
                    if (!newvers.isEmpty()) {
                        mapping.addNewChart(name, newvers);
                    }
                    return idx;
                }
        ).flatMapCompletable(this::indexToStorage);
    }

    /**
     * Return an empty Index mappings.
     * @return The empty yaml mappings.
     */
    private static Map<String, Object> empty() {
        final Map<String, Object> res = new HashMap<>(3);
        res.put("apiVersion", "v1");
        res.put("entries", new HashMap<String, Object>(0));
        res.put("generated", ZonedDateTime.now().format(IndexYaml.TIME_FORMATTER));
        return res;
    }

    /**
     * Generate exception.
     * @return Not found exception.
     */
    private static <T> Single<T> notFoundException() {
        return Single.error(
            new FileNotFoundException(
                String.format("File '%s' is not found", IndexYaml.INDEX_YAML)
            )
        );
    }

    /**
     * Perform an update.
     * @param index The index yaml mappings.
     * @param tgz The archive.
     */
    private void update(final Map<String, Object> index, final TgzArchive tgz) {
        final ChartYaml chart = tgz.chartYaml();
        final IndexYamlMapping mapping = new IndexYamlMapping(index);
        final String name = chart.name();
        mapping.addNewChart(name, new ArrayList<Map<String, Object>>(0));
        final List<Map<String, Object>> versions = mapping.entriesByChart(name);
        if (versions.stream().noneMatch(map -> map.get("version").equals(chart.version()))) {
            final Map<String, Object> newver = new HashMap<>();
            newver.put("created", ZonedDateTime.now().format(IndexYaml.TIME_FORMATTER));
            newver.put(
                "urls",
                new ArrayList<>(
                    Collections.singleton(String.format("%s%s", this.base, tgz.name()))
                )
            );
            newver.put("digest", tgz.digest());
            newver.putAll(chart.fields());
            // @todo #32:30min Create a unit test for urls field
            //  One of the fields Index.yaml require is "urls" field. The test should make verify
            //  that field has been generated correctly.
            versions.add(newver);
        }
    }

    /**
     * Obtain index.yaml file from storage.
     * @param notexist Value if index.yaml does not exist in the storage.
     * @return Mapping for index.yaml if exists, otherwise value specified in the parameter.
     */
    private Single<Map<String, Object>> indexFromStrg(final Single<Map<String, Object>> notexist) {
        return this.storage.exists(IndexYaml.INDEX_YAML)
            .flatMap(
                exist -> {
                    final Single<Map<String, Object>>  result;
                    if (exist) {
                        result =
                            this.storage.value(IndexYaml.INDEX_YAML)
                                .flatMap(content -> new Concatenation(content).single())
                                .map(buf -> new String(new Remaining(buf).bytes()))
                                .map(content -> new Yaml().load(content));
                    } else {
                        result = notexist;
                    }
                    return result;
                }
            );
    }

    /**
     * Save index mapping to storage.
     * @param index Mapping for `index.yaml`
     * @return The operation result.
     */
    private Completable indexToStorage(final Map<String, Object> index) {
        final DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        return this.storage.save(
            IndexYaml.INDEX_YAML,
            new Content.From(
                new Yaml(options).dump(index).getBytes(StandardCharsets.UTF_8)
            )
        );
    }
}
