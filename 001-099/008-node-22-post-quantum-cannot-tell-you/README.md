# Note 008: Node.js 22 went post-quantum

Lab for the note [Node.js 22 went post-quantum and cannot tell you](https://www.appseclab.io/blog/008-node-22-post-quantum-cannot-tell-you/).

Node.js 22.20.0 and later carry OpenSSL 3.5, whose default TLS key exchange is the post-quantum hybrid `X25519MLKEM768`. This lab runs a Node.js TLS server and client on `127.0.0.1` with a relay between them that reads the server's first message off the wire. It compares the group on the wire with what Node reports to the application through `getEphemeralKeyInfo()`, 200 handshakes per case. A second script asks the same Node.js for ML-KEM and ML-DSA outside TLS.

## What is here

| File | What it is |
| --- | --- |
| `pq-handshake.mjs` | The handshake test. The note prints its core: the group table, the ServerHello reader, the relay and the client's connect call. Those lines are in the file unchanged, between two markers. The cases, the loop, the timing and the JSON line were written for this repository |
| `api-check.mjs` | Asks `crypto` and Web Crypto for ML-KEM-768 and ML-DSA-65. The note prints no code for it, so it was written for this repository |
| `results-handshake.txt`, `results-api.txt`, `results-ubuntu.txt` | The re-run of 18 November 2026 |

## Run it

It needs Node.js 22.23.4, the version of the note (bundled OpenSSL 3.5.9), and an `openssl` command, 3.0 or later, to make the test certificate. Everything stays on `127.0.0.1`, on ports the system picks.

```bash
openssl req -x509 -newkey ec -pkeyopt ec_paramgen_curve:P-256 -noenc \
  -keyout key.pem -out cert.pem -subj /CN=localhost \
  -addext subjectAltName=DNS:localhost -days 30
node pq-handshake.mjs 200 | uniq -c
node pq-handshake.mjs 200 TLSv1.3 | uniq -c
node api-check.mjs
node -p process.versions.openssl
```

The first command makes a self-signed ECDSA P-256 certificate for `localhost`. The `.gitignore` in this folder keeps `key.pem` and `cert.pem` out of the repository.

Each handshake prints one line, so `uniq -c` folds the 200 equal lines of a case into one, with the count in front. Each run takes about four seconds. The second run limits client and server to TLS 1.3, for the two smaller ClientHello sizes the note also gives.

The note also compares with the system OpenSSL of Ubuntu 24.04. The `ubuntu:24.04` Docker image has the library and not the `openssl` command, so this installs the package from Ubuntu's repository inside a container that is thrown away:

```bash
docker run --rm ubuntu:24.04 sh -c 'apt-get update -qq && apt-get install -y -qq openssl >/dev/null &&
  openssl version &&
  openssl s_client -connect 127.0.0.1:443 -groups X25519MLKEM768 </dev/null'
```

Nothing has to listen on that port: an OpenSSL that does not know the group stops before it connects, with `group 'X25519MLKEM768' cannot be set`.

## Reading the output

A line per handshake, printed by the note's own code: the group read from the ServerHello, then what `getEphemeralKeyInfo()` returns on the client.

```
 200 { group: 'X25519MLKEM768', hrr: false } {}
 200 { group: 'X25519', hrr: false } { type: 'ECDH', name: 'X25519', size: 253 }
```

Then one JSON line per case:

| Field | Meaning |
| --- | --- |
| `server`, `client` | The groups each side was given in `ecdhCurve`, or `default` for none |
| `completed` | Handshakes that finished, out of `runs` |
| `wire` | The group in the ServerHello, and `hrr: true` when that first message was a HelloRetryRequest |
| `nodeClient`, `nodeServer` | `getEphemeralKeyInfo()` on the client's socket and on the server's |
| `clientHello`, `clientHellos` | Size of the client's first write, and of every ClientHello it sent |
| `offered`, `keyShares` | The groups the ClientHello lists, and the ones it sends a key for |
| `serverFlight` | Bytes from the server up to the end of the handshake |
| `serverBytes` | All bytes from the server, session tickets and close included |
| `medianMs` | Median time from `tls.connect` to the end of the handshake |
| `clientError`, `serverError` | The error codes of a handshake that failed |

The note's table. The first seven cases follow it in order:

| Server groups | Client groups | On the wire | Node tells the app | ClientHello |
| --- | --- | --- | --- | --- |
| default | default | X25519MLKEM768 | `{}` | 1,586 bytes |
| X25519 | X25519 | X25519 | `ECDH, X25519` | 352 bytes |
| X25519 | default | X25519 | `ECDH, X25519` | 1,586 bytes |
| default | X25519 | X25519 | `ECDH, X25519` | 352 bytes |
| X25519 | hybrid, then X25519 | X25519, after a HelloRetryRequest | `ECDH, X25519` | 1,538 bytes, then a second one of 354 |
| X25519 | hybrid only | handshake failure | none | |
| hybrid only | X25519 | handshake failure | none | |

The other numbers of the note:

- The server's reply grows from 1,343 to 2,431 bytes with the hybrid. Read `serverBytes` of the second case and of the first.
- The median handshake goes from 2.06 ms to 2.28 ms. Read `medianMs` of the same two cases.
- Limited to TLS 1.3, the ClientHello is 235 bytes with X25519 alone and 1,469 bytes with the default groups. Read `clientHello` in the second run.
- `nodeServer` is `null` in every case.
- A client with `ecdhCurve` set to `P-256:P-384` negotiates P-256 and never offers the hybrid, with no error or warning. That is the eighth case: read `wire` and `offered`.
- `api-check.mjs`: `tls.DEFAULT_ECDH_CURVE` is `auto`, `crypto.generateKeyPairSync` throws `ERR_INVALID_ARG_VALUE` for `ml-kem-768` and for `ml-dsa-65`, and Web Crypto rejects `ML-KEM-768` and `ML-DSA-65`.
- `node -p process.versions.openssl` prints `3.5.9`.

## Limits

- The note prints the core of the script, not all of it. The rest was written for this repository, and the numbers in the note are the ones of this run, of 18 November 2026. What they depend on:
  - **ClientHello sizes, on the TLS versions offered.** With no TLS options Node.js 22.23.4 sends a ClientHello of 1,586 bytes, 352 bytes with X25519 alone, and 1,538 bytes in the HelloRetryRequest case. Limited to TLS 1.3 with `minVersion: 'TLSv1.3'` it sends 1,469, 235 and 1,421. The growth is 4.5 times with Node's own TLS versions and 6.25 times with TLS 1.3 only. Both hybrid sizes are over the 1,460 bytes of one TCP segment.
  - **Server bytes, on the certificate.** 1,343 to 2,431 with ours, which is 374 bytes. A certificate made again can be a byte or two longer or shorter, because the size of its ECDSA signature varies. The difference between the two cases is 1,088 bytes, the size of the ML-KEM-768 ciphertext.
  - **Timing, on the machine.** 2.06 ms to 2.28 ms, on a machine that was doing other work during the run. A first run on 15 November 2026, with a script that was not kept, gave 1.31 ms to 1.61 ms. The hybrid was slower in both.
- Also in the results: the `openssl` of Ubuntu 24.04 is 3.0.13 and refuses `-groups X25519MLKEM768` ([results-ubuntu.txt](results-ubuntu.txt)), the group on the wire and Node's answer in all eight cases, `null` on the server, the two failures, the second ClientHello after the HelloRetryRequest, the two `ERR_INVALID_ARG_VALUE`, and Web Crypto, which answers `Unrecognized algorithm name` for both names. With the key usages of a line that has ML-KEM, the call fails one step earlier, at `encapsulateKey`.
- Server, relay and client share one process, so the timing measures CPU work in that process. It says nothing about a network.
- Only Node.js 22.23.4 was run. The note says that releases before 22.20.0 carry OpenSSL 3.0 and that newer Node.js lines can report the group by name. Neither was tried here.
