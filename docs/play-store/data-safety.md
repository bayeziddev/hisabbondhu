# Data safety — import file + answers

## Fastest way: import the CSV
Play Console → **App content → Data safety → Import from CSV** → choose
[`data-safety/hisabbondhu-data-safety.csv`](data-safety/hisabbondhu-data-safety.csv) → review → **Save** → **Submit**.

The file was built from Google's own template (same 782 rows/IDs). Regenerate after changes with
`python3 docs/play-store/data-safety/build_csv.py`.

## What the file declares
- Collects user data: **Yes** · Encrypted in transit: **Yes** · Account creation: **Username and other authentication** (email + one-time code)
- Users can request deletion: **Yes** — in-app (More → Delete account & data) and
  `https://bayeziddev.github.io/hisabbondhu/data-deletion.html` (used for both deletion URLs)
- Independent security review: No · Accounts created outside the app: No
- **Nothing is shared** with third parties (Cloudflare and Resend are service providers acting for us — not “sharing” under Play's definition)
- No data is processed ephemerally

| Data type | Required? | Why collected |
|---|---|---|
| Personal info → Name | Required | App functionality, Account management, Developer communications, Advertising or marketing (only with opt-in) |
| Personal info → Email address | Required | App functionality, Account management, Developer communications, Fraud prevention & security, Advertising or marketing (opt-in) |
| Personal info → Phone number | Required | App functionality, Account management, Developer communications, Advertising or marketing (opt-in) |
| Personal info → Address | Optional | App functionality |
| Personal info → User IDs | Required | App functionality, Account management, Fraud prevention & security |
| Financial info → Other financial info (shop ledger) | Required | App functionality |
| Contacts → Contacts (customers/suppliers the user adds) | Optional | App functionality |
| Messages → Other in-app messages (contact form) | Optional | Developer communications |
| Device or other IDs | Required | App functionality, Account management, Fraud prevention & security |

**Not collected:** location, health, race/religion/politics/sexual orientation, photos, videos, audio, files, calendar,
SMS/call log, emails, web history, installed apps, crash logs, Advertising ID, card/bank numbers, credit score.

> ⚠️ The `data_safety_export.csv` you uploaded came from a different app (deletion URL `…/amader-tangail/…`) and declared
> almost every data type (health, precise location, race, political views…). Importing that for HisabBondhu would be a false
> declaration and can get the app rejected — use the file above instead.
