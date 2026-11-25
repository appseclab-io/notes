#!/bin/sh
# Writes the six SBOMs of the note and prints the table.
#
# It builds the specimen's image and saves it to a file for the scanners to read. Nothing here starts a container
# from that image, and no package of the specimen is ever run.
#
#   sh run.sh
#
# Needs Docker, and Node.js 22 with npm 11. The network is used to resolve the lockfile, to ask npm audit and to
# build the image; the six scans run with the network off.
#
#   IMAGE_TAR=/path/to/file.tar sh run.sh    where the saved image goes (about 65 MB; keep it out of the repository)
set -eu
cd "$(dirname "$0")"
here=$(pwd)
tar="${IMAGE_TAR:-${TMPDIR:-/tmp}/sbom-demo-image.tar}"

# On Intel or AMD, use the tag v13.8.0-amd64 and, in app/Dockerfile, the asset jq-linux-amd64.
CDXGEN=ghcr.io/cdxgen/cdxgen:v13.8.0-arm64
SYFT=anchore/syft:v1.57.2
TRIVY=aquasec/trivy:0.76.2

echo "== The lockfile, and what npm audit says about it"
(cd app && npm install --package-lock-only --ignore-scripts && npm audit --package-lock-only)

echo "== 1. npm sbom, from the lockfile"
(cd app && npm sbom --sbom-format cyclonedx --package-lock-only) > sbom-npm.json

echo "== 2. cdxgen, on the folder"
docker run --rm --network none -v "$here/app:/app:ro" -v "$here:/out" "$CDXGEN" \
  --spec-version 1.7 -o /out/sbom-cdxgen.json /app

echo "== 3. Syft, on the folder"
docker run --rm --network none -e SYFT_CHECK_FOR_APP_UPDATE=false -v "$here/app:/src:ro" "$SYFT" \
  scan dir:/src -o cyclonedx-json > sbom-syft-dir.json

echo "== 4. Trivy, on the folder"
docker run --rm --network none -v "$here/app:/src:ro" "$TRIVY" \
  fs --format cyclonedx --quiet /src > sbom-trivy-dir.json

echo "== The image: built and saved to a file, never run"
docker build -q -t sbom-demo:lab app
docker save sbom-demo:lab -o "$tar"

echo "== 5. Syft, on the image"
docker run --rm --network none -e SYFT_CHECK_FOR_APP_UPDATE=false -v "$tar:/in/image.tar:ro" "$SYFT" \
  scan docker-archive:/in/image.tar -o cyclonedx-json > sbom-syft-image.json

echo "== 6. Trivy, on the image"
docker run --rm --network none -v "$tar:/in/image.tar:ro" "$TRIVY" \
  image --input /in/image.tar --format cyclonedx --quiet > sbom-trivy-image.json

echo "== What each SBOM lists"
node check.mjs --detail
