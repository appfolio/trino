# Guide for trino-af Plugin
The `trino-af` plugin implements the `af_find_datasource_by_guid` function which receives a vhost guid and then calls [the registry app](https://github.com/appfolio/registry_app) to get the slice the vhost is active on.

Vhost databases with the same guid can exist on multiple slices. The function can be used in Trino's row filter to make sure Trino only return rows belong to the slice a vhost is active on.

# Development

## Install dependencies

```commandline
brew install trino jenv
```

## Install Java

Trino works with Temurin JDK 25.
```commandline
brew install temurin@25
jenv add /Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home
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
git checkout 479-af
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

`target/trino-af-479.zip` is the plugin file

## Push

CircleCI pushes a Trino container image with the plugin installed to ECR in the data-api-prod account.

To manually push an image:

```commandline
awsume data-api-prod
./build-and-push.sh
```

The output should have the image tag which can be used in Kubernetes Trino setup.

# Upgrade Trino

## Upgrade local dependencies

```commandline
brew install trino jenv
```

## Upgrade Java

Trino usually works with the latest JDK. [Trino's documentation](https://github.com/trinodb/trino?tab=readme-ov-file#build-requirements) specifies which JDK version to use. If the local JDK needs upgrade (24 -> 25, for example), follow the instructions below:

```commandline
brew install temurin@25
jenv add /Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home
jenv rehash --force
```

## Pull new tags

```commandline
git fetch upstream --tags
git push origin --tags
```

## Checkout a branch

Take upgrading to Trino 479 as an example:

```commandline
git checkout 479
git checkout -b 479-af
```

## Cherry-pick from the previous version

Cherry-pick last two changes from [our Trino 477 branch](https://github.com/appfolio/trino/commits/477-af/) for 479 upgrade.

```commandline
git cherry-pick 24d05d8 8c42954
```

## Upgrade to the new version

- In `plugin/trino-af/Dockerfile`, upgrade Trino base image to the new version and change the source path of COPY as well.
- In `plugin/trino-af/pom.xml`, update the parent `trino-root` version to be the new version.
- In `.circleci/config.yml`, upgrade OpenJDK image to match Trino's JDK version.
- In `plugin/trino-af/README.md`, update all references of the previous Trino version to the new version as well as JDK version.

## Test

Run automated tests:

```commandline
cd plugin/trino-af/
../../mvnw test
```

If there are test failures, get Claude Code to help fix those failures.

## Commit and push

It may be good to separate changes to different commits for readability. Commit and push all changes. [CircleCI](https://app.circleci.com/pipelines/github/appfolio/trino) should build and push a new container image for Trino.
