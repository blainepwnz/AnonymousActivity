package codes.titanium.anonymousactivity.anonymous

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger

@DslMarker
annotation class AnonymousActivityDsl

fun Context.launchAnonymousActivity(activityFun: ActivityContext.() -> Unit) =
    ActivityContext().apply(activityFun).launch(this)

@AnonymousActivityDsl
class ActivityContext {

    private var updateIntent: Intent.() -> Unit = {}
    private var onCreate: AnonymousActivity.() -> Unit = {}
    private var onResume: AnonymousActivity.() -> Unit = {}
    private var onDestroy: AnonymousActivity.() -> Unit = {}
    private var onStop: AnonymousActivity.() -> Unit = {}
    private var onPause: AnonymousActivity.() -> Unit = {}
    private var onActivityResult: AnonymousActivity.(requestCode: Int, resultCode: Int, data: Intent?) -> Unit =
        { _, _, _ -> }
    private var onRequestPermissionsResult: AnonymousActivity.(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) -> Unit =
        { _, _, _ -> }

    fun onCreate(init: AnonymousActivity.() -> Unit) {
        onCreate = init
    }

    fun updateIntent(init: Intent.() -> Unit) {
        updateIntent = init
    }

    fun onResume(init: AnonymousActivity.() -> Unit) {
        onResume = init
    }

    fun onPause(init: AnonymousActivity.() -> Unit) {
        onPause = init
    }

    fun onStop(init: AnonymousActivity.() -> Unit) {
        onStop = init
    }

    fun onDestroy(init: AnonymousActivity.() -> Unit) {
        onDestroy = init
    }

    fun onActivityResult(init: AnonymousActivity.(requestCode: Int, resultCode: Int, data: Intent?) -> Unit) {
        onActivityResult = init
    }

    fun onRequestPermissionResult(init: AnonymousActivity.(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) -> Unit) {
        onRequestPermissionsResult = init
    }

    fun launch(context: Context) {
        val intent = Intent(context, AnonymousActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .apply(updateIntent)
            .putExtra(EXTRA_MESSENGER, Messenger(Handler(Looper.getMainLooper(), Handler.Callback {
                handleMessage(it)
                return@Callback true
            })))
        context.startActivity(intent)
    }

    private fun handleMessage(message: Message) {
        when (message.what) {
            ON_CREATE -> onCreate(message.obj as AnonymousActivity)
            ON_RESUME -> onResume(message.obj as AnonymousActivity)
            ON_PAUSE -> onPause(message.obj as AnonymousActivity)
            ON_STOP -> onStop(message.obj as AnonymousActivity)
            ON_DESTROY -> onDestroy(message.obj as AnonymousActivity)
            ON_ACTIVITY_RESULT -> {
                val parcel = (message.obj as OnActivityResultParcel)
                onActivityResult(parcel.anonymousActivity, parcel.requestCode, parcel.resultCode, parcel.data)
            }
            ON_REQUEST_PERMISSION_RESULT -> {
                val parcel = (message.obj as OnRequestPermissionResultParcel)
                onRequestPermissionsResult(
                    parcel.anonymousActivity,
                    parcel.requestCode,
                    parcel.permissions,
                    parcel.grantResults
                )
            }
        }
    }
}

internal class OnActivityResultParcel(
    val anonymousActivity: AnonymousActivity,
    val requestCode: Int, val resultCode: Int, val data: Intent?
)

internal class OnRequestPermissionResultParcel(
    val anonymousActivity: AnonymousActivity,
    val requestCode: Int,
    val permissions: Array<out String>,
    val grantResults: IntArray
)

internal val ON_CREATE = 0
internal val ON_ACTIVITY_RESULT = 1
internal val ON_REQUEST_PERMISSION_RESULT = 2
internal val ON_DESTROY = 3
internal val ON_RESUME = 4
internal val ON_STOP = 5
internal val ON_PAUSE = 6
internal val EXTRA_MESSENGER = "extra messenger"
