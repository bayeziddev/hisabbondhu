# Data safety — import file + answers

## Fastest way: import the CSV
Play Console → **App content → Data safety → Import from CSV** → choose
[`data-safety/hisabbondhu-data-safety.csv`](data-safety/hisabbondhu-data-safety.csv) → review → **Save** → **Submit**.

The file was built from Google's own template (same 782 rows/IDs). Regenerate after changes with
`python3 docs/play-store/data-safety/build_csv.py`.

## When to import which version
- The CSV now also declares **App activity → Other actions** (referral codes and Hisab points). Import it together with the **first release
  that contains the referral screen**. A Data safety form may declare data an app can collect, but it must never miss data the app does collect.
- Personal and mess ledgers do not add new data *types* (they are covered by Financial info and Contacts above); only the descriptions changed.
  When the mess feature ships, re-check: if notices/chat-like text is added (M14), add **App activity → Other user-generated content**.

## What the file declares
- Collects user data: **Yes** · Encrypted in transit: **Yes** · Account creation: **Username and other authentication** (email + one-time code)
- Users can request deletion: **Yes** — in-app (More → Delete account & data) and
  `https://bayeziddev.github.io/hisabbondhu/data-deletion.html` (used for both deletion URLs)
- Independent security review: No · Accounts created outside the app: No
- **Nothing is shared** with third parties (Cloudflare and Resend are service providers acting for us — not “sharing” under Play's definition)
- **Mess/group ledgers** are shown only to the group's approved members. Each member joins on purpose (code + manager approval) and the app says
  that the whole group sees the group's records, so this is a *user-initiated transfer the user reasonably expects* — Play's Data safety guidance
  excludes that from "sharing". It is explained in the privacy policy (sections 1, 3, 4 and 6).
- No data is processed ephemerally

| Data type | Required? | Why collected |
|---|---|---|
| Personal info → Name | Required | App functionality, Account management, Developer communications, Advertising or marketing (only with opt-in) |
| Personal info → Email address | Required | App functionality, Account management, Developer communications, Fraud prevention & security, Advertising or marketing (opt-in) |
| Personal info → Phone number | Required | App functionality, Account management, Developer communications, Advertising or marketing (opt-in) |
| Personal info → Address | Optional | App functionality |
| Personal info → User IDs | Required | App functionality, Account management, Fraud prevention & security |
| Financial info → Other financial info (shop ledger; personal income/expenses; mess meals, grocery, deposits, rent and bills) | Required | App functionality |
| Contacts → Contacts (customers, suppliers, shops owed, people lent to/borrowed from, mess members — names and numbers the user types) | Optional | App functionality |
| App activity → Other actions (referral code used, Hisab points history, mess join requests) | Optional | App functionality, Fraud prevention & security |
| Messages → Other in-app messages (contact form) | Optional | Developer communications |
| Device or other IDs | Required | App functionality, Account management, Fraud prevention & security |

**Not collected:** location, health, race/religion/politics/sexual orientation, photos, videos, audio, files, calendar,
SMS/call log, emails, web history, installed apps, crash logs, Advertising ID, card/bank numbers, credit score.

> ⚠️ The `data_safety_export.csv` you uploaded came from a different app (deletion URL `…/amader-tangail/…`) and declared
> almost every data type (health, precise location, race, political views…). Importing that for HisabBondhu would be a false
> declaration and can get the app rejected — use the file above instead.
