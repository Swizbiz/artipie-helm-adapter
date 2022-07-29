/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/helm-adapter/LICENSE.txt
 */
package com.artipie.helm.http;

import com.artipie.asto.Storage;
import com.artipie.http.Slice;
import com.artipie.http.auth.Action;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthSlice;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;

/**
 * HelmSlice.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 */
public final class HelmSlice extends Slice.Wrap {

    /**
     * Ctor.
     *
     * @param storage The storage.
     * @param base The base path the slice is expected to be accessed from. Example: https://central.artipie.com/helm
     */
    public HelmSlice(final Storage storage, final String base) {
        this(storage, base, Permissions.FREE, Authentication.ANONYMOUS);
    }

    /**
     * Ctor.
     *
     * @param storage The storage.
     * @param base The base path the slice is expected to be accessed from. Example: https://central.artipie.com/helm
     * @param perms Access permissions.
     * @param auth Authentication.
     */
    public HelmSlice(
        final Storage storage,
        final String base,
        final Permissions perms,
        final Authentication auth) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new RtRule.Any(
                        new ByMethodsRule(RqMethod.PUT),
                        new ByMethodsRule(RqMethod.POST)
                    ),
                    new BasicAuthSlice(
                        new PushChartSlice(storage),
                        auth,
                        new Permission.ByName(perms, Action.Standard.WRITE)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.GET),
                        new RtRule.ByPath(DownloadIndexSlice.PTRN)
                    ),
                    new BasicAuthSlice(
                        new DownloadIndexSlice(base, storage),
                        auth,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new ByMethodsRule(RqMethod.GET),
                    new BasicAuthSlice(
                        new SliceDownload(storage),
                        auth,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(DeleteChartSlice.PTRN_DEL_CHART),
                        new ByMethodsRule(RqMethod.DELETE)
                    ),
                    new BasicAuthSlice(
                        new DeleteChartSlice(storage),
                        auth,
                        new Permission.ByName(perms, Action.Standard.DELETE)
                    )
                ),
                new RtRulePath(
                    RtRule.FALLBACK,
                    new SliceSimple(new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED))
                )
            )
        );
    }
}
