package codes.titanium.anonymousactivity

object AnonymousActivityLogger {
    internal var loggerFunc: (message: String) -> Unit = { }
    fun setLogger(loggerFunc: (message: String) -> Unit) {
        AnonymousActivityLogger.loggerFunc = loggerFunc
    }
    internal fun log(text: String) = loggerFunc(text)
}

internal operator fun <A, B> (A.() -> Unit).plus(p: A.() -> B): A.() -> B = {
    this@plus()
    p()
}

internal fun logFunc(text: String): Any.() -> Unit = { log(text) }
internal fun Any.log(text: String) = AnonymousActivityLogger.log("AnonymousActivity(${hashCode()}) : $text")

