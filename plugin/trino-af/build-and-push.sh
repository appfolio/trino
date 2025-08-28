#!/bin/bash
set -e

TRINO_BASE_VERSION=476
TRINO_AF_REPO=data-api/trino-af

echo "Step 1: Building Maven package..."
../../mvnw clean package

echo "Step 2: Finding ECR repository for trino-af..."
ECR_REPO_URI=$(aws ecr describe-repositories --repository-names $TRINO_AF_REPO --query 'repositories[0].repositoryUri' --output text)
if [ "$ECR_REPO_URI" == "None" ] || [ -z "$ECR_REPO_URI" ]; then
    echo "Error: ECR repository 'trino-af' not found"
    exit 1
fi
echo "Found ECR repository: $ECR_REPO_URI"

echo "Step 3: Finding latest version tag..."
LATEST_VERSION=$(aws ecr list-images --repository-name $TRINO_AF_REPO --query 'imageIds[?imageTag!=`null`].imageTag' --output text | tr '\t' '\n' | grep -E '\-[0-9]+$' | sort -V | tail -1)
if [ -z "$LATEST_VERSION" ]; then
    NEW_VERSION="$TRINO_BASE_VERSION-1"
else
    # Extract the number after the dash and increment it
    VERSION_NUMBER=$(echo "$LATEST_VERSION" | grep -oE '[0-9]+$')
    NEW_VERSION="$TRINO_BASE_VERSION-$((VERSION_NUMBER + 1))"
fi
echo "Latest version: ${LATEST_VERSION:-none}, New version: $NEW_VERSION"

echo "Step 4: Authenticating with ECR..."
aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_REPO_URI

echo "Step 5: Building multi-architecture Docker image..."
docker buildx create --use --name multiarch-builder 2>/dev/null || docker buildx use multiarch-builder
docker buildx build --platform linux/arm64,linux/amd64 -t $ECR_REPO_URI:$NEW_VERSION --push .

echo "Successfully built and pushed $ECR_REPO_URI:$NEW_VERSION"
