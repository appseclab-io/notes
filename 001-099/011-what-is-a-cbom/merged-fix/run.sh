#!/bin/sh
# Runs a cdxgen source tree inside the cdxgen image, with the dependencies the
# image already has. The source is mounted read-only at /src and the code to
# scan read-only at /app. Nothing is installed, so the network can be off.
#
# Argument 1: where to write the CBOM.
set -u

here=$(dirname "$0")

mkdir -p /tmp/cdx || exit 1
for item in bin lib data package.json; do
  cp -R "/src/$item" /tmp/cdx/ || exit 1
done
ln -s /opt/cdxgen/node_modules /tmp/cdx/node_modules || exit 1

node "$here/deps.mjs" /opt/cdxgen/package.json /tmp/cdx/package.json || exit 1
exec node /tmp/cdx/bin/cdxgen.js --include-crypto --spec-version 1.7 -o "$1" /app
