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

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.airlift.bootstrap.Bootstrap;
import io.airlift.bootstrap.LifeCycleManager;
import io.airlift.http.server.testing.TestingHttpServer;
import io.airlift.http.server.testing.TestingHttpServerModule;
import io.airlift.json.JsonCodec;
import io.airlift.node.testing.TestingNodeModule;
import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class FakeRegistryServer
{
    private final LifeCycleManager lifeCycleManager;
    private final URI baseUri;

    public FakeRegistryServer()
    {
        Bootstrap app = new Bootstrap(
                new TestingNodeModule(),
                new TestingHttpServerModule("fake-registry-server"),
                new RegistryServerModule());

        Injector injector = app
                .doNotInitializeLogging()
                .initialize();

        lifeCycleManager = injector.getInstance(LifeCycleManager.class);
        baseUri = injector.getInstance(TestingHttpServer.class).getBaseUrl();
    }

    public void stop()
    {
        lifeCycleManager.stop();
    }

    public URI resolve(String s)
    {
        return baseUri.resolve(s);
    }

    public URI getBaseUri()
    {
        return baseUri;
    }

    private static class RegistryServerModule
            implements Module
    {
        @Override
        public void configure(Binder binder)
        {
            binder.bind(Servlet.class).toInstance(new RegistryHttpServlet());
        }
    }

    private static class RegistryHttpServlet
            extends HttpServlet
    {
        private static final String EXPECTED_USERNAME = "testuser";
        private static final String EXPECTED_PASSWORD = "testpassword";
        private final JsonCodec<VhostResponse> vhostResponseCodec = JsonCodec.jsonCodec(VhostResponse.class);
        // Mock data for testing
        private final Map<String, VhostResponse> mockData = createMockData();

        private static Map<String, VhostResponse> createMockData()
        {
            Map<String, VhostResponse> data = new HashMap<>();
            data.put("test-guid-1", new VhostResponse("test-guid-1", "example.com", "slice1.poh1", "app1"));
            data.put("test-guid-2", new VhostResponse("test-guid-2", "test.com", "slice2.por1", "app2"));
            data.put("sample-guid", new VhostResponse("sample-guid", "sample.org", "host.sample.org", "sample-app"));
            return data;
        }

        private boolean isAuthorized(HttpServletRequest request)
        {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Basic ")) {
                return false;
            }

            String base64Credentials = authHeader.substring("Basic ".length());
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            String[] parts = credentials.split(":", 2);

            if (parts.length != 2) {
                return false;
            }

            return EXPECTED_USERNAME.equals(parts[0]) && EXPECTED_PASSWORD.equals(parts[1]);
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws IOException
        {
            // Check basic authentication
            if (!isAuthorized(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setHeader("WWW-Authenticate", "Basic realm=\"Registry Server\"");
                response.getWriter().write("{\"error\": \"Unauthorized\"}");
                return;
            }

            String path = request.getPathInfo();

            if ("/vhosts".equals(path)) {
                String id = request.getParameter("id");
                if (id == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"error\": \"Missing id parameter\"}");
                    return;
                }

                VhostResponse vhostResponse = mockData.get(id);
                if (vhostResponse == null) {
                    // Return a default response for unknown GUIDs
                    vhostResponse = new VhostResponse(id, "default.com", "defaultslice.defaultvdc", "default-app");
                }

                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(vhostResponseCodec.toJson(vhostResponse));
            }
            else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\": \"Not found\"}");
            }
        }
    }

    // Local copy of VhostResponse record for JSON serialization
    public record VhostResponse(String guid, String domain, String host, String app)
    {
    }
}
