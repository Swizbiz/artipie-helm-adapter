/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/helm-adapter/LICENSE.txt
 */
package com.artipie.helm.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.helm.test.ContentOfIndex;
import com.artipie.http.misc.RandomFreePort;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * IT for remove operation.
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class HelmDeleteIT {
    /**
     * Vert instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * The server.
     */
    private VertxSliceServer server;

    /**
     * Port.
     */
    private int port;

    /**
     * URL connection.
     */
    private HttpURLConnection conn;

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.port = new RandomFreePort().get();
        this.server = new VertxSliceServer(
            HelmDeleteIT.VERTX,
            new LoggingSlice(
                new HelmSlice(this.storage, String.format("http://localhost:%d", this.port))
            ),
            this.port
        );
        this.server.start();
    }

    @AfterAll
    static void tearDownAll() {
        HelmDeleteIT.VERTX.close();
    }

    @AfterEach
    void tearDown() {
        this.conn.disconnect();
        this.server.close();
    }

    @Test
    void chartShouldBeDeleted() throws Exception {
        Stream.of("index.yaml", "ark-1.0.1.tgz", "ark-1.2.0.tgz", "tomcat-0.4.1.tgz")
            .forEach(source -> new TestResource(source).saveTo(this.storage));
        this.conn = (HttpURLConnection) new URL(
            String.format("http://localhost:%d/charts/tomcat", this.port)
        ).openConnection();
        this.conn.setRequestMethod(RqMethod.DELETE.value());
        this.conn.setDoOutput(true);
        MatcherAssert.assertThat(
            "Response status is not 200",
            this.conn.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        MatcherAssert.assertThat(
            "Archive was not deleted",
            this.storage.exists(new Key.From("tomcat-0.4.1.tgz")).join(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Index was not updated",
            new ContentOfIndex(this.storage).index().byChart("tomcat").isEmpty(),
            new IsEqual<>(true)
        );
    }
}
