# Note 001: SAST on four planted flaws

Lab for the note [What is SAST? We ran it on four planted flaws](https://www.appseclab.io/blog/001-what-is-sast/).

A static analysis tool reads code without running it. This lab runs Semgrep on a 31-line Express app with four planted flaws: first with five rule sets one at a time, then with the two that found something together. It shows what the tool reports, and which two flaws it does not see.

## What is in the folder

| File | What it is |
| --- | --- |
| `app.js` | The app, exactly as the note prints it |
| `results.txt` | Our run of the note's command: the two rule sets together |
| `results-rulesets.txt` | Our run of each of the five rule sets alone |

`app.js` is vulnerable on purpose. It is there to be read by the scanner, not to be started: it asks for `express` and for a `./db` module, and neither is in this folder.

## Run it

It needs Docker and the image `semgrep/semgrep:1.183.0`, the version the note used. Semgrep fetches the rule sets from its registry on every run, so it needs the network. Nothing is installed and `app.js` never runs.

The note's command, the two rule sets together:

```bash
docker run --rm -v "$PWD:/src" -w /src semgrep/semgrep:1.183.0 \
  semgrep scan --config p/default --config p/expressjs --metrics=off app.js
```

The five rule sets of the note's table, one at a time:

```bash
for ruleset in p/javascript p/nodejs p/owasp-top-ten p/expressjs p/default; do
  docker run --rm -v "$PWD:/src" -w /src semgrep/semgrep:1.183.0 \
    semgrep scan --config "$ruleset" --metrics=off app.js
done
```

Each scan takes 5 to 10 seconds once the image is on your machine.

## Reading the output

The last line of a scan says how many rules ran and how many findings they gave. With the two rule sets together it is `Ran 203 rules on 1 file: 3 findings.`

| Line of `app.js` | Rule | What it is |
| --- | --- | --- |
| 9 | `tainted-sql-string` | Flaw 1, the SQL built by joining strings |
| 15 | `detect-child-process` | Flaw 2, user input in a shell command |
| 5 | `express-check-csurf-middleware-usage` | Not planted: no CSRF middleware. For this app the note reads it as noise |

Nothing is reported for lines 19 to 24, flaw 3, the same SQL injection one function call away. Nothing is reported for line 28, flaw 4, the invoice any signed-in user can read. Those two absences are the result the note is about.

One rule set at a time, as in [results-rulesets.txt](results-rulesets.txt):

| Rule set | Rules run on `app.js` | Findings |
| --- | --- | --- |
| `p/javascript` | 68 | none |
| `p/nodejs` | 36 | none |
| `p/owasp-top-ten` | 70 | none |
| `p/expressjs` | 65 | flaw 1, line 9 |
| `p/default` | 200 | flaw 2, line 15, and the CSRF note on line 5 |

The note gives the findings of each rule set and the 203 rules of the combined run. The rule counts of the single runs are from our run of 18 November 2026.

Semgrep exits with code 0 here even with findings, because the command does not ask it to fail on them.

## Limits

- The rule sets have no version in the command. The registry serves what it holds that day, so a later run can load more or fewer rules and report something else. The note ran on 7 November 2026 and the results here are from 18 November 2026: the numbers the note reports came out the same.
- The command pins the image at 1.183.0, the version of the note. Without the tag Docker uses `latest`, whatever version that is on the day. A newer engine can follow data further than this one did, and flaw 3 is the first place that would show.
- Semgrep 1.183.0 prints that a newer version is available. That line is expected.
- Only the Community Edition engine and the community rule sets were run, on one file.
- `--metrics=off` stops usage metrics. The scan still contacts the registry to fetch the rules.
- The results files are the tool's raw output, with its own box drawing and symbols.
