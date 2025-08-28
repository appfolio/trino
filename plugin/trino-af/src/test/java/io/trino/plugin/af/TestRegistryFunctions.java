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
import io.trino.sql.SqlPath;
import io.trino.sql.query.QueryAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;

import java.net.URI;
import java.util.Optional;

import static io.trino.spi.type.VarcharType.VARCHAR;
import static io.trino.testing.TestingSession.testSessionBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@TestInstance(PER_CLASS)
@Execution(SAME_THREAD)
public class TestRegistryFunctions
{
    QueryAssertions assertions;
    FakeRegistryServer fakeRegistryServer;

    @BeforeAll
    public void init()
    {
        fakeRegistryServer = new FakeRegistryServer();
        URI registerServerUri = fakeRegistryServer.getBaseUri();

        assertions = new QueryAssertions(testSessionBuilder()
                .setPath(SqlPath.buildPath("af.af", Optional.empty()))
                .build());
        assertions.addPlugin(new AfPlugin());
        assertions.getQueryRunner().createCatalog("af", "af", ImmutableMap.<String, String>builder()
                .put("af.registry.endpoint", registerServerUri.toString())
                .put("af.registry.username", "testuser")
                .put("af.registry.password", "testpassword")
                .buildOrThrow());
    }

    @AfterAll
    public void teardown()
    {
        assertions.close();
        fakeRegistryServer.stop();
    }

    @Test
    public void testShowFunctions()
    {
        assertThat(assertions.query("SHOW FUNCTIONS LIKE 'af_%'"))
                .skippingTypesCheck()
                .matches("""
                        VALUES
                        ('af_find_datasource_by_guid', 'varchar', 'varchar', 'scalar', false, 'Find datasource by vhost guid')
                        """);
    }

    @Test
    public void testGetDatasourceByGuid()
    {
        assertThat(assertions.function("af_find_datasource_by_guid", "'test-guid-1'"))
                .hasType(VARCHAR)
                .isEqualTo("slice1");

        assertThat(assertions.function("af_find_datasource_by_guid", "'test-guid-2'"))
                .hasType(VARCHAR)
                .isEqualTo("slice2");
    }
}
