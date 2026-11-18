# Note 007: step one of a PQC migration

Lab for the note [Step one of NIST's PQC migration, run on our own site](https://www.appseclab.io/blog/007-nist-pqc-migration-step-one/).

A cryptographic inventory starts by asking each TLS endpoint which key exchange it accepts. This lab offers an endpoint only the post-quantum hybrid group, `X25519MLKEM768`, so the answer is either a negotiated group or a failed handshake, with no silent fallback to a classical group. It asks an HTTPS endpoint and an FTP server after `AUTH TLS`.

## What is here

| File | What it is |
| --- | --- |
| `commands.sh` | The two commands printed in the note, unchanged, with the note's placeholder hosts |
| `run.sh` | Swaps the placeholders for the hosts you name and runs the two commands. A third argument changes the group offered |
| `results.txt` | The re-run of 18 November 2026 against the lab's own site |

## Run it

It needs OpenSSL 3.5 or later as `openssl`, the first release that knows `X25519MLKEM768`. The note and the re-run used OpenSSL 4.1.0 from Homebrew. The `openssl` that ships with macOS is LibreSSL and does not know the group, so check `openssl version` first.

This lab has no local target. Each command makes one TLS handshake with the host you name and then closes. On FTP it sends `AUTH TLS` first and never logs in. Name only hosts you own.

```bash
sh run.sh WEB_HOST FTP_HOST
sh run.sh WEB_HOST FTP_HOST X25519
```

The first line is the note's test: hybrid only. The second is the classical pass the note mentions, the same two commands with `-groups X25519`. The run in `results.txt` used `www.appseclab.io` and `ftp.appseclab.io`.

## Reading the output

The first command keeps two kinds of line:

- `Negotiated TLS1.3 group: X25519MLKEM768` means the endpoint accepted the hybrid.
- A line with `alert` means it refused. There is no post-quantum key exchange on that endpoint.

The second command prints everything `s_client` says. A server that refuses the hybrid shows `tls alert handshake failure`, `no peer certificate available` and `Cipher is (NONE)`.

In the classical pass the first command prints nothing when the handshake works. OpenSSL 4.1.0 names a classical group in a line that starts with `Peer Temp Key`, and the filter of the printed command does not keep it. The note says so and tells you to add that line to the `grep`. An empty answer with no alert is a completed classical handshake. Part 3 of `results.txt` repeats that command with a wider filter to show it.

What the note found on its own site, and what to expect there:

- **Reader to Cloudflare.** `X25519MLKEM768`, with an ECDSA P-256 certificate.
- **The FTP channel of the deploy.** The server supports TLS 1.3, negotiates X25519 only and refuses the hybrid. Its certificate names the shared server, not the FTP name of the site.

Both FTP facts are in the output of the classical pass: `Protocol: TLSv1.3` and `Peer Temp Key: X25519, 253 bits`.

## Limits

- These are live endpoints. A CDN or a hosting company can turn the hybrid on or off without notice.
- What differed in the re-run of 18 November 2026: nothing on the two paths above. The edge negotiated the hybrid. The FTP server refused it with alert 40 and negotiated X25519 over TLS 1.3, with an RSA 2048 certificate.
- The note's table has a row for the path from Cloudflare to the origin server: hybrid accepted, RSA 2048 certificate. The note does not name the origin, so that row was not re-run here.
- Two findings of the note have no command in it and were not re-run: that the deploy used plain FTP, and that the DNS zone is not signed.
- `s_client` does not check that a certificate matches the host name unless you add `-verify_hostname`. `Verify return code: 0 (ok)` in the FTP pass says the chain is valid. It does not say the certificate names the host you asked for, and in this run it does not.
- In `results.txt` three things were replaced by a marker in square brackets: the IP address of the shared host, its name in the certificate, and the body of that certificate. The note does not publish them, so the results do not either.
- Only OpenSSL 4.1.0 was run. Another version can word these lines differently.
