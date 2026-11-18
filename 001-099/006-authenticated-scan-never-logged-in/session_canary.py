# session_canary.py. Pass it to any zap-*-scan.py with
# --hook=/zap/wrk/session_canary.py
import json
import os
from urllib.parse import urlsplit

from zap_common import UserInputException

# Any route that answers 200 only with a valid session.
ROUTE = "/api/me"
RESULT = "/zap/wrk/session-canary.json"


def probe(zap):
    # For example https://api.example
    target = os.environ["CANARY_TARGET"]
    request = (
        f"GET {target}{ROUTE} HTTP/1.1\r\n"
        f"Host: {urlsplit(target).netloc}\r\n"
        "User-Agent: zap-session-canary\r\n"
        "Accept: application/json\r\n\r\n"
    )
    # Goes through the Replacer, like the scan.
    sent = zap.core.send_request(request, "false")
    if not sent:
        return 0
    return int(sent[0]["responseHeader"].split(" ", 2)[1])


def record(moment, status):
    print(f"canary {moment}: {status}", flush=True)
    found = {}
    if os.path.exists(RESULT):
        with open(RESULT) as saved:
            found = json.load(saved)
    found[moment] = status
    with open(RESULT, "w") as out:
        json.dump(found, out)


def zap_started(zap, target):
    status = probe(zap)
    record("start", status)
    if status != 200:
        raise UserInputException(
            f"no session reached the target ({status}):"
            " this would be an anonymous scan"
        )


def zap_pre_shutdown(zap):
    record("end", probe(zap))
