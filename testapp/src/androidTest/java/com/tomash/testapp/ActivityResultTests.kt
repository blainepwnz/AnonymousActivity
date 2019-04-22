package com.tomash.testapp

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import codes.titanium.anonymousactivity.anonymous.launchAnonymousActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityResultTests {

    //device.findObject(new UiSelector().description("Shutter button")).click();

    @Test
    fun denyPermissionsWorkingCorrectly() {
        wrapWithLatch {
            appContext.launchAnonymousActivity {
                onCreate {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package", application.packageName, null)
                    intent.data = uri
                    startActivityForResult(intent, 1)
                }
            }
        }
    }

}