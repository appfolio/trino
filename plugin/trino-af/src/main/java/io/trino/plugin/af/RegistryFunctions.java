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

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.airlift.slice.Slice;
import io.trino.spi.function.BoundSignature;
import io.trino.spi.function.FunctionDependencies;
import io.trino.spi.function.FunctionId;
import io.trino.spi.function.FunctionMetadata;
import io.trino.spi.function.FunctionProvider;
import io.trino.spi.function.InvocationConvention;
import io.trino.spi.function.ScalarFunctionAdapter;
import io.trino.spi.function.ScalarFunctionImplementation;
import io.trino.spi.function.Signature;
import io.trino.spi.type.TypeSignature;

import java.lang.invoke.MethodHandle;
import java.util.List;

import static io.airlift.slice.Slices.utf8Slice;
import static io.trino.spi.function.InvocationConvention.InvocationArgumentConvention.NEVER_NULL;
import static io.trino.spi.function.InvocationConvention.InvocationReturnConvention.FAIL_ON_NULL;
import static io.trino.spi.type.VarcharType.VARCHAR;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import static java.util.Collections.nCopies;
import static java.util.Objects.requireNonNull;

public class RegistryFunctions
        implements FunctionProvider
{
    private static final TypeSignature TEXT = VARCHAR.getTypeSignature();
    private static final List<FunctionMetadata> FUNCTIONS = ImmutableList.<FunctionMetadata>builder()
            .add(function("af_find_datasource_by_guid")
                    .description("Find datasource by vhost guid")
                    .signature(signature(TEXT, TEXT))
                    .build())
            .build();
    private static final MethodHandle AF_FIND_DATASOURCE_BY_GUID;

    static {
        try {
            AF_FIND_DATASOURCE_BY_GUID = lookup().findSpecial(RegistryFunctions.class, "afFindDatasourceByGuid", methodType(Slice.class, Slice.class), RegistryFunctions.class);
        }
        catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private final RegistryClient client;

    @Inject
    public RegistryFunctions(RegistryClient client)
    {
        this.client = requireNonNull(client, "client is null");
    }

    private static String getDataSourceFromHost(String host)
    {
        int idx = host.indexOf('.');
        if (idx < 0) {
            return host;
        }
        return host.substring(0, idx);
    }

    private static FunctionMetadata.Builder function(String name)
    {
        return FunctionMetadata.scalarBuilder(name)
                .functionId(new FunctionId(name))
                .nondeterministic();
    }

    private static Signature signature(TypeSignature returnType, TypeSignature... argumentTypes)
    {
        return Signature.builder()
                .returnType(returnType)
                .argumentTypes(List.of(argumentTypes))
                .build();
    }

    public List<FunctionMetadata> getFunctions()
    {
        return FUNCTIONS;
    }

    @Override
    public ScalarFunctionImplementation getScalarFunctionImplementation(
            FunctionId functionId,
            BoundSignature boundSignature,
            FunctionDependencies functionDependencies,
            InvocationConvention invocationConvention)
    {
        String name = functionId.toString();
        MethodHandle handle = switch (name) {
            case "af_find_datasource_by_guid" -> AF_FIND_DATASOURCE_BY_GUID;
            default -> throw new IllegalArgumentException("Invalid function ID: " + functionId);
        };
        handle = handle.bindTo(this);

        InvocationConvention actualConvention = new InvocationConvention(
                nCopies(boundSignature.getArity(), NEVER_NULL),
                FAIL_ON_NULL,
                false,
                false);

        handle = ScalarFunctionAdapter.adapt(
                handle,
                boundSignature.getReturnType(),
                boundSignature.getArgumentTypes(),
                actualConvention,
                invocationConvention);

        return ScalarFunctionImplementation.builder()
                .methodHandle(handle)
                .build();
    }

    public Slice afFindDatasourceByGuid(Slice guid)
    {
        String host = client.findHostByGuid(guid.toStringUtf8());
        String datasource = getDataSourceFromHost(host);
        return utf8Slice(datasource);
    }
}
