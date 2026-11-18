#!/bin/sh
# The kex= line of Cloudflare's trace page, read by three clients on the same machine.
# The note prints one-line commands for curl and Node.js; this file was written for the repository.
# /usr/bin/curl is the curl that ships with macOS. A curl from another source can answer differently.
node trace.cjs
printf '%s\t' "$(/usr/bin/curl --version | head -n 1)"
/usr/bin/curl -s https://www.cloudflare.com/cdn-cgi/trace | grep '^kex='
