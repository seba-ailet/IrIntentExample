# Interaction with the Ailet App Using Intents

This page describes how to configure interaction through the intent mechanism for your SFA application and the Ailet app.
- [Interaction with the Ailet App Using Intents](#interaction-with-the-ailet-app-using-intents)
  - [Request](#request)
    - [Methods](#methods)
    - [Parameters](#parameters)
    - [Method Call Example](#method-call-example)
  - [Response](#response)
    - [Response Data Format](#response-data-format)
    - [Getting Images from "report"](#getting-images-from-report)
    - [Statuses](#statuses)
    - [Example of Processing a Response](#example-of-processing-a-response)
  - [Broadcast Message](#broadcast-message)
    - [Broadcast Message Content](#broadcast-message-content)
    - [Broadcast Message Processing Example](#broadcast-message-processing-example)
    - [Sample report](#sample-report)
  - [Integration Examples](#integration-examples)

## Request
 
### Methods

The following methods are available for request actions from the Ailet app:

- **com.intrtl.app.ACTION_VISIT** - creating / editing a visit (activity);

- **com.intrtl.app.ACTION_REPORT** - getting visit report (json);

- **com.intrtl.app.ACTION_SUMMARY_REPORT** - getting visit summary report (activity);

- **com.intrtl.app.ACTION_SYNC** - starting the background process of transferring photos and obtaining results


### Parameters

| **Name** | **Description** | **Required for methods** |
|----------------|------------------------------|-----------------------------|
| ***Parameters:*** | | |
| **action**     | The method used.  |  |
| | | |
| ***Extras:***     | | |
| **login**      | The user's login.    | All. |
| **password**   | The user's password. | All. |
| **id**         | The ID of the user.  | All. Only if the *technical user* is used. | |
| **visit_id**   | The ID of the visit. | ACTION_VISIT, ACTION_REPORT, ACTION_SUMMARY_REPORT|
| **task_id**    | The ID of the task.  | ACTION_VISIT|
| **store_id**   | The ID of the store. | ACTION_VISIT|


### Method Call Example

```kotlin
Intent intent = new Intent();
if (intent != null) {
    intent.setAction("com.intrtl.app.ACTION_VISIT");
    intent.setFlags(0);                    
    intent.putExtra("login", user);
    intent.putExtra("password", password);
    intent.putExtra("id", user_id);
    intent.putExtra("visit_id", visit_id);
    intent.putExtra("store_id", store_id);
    startActivityForResult(intent, ACTIVITY_RESULT_START_IR_VISIT);
}
```

## Response

To return the result, a **FileProvider** is used; the intent contains the Uri of the data file in the data attribute.

| **Field**     | **Description** |
|---------------|--------------------|
| **error**     | Error, if resultCode == RESULT_CANCELED |
| **data**      | Uri of the file with result of the action. |

### Response Data Format

| **Field**     | **Description** | **Availability in response** |
|--------------------------|-----------------------|-------------------------|
| **status**    | Method execution status. | always |
| **user_id**   | The identifier of the user.               | except for the ACTION_SYNC method |
| **store_id**  | The identifier of the store.   | except for the ACTION_SYNC method |
| **task_id**   | The identifier of the task.     | except for the ACTION_SYNC method |
| **visit_id**  | The identifier of the visit.     | except for the ACTION_SYNC method |
| **internal_visit_id**  | The internal identifier of the visit.     | except for the ACTION_SYNC method |
| **install_id**| The identifier of the installaition. |except for the ACTION_SYNC method |
| **photosCounter** | The number of photos taken. | if status != error and method != ACTION_SYNC |
| **scenesCounter** | The number of scenes.     | if status != error and method != ACTION_SYNC |
| **notDetectedPhotosCounter** | Number of photos for which no data was received. | if status != error and method != ACTION_SYNC |
| **notDetectedScenesCounter** | Number of scenes for which no data is received. | if status != error and method != ACTION_SYNC |
| **report**    | Report. | if status == RESULT_OK and method != ACTION_SYNC |

### Getting Images from "report"

In new versions of the Android OS (9 and later), to get the image file path, you must use `image_uri` instead of `image_path`.

The example of getting image:

```kotlin
private fun readBitmapFromUri(uri: Uri): Bitmap? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(this.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    } else {
        MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
    }
}
```

```kotlin
val photosJSON = json.getJSONObject("report").getJSONObject("photos")
val photoNamesList = ArrayList<String>()
for ( i in 0 until photosJSON.length()) {
    photoNamesList.add(photosJSON.names()[i] as String)
}
val arrayOfBitmap = photoNamesList.map {
    val photoUri = Uri.parse(photosJSON.getString(it))
    readBitmapFromUri(photoUri)
}
```

### Statuses

Possible statuses of the error response field are given below.

| **Status** | **Description** |
|------------------------|--------------------------|
| RESULT_OK  | Successfully. |
| RESULT_INPROGRESS | Data in processing. |
| RESULT_INPROGRESS_OFFLINE | Data in processing (offline application mode). |
| RESULT_EMPTY | Blank report. |
| ERROR_NOVISIT | The visit does not exist. |
| ERROR_READONLY_VISIT | The visit is read-only. |
| ERROR_INCORRECT_INPUT_PARAMS | Invalid input parameters. |
| ERROR_VISIT_ID_INCORRECT | Invalid visit ID. |
| ERROR_AUTH | Authorisation error. |
| ERROR_PHOTO | Photo with an error. |
| ERROR_BUSY | Method is already running. |
| ERROR_CANT_LOAD_VISIT | Unable to load visit due to the lack of internet connection. |

### Example of Processing a Response

```kotlin
@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            String mode = "";
            switch (requestCode) {

                case (ACTIVITY_RESULT_START_IR_REPORT):
                    mode = "reports";
                    break;

                case (ACTIVITY_RESULT_START_IR_VISIT):
                    mode = "visit";
                    break;

                case (ACTIVITY_RESULT_START_IR_SUMMARYREPORT):
                    mode = "summaryReport";
                    break;
            }

            if (data.getData() != null) {
                String result = readFromUri(data.getData());                
                try {
                    JSONObject json = new JSONObject(result);
                    Log.i("report", json.toString());                                        
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            //On Error
        }
    }

    private String readFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();
            String string;
            while ((string = reader.readLine()) != null) {
                stringBuffer.append(string);
            }
            reader.close();
            inputStreamReader.close();
            inputStream.close();
            return stringBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
```

## Broadcast Message

When you call the `com.intrtl.app.ACTION_VISIT` method and a user takes a photo, the background process of transferring 
photos and receiving reports is launched, which, upon completion, generates a broadcast message 
`com.intrtl.app.BROADCAST_VISIT_COMPLETED`. It passes the recognition report to the third-party application.

### Broadcast Message Content

| **Field** | **Description** |
|---------------|-------------------------------------|
| **visit_id**  | The ID of the visit. |
| **internal_visit_id** | The inner ID of the visit. |
| **user_id**   | The ID of the user. |
| **store_id**  | The ID of the store. |
| **task_id**   | The ID of the task on the portal. |
| **total_photos** | Total number of photos (does not include poor quality photos). |
| **completed_photos** | Number of processed photos. | 
| **result**    | URI (string) of the file with the report. |

### Broadcast Message Processing Example

```kotlin
new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            try {
                String reportString = readFromUri(new Uri().parse(extras.getString("result")))
                JSONObject reportJson = new JSONObject(reportString);                
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
};

this.registerReceiver(shareShelfBroadcast, new IntentFilter("com.intrtl.app.BROADCAST_VISIT_COMPLETED"));
```

### Sample report 

The content example of the **result** field in broadcast and **getData()** in onActivityResult.

[Sample report](https://github.com/intrtl/AiletLibraryExamples/blob/master/Android/IrIntentExample/report_exaple_en.json)

## Integration Examples

* **Step 1:** Call the Ailet application with the `visit` method.
* **Step 2:** Perform a visit with multiple photos.
* **Step 3:** Exit the Ailet application.
* **Step 4:** Check the result:
  
    `RESULT_INPROGRESS` - wait for a broadcast message about readiness.

    `RESULT_OK` - the report is ready.

* **Step 5:** When receiving a broadcast with the RESULT_OK status, process the report.
  You can also call the Ailet app with the `REPORT` or the`SUMMARY_Report` methods.

