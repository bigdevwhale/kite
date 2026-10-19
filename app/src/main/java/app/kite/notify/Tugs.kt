package app.kite.notify

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.kite.MainActivity
import app.kite.R
import app.kite.container
import app.kite.data.Pull
import app.kite.data.Settings
import app.kite.domain.Plan
import app.kite.domain.Source
import app.kite.domain.Where
import app.kite.localized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The tugs: a few times a day, at moments nobody can guess, a soft buzz and "Where are you?".
 * "Here" answers from the notification itself; anything else opens the app to reel the string in.
 */
object Tugs {
    private const val CHANNEL = "tugs_v1"
    private const val ID = 3
    internal const val TUG = "app.kite.notify.TUG"
    internal const val HERE = "app.kite.notify.HERE"

    /** A tug left unanswered fades away; by then the moment it asked about is gone. */
    private const val TIMEOUT_MS = 15 * 60_000L

    /** You were just here (or just reeled in): the next tug within this long is skipped. */
    private const val RECENT_MS = 20 * 60_000L

    /** Two short, soft pulls on the string. */
    private val buzz = longArrayOf(0, 70, 140, 70)

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) != null) return
        val l = context.localized()
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, l.getString(R.string.channel), NotificationManager.IMPORTANCE_HIGH).apply {
                description = l.getString(R.string.channel_desc)
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = buzz
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    private fun pi(context: Context) = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, TugReceiver::class.java).setAction(TUG),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** When the next tug comes, or null with the string slack. */
    fun next(s: Settings, now: LocalDateTime = LocalDateTime.now(), zone: ZoneId = ZoneId.systemDefault()): LocalDateTime? {
        if (!s.onboarded || !s.tugs) return null
        val quiet = if (s.quietUntil > 0) LocalDateTime.ofInstant(Instant.ofEpochMilli(s.quietUntil), zone) else null
        return Plan.next(now, s.from, s.to, s.perDay, s.salt, quiet)
    }

    suspend fun reschedule(context: Context) {
        val s = context.container.settings.now()
        val am = context.getSystemService(AlarmManager::class.java)
        val p = pi(context)
        val zone = ZoneId.systemDefault()
        val at = next(s, LocalDateTime.now(zone), zone)
        if (at == null) {
            am.cancel(p)
            return
        }
        // A tug at a moment nobody can guess doesn't need to-the-second precision; this avoids the exact-alarm permission.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.atZone(zone).toInstant().toEpochMilli(), p)
    }

    suspend fun fire(context: Context) {
        val s = context.container.settings.now()
        if (!s.onboarded || !s.tugs || !canNotify(context)) return
        if (System.currentTimeMillis() < s.quietUntil) return
        val last = context.container.db.dao().last()
        if (last != null && System.currentTimeMillis() - last.at < RECENT_MS) return
        show(context)
    }

    /** The tug itself; also sent from settings as a try. */
    fun show(context: Context) {
        val l = context.localized()
        val open = PendingIntent.getActivity(
            context, 100,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_WHERE)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val here = PendingIntent.getBroadcast(
            context, 101,
            Intent(context, HereReceiver::class.java).setAction(HERE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.kite))
            .setContentTitle(l.getString(R.string.tug_title))
            .setContentText(l.getString(R.string.tug_text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSilent(false)
            .setVibrate(buzz)
            .setTimeoutAfter(TIMEOUT_MS)
            .setContentIntent(open)
            .addAction(0, l.getString(R.string.tug_here), here)
            .addAction(0, l.getString(R.string.tug_away), open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID, n)
        } catch (_: SecurityException) {
        }
    }

    fun clear(context: Context) = NotificationManagerCompat.from(context).cancel(ID)
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class TugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Tugs.TUG) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Tugs.fire(context)
                Tugs.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}

/** "Here" straight from the notification: the kite was in the grass all along. */
class HereReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Tugs.HERE) return
        Tugs.clear(context)
        val pending = goAsync()
        receiverScope.launch {
            try {
                context.container.db.dao().insert(Pull(where = Where.HERE.name, source = Source.TUG.name, reeled = true))
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Tugs.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
