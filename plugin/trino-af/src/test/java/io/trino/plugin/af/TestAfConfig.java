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
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static io.airlift.configuration.testing.ConfigAssertions.assertFullMapping;
import static io.airlift.configuration.testing.ConfigAssertions.assertRecordedDefaults;
import static io.airlift.configuration.testing.ConfigAssertions.recordDefaults;

public class TestAfConfig
{
    @Test
    public void testDefaults()
    {
        assertRecordedDefaults(recordDefaults(AfConfig.class)
                .setRegistryEndpoint(null)
                .setRegistryUsername(null)
                .setRegistryPassword(null)
                .setRegistryCacheTtl(0)
                .setRegistryCacheMaxEntries(0));
    }

    @Test
    public void testExplicitPropertyMappings()
    {
        Map<String, String> properties = ImmutableMap.<String, String>builder()
                .put("af.registry.endpoint", "http://localhost:8080/registry")
                .put("af.registry.username", "testuser")
                .put("af.registry.password", "testpassword")
                .put("af.registry.cache-ttl", "10")
                .put("af.registry.cache-max-entries", "1000")
                .buildOrThrow();

        AfConfig expected = new AfConfig()
                .setRegistryEndpoint(URI.create("http://localhost:8080/registry"))
                .setRegistryUsername("testuser")
                .setRegistryPassword("testpassword")
                .setRegistryCacheTtl(10)
                .setRegistryCacheMaxEntries(1000);

        assertFullMapping(properties, expected);
    }
}
