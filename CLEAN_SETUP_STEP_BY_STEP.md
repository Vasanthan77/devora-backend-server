# Clean Android Enterprise Setup (Without Google Workspace)
## Step-by-Step Procedure

---

## Prerequisites

Before you start, you need:

✅ **Google Account** (your personal Gmail or work account)  
✅ **Google Cloud Project** (you already have: mdmapp-491208)  
✅ **Service Account JSON key** (already created in your current setup)  
✅ **Android Device** (Poco X5 Pro V2338 ready for factory reset)  
✅ **Your Backend Code** (Spring Boot already deployed)  

---

## Phase 1: Delete Old Enterprise (Optional but Recommended)

### Step 1.1: Verify Current Enterprise ID
Your current enterprise: `enterprises/LC01oh6rj0`

### Step 1.2: Delete via Google Cloud Console

1. Go to: **Google Cloud Console** → https://console.cloud.google.com
2. Project: Select `mdmapp-491208`
3. Search bar: Type `androidmanagement`
4. Click: **Android Management API**
5. Click: **Credentials** (left sidebar)
6. Find: Your service account
7. **Note down** the email: `mdm-device@mdmapp-491208.iam.gserviceaccount.com`

### Step 1.3: Delete Enterprise via API (Optional)

If you want to fully clean up, you can call:

```bash
curl -X DELETE \
  https://androidmanagement.googleapis.com/v1/enterprises/LC01oh6rj0 \
  -H "Authorization: Bearer [YOUR_ACCESS_TOKEN]"
```

**Don't worry if this fails** — You can just ignore the old enterprise. Creating a new one won't conflict.

---

## Phase 2: Create New Android Enterprise (Clean Setup)

### Step 2.1: Go to Android Enterprise Signup

1. Open browser: **https://developers.google.com/android/management/quickstart**
2. Follow the official enterprise binding flow from Google docs
3. If you are using your own backend/console, generate a sign-up URL using:
  - `signupUrls.create` (official method)
  - Docs: **https://developers.google.com/android/management/create-enterprise**

Note: The old direct URL `https://accounts.google.com/signup/v2/mobilework` can return errors or redirects for some accounts/regions.

### Step 2.2: Sign In with Your Google Account

- Email: Your Gmail or organization email
- Password: Your password
- Click: **Next**

### Step 2.3: Fill Enterprise Details

You'll see a form like this:

```
┌─────────────────────────────────────────┐
│  Create Your Android Enterprise Account │
├─────────────────────────────────────────┤
│                                         │
│  Organization Name: [________________]  │
│  Example: "Devora MDM" or "Your Company"
│                                         │
│  Country/Region: [India ▼]              │
│                                         │
│  Organization Email:                    │
│  [your-email@gmail.com]                 │
│                                         │
│  Organization Website (optional):       │
│  [_____________________________]         │
│                                         │
│  [✓] I agree to Terms of Service        │
│                                         │
│         [Next]                          │
│                                         │
└─────────────────────────────────────────┘
```

**Fill in:**
- **Organization Name**: "Devora MDM" or your company name
- **Country**: India
- **Email**: Your current email
- **Website**: Leave blank (optional)
- **Terms**: Check the checkbox

Click: **Next**

### Step 2.4: ⚠️ CRITICAL: Skip Google Workspace

You'll see a screen like:

```
┌──────────────────────────────────────────────┐
│  Complete Your Setup                         │
├──────────────────────────────────────────────┤
│                                              │
│  □ Set up Google Workspace                   │
│    ☐ Business Starter ($12/user/month)       │
│    ☐ Business Standard ($18/user/month)      │
│    ☐ Business Plus ($24/user/month)          │
│                                              │
│  [Skip] ← CLICK THIS                    [✓] │
│                                              │
└──────────────────────────────────────────────┘
```

👉 **Click: "Skip" button** (Don't check any Workspace plan)

### Step 2.5: Review and Confirm

You'll see:

```
Organization Set Up Successfully!

Enterprise ID: enterprises/[NEW_ID_HERE]
Organization: Devora MDM
Setup Status: Complete
Workspace: Not Enabled (✓ Good!)
```

**Copy your NEW enterprise ID** — You'll need it in your backend.

Example: `enterprises/LC1234567890` (will be different from your current one)

Click: **Done** or **Finish**

---

## Phase 3: Update Your Backend Code

### Step 3.1: Find Your Enterprise ID

After setup, you'll see your new enterprise ID in the welcome screen.

**Save it**: Example format: `enterprises/LC[random]`

### Step 3.2: Update Backend Configuration

**File**: `mdm-backend/src/main/java/com/mdm/mdm_backend/service/AmapiService.java`

**Find this line** (around line 50-70):

```java
private static final String ENTERPRISE_NAME = "enterprises/LC01oh6rj0";
```

**Replace with your new enterprise ID**:

```java
private static final String ENTERPRISE_NAME = "enterprises/LC1234567890"; // Your new ID
```

### Step 3.3: Verify Service Account Has Access

Google automatically grants the service account (`mdm-device@mdmapp-491208.iam.gserviceaccount.com`) access to your new enterprise during signup.

**No additional permissions needed.**

### Step 3.4: Rebuild and Redeploy Backend

```bash
# In mdm-backend folder
./mvnw clean package
# Railway will auto-redeploy, or manually push to Railway
```

**Verify deployment**: Call the backend health endpoint:

```bash
curl https://devora-backend-server-production.up.railway.app/api/amapi/health
```

Expected response:
```json
{"status":"ok","message":"AMAPI service is running"}
```

---

## Phase 4: Test Token Generation (NEW ENTERPRISE)

### Step 4.1: Generate Enrollment Token with New Enterprise

```bash
curl -X POST \
  -H "Authorization: Basic bWRtLWRldmljZTpTZWN1cmVQYXNzMTIz" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "emp001",
    "employeeName": "Test User",
    "type": "DEVICE_OWNER"
  }' \
  https://devora-backend-server-production.up.railway.app/api/enrollment/generate
```

**Expected response** (with new enterprise ID):

```json
{
  "token": "ANODNUQKZKUOC...",
  "qrCode": "eyJub25jZSI6I...",
  "expiresAt": "2026-03-26T08:28:47Z",
  "enrollmentConstraints": [{
    "managementMode": "DEVICE_OWNER"
  }],
  "allowPersonalUsage": "PERSONAL_USAGE_DISALLOWED"
}
```

✅ **If you get a valid token**: New enterprise is working!

---

## Phase 5: Android App Changes (Minimal)

### Step 5.1: Check App Configuration

**File**: `Zecruit-App/app/src/main/java/com/devora/devicemanager/network/ApiClient.kt`

Find:
```kotlin
val baseUrl = "https://devora-backend-server-production.up.railway.app"
```

**No changes needed** — It dynamically gets the enterprise from the backend response.

### Step 5.2: Rebuild Android App

```bash
cd Zecruit-App
./gradlew clean assembleDebug
./gradlew installDebug
```

**Result**: `app-debug.apk` installed on V2338

---

## Phase 6: Device Enrollment Test (The Real Test)

### Step 6.1: Factory Reset Device

1. Hold **Power + Volume Down** (5 seconds)
2. Select: **"Wipe data/factory reset"**
3. Wait for reset (2-3 minutes)
4. Device boots to Android Setup Wizard

### Step 6.2: Enable QR Provisioning

At Setup Wizard first screen:

1. Tap screen at same position **6 times rapidly**
2. A code detection overlay appears
3. It's ready for QR scanning

### Step 6.3: Generate QR Code from App

**On separate device/laptop:**

1. Open your Android app (or browser to your admin console)
2. Go to: **Admin > Generate Enrollment**
3. Fill in:
   - Employee ID: `test001`
   - Employee Name: `Test User`
   - Type: `DEVICE_OWNER`
4. Click: **Generate Token**
5. QR code displays on screen

### Step 6.4: Scan QR Code on Device

1. Point Poco V2338 camera at QR code
2. Hold steady for 3-5 seconds
3. Device starts provisioning automatically

### Step 6.5: Verify Enrollment Success

Device should show:

```
Setting up your device...
Installing Android Device Policy...
Configuring device settings...
Enrollment complete!
```

✅ **No "usage limits" error** (because we removed Workspace!)  
✅ **Device enrolls successfully**

---

## Phase 7: Verify in Google Admin Console

### Step 7.1: Go to Google Admin

1. Open: **https://admin.google.com**
2. Sign in with your account
3. Menu (left): **Devices**
4. Click: **Manage all**

### Step 7.2: Look for Your Device

You should see:

```
Device Name: V2338 (or automatic name)
Status: Enabled
Enrollment Status: Enrolled
Management Mode: Device Owner
Last Update: [Today's date]
```

✅ **Device appears in console** = Enrollment successful!

---

## Phase 8: Test Basic Functionality

### Step 8.1: Test from Backend

Query enrolled devices:

```bash
curl -H "Authorization: Bearer [TOKEN]" \
  https://androidmanagement.googleapis.com/v1/enterprises/LC1234567890/devices
```

You should see your device in the response.

### Step 8.2: Test from Admin Console

Go to Google Admin > Devices > Your Device

Try:
- ✅ View device details
- ✅ See enrolled apps
- ✅ Check policy status
- ✅ Remote lock (test)
- ✅ Remote reboot (test)

---

## Summary Checklist

- [ ] Old enterprise deleted (optional but clean)
- [ ] New Android Enterprise account created
- [ ] Workspace skipped (✓ no payment needed!)
- [ ] New enterprise ID noted
- [ ] Backend code updated with new enterprise ID
- [ ] Backend redeployed and tested
- [ ] Android app rebuilt
- [ ] Device factory reset
- [ ] QR enrollment completed successfully
- [ ] Device appears in Google Admin Console
- [ ] Device management functional

---

## Common Issues & Fixes

### Issue 1: "Invalid enterprise" error when generating token

**Cause**: Backend still using old enterprise ID

**Fix**:
```bash
# Check backend has correct enterprise ID
curl https://devora-backend-server-production.up.railway.app/api/amapi/enterprise/info

# Should show new enterprise ID
```

### Issue 2: Device still shows "usage limits" error

**Cause**: Device is trying to use old enterprise from cache

**Fix**:
1. Factory reset completely
2. Skip all setup steps
3. Tap for QR on fresh setup wizard

### Issue 3: QR code scanner doesn't appear

**Cause**: Wrong tap location or not enough taps

**Fix**:
1. Tap **6 times** at center of screen
2. Tap deliberately (not too fast)
3. Wait 2 seconds after 6th tap

### Issue 4: "Service account not authorized" error

**Cause**: Service account missing AMAPI permissions

**Fix**:
1. Go to: Google Cloud Console > IAM
2. Find service account: `mdm-device@...`
3. Add role: **Android Device Management Partner**
4. Wait 5 minutes for permissions to propagate

---

## Time Breakdown

| Step | Time |
|------|------|
| Delete old enterprise | 2 min |
| Create new enterprise | 3 min |
| Update backend code | 2 min |
| Rebuild & deploy | 3 min |
| Test token generation | 2 min |
| Factory reset device | 3 min |
| QR enrollment | 5 min |
| Verify in console | 2 min |
| **Total** | **~22 minutes** |

---

## After Successful Enrollment

Once device is enrolled:

✅ You have a working MDM system with ZERO payment  
✅ All 80+ AMAPI features available  
✅ No Google Workspace interference  
✅ Device fully managed and controllable  
✅ Ready for production use

**Next steps:**
- Add more devices (repeat same QR process)
- Deploy apps to devices
- Create policies for security settings
- Set up monitoring and compliance

---

## Need Help?

If you hit any errors during these steps, message me with:
1. **Which step** you're on
2. **The exact error message**
3. **What you tried**

I'll help debug! 🎯
