# Play Console → App content (all other declarations)

| Section | Answer |
|---|---|
| **Privacy policy** | `https://bayeziddev.github.io/hisabbondhu/privacy.html` |
| **App access** | **All or some functionality is restricted** → Add instructions: *Name:* “Reviewer account”. *Username/email:* the value of your `REVIEW_EMAIL` secret (e.g. `play.review@sayadbayezid.com`). *Password/code:* the value of `REVIEW_OTP`. *Instructions:* “1) Choose a language → Get started. 2) Enter the email above → Send code. 3) Enter the 6-digit code above → Verify (no email is needed for this test account). 4) Enter any name and mobile number (e.g. 01712345678) → Continue. 5) Enter a shop name → Start.” Tick “No other information is required”. |
| **Ads** | No, my app does not contain ads |
| **Content rating (IARC)** | Category: *Utility, Productivity, Communication or Other*. Violence/sexuality/language/drugs/gambling: No. Users interact or exchange content: **No**. Shares location: No. Digital purchases: No. → Expected rating: **Everyone / 3+** |
| **Target audience** | **18 and over** only. “Could the app unintentionally appeal to children?” → No |
| **News app** | No |
| **COVID-19 contact tracing / status** | No |
| **Data safety** | Import `data-safety/hisabbondhu-data-safety.csv` (see `data-safety.md`) |
| **Government app** | No |
| **Financial features** | “My app doesn't provide any financial features” — it is a bookkeeping/record tool; it does not hold, move, lend or invest money |
| **Health apps** | No health features |
| **Account deletion** | In-app path + URL `https://bayeziddev.github.io/hisabbondhu/data-deletion.html` |
| **Advertising ID** | No — the app does not use it (the AD_ID permission is removed in the manifest) |
| **Contact details (Store settings)** | Email `support@sayadbayezid.com` · Phone `+8801519601517` · Website `https://bayeziddev.github.io/hisabbondhu/` |
| **Foreground service / exact alarms / accessibility / VPN / SMS & Call log / all-files access / background location** | Not used |

## Permissions used (for your reference)
| Permission | Why |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Backup & sync when online; show offline status |
| `POST_NOTIFICATIONS` | Due-collection reminders (asked only when the user enables a reminder) |
| `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` (added by WorkManager) | Resume pending sync after phone restart |

No contacts permission: picking a customer from the phonebook uses the system picker, which shares only the chosen number.
