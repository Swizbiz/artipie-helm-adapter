/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/helm-adapter/LICENSE.txt
 */
package com.artipie.helm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.helm.metadata.IndexYamlMapping;
import com.artipie.helm.test.ContentOfIndex;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link DeleteChartSlice}.
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class DeleteChartSliceTest {
    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"", "/charts", "/charts/", "/charts/name/1.3.2/extra", "/wrong/name/0.1.1"}
        )
    void returnBadRequest(final String rqline) {
        MatcherAssert.assertThat(
            new DeleteChartSlice(this.storage).response(
                new RequestLine(RqMethod.DELETE, rqline).toString(),
                Headers.EMPTY,
                Content.EMPTY
            ),
            new RsHasStatus(RsStatus.BAD_REQUEST)
        );
    }

    @Test
    void deleteAllVersionsByName() {
        final String arkone = "ark-1.0.1.tgz";
        final String arktwo = "ark-1.2.0.tgz";
        Stream.of("index.yaml", "ark-1.0.1.tgz", "ark-1.2.0.tgz", "tomcat-0.4.1.tgz")
            .forEach(source -> new TestResource(source).saveTo(this.storage));
        MatcherAssert.assertThat(
            "Response status is not 200",
            new DeleteChartSlice(this.storage).response(
                new RequestLine(RqMethod.DELETE, "/charts/ark").toString(),
                Headers.EMPTY,
                Content.EMPTY
            ),
            new RsHasStatus(RsStatus.OK)
        );
        MatcherAssert.assertThat(
            "Deleted chart is present in index",
            new ContentOfIndex(this.storage).index()
                .byChart("ark").isEmpty(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Archive of deleted chart remains",
            this.storage.exists(new Key.From(arkone)).join(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Archive of deleted chart remains",
            this.storage.exists(new Key.From(arktwo)).join(),
            new IsEqual<>(false)
        );
    }

    @Test
    void deleteByNameAndVersion() {
        Stream.of("index.yaml", "ark-1.0.1.tgz", "ark-1.2.0.tgz", "tomcat-0.4.1.tgz")
            .forEach(source -> new TestResource(source).saveTo(this.storage));
        MatcherAssert.assertThat(
            "Response status is not 200",
            new DeleteChartSlice(this.storage).response(
                new RequestLine(RqMethod.DELETE, "/charts/ark/1.0.1").toString(),
                Headers.EMPTY,
                Content.EMPTY
            ),
            new RsHasStatus(RsStatus.OK)
        );
        final IndexYamlMapping index = new ContentOfIndex(this.storage).index();
        MatcherAssert.assertThat(
            "Deleted chart is present in index",
            index.byChartAndVersion("ark", "1.0.1").isPresent(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Second chart was also deleted",
            index.byChartAndVersion("ark", "1.2.0").isPresent(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Archive of deleted chart remains",
            this.storage.exists(new Key.From("ark-1.0.1.tgz")).join(),
            new IsEqual<>(false)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"/charts/not-exist", "/charts/ark/0.0.0"})
    void failsToDeleteByNotExisted(final String rqline) {
        Stream.of("index.yaml", "ark-1.0.1.tgz", "ark-1.2.0.tgz", "tomcat-0.4.1.tgz")
            .forEach(source -> new TestResource(source).saveTo(this.storage));
        MatcherAssert.assertThat(
            new DeleteChartSlice(this.storage).response(
                new RequestLine(RqMethod.DELETE, rqline).toString(),
                Headers.EMPTY,
                Content.EMPTY
            ),
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }
}
