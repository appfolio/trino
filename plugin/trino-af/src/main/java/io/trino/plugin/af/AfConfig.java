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

import io.airlift.configuration.Config;
import io.airlift.configuration.ConfigSecuritySensitive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.net.URI;

public class AfConfig
{
    private URI registryEndpoint;
    private String registryUsername;
    private String registryPassword;
    private int registryCacheTtl;
    private int registryCacheMaxEntries;

    @NotNull
    public URI getRegistryEndpoint()
    {
        return registryEndpoint;
    }

    @Config("af.registry.endpoint")
    public AfConfig setRegistryEndpoint(URI registryEndpoint)
    {
        this.registryEndpoint = registryEndpoint;
        return this;
    }

    @NotBlank
    public String getRegistryUsername()
    {
        return registryUsername;
    }

    @Config("af.registry.username")
    public AfConfig setRegistryUsername(String registryUsername)
    {
        this.registryUsername = registryUsername;
        return this;
    }

    @NotBlank
    public String getRegistryPassword()
    {
        return registryPassword;
    }

    @Config("af.registry.password")
    @ConfigSecuritySensitive
    public AfConfig setRegistryPassword(String registryPassword)
    {
        this.registryPassword = registryPassword;
        return this;
    }

    @NotNull
    public int getRegistryCacheTtl()
    {
        return registryCacheTtl;
    }

    @Config("af.registry.cache-ttl")
    public AfConfig setRegistryCacheTtl(int registryCacheTtl)
    {
        this.registryCacheTtl = registryCacheTtl;
        return this;
    }

    @NotNull
    public int getRegistryCacheMaxEntries()
    {
        return registryCacheMaxEntries;
    }

    @Config("af.registry.cache-max-entries")
    public AfConfig setRegistryCacheMaxEntries(int registryCacheMaxEntries)
    {
        this.registryCacheMaxEntries = registryCacheMaxEntries;
        return this;
    }
}
