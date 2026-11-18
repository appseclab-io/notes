# Note 005: two users, one invoice

Lab for the note [The test no scanner wrote: two users, one invoice](https://www.appseclab.io/blog/005-two-users-one-invoice/).

An invoices API lets any signed-in customer read another customer's invoice. This lab runs a static scanner and a fuzzer on it and counts, on the server, how many times the fuzzer read the other customer's invoice without reporting it. Then it runs a test with two users, which fails on the flaw and passes when the ownership check is turned on.

## What is here

| File | What it is |
| --- | --- |
| `api.js` | The API, as the note prints it. The flaw is there on purpose. `OWNERSHIP_CHECK=1` turns on the missing check |
| `openapi.json` | The contract the fuzzer reads, as the note prints it |
| `two-users.test.js` | The test with two customers, as the note prints it |
| `count.js` | Not in the note. It starts the same API and counts what it is asked for, without changing `api.js` |
| `results-*.txt` | The raw output of the run on 18 November 2026 |

## Run it

It needs Node.js 22 (the note ran on 22.22.0) and nothing from npm. The two scanners run from their Docker images, `semgrep/semgrep:1.183.0` and `schemathesis/schemathesis:4.28.0`. Port 8092 must be free for the fuzzer.

The test with two users, without the check and with it:

```bash
node --test two-users.test.js
OWNERSHIP_CHECK=1 node --test two-users.test.js
```

The static scan. It downloads the two rulesets from the Semgrep registry:

```bash
docker run --rm -v "$PWD:/src" -w /src semgrep/semgrep:1.183.0 \
  semgrep scan --config p/default --config p/nodejs --metrics=off \
  api.js openapi.json two-users.test.js
```

The fuzzer, signed in as Ana. In one terminal, start the API with the counter:

```bash
node count.js
```

In another, run Schemathesis. When it ends, stop `count.js` with Ctrl+C and it prints the counts:

```bash
docker run --rm schemathesis/schemathesis:4.28.0 run \
  http://host.docker.internal:8092/openapi.json \
  --header "Authorization: Bearer token-ana" --seed 7 --max-examples 50
```

`host.docker.internal` is how a container reaches your machine under Docker Desktop. On Linux, add `--network host` and use `127.0.0.1` in its place. That form was not part of this run.

## Reading the output

| Measurement | What the note says | Results file |
| --- | --- | --- |
| Test, no check | One of four cases fails: `not ok 2 - Ana cannot read Bruno's invoice`, with `got 200: {"id":2,"owner":"bruno","items":7}` | [results-two-users.txt](results-two-users.txt) |
| Test, `OWNERSHIP_CHECK=1` | All four pass | [results-two-users.txt](results-two-users.txt) |
| Semgrep, `p/default` and `p/nodejs` | 0 findings | [results-semgrep.txt](results-semgrep.txt) |
| Schemathesis as Ana, seed 7 | 1,958 test cases and one failure: `TRACE /invoices` answered 404 where 405 was expected | [results-schemathesis.txt](results-schemathesis.txt) |
| Count on the server | Bruno's invoice went to Ana 18 times, with status 200 every time | [results-schemathesis.txt](results-schemathesis.txt) |

The run on 18 November 2026 gave each of these numbers, on Node.js 22.22.0, Semgrep 1.183.0 and Schemathesis 4.28.0.

What to look for:

- In a terminal, Node prints the test results with check marks. Sent to a file or a pipe, it prints the `ok` and `not ok` lines the note shows.
- The failing case prints Bruno's invoice in full. That line is the finding.
- Schemathesis lists one failure and says nothing about `/invoices/2`. The line `Bruno's invoice sent to Ana with status 200: 18` comes from `count.js`, not from the fuzzer.
- `count.js` also shows 1,288 answers of 401. Those are Schemathesis's own repeats of each request without the token and with an invalid one, to check that the API refuses them.

## Limits

- **The seed does not fix the size of the run.** On 18 November 2026 the note's command ran nine times: the run in `results-schemathesis.txt` and the eight in [results-schemathesis-repeats.txt](results-schemathesis-repeats.txt). Six gave 1,958 cases, one failure and 18 reads. Runs 2 and 8 generated 3,861 and 3,379 cases, with the same failure and the same 18 reads: their stateful phase ran 443 and 356 scenarios where the others ran 195. We did not trace why. Schemathesis 4.28.0 has code that starts that phase again, on another seed, after a result it cannot reproduce or a case it could not generate, and both runs happened while the machine was doing other work.
- **A run can lose a connection.** Run 1 in that file lost one between the container and the API. Schemathesis reported `1 failure, 1 error` and 2,624 cases, and the server counted 25 reads. If your run ends with an error, run it again.
- **Without `--max-examples 50` the size changes on every run.** Three runs on the same seed gave 5,013, 4,481 and 9,699 cases, and 40, 39 and 36 reads of Bruno's invoice: [results-schemathesis-without-max-examples.txt](results-schemathesis-without-max-examples.txt).
- **What did not change.** The one failure, `TRACE /invoices`, was the same in all twelve runs, and not one of them reported the reads of Bruno's invoice.
- **Semgrep's rules change.** The rulesets come from the registry on the day of the scan. On 18 November 2026 the two held 1,074 rules and 203 of them ran on the three files. The note does not give the count of its day. A later ruleset may report something here.
- **`count.js` is this folder's way of counting.** The note describes the count in one sentence and does not print the code. `count.js` counts an answer when the request had Ana's token, the status was 200 and the body was an invoice owned by Bruno.
- **The token lookup was changed on 18 November 2026.** Until then `api.js` kept the two tokens in a plain object, so a token named after something every object has, such as `constructor`, counted as signed in. Nobody planted that, and neither scanner reported it. The file now keeps the tokens in a `Map`, the note prints it that way, and every result in this folder was measured again with it. To check, start `node api.js` and ask with that token: `curl -s -o /dev/null -w '%{http_code}\n' -H 'Authorization: Bearer constructor' http://127.0.0.1:8092/invoices/2` prints 401.
- **Paths in the test output.** Node prints the full path of the test file when a case fails. In `results-two-users.txt` that prefix was replaced with `.`.
- **What always differs.** Times, and the test case id Schemathesis prints.
- **One runtime.** The test gave the same three passes and one failure, and the same four passes, on Node.js 22.23.4.
- The API keeps three invoices in memory and knows two fixed tokens. It is a specimen to measure, not code to deploy.
