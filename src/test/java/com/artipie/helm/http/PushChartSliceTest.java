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
import com.artipie.helm.test.ContentOfIndex;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import org.cactoos.list.ListOf;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link PushChartSlice}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class PushChartSliceTest {
    /**
     * Storage for tests.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void shouldNotUpdateAfterUpload() {
        final String tgz = "ark-1.0.1.tgz";
        MatcherAssert.assertThat(
            "Wrong status, expected OK",
            new PushChartSlice(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.GET, "/?updateIndex=false"),
                Headers.EMPTY,
                new Content.From(new TestResource(tgz).asBytes())
            )
        );
        MatcherAssert.assertThat(
            "Index was generated",
            this.storage.list(Key.ROOT).join(),
            new IsEqual<>(new ListOf<Key>(new Key.From(tgz)))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"/?updateIndex=true", "/"})
    void shouldUpdateIndexAfterUpload(final String uri) {
        final String tgz = "ark-1.0.1.tgz";
        MatcherAssert.assertThat(
            "Wrong status, expected OK",
            new PushChartSlice(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.GET, uri),
                Headers.EMPTY,
                new Content.From(new TestResource(tgz).asBytes())
            )
        );
        MatcherAssert.assertThat(
            "Index was not updated",
            new ContentOfIndex(this.storage).index()
                .entries().keySet(),
            new IsEqual<>(new SetOf<>("ark"))
        );
    }
}
