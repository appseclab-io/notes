# Note 004: is your HTTPS post-quantum

Lab for the note [Is your HTTPS post-quantum? We checked 19 sites](https://www.appseclab.io/blog/004-is-your-https-post-quantum/).

Chrome offers a post-quantum key exchange, `X25519MLKEM768`, on every HTTPS connection, and reports what each server accepted through the DevTools protocol. This lab opens a list of sites once each and prints the key exchange group and the signature algorithm Chrome negotiated for the page. A second step asks one server the same question from three clients on the same machine: Chrome, Node.js and curl.

## What is here

| File | What it is |
| --- | --- |
| `pq-check.cjs` | The script printed in the note, unchanged |
| `sites.txt` | The home pages of the note's 19 sites, in the order of its table |
| `trace.cjs`, `trace.sh` | The client test. The note prints a one-line command for curl and one for Node.js. These two files, written for this repository, ask with all three clients, Chrome included |
| `results-sites.txt`, `results-trace.txt` | The re-run of 18 November 2026 |

## Run it

It needs Node.js 22 or later, Google Chrome and, for the client test, the curl that ships with macOS. Playwright drives the Chrome you already have and downloads no browser.

The note ran on Chrome 156.0.8078.80, Playwright 1.63.0, Node.js 22.22.0 (bundled OpenSSL 3.5.4) and curl 8.7.1 (LibreSSL 3.3.6). The Node.js version matters for the client test, because Node.js carries its own TLS library.

```bash
npm install
node pq-check.cjs $(cat sites.txt)
sh trace.sh
```

The second command loads each page of `sites.txt` once, in a fresh browser context, and takes under a minute. To check your own sites, pass them instead of the list:

```bash
node pq-check.cjs https://www.example.com/
```

`trace.sh` opens `https://www.cloudflare.com/cdn-cgi/trace` once with each of the three clients and prints only the line that starts with `kex=`. That page also shows the IP address of whoever asks, which the scripts never print.

## Reading the output

`pq-check.cjs` prints Chrome's version, then one line per site: the address, the TLS version, the key exchange group and the signature algorithm, separated by tabs. A site that returns no page prints `no answer`.

What the note found, and what to expect:

- 16 of the 19 sites negotiate `X25519MLKEM768`. Three negotiate the classical `X25519` alone: `github.com`, `www.wikipedia.org` and `www.gov.br`.
- All 19 use TLS 1.3.
- Every signature is `ECDSA P-256` or `RSA-PSS`. None is post-quantum.

`trace.sh` prints one line per client. In the note:

| Client | TLS library | kex |
| --- | --- | --- |
| Chrome 156 | its own | X25519MLKEM768 |
| Node.js 22.22.0 | OpenSSL 3.5.4 | X25519MLKEM768 |
| curl 8.7.1 that ships with macOS | LibreSSL 3.3.6 | X25519 |

## Limits

- These are live sites. Any of them can turn the hybrid on or off on any day, and a large site can answer differently from another region or another of its servers. The results files show one page load per site, from one network, on one day.
- Chrome updates itself. The note ran on 156.0.8078.80, which was no longer on the machine. The re-run used 157.0.8103.79.
- What differed in the re-run of 18 November 2026: nothing in the results. All 19 lines match the note's table, and the three clients gave the same three answers. Only Chrome's version changed.
- The last column is the algorithm the server signed the handshake with. It tells the type of key in the site's certificate, ECDSA or RSA. It does not show the algorithm the certificate authority used to sign the certificate.
- A site behind a CDN answers for the CDN's edge. The connection from the edge to the site's own server is another one, and this test cannot see it.
- `trace.sh` calls `/usr/bin/curl`, the one macOS ships. A curl from another source is built with another TLS library and can answer differently, which is the point of that test.
