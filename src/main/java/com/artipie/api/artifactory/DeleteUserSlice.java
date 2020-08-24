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
package com.artipie.api.artifactory;

import com.artipie.Settings;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import org.reactivestreams.Publisher;

/**
 * Artifactory `DELETE /api/security/users/{userName}` endpoint,
 * deletes user record from credentials.
 *
 * @since 0.10
 * @todo #444:30min Create class `UserFromRqLine`.
 *  Logic for getting username from request line is the same in 3 classes: here,
 *  `GetUserSlice` and in `AddUpdateUserSlice`. It would be nice to introduce
 *  class to obtain username from request line and move GetUserSlice.PTRN
 *  there. Class can be created in this package, accept line from request in ctor
 *  and have one Optional{String} get() method to get username.
 */
public final class DeleteUserSlice implements Slice {
    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * Ctor.
     * @param settings Setting
     */
    public DeleteUserSlice(final Settings settings) {
        this.settings = settings;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Response res;
        final Matcher matcher = GetUserSlice.PTRN.matcher(
            new RequestLineFrom(line).uri().toString()
        );
        if (matcher.matches()) {
            final String username = matcher.group("username");
            res = new AsyncResponse(
                this.settings.credentials().thenCompose(
                    cred -> cred.users().thenApply(
                        users -> users.contains(username)
                    ).thenCompose(
                        has -> {
                            final CompletionStage<Response> resp;
                            if (has) {
                                resp = cred.remove(username)
                                    .thenApply(ok -> new RsWithStatus(RsStatus.OK));
                            } else {
                                resp = CompletableFuture.completedFuture(StandardRs.NOT_FOUND);
                            }
                            return resp;
                        }
                    )
                )
            );
        } else {
            res = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return res;
    }
}