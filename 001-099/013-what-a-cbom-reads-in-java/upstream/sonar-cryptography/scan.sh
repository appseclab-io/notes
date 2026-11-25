#!/usr/bin/env bash
# Scans the three modules as one project with a SonarQube server that has the
# cryptography plugin, and leaves the scanner log beside this script.
#
#   SONAR_HOST_URL=https://sonar.example.com ./scan.sh
#
# The server needs a project notes-java-cbom-repro on a Java quality profile
# with the rule "Cryptographic Inventory (CBOM)". The token is read from
# SONAR_TOKEN, or asked for without echo when it is unset.
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
echo "Scanning (log: sonar-repro.log) ..."
mvn -B -l sonar-repro.log compile \
  org.sonarsource.scanner.maven:sonar-maven-plugin:5.9.0.7382:sonar \
  -Dsonar.host.url="${SONAR_HOST_URL}" \
  -Dsonar.projectKey=notes-java-cbom-repro
code=$?
echo "repro: exit ${code}"
exit "${code}"
