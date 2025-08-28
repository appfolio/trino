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

import com.google.common.collect.ImmutableMap;
import io.airlift.log.Logger;
import io.trino.testing.DistributedQueryRunner;
import io.trino.testing.QueryRunner;

import java.net.URI;

import static io.trino.testing.TestingSession.testSessionBuilder;

public final class AfQueryRunner
{
    private AfQueryRunner()
    {
    }

    public static void main(String[] args)
            throws Exception
    {
        FakeRegistryServer fakeRegistryServer = new FakeRegistryServer();
        URI registerServerUri = fakeRegistryServer.getBaseUri();

        QueryRunner queryRunner = DistributedQueryRunner.builder(testSessionBuilder().build())
                .addCoordinatorProperty("http-server.http.port", "8080")
                .addCoordinatorProperty("sql.path", "af.af")
                .build();
        queryRunner.installPlugin(new AfPlugin());

        queryRunner.createCatalog("af", "af", ImmutableMap.<String, String>builder()
                .put("af.registry.endpoint", registerServerUri.toString())
                .put("af.registry.username", "testuser")
                .put("af.registry.password", "testpassword")
                .put("af.registry.cache-ttl", "10")
                .put("af.registry.cache-max-entries", "1000")
                .buildOrThrow());
        Logger log = Logger.get(AfQueryRunner.class);
        log.info("======== SERVER STARTED ========");
        log.info("\n====\n%s\n====", queryRunner.getCoordinator().getBaseUrl());
        log.info("Registry Server URI: %s", registerServerUri);
    }
}
