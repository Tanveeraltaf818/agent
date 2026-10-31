#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
gradle -q assembleRelease
mkdir -p out
cp app/build/outputs/apk/release/app-release.apk out/agent.apk
ls -la out/
