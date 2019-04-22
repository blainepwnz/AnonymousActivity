package codes.titanium.anonymousactivity.anonymous

import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.support.v7.app.AppCompatActivity

@AnonymousActivityDsl
class AnonymousActivity : AppCompatActivity() {

    private var messenger: Messenger? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messenger = intent.getParcelableExtra(EXTRA_MESSENGER)
        messenger?.send(createActivityParcel(ON_CREATE))
    }

    override fun onResume() {
        super.onResume()
        messenger?.send(createActivityParcel(ON_RESUME))
    }

    override fun onPause() {
        super.onPause()
        messenger?.send(createActivityParcel(ON_PAUSE))
    }

    override fun onStop() {
        super.onStop()
        messenger?.send(createActivityParcel(ON_STOP))
    }

    override fun onDestroy() {
        super.onDestroy()
        messenger?.send(createActivityParcel(ON_DESTROY))
    }

    private fun createActivityParcel(what: Int) = Message.obtain().apply {
        this.what = what
        this.obj = this@AnonymousActivity
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        messenger?.send(Message.obtain().apply {
            this.what = ON_ACTIVITY_RESULT
            this.obj = OnActivityResultParcel(this@AnonymousActivity, requestCode, resultCode, data)
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        messenger?.send(Message.obtain().apply {
            this.what = ON_REQUEST_PERMISSION_RESULT
            this.obj = OnRequestPermissionResultParcel(this@AnonymousActivity, requestCode, permissions, grantResults)
        })
    }

}

/**
 * Finishes activity with no animation
 */
fun AnonymousActivity.finishSilently() {
    finish()
    overridePendingTransition(0, 0)
}