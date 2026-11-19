# Play Console → App content (all other declarations)

| Section | Answer |
|---|---|
| **Privacy policy** | `https://bayeziddev.github.io/hisabbondhu/privacy.html` |
| **App access** | *All functionality is available without special access* — login is optional. If you want reviewers to test sync, choose “All or some functionality is restricted” and give: phone = your `REVIEW_PHONE`, code = your `REVIEW_OTP`, instructions: “More → Online backup & sync → Continue with mobile number → enter the phone → enter the code.” |
| **Ads** | No, my app does not contain ads |
| **Content rating (IARC)** | Category: *Utility, Productivity, Communication or Other*. Violence/sexuality/language/drugs/gambling: No. Users interact or exchange content: **No**. Shares location: No. Digital purchases: No. → Expected rating: **Everyone / 3+** |
| **Target audience** | **18 and over** only. “Could the app unintentionally appeal to children?” → No |
| **News app** | No |
| **COVID-19 contact tracing / status** | No |
| **Data safety** | See `data-safety.md` |
| **Government app** | No |
| **Financial features** | “My app doesn't provide any financial features” — it is a bookkeeping/record tool; it does not hold, move, lend or invest money |
| **Health apps** | No health features |
| **Account deletion** | In-app path + URL `https://bayeziddev.github.io/hisabbondhu/data-deletion.html` |
| **Advertising ID** | No — the app does not use it (the AD_ID permission is removed in the manifest) |
| **Foreground service / exact alarms / accessibility / VPN / SMS & Call log / all-files access / background location** | Not used |

## Permissions used (for your reference)
| Permission | Why |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Backup & sync when online; show offline status |
| `POST_NOTIFICATIONS` | Due-collection reminders (asked only when the user enables a reminder) |
| `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` (added by WorkManager) | Resume pending sync after phone restart |

No contacts permission: picking a customer from the phonebook uses the system picker, which shares only the chosen number.
