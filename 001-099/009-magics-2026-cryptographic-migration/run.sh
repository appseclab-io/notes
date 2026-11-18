#!/bin/sh
# Runs the note's commands in a work directory, because they write a 1 GiB file.
# usage: sh run.sh WORKDIR [MiB]
#   WORKDIR  a folder outside the repository with room for the file (created if missing)
#   MiB      size of the message; 1024 by default, as in the note. The note's second run used 256.
#
# commands.sh is the note's code block, unchanged. This wrapper adds three things around it:
#   - /usr/bin/time in front of each "openssl pkeyutl -sign", to read its peak memory, as the note says
#   - the exit status of cmp and the size of each file, at the end
#   - 50 more ECDSA signatures of the same digest, to see the range of their sizes
set -u
usage='usage: sh run.sh WORKDIR [MiB]'
work=${1:?$usage}
mib=${2:-1024}
here=$(cd "$(dirname "$0")" && pwd)
mkdir -p "$work" && cd "$work" || exit 1

case $(uname) in Darwin) flag=-l ;; *) flag=-v ;; esac
openssl() {
  if [ "$1" = pkeyutl ] && [ "$2" = -sign ]; then
    echo "--- /usr/bin/time $flag openssl $*"
    /usr/bin/time "$flag" openssl "$@"
  else
    command openssl "$@"
  fi
}

openssl version
echo "message: $mib MiB"
sed "s/count=1024/count=$mib/" "$here/commands.sh" > commands-run.sh
. ./commands-run.sh

echo "--- added by run.sh: cmp again, and the size of each file in bytes"
cmp full.sig mu.sig
echo "cmp full.sig mu.sig: exit status $?"
wc -c msg.bin digest.bin pk.bin tr.bin mu.bin ec.sig full.sig mu.sig
echo "--- added by run.sh: 50 more ECDSA signatures of the same digest (how many, bytes)"
i=0
while [ "$i" -lt 50 ]; do
  command openssl pkeyutl -sign -inkey ec.pem -in digest.bin -pkeyopt digest:sha256 | wc -c
  i=$((i + 1))
done | sort -n | uniq -c
