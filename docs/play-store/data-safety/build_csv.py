#!/usr/bin/env python3
"""Builds HisabBondhu's Google Play Data safety CSV from Google's template.
Usage: python3 build_csv.py  → writes hisabbondhu-data-safety.csv (import in Play Console → App content → Data safety)."""
import csv, os

HERE = os.path.dirname(os.path.abspath(__file__))
DELETION_URL = "https://bayeziddev.github.io/hisabbondhu/data-deletion.html"

TOP = {
    ("PSL_DATA_COLLECTION_COLLECTS_PERSONAL_DATA", ""): "true",
    ("PSL_DATA_COLLECTION_ENCRYPTED_IN_TRANSIT", ""): "true",
    ("PSL_SUPPORTED_ACCOUNT_CREATION_METHODS", "PSL_ACM_USER_ID_OTHER_AUTH"): "true",  # email + one-time code
    ("PSL_ACCOUNT_DELETION_URL", ""): DELETION_URL,
    ("PSL_SUPPORT_DATA_DELETION_BY_USER", "DATA_DELETION_YES"): "true",
    ("PSL_DATA_DELETION_URL", ""): DELETION_URL,
    ("PSL_INDEPENDENTLY_VALIDATED", ""): "false",
    ("PSL_HAS_OUTSIDE_APP_ACCOUNTS", ""): "false",
}

APP, ACCOUNT, DEV_COMMS, SECURITY, MARKETING = (
    "PSL_APP_FUNCTIONALITY", "PSL_ACCOUNT_MANAGEMENT", "PSL_DEVELOPER_COMMUNICATIONS",
    "PSL_FRAUD_PREVENTION_SECURITY", "PSL_ADVERTISING",
)
# data type -> (category question, required?, collection purposes). Nothing is shared with third parties.
TYPES = {
    "PSL_NAME":         ("PSL_DATA_TYPES_PERSONAL", True,  [APP, ACCOUNT, DEV_COMMS, MARKETING]),
    "PSL_EMAIL":        ("PSL_DATA_TYPES_PERSONAL", True,  [APP, ACCOUNT, DEV_COMMS, SECURITY, MARKETING]),
    "PSL_PHONE":        ("PSL_DATA_TYPES_PERSONAL", True,  [APP, ACCOUNT, DEV_COMMS, MARKETING]),
    "PSL_ADDRESS":      ("PSL_DATA_TYPES_PERSONAL", False, [APP]),
    "PSL_USER_ACCOUNT": ("PSL_DATA_TYPES_PERSONAL", True,  [APP, ACCOUNT, SECURITY]),
    "PSL_OTHER":        ("PSL_DATA_TYPES_FINANCIAL", True, [APP]),          # business ledger records
    "PSL_CONTACTS":     ("PSL_DATA_TYPES_CONTACTS", False, [APP]),           # customers/suppliers the user adds
    "PSL_OTHER_MESSAGES": ("PSL_DATA_TYPES_EMAIL_AND_TEXT", False, [DEV_COMMS]),  # contact-form messages
    "PSL_DEVICE_ID":    ("PSL_DATA_TYPES_IDENTIFIERS", True, [APP, ACCOUNT, SECURITY]),
}

rows = list(csv.reader(open(os.path.join(HERE, "play-template.csv"), encoding="utf-8")))
header, body = rows[0], rows[1:]
answers = dict(TOP)
for t, (cat, required, purposes) in TYPES.items():
    answers[(cat, t)] = "true"
    base = f"PSL_DATA_USAGE_RESPONSES:{t}:"
    answers[(base + "PSL_DATA_USAGE_COLLECTION_AND_SHARING", "PSL_DATA_USAGE_ONLY_COLLECTED")] = "true"
    answers[(base + "PSL_DATA_USAGE_EPHEMERAL", "")] = "false"
    answers[(base + "DATA_USAGE_USER_CONTROL",
             "PSL_DATA_USAGE_USER_CONTROL_REQUIRED" if required else "PSL_DATA_USAGE_USER_CONTROL_OPTIONAL")] = "true"
    for p in purposes:
        answers[(base + "DATA_USAGE_COLLECTION_PURPOSE", p)] = "true"

used = set()
for r in body:
    key = (r[0], r[1])
    r[2] = answers.get(key, "")
    if key in answers:
        used.add(key)
missing = set(answers) - used
assert not missing, f"keys not in template: {missing}"

out = os.path.join(HERE, "hisabbondhu-data-safety.csv")
with open(out, "w", encoding="utf-8", newline="") as f:
    csv.writer(f, lineterminator="\n").writerows([header] + body)
print(f"wrote {out}: {len(body)} rows, {len(used)} answers")
