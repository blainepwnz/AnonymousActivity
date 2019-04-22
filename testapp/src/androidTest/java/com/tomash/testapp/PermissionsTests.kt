package com.tomash.testapp

import android.Manifest
import android.app.Activity
import android.support.v7.app.AlertDialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import codes.titanium.anonymousactivity.permissions.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class PermissionsTests {

    var cachedActivity: Activity? = null

    @Test
    fun allowPermissionsOnFirstTimeAllowingPermissions() {
        wrapWithLatchAndCheckActivityDestroy {
            askPermission { onAllow { countDown() } }
            it.clickOnAllow()
        }
    }

    @Test
    fun ifPermissionsAreAllowedNotLaunchesActivity() {
        allowPermissionsOnFirstTimeAllowingPermissions()
        wrapWithLatch { askPermission { onAllow { countDown() } } }
        assertThat(cachedActivity).isNull()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Permission rationale
    ///////////////////////////////////////////////////////////////////////////

    @Test
    fun onFirstPermissionCallRationaleDialogIsNotShown() {
        wrapWithLatchAndCheckActivityDestroy {
            askPermission { onAllow { countDown() } }
            it.clickOnAllow()
        }
    }

    @Test
    fun showingDialogWithCustomRationaleIfDefinedAndAllowingPermissions() {
        askPermissionWithDeny()
        wrapWithLatchAndCheckActivityDestroy { uiDevice ->
            askPermission {
                onAllow { countDown() }
                permissionRationale {
                    it.dialogWithOneButton {
                        requestPermission()
                        uiDevice.clickOnAllow()
                    }
                }
            }
            uiDevice.clickOnDialogButton()
        }
    }

    @Test
    fun cancelDialogWithCustomRationaleIfDefinedDeniesWithReason() {
        askPermissionWithDeny()
        wrapWithLatchAndCheckActivityDestroy { uiDevice ->
            askPermission {
                onDeny { checkDenyReasonAndCountDown(it, CancelPermissionRationale) }
                permissionRationale { it.dialogWithOneButton {} }
            }
            uiDevice.clickSomewhereOutsideDialog()
        }
    }

    @Test
    fun showsRationaleAfterDenyOnFirstCallOfPermissions() {
        wrapWithLatchAndCheckActivityDestroy { uiDevice ->
            askPermission {
                onDeny { checkDenyReasonAndCountDown(it, DenyPermissionRationale) }
                permissionRationale { it.dialogWithOneButton { deny(DenyPermissionRationale) } }
            }
            uiDevice.clickOnDeny()
            uiDevice.clickOnDialogButton()
        }
    }

    @Test
    fun showingDialogWithCustomRationaleIfDefinedAndDenyingPermissions() {
        askPermissionWithDeny()
        showsRationaleAfterDenyOnFirstCallOfPermissions()
    }

    @Test
    fun ifBothPermissionsRationaleAndNeverAskAgainRationaleDefined_OnDenyOfPermissionsRationaleNeverAskAgainRationaleIsNotShown() {
        wrapWithLatchAndCheckActivityDestroy { uiDevice ->
            askPermission {
                onDeny { checkDenyReasonAndCountDown(it, CancelPermissionRationale) }
                permissionRationale { it.dialogWithOneButton { } }
            }
            uiDevice.clickOnDeny()
            uiDevice.clickSomewhereOutsideDialog()
        }
    }

    @Test
    fun ifBothPermissionsRationaleAndNeverAskAgainRationaleDefined_AndPermissionWasDeniedBefore_OnDenyOfPermissionsRationaleNeverAskAgainRationaleIsNotShown() {
        askPermissionWithDeny()
        ifBothPermissionsRationaleAndNeverAskAgainRationaleDefined_OnDenyOfPermissionsRationaleNeverAskAgainRationaleIsNotShown()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Never ask again rationale
    ///////////////////////////////////////////////////////////////////////////

    @Test
    fun showingDialogWithNeverAskAgainAndDenyingIt() {
        makePermissionsNeverAskAgain()
        wrapWithLatchAndCheckActivityDestroy { uiDevice ->
            askPermission {
                onDeny { checkDenyReasonAndCountDown(it, CancelNeverAskAgainRationale) }
                neverAskAgainRationale { it.dialogWithOneButton { deny(CancelNeverAskAgainRationale) } }
            }
            uiDevice.clickOnDialogButton()
        }
    }

    @Test
    fun showingDialogWithNeverAskAgainAndGoingToSettingsToEnablePermission() {
        makePermissionsNeverAskAgain()
        wrapWithLatch { uiDevice ->
            askPermission {
                onAllow { countDown() }
                neverAskAgainRationale { it.dialogWithOneButton { openSettings() } }
            }
            uiDevice.clickOnDialogButton()
            uiDevice.clickOnPermissions {}
            uiDevice.clickOnLocation {
                uiDevice.pressBack()
                uiDevice.pressBack()
            }
        }
    }

    @Test
    fun showingDialogWithNeverAskAgainAndGoingToSettingsAndDisablePermission() {
        makePermissionsNeverAskAgain()
        wrapWithLatch { uiDevice ->
            askPermission {
                onDeny { checkDenyReasonAndCountDown(it, DenyNeverAskAgainSettings) }
                neverAskAgainRationale { it.dialogWithOneButton { openSettings() } }
            }
            uiDevice.clickOnDialogButton()
            uiDevice.clickOnPermissions {
                uiDevice.pressBack()
                uiDevice.pressBack()
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper functions
    ///////////////////////////////////////////////////////////////////////////

    private fun Activity.dialogWithOneButton(init: () -> Unit = {}) = AlertDialog.Builder(this)
        .setTitle("Grant permission")
        .setMessage("Grant permission plix plox")
        .setPositiveButton(android.R.string.ok) { _, _ -> init() }
        .setIcon(android.R.drawable.ic_dialog_alert)
        .create()

    private fun askPermissionWithDeny() {
        wrapWithLatchAndCheckActivityDestroy {
            askPermission { onDeny { checkDenyReasonAndCountDown(MissingPermissionRationale, it) } }
            it.clickOnDeny()
        }
    }

    private fun makePermissionsNeverAskAgain() {
        askPermissionWithDeny()
        wrapWithLatchAndCheckActivityDestroy {
            //asking permission one more time and checking never ask again
            askPermission {
                onDeny { checkDenyReasonAndCountDown(MissingNeverAskAgainRationale, it) }
                neverAskAgainRationale { null }
            }
            it.clickOnNeverAskAgain()
            it.clickOnDeny()
        }
    }

    private fun wrapWithLatchAndCheckActivityDestroy(latchFun: CountDownLatch.(UiDevice) -> Unit) {
        wrapWithLatch(latchFun)
        wrapWithLatch {
            await(2000, TimeUnit.MILLISECONDS)
            countDown()
        }
        assertThat(cachedActivity).isNotNull
        assertThat(cachedActivity!!.isFinishing || cachedActivity!!.isDestroyed).isTrue()
        cachedActivity = null
    }

    private fun askPermission(permissionsActivityContext: PermissionsActivityContext.() -> Unit) {
        appContext.launchActivityForPermissions(Manifest.permission.ACCESS_FINE_LOCATION) {
            onActivityContextAvailable { cachedActivity = it }
            onAllow { fail("should not allow permission") }
            onDeny { fail("should not deny permisson") }
            neverAskAgainRationale { fail("should not be shown") }
            permissionsActivityContext()
        }
    }

    private fun UiDevice.clickOnDeny() = wait(Until.findObject(By.text("DENY")), maxAwaitDelay)?.click()
    private fun UiDevice.clickOnDialogButton() = wait(Until.findObject(By.text("OK")), maxAwaitDelay)?.click()
    private fun UiDevice.clickOnAllow() = wait(Until.findObject(By.text("ALLOW")), maxAwaitDelay)?.click()
    private fun UiDevice.clickSomewhereOutsideDialog() = wait(Until.findObject(By.text("OK")), maxAwaitDelay)?.also { click(142, 142); }
    private fun UiDevice.clickOnNeverAskAgain() = wait(Until.findObject(By.textContains("again")), maxAwaitDelay)?.click()
    private fun UiDevice.clickOnPermissions(afterClick: () -> Unit) = wait(Until.findObject(By.text("Permissions")), maxAwaitDelay)?.click()
        .also { afterClick() }

    private fun UiDevice.clickOnLocation(afterClick: () -> Unit) =
        wait(Until.findObject(By.text("Location")), maxAwaitDelay)?.click().also { afterClick() }


    private fun CountDownLatch.checkDenyReasonAndCountDown(expected: DenyReason, actual: DenyReason) =
        assertThat(expected == actual).isTrue().apply { countDown() }

}
