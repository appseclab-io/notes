#!/bin/sh
# Runs the note's two commands against your own hosts.
# usage: sh run.sh WEB_HOST FTP_HOST [GROUP]
#
# commands.sh is the note's code block, unchanged, with its placeholder hosts.
# This wrapper swaps www.example.com for WEB_HOST and ftp.example.com for FTP_HOST and runs the result.
# A third argument swaps the group offered, for the classical pass (X25519).
# Only point it at hosts you own.
set -u
usage='usage: sh run.sh WEB_HOST FTP_HOST [GROUP]'
web=${1:?$usage}
ftp=${2:?$usage}
group=${3:-X25519MLKEM768}
cd "$(dirname "$0")" || exit 1
openssl version
echo "groups offered: $group"
sed -e "s/www\.example\.com/$web/g" -e "s/ftp\.example\.com/$ftp/g" \
  -e "s/X25519MLKEM768/$group/g" commands.sh | sh
