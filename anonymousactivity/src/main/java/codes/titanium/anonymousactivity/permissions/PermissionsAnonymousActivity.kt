package codes.titanium.anonymousactivity.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import codes.titanium.anonymousactivity.anonymous.ActivityContext
import codes.titanium.anonymousactivity.anonymous.AnonymousActivity
import codes.titanium.anonymousactivity.anonymous.finishSilently

fun Context.launchActivityForPermissions(permission: String, init: PermissionsActivityContext.() -> Unit) =
    PermissionsActivityContext(permission).apply(init).launch(this)

class PermissionsActivityContext(private val permission: String) {

    private var onAllow: () -> Unit = {}
    private var onActivityContextAvailable: (Activity) -> Unit = {}
    private var onDeny: (DenyReason) -> Unit = {}
    private var neverAskAgainRationale: (Activity) -> AlertDialog? = { null }
    private var permissionRationale: (Activity) -> AlertDialog? = { null }
    private var didShownNeverAskAgainRationale = false
    private var currentDialog: AlertDialog? = null
    /**
     * Cached functions
     */
    private var openSettingsFunc: () -> Unit = {}
    private var requestPermissionFunc: () -> Unit = {}
    private var finishActivityFunc: () -> Unit = {}

    fun requestPermission() = requestPermissionFunc()

    fun openSettings() = openSettingsFunc()

    fun deny(reason: DenyReason, shouldFinish: Boolean = true) {
        onDeny(reason)
        if (shouldFinish)
            finishActivityFunc()
    }

    fun neverAskAgainRationale(init: (Activity) -> AlertDialog?) {
        neverAskAgainRationale = init
    }

    fun permissionRationale(init: (Activity) -> AlertDialog?) {
        permissionRationale = init
    }

    fun onAllow(init: () -> Unit) {
        onAllow = {
            init()
            currentDialog?.dismiss()
            finishActivityFunc()
        }
    }

    fun onDeny(init: (DenyReason) -> Unit) {
        onDeny = {
            init(it)
            currentDialog?.dismiss()
        }
    }

    fun onActivityContextAvailable(init: (Activity) -> Unit) {
        onActivityContextAvailable = init
    }

    private fun Activity.shouldShowRationale() =
        ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

    private fun Context.isGranted() =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun Activity.openSettingsForApp() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun onResume(anonymousActivity: AnonymousActivity) {
        if (didShownNeverAskAgainRationale) {
            if (anonymousActivity.isGranted())
                onAllow()
            else
                deny(DenyNeverAskAgainSettings)
        }
    }

    private fun onCreate(anonymousActivity: AnonymousActivity) {
        requestPermissionFunc = { ActivityCompat.requestPermissions(anonymousActivity, arrayOf(permission), 0) }
        openSettingsFunc = { anonymousActivity.openSettingsForApp() }
        finishActivityFunc = { anonymousActivity.finishSilently() }
        onActivityContextAvailable(anonymousActivity)
        if (anonymousActivity.shouldShowRationale()) {
            permissionRationale(anonymousActivity)?.apply {
                showAndAssignDialog(CancelPermissionRationale)
                return
            }
        }
        requestPermission()
    }

    private fun onRequestPermissionResult(anonymousActivity: AnonymousActivity, grantResults: IntArray) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onAllow()
        } else {
            if (anonymousActivity.shouldShowRationale()) {
                permissionRationale(anonymousActivity)?.showAndAssignDialog(CancelPermissionRationale)
                    ?: deny(MissingPermissionRationale)
            } else {
                neverAskAgainRationale(anonymousActivity)?.apply {
                    setOnShowListener { didShownNeverAskAgainRationale = true }
                    showAndAssignDialog(CancelNeverAskAgainRationale)
                } ?: deny(MissingNeverAskAgainRationale)
            }
        }
    }

    internal fun launch(ctx: Context) {
        if (ctx.isGranted()) {
            onAllow()
        } else {
            ActivityContext().apply {
                onCreate { this@PermissionsActivityContext.onCreate(this) }
                onResume { this@PermissionsActivityContext.onResume(this) }
                onRequestPermissionResult { _, _, results ->
                    this@PermissionsActivityContext.onRequestPermissionResult(
                        this,
                        results
                    )
                }
            }.launch(ctx)
        }
    }

    private fun AlertDialog.showAndAssignDialog(cancelReason: DenyReason) {
        currentDialog?.dismiss()
        show()
        setOnCancelListener { deny(cancelReason) }
        currentDialog = this
    }

}

sealed class DenyReason(val isNeverAskAgain: Boolean)
object MissingNeverAskAgainRationale : DenyReason(true)
object CancelNeverAskAgainRationale : DenyReason(true)
object DenyNeverAskAgainRationale : DenyReason(true)
object DenyNeverAskAgainSettings : DenyReason(true)

object MissingPermissionRationale : DenyReason(false)
object CancelPermissionRationale : DenyReason(false)
object DenyPermissionRationale : DenyReason(false)
