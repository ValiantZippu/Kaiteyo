package ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalTime
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.notification.ReminderNotificationConfiguration
import ua.syt0r.kanji.core.notification.ReminderNotificationContract
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.presentation.common.MultiplatformDialog
import ua.syt0r.kanji.presentation.common.resources.string.getStrings
import ua.syt0r.kanji.presentation.common.resources.string.resolveString
import ua.syt0r.kanji.presentation.common.theme.errorColors
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SettingGroup
import ua.syt0r.kanji.presentation.screen.main.screen.home.screen.settings.components.SettingRow

// ============================================
// ANDROID REMINDER CATEGORY
// Daily study reminder with notification
// permission handling and WorkManager
// scheduling. Injected on Android via the
// custom settings categories qualifier.
// ============================================

class AndroidReminderSettingsCategory(
    private val appPreferences: PreferencesContract.AppPreferences,
    private val reminderScheduler: ReminderNotificationContract.Scheduler,
    private val analyticsManager: AnalyticsManager
) : SettingsScreenContract.Category {

    private val s = getStrings().center

    override val id: String = "notifications"
    override val title: String = s.categoryNotifications
    override val subtitle: String = s.categoryNotificationsSubtitle
    override val keywords: List<String> =
        listOf("reminder", "notification", "daily", "schedule", "time", "alert")
    override val icon: ImageVector? = Icons.Default.Notifications

    override val reset: (suspend () -> Unit)? = {
        appPreferences.reminderEnabled.set(false)
        appPreferences.reminderTime.set(LocalTime(9, 0))
        reminderScheduler.unscheduleNotification()
    }

    override val descriptors: List<SettingDescriptor> = listOf(
        SettingDescriptor(
            id = "reminder",
            title = s.categoryNotifications,
            description = s.categoryNotificationsSubtitle,
            keywords = listOf("reminder", "notification", "daily", "time"),
            render = { ReminderContent() }
        )
    )

    @Composable
    override fun content(mainNavigationState: MainNavigationState) {
        SettingGroup(
            title = null,
            children = listOf(
                { ReminderContent() }
            )
        )
    }

    @Composable
    private fun ReminderContent() {
        val scope = rememberCoroutineScope()
        val configuration = remember {
            mutableStateOf(
                ReminderNotificationConfiguration(
                    enabled = runBlocking { appPreferences.reminderEnabled.get() },
                    time = runBlocking { appPreferences.reminderTime.get() }
                )
            )
        }

        // Mirror external changes (reset from another screen).
        LaunchedEffect(Unit) {
            appPreferences.reminderEnabled.onModified.collect { enabled ->
                if (enabled != configuration.value.enabled) {
                    configuration.value = configuration.value.copy(enabled = enabled)
                }
            }
        }
        LaunchedEffect(Unit) {
            appPreferences.reminderTime.onModified.collect { time ->
                if (time != configuration.value.time) {
                    configuration.value = configuration.value.copy(time = time)
                }
            }
        }

        fun persist(value: ReminderNotificationConfiguration) {
            configuration.value = value
            scope.launch {
                appPreferences.reminderEnabled.set(value.enabled)
                appPreferences.reminderTime.set(value.time)
                if (value.enabled) {
                    reminderScheduler.scheduleNotification(value.time)
                    analyticsManager.sendEvent("reminder_enabled") {
                        put("time", value.time.toString())
                    }
                } else {
                    reminderScheduler.unscheduleNotification()
                    analyticsManager.sendEvent("reminder_disabled")
                }
            }
        }

        var shouldShowReminderDialog by remember { mutableStateOf(false) }
        if (shouldShowReminderDialog) {
            ReminderDialog(
                configuration.value,
                onDismissRequest = { shouldShowReminderDialog = false },
                onChanged = {
                    persist(it)
                    shouldShowReminderDialog = false
                }
            )
        }

        val strings = getStrings().settings
        val statusMessage = if (configuration.value.enabled)
            strings.reminderEnabled
        else strings.reminderDisabled
        val timeMessage = "%02d:%02d".format(
            configuration.value.time.hour,
            configuration.value.time.minute
        )

        val surfaceColors = LocalSurfaceColors.current
        val accent = LocalKaiteyoAccent.current
        SettingRow(
            title = strings.reminderTitle,
            description = "$statusMessage · $timeMessage",
            onClick = { shouldShowReminderDialog = true },
            trailing = {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = surfaceColors.textMuted
                )
            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDialog(
    configuration: ReminderNotificationConfiguration,
    onDismissRequest: () -> Unit,
    onChanged: (ReminderNotificationConfiguration) -> Unit
) {

    val strings = resolveString { reminderDialog }
    var notificationEnabled by remember {
        mutableStateOf(configuration.enabled)
    }

    val context = LocalContext.current
    val hasNotificationPermission by context.isNotificationPermissionGranted()

    val timePickerState = rememberTimePickerState(
        initialHour = configuration.time.hour,
        initialMinute = configuration.time.minute,
        is24Hour = true
    )

    MultiplatformDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(strings.title) },
        content = {

            if (!hasNotificationPermission) {
                NoNotificationPermissionCard()
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.enabledLabel, Modifier.weight(1f))
                Switch(
                    enabled = hasNotificationPermission,
                    checked = notificationEnabled,
                    onCheckedChange = { notificationEnabled = !notificationEnabled }
                )
            }

            TimeInput(state = timePickerState)

        },
        buttons = {
            TextButton(onDismissRequest) { Text(strings.cancelButton) }
            TextButton(
                onClick = {
                    val updatedConfiguration = ReminderNotificationConfiguration(
                        enabled = notificationEnabled,
                        time = LocalTime(timePickerState.hour, timePickerState.minute)
                    )
                    onChanged(updatedConfiguration)
                }
            ) {
                Text(strings.applyButton)
            }
        }
    )
}

@Composable
private fun NoNotificationPermissionCard() {

    val permissionActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    val context = LocalContext.current
    val strings = resolveString { reminderDialog }

    ListItem(
        headlineContent = { Text(strings.noPermissionLabel) },
        trailingContent = {
            TextButton(
                onClick = {
                    val activity = context.findActivity()!!

                    val isPermissionsRequestDenied = !ActivityCompat
                        .shouldShowRequestPermissionRationale(activity, POST_NOTIFICATIONS)

                    if (isPermissionsRequestDenied) {
                        openNotificationSettings(context)
                    } else {
                        permissionActivityLauncher.launch(POST_NOTIFICATIONS)
                    }
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(strings.noPermissionButton)
            }
        },
        colors = ListItemDefaults.errorColors(),
        modifier = Modifier.clip(MaterialTheme.shapes.medium)
    )
}

@Composable
private fun Context.isNotificationPermissionGranted(): State<Boolean> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
        return remember { mutableStateOf(true) }

    val isGranted = remember {
        mutableStateOf(checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted.value = checkSelfPermission(POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    return isGranted
}

private fun Context.findActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent()
    intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    context.startActivity(intent)
}
