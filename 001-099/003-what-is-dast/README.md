# Note 003: DAST on a three-route API

Lab for the note [What is DAST? We let a fuzzer loose on a three-route API](https://www.appseclab.io/blog/003-what-is-dast/).

A small items API with an OpenAPI contract and three planted bugs runs on your machine. Schemathesis reads the contract, sends requests it generates from it, and checks every response against the same contract. The lab shows what a fuzzer finds from outside, with no access to the code.

## What is in the folder

| File | What it is |
| --- | --- |
| `api.js` | The API, exactly as the note prints it. Plain Node, no dependencies |
| `results.txt` | Our run of the note's command |
| `results-replay.txt` | The `curl` commands of that run, sent again from the host |

`api.js` has its bugs on purpose. It listens on `127.0.0.1:8091` only and keeps its items in memory.

## Run it

It needs Node.js (the note used 22.22.0), Docker and the image `schemathesis/schemathesis:4.28.0`. Port 8091 must be free. The requests go from the container to your own machine and nowhere else.

In one terminal:

```bash
node api.js
```

In another:

```bash
docker run --rm schemathesis/schemathesis:4.28.0 run \
  http://host.docker.internal:8091/openapi.json --seed 7 --max-examples 50
```

On Linux, add `--network host` and use `localhost` in place of `host.docker.internal`.

The run takes about 10 seconds. Stop `api.js` with Ctrl+C, and start it again before another run: one run leaves 253 new items in it, and the ids in the report start from what is already there.

## Reading the output

The summary at the end says `774 generated, 4 found 5 unique failures, 3 skipped`, and the last line says `5 failures`. Schemathesis exits with code 1 because it found failures. That is the expected result here.

| Operation | What Schemathesis reports | Bug |
| --- | --- | --- |
| `GET /items` | Response violates schema: `"1" is not of type "integer"` | A |
| `POST /items` | API accepted schema-violating request: a name that breaks `minLength` and `maxLength` got a 201 | B |
| `GET /items/{id}` | Server error, and undocumented HTTP status code: 500 where 200 and 404 are documented | C |
| `TRACE /items` | Unsupported method returned 404, expected 405 Method Not Allowed | not planted |

Four test cases failed and one of them, `GET /items/{id}`, counts as two failures. That is the "4 found 5".

- The request that broke the server for bug C is `GET /items/null%2Cnull`.
- The item the API accepted for bug B has id 18 and a name made of control characters and characters from many scripts.
- Each failure ends with a `curl` command. They name `host.docker.internal`, the container's name for your machine. From your own shell use `localhost`: [results-replay.txt](results-replay.txt) is that, with the same four answers.
- The line `Schema Coverage report: ./schema-coverage.html` names a file written inside the container. It is gone when the container exits.

## Limits

- The seed fixes the requests, not the clock. The note reports about 6 seconds for its first run, of 7 November 2026, and 9 to 11 for these. Our three runs of 18 November 2026 took 10.6, 11.1 and 8.6 seconds by Schemathesis's own count, on a machine that was running other work. [results.txt](results.txt) is the first of them.
- Across those three runs, each on a freshly started `api.js`, the 774 test cases, the five failures and the failing requests were the same. The times and the test case IDs, short labels such as `qbRJpo`, were not.
- Another Schemathesis version can generate other requests from the same seed and has other checks, so pin the image tag.
- The Linux form of the command was not run here. The results are from Docker Desktop on macOS.
- The results files are the tool's raw output, with its own symbols, and with the control characters of the name it sent.
