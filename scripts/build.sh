#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build
javac -Xlint:all -Werror -d build src/*.java
