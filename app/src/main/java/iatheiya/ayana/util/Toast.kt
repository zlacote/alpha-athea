package iatheiya.ayana.util

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

/** A short message at the bottom of the screen, the one way Ayana shows them. */
fun Context.toast(text: CharSequence) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

fun Context.toast(@StringRes text: Int) = toast(getString(text))
