# Play Console → App content → Data safety (answers)

**Does your app collect or share any of the required user data types?** → **Yes**
**Is all of the user data collected by your app encrypted in transit?** → **Yes** (HTTPS only; cleartext blocked)
**Do you provide a way for users to request that their data is deleted?** → **Yes**
  - In-app: More → Delete account & data
  - URL: `https://bayeziddev.github.io/hisabbondhu/data-deletion.html`

> Data typed by users stays **on the device** unless they choose to log in for backup/sync. Play counts data as
> “collected” only when it leaves the device, so the items below are marked **Optional** (only when the user logs in).
> Our service providers (Cloudflare hosting, SMS gateway) process data on our behalf — this is **not “sharing”**
> under Play's definition. **No data is shared** with third parties.

| Category → Data type | Collected | Shared | Optional? | Ephemeral? | Purposes |
|---|---|---|---|---|---|
| Personal info → **Name** | Yes | No | Optional | No | App functionality, Account management |
| Personal info → **Phone number** | Yes | No | Optional (login) | No | App functionality, Account management, Fraud prevention & security |
| Personal info → **Address** | Yes | No | Optional | No | App functionality |
| Personal info → **Email address** | Yes | No | Optional (contact form) | No | Developer communications |
| Financial info → **Other financial info** (business ledger: sales, dues, expenses) | Yes | No | Optional (sync) | No | App functionality |
| Contacts → **Contacts** (customer/supplier names & numbers the user enters) | Yes | No | Optional | No | App functionality |
| Messages → **Other in-app messages** (contact form) | Yes | No | Optional | No | Developer communications |
| Device or other IDs → **Device or other IDs** (random app-generated device ID) | Yes | No | Optional (login) | No | App functionality, Fraud prevention & security |

**Not collected:** location, web browsing, photos/videos, audio, files, calendar, health, app activity analytics,
crash logs (no crash SDK), Advertising ID, purchase history, credit info, payment card/bank details.

**Security practices to tick:** Data is encrypted in transit ✔ · Users can request deletion ✔ ·
(Optional) Independent security review — leave unticked unless you obtain one.
