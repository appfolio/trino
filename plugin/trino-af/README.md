# Guide for trino-af Plugin
The `trino-af` plugin implements the `af_find_datasource_by_guid` function which receives a vhost guid and then calls [the registry app](https://github.com/appfolio/registry_app) to get the slice the vhost is active on.

Vhost databases with the same guid can exist on multiple slices. The function can be used in Trino's row filter to make sure Trino only return rows belong to the slice a vhost is active on.

# Development

## Install dependencies

```commandline
brew install trino jenv
```

## Install Java

Trino works with Temurin JDK 24.
```commandline
brew install temurin
jenv add /Library/Java/JavaVirtualMachines/temurin-24.jdk/Contents/Home
```

## Create a branch

### Pull all tags

May need to run the following if new trino verions are released.

```commandline
git remote add upstream https://github.com/trinodb/trino.git
git fetch upstream --tags
git push origin --tags
```

### Check out the Branch

```commandline
git checkout 476-af
```

All development work happen in this branch.

## Test

Run automated tests:

```commandline
cd plugin/trino-af/
../../mvnw test
```

Start a test server for manual testing:

```commandline
cd plugin/trino-af
../../mvnw compile test-compile exec:java
```

Access the test server:

```commandline
trino --server http://localhost:8080 --execute "select af.af.af_find_datasource_by_guid('test-guid-1')"
```

### Code Coverage

JaCoCo code coverage report is generated on CircleCI.

To manually generate coverage reports:

```commandline
cd plugin/trino-af/
mvn clean test -Djacoco.skip=false -Djava.vendor="Eclipse Adoptium"
```

After running, view the coverage report:

```commandline
open target/site/jacoco/index.html
```

The coverage report shows:
- **Instruction Coverage**: Percentage of bytecode instructions executed
- **Branch Coverage**: Percentage of conditional branches tested
- **Line Coverage**: Percentage of source code lines executed
- **Method Coverage**: Percentage of methods called
- **Class Coverage**: Percentage of classes instantiated

## Build

The plugin is built on CircleCI.

To manually build the plugin:

```commandline
cd plugin/trino-af/
../../mvnw package
```

`target/trino-af-476.zip` is the plugin file

## Push

CircleCI pushes a Trino container image with the plugin installed to ECR in the data-api-prod account.

To manually push an image:

```commandline
awsume data-api-prod
./build-and-push.sh
```

The output should have the image tag which can be used in Kubernetes Trino setup.
