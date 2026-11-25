#!/bin/sh
# Does Trivy write, in any output, where an npm dependency that is not from the registry came from?
# Two lockfiles: one dependency from a tarball address, one from git. Trivy only reads them:
# nothing is installed or executed, and the network stays off. Needs Docker.
#
#   sh run.sh > results.txt 2>&1
set -eu
cd "$(dirname "$0")"
here=$(pwd)
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

TRIVY=aquasec/trivy:0.76.2

docker run --rm --network none "$TRIVY" --version

for variant in tarball git; do
  echo
  echo "== $variant: what the lockfile says"
  grep '"resolved"' "$variant/package-lock.json"
  for format in cyclonedx spdx-json json; do
    # Trivy's own JSON lists packages only when asked, and needs a scanner that works offline.
    options=""
    if [ "$format" = json ]; then options="--scanners license --list-all-pkgs"; fi
    # shellcheck disable=SC2086
    docker run --rm --network none -v "$here/$variant:/src:ro" "$TRIVY" \
      fs --format "$format" $options --quiet /src > "$work/$variant-$format.json"
    echo "-- $format: lines with github.com: $(grep -c 'github.com' "$work/$variant-$format.json" || true); package URLs:"
    grep -o 'pkg:npm/[^"]*' "$work/$variant-$format.json" | sort -u
  done
done

echo
echo "== Trivy still holds the address when it writes the report: a custom template prints it"
docker run --rm --network none -v "$here/tarball:/src:ro" "$TRIVY" \
  fs --format template --scanners license --list-all-pkgs --quiet \
  --template '{{ range . }}{{ range .Packages }}{{ .Name }}: {{ .ExternalReferences }}{{ "\n" }}{{ end }}{{ end }}' /src
