#!/usr/bin/env bash
# Scans app/ and probe/ with a SonarQube server that has the cryptography
# plugin, and leaves each scanner log beside this script.
#
#   SONAR_HOST_URL=https://sonar.example.com ./scan.sh
#
# The server needs two projects, notes-java-cbom-app and notes-java-cbom-probe,
# on a Java quality profile with the rule "Cryptographic Inventory (CBOM)".
# The token is read from SONAR_TOKEN, or asked for without echo when it is unset.
set -u

: "${SONAR_HOST_URL:?set SONAR_HOST_URL to the address of your SonarQube}"

if [ -z "${SONAR_TOKEN:-}" ]; then
  read -r -s -p "SonarQube token: " SONAR_TOKEN
  echo
  export SONAR_TOKEN
fi
if [ -z "${SONAR_TOKEN}" ]; then
  echo "No token given, nothing was scanned." >&2
  exit 2
fi

cd "$(dirname "$0")" || exit 2
status=0
for part in app probe; do
  echo "Scanning ${part} (log: sonar-${part}.log) ..."
  (cd "${part}" && mvn -B -l "../sonar-${part}.log" compile \
    org.sonarsource.scanner.maven:sonar-maven-plugin:5.9.0.7382:sonar \
    -Dsonar.host.url="${SONAR_HOST_URL}" \
    -Dsonar.projectKey="notes-java-cbom-${part}")
  code=$?
  echo "${part}: exit ${code}"
  [ "${code}" -eq 0 ] || status=1
done
exit "${status}"
