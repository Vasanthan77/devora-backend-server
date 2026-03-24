# AMAPI Enterprise Upgrade Guide (Implemented + Manual Steps)

This project now supports the EMM-initiated enterprise upgrade flow from Google AMAPI docs:
https://developers.google.com/android/management/upgrade-an-enterprise

## What is implemented in backend

1. Enterprise details + eligibility check
- `GET /api/amapi/enterprise`
- `GET /api/amapi/enterprise/upgrade/eligibility`

2. Generate enterprise upgrade URL (EMM-initiated flow)
- `POST /api/amapi/enterprise/upgrade-url`
- Optional request body:
```json
{
  "adminEmail": "it-admin@example.com",
  "allowedDomains": ["example.com", "*.example.com"]
}
```

3. Upgrade status verification after completion
- `GET /api/amapi/enterprise/upgrade/status`
- Also checks `enterpriseType` from `enterprises.get`.

4. Pub/Sub topic update endpoint supports notification types
- `POST /api/amapi/enterprise/pubsub`
- Optional query: `notificationTypes=ENROLLMENT,STATUS_REPORT,COMMAND,ENTERPRISE_UPGRADE`

5. Webhook supports Pub/Sub notification types including enterprise upgrades
- `POST /webhook/amapi`
- Handles `ENTERPRISE_UPGRADE`, `ENROLLMENT`, `STATUS_REPORT`, `COMMAND`, `test`.

## Required manual Google Cloud / AMAPI setup

1. Enable Pub/Sub API in the same GCP project as AMAPI.
2. Create a Pub/Sub topic and subscription.
3. Grant publisher role to:
- `android-cloud-policy@system.gserviceaccount.com`
- Role: `roles/pubsub.publisher`
4. Set enterprise topic via backend endpoint:
- `/api/amapi/enterprise/pubsub`
5. Configure your push subscription endpoint to:
- `/webhook/amapi`

## Recommended upgrade runbook

1. Check enterprise type:
- `GET /api/amapi/enterprise/upgrade/eligibility`
2. If eligible (`MANAGED_GOOGLE_PLAY_ACCOUNTS_ENTERPRISE`), generate URL:
- `POST /api/amapi/enterprise/upgrade-url`
3. Show URL only to authorized IT admin users.
4. IT admin completes Google upgrade flow.
5. Wait for Pub/Sub `ENTERPRISE_UPGRADE` event with `UPGRADE_STATE_SUCCEEDED`.
6. Verify final state:
- `GET /api/amapi/enterprise/upgrade/status`
- Expect `enterpriseType = MANAGED_GOOGLE_DOMAIN`.
7. Remove any UI messaging that asks admins to upgrade.

## Notes

- Enterprise ID remains the same after upgrade.
- Contact info can change/reset after upgrade and may need updates in Google Admin Console.
- ENTERPRISE_UPGRADE notifications are immediate per AMAPI docs.
