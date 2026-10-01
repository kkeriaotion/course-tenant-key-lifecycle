#!/usr/bin/env sh
set -eu

BUILD_DIR="${TMPDIR:-/tmp}/tenant-keys-java-build"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java src/test/java -name '*.java' -print)
java -cp "$BUILD_DIR" education.tenantkeys.TenantLifecycleServiceTest
