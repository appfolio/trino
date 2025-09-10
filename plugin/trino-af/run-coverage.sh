#!/bin/bash

# Script to run tests with JaCoCo code coverage for trino-af plugin
# This script enables JaCoCo specifically for this plugin

echo "Running tests with JaCoCo code coverage for trino-af plugin..."

# Change to the plugin directory
cd "$(dirname "$0")"

# Run tests with JaCoCo enabled (using Java 21 compatible version)
mvn clean test -Djacoco.skip=false -Djava.vendor="Eclipse Adoptium"

echo "Coverage report generated at: target/site/jacoco/index.html"
echo "To view the report, open: target/site/jacoco/index.html in your browser"
