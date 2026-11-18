# Note 006: the session canary

Lab for the note [Our authenticated DAST scan never logged in](https://www.appseclab.io/blog/006-authenticated-scan-never-logged-in/).

The note tells how a nightly ZAP scan, set up as an authenticated scan, ran for five weeks with no session: one wrong value made ZAP drop every Replacer rule, and the scan still exited 0. This folder holds the code the note prints: the wrong rule, the fixed rule and the hook that now fails a scan that is not signed in. There is no lab to run here and no results file, because the scan in the note ran on an application that is not public.

## What is here

| File | What it is |
| --- | --- |
| `replacer-rule-wrong.txt` | The Replacer line as it was passed to ZAP. `REQ_HEADER_ADD` is not a value the Replacer add-on knows |
| `replacer-rule-fixed.txt` | The rule after the fix, with `REQ_HEADER` |
| `session_canary.py` | The hook for ZAP's scan scripts. It asks for a protected route before the scan starts and again before ZAP shuts down |

The three files are the note's code blocks, byte for byte. The note names `session_canary.py`. The names of the two rule files are this folder's. `<value>` and `api.example` are placeholders.

## Run it

Nothing in this folder runs by itself. The hook needs ZAP's Docker scan scripts (the note used ZAP 2.17) and a target, and the only target to use is an application you own.

What you can reproduce, on your own application:

1. **The silent failure.** Start any `zap-*-scan.py` with a Replacer rule whose `matchtype` is `REQ_HEADER_ADD`, as in `replacer-rule-wrong.txt`, and read `zap.replacer.rules` from a hook. The note says the list is empty and the scan exits 0.
2. **The canary.** Run the same scan with `session_canary.py` as its hook. The note says it stops with exit code 3 before the scan begins.

What you cannot reproduce from here: the note's numbers. They come from the log of the application that was scanned.

How the note says to apply the check:

- Choose a route that answers 200 only with a valid session, and set `ROUTE` in the hook to it. The note's is `/api/me`.
- Put the hook in the folder the scan scripts see as `/zap/wrk` and pass `--hook=/zap/wrk/session_canary.py`.
- Set `CANARY_TARGET` in the environment of the scan to the origin of your application, for example `https://api.example`.
- Pass the Replacer rule on ZAP's command line, as in `replacer-rule-fixed.txt`, with your own cookie. The `url` field is a regular expression matched against the whole URL, so it needs the `.*` at the end.
- After the scan, make your pipeline read `session-canary.json`. Both values must be 200, and a missing file must fail the run. If the file given to `--hook` does not exist, ZAP logs a warning and scans without it.

## Reading the output

The hook prints one line each time it asks, `canary start: 200` and `canary end: 200`, and writes both to `/zap/wrk/session-canary.json`:

```json
{"start": 200, "end": 200}
```

- A `start` other than 200 raises `UserInputException`, with the message `no session reached the target`. That is what stops the scan.
- A status of `0` means the request got no answer.
- The check at the end cannot stop anything: the reports are written by then. It is there for a session that expired halfway, and only the JSON carries its result.

The numbers in the note, for reference:

| When | What the application's log showed |
| --- | --- |
| Before the fix, two nightly runs | Protected routes answered 401 28,396 times and 200 at most 5 times |
| After the fix and the canary | 12,042 responses of 200 on protected routes and no 401 there, with no 429 and no 5xx in 32,453 requests |
| The fuzzer, before | 953 of 3,119 requests stopped at the rate limiter |
| The fuzzer, after | 3,134 cases and no 429 |

## Limits

- **Nothing here was run.** No scan was made for this folder, against any target. The files were copied from the note on 18 November 2026, and the hook was only checked to parse as Python 3.
- **The note does not print everything it describes.** The full scan command, the hook that reads `zap.replacer.rules`, the step that reads the JSON, the check that the response names the scan user, and the fuzzer's settings (the header that identifies the scan, and failing on any 429) are told in words. You have to write those for your own pipeline.
- **The hook leans on ZAP's scan scripts.** It imports `zap_common`, uses the `zap_started` and `zap_pre_shutdown` hooks, and counts on `zap.core.send_request` going through the Replacer. Another ZAP version can behave differently.
- **A 200 proves a session, not a user.** The printed hook accepts any 200. The note says the lab's own version also checks that the response names the scan user.
