#!/bin/sh
# Two checks behind sentences of the note, beyond the table. Needs Docker; the network stays off.
#
#   sh extras.sh
set -eu
cd "$(dirname "$0")"
here=$(pwd)
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

CDXGEN=ghcr.io/cdxgen/cdxgen:v13.8.0-arm64
SYFT=anchore/syft:v1.57.2
TRIVY=aquasec/trivy:0.76.2

# Prints "name@version" for every library of the CycloneDX file given on standard input.
libraries() {
  node -e '
    let text = "";
    process.stdin.on("data", (chunk) => (text += chunk)).on("end", () => {
      const list = (JSON.parse(text).components ?? []).filter((c) => c.type === "library");
      console.log(list.map((c) => `${c.name}@${c.version}`).join(", ") || "no library");
    });'
}

echo "== 1. The development dependency (mime), with each tool's switch turned on"
echo "Syft, SYFT_JAVASCRIPT_INCLUDE_DEV_DEPENDENCIES=true:"
docker run --rm --network none -e SYFT_CHECK_FOR_APP_UPDATE=false -e SYFT_JAVASCRIPT_INCLUDE_DEV_DEPENDENCIES=true \
  -v "$here/app:/src:ro" "$SYFT" scan dir:/src -o cyclonedx-json 2>/dev/null | libraries
echo "Trivy, --include-dev-deps:"
docker run --rm --network none -v "$here/app:/src:ro" "$TRIVY" \
  fs --format cyclonedx --quiet --include-dev-deps /src 2>/dev/null | libraries

echo "== 2. Why cdxgen listed jQuery: the same file, three ways"
mkdir -p "$work/a/js" "$work/b/js" "$work/c/js" "$work/out"
# a. The same bytes under a name that does not end in min.js
cp app/public/js/jquery-3.4.1.min.js "$work/a/js/jquery-3.4.1.js"
# b. The same name, without the first line (the banner "/*! jQuery v3.4.1 | ...")
tail -n +2 app/public/js/jquery-3.4.1.min.js > "$work/b/js/jquery-3.4.1.min.js"
# c. The lodash file of vendor/, renamed to end in min.js (its header is not a "/*!" banner)
cp app/vendor/lodash.js "$work/c/js/lodash.min.js"
for variant in a b c; do
  docker run --rm --network none -v "$work/$variant:/app:ro" -v "$work/out:/out" "$CDXGEN" \
    --spec-version 1.7 -t js -o "/out/$variant.json" /app > /dev/null 2>&1
  printf '%s: ' "$variant"
  libraries < "$work/out/$variant.json"
done

echo "== 3. Why Syft listed jq: the same binary under its own name and under another"
# The binary comes out of the image run.sh built. The container is created to copy the file and is never started.
mkdir -p "$work/named/bin" "$work/renamed/bin"
container=$(docker create sbom-demo:lab)
docker cp -q "$container:/usr/local/bin/jq" "$work/named/bin/jq"
docker rm "$container" > /dev/null
cp "$work/named/bin/jq" "$work/renamed/bin/json-tool"
for variant in named renamed; do
  printf '%s: ' "$variant"
  docker run --rm --network none -e SYFT_CHECK_FOR_APP_UPDATE=false -v "$work/$variant:/src:ro" "$SYFT" \
    scan dir:/src -o cyclonedx-json 2>/dev/null | node -e '
      let text = "";
      process.stdin.on("data", (chunk) => (text += chunk)).on("end", () => {
        const list = (JSON.parse(text).components ?? []).filter((c) => c.type !== "file");
        console.log(list.map((c) => `${c.type} ${c.name}@${c.version}`).join(", ") || "no entry");
      });'
done
