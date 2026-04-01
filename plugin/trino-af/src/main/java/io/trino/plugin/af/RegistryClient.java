/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.plugin.af;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import io.airlift.http.client.HttpClient;
import io.airlift.http.client.Request;
import io.airlift.json.JsonCodec;
import io.trino.cache.NonEvictableLoadingCache;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static io.airlift.http.client.JsonResponseHandler.createJsonResponseHandler;
import static io.airlift.http.client.Request.Builder.prepareGet;
import static io.airlift.json.JsonCodec.jsonCodec;
import static io.trino.cache.SafeCaches.buildNonEvictableCache;

public class RegistryClient
{
    private static final JsonCodec<VhostResponse> VHOST_RESPONSE_CODEC = jsonCodec(VhostResponse.class);
    private final URI endpoint;
    private final String username;
    private final String password;
    private final HttpClient httpClient;
    private final NonEvictableLoadingCache<String, String> hostCache;

    @Inject
    public RegistryClient(@ForRegistryClient HttpClient httpClient, AfConfig config)
    {
        this.httpClient = httpClient;
        this.endpoint = config.getRegistryEndpoint();
        this.username = config.getRegistryUsername();
        this.password = config.getRegistryPassword();
        this.hostCache = buildNonEvictableCache(
                CacheBuilder.newBuilder()
                        .maximumSize(config.getRegistryCacheMaxEntries())
                        .expireAfterWrite(Duration.ofSeconds(config.getRegistryCacheTtl())),
                CacheLoader.from(this::fetchHostByGuid));
    }

    private static String getBasicAuthHeader(String username, String password)
    {
        String token = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    public String findHostByGuid(String guid)
    {
        try {
            return hostCache.getUnchecked(guid);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to fetch host for guid: " + guid, e);
        }
    }

    private String fetchHostByGuid(String guid)
    {
        String query = "?id=" + URLEncoder.encode(guid, StandardCharsets.UTF_8);
        URI requestUri = endpoint.resolve("/vhosts" + query);
        Request request = prepareGet()
                .setUri(requestUri)
                .setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthHeader(username, password))
                .build();

        VhostResponse response = httpClient.execute(request, createJsonResponseHandler(VHOST_RESPONSE_CODEC));
        return response.host();
    }

    public record VhostResponse(String guid, String domain, String host, String app)
    {
    }
}
