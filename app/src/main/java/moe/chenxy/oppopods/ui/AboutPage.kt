package moe.chenxy.oppopods.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.chenxy.oppopods.R
import moe.chenxy.oppopods.config.ConfigManager
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    themeMode: MutableState<Int> = mutableStateOf(0),
    onThemeModeChange: (Int) -> Unit = {},
    accentMode: MutableState<Int> = mutableStateOf(0),
    onAccentModeChange: (Int) -> Unit = {},
    floatingBottomBar: MutableState<Boolean> = mutableStateOf(false),
    onFloatingBottomBarChange: (Boolean) -> Unit = {},
    blurBottomBar: MutableState<Boolean> = mutableStateOf(false),
    onBlurBottomBarChange: (Boolean) -> Unit = {},
    desktopIconHidden: MutableState<Boolean> = mutableStateOf(false),
    onDesktopIconHiddenChange: (Boolean) -> Unit = {},
    logLevel: MutableState<Int> = mutableStateOf(ConfigManager.LOG_LEVEL_BASIC),
    onLogLevelChange: (Int) -> Unit = {},
    appLanguage: MutableState<Int> = mutableStateOf(AppLocale.SYSTEM),
    onAppLanguageChange: (Int) -> Unit = {},
    autoGameMode: MutableState<Boolean> = mutableStateOf(false),
    onAutoGameModeChange: (Boolean) -> Unit = {},
    openHeyTap: MutableState<Boolean> = mutableStateOf(false),
    onOpenHeyTapChange: (Boolean) -> Unit = {},
    adaptiveMode: MutableState<Boolean> = mutableStateOf(true),
    onAdaptiveModeChange: (Boolean) -> Unit = {},
    fakeDeviceId: MutableState<String> = mutableStateOf(ConfigManager.DEFAULT_FAKE_DEVICE_ID),
    onFakeDeviceIdChange: (String) -> Unit = {},
    onOpenAbout: () -> Unit = {}
) {
    val showHeyTapWarning = remember { mutableStateOf(false) }
    val themeOptions = listOf(
        stringResource(R.string.theme_follow_system),
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_dark)
    )
    val accentOptions = listOf(
        stringResource(R.string.color_default),
        stringResource(R.string.color_monet),
    )
    val languageOptions = listOf(
        stringResource(R.string.language_system),
        stringResource(R.string.language_chinese),
        stringResource(R.string.language_english),
    )
    val logLevelValues = listOf(ConfigManager.LOG_LEVEL_OFF, ConfigManager.LOG_LEVEL_BASIC, ConfigManager.LOG_LEVEL_DEBUG)
    val logLevelOptions = listOf(
        stringResource(R.string.log_level_off),
        stringResource(R.string.log_level_basic),
        stringResource(R.string.log_level_debug),
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 12.dp,
            start = 12.dp,
            end = 12.dp
        ),
    ) {
        item {
            Card {
                OverlayDropdownPreference(
                    title = stringResource(R.string.theme_title),
                    items = themeOptions,
                    selectedIndex = themeMode.value,
                    onSelectedIndexChange = { onThemeModeChange(it) }
                )
                OverlayDropdownPreference(
                    title = stringResource(R.string.theme_color),
                    summary = stringResource(R.string.theme_color_summary),
                    items = accentOptions,
                    selectedIndex = accentMode.value.coerceIn(accentOptions.indices),
                    onSelectedIndexChange = { onAccentModeChange(it) }
                )
                SwitchPreference(
                    title = stringResource(R.string.floating_bottom_bar),
                    summary = stringResource(R.string.floating_bottom_bar_summary),
                    checked = floatingBottomBar.value,
                    onCheckedChange = { onFloatingBottomBarChange(it) }
                )
                SwitchPreference(
                    title = stringResource(R.string.blur_bottom_bar),
                    summary = stringResource(R.string.blur_bottom_bar_summary),
                    checked = blurBottomBar.value,
                    onCheckedChange = { onBlurBottomBarChange(it) }
                )
            }
        }

        item {
            Card(modifier = Modifier.padding(top = 12.dp)) {
                OverlayDropdownPreference(
                    title = stringResource(R.string.language),
                    summary = stringResource(R.string.language_summary),
                    items = languageOptions,
                    selectedIndex = appLanguage.value.coerceIn(languageOptions.indices),
                    onSelectedIndexChange = { onAppLanguageChange(it) }
                )
                OverlayDropdownPreference(
                    title = stringResource(R.string.log_level),
                    summary = stringResource(R.string.log_level_summary),
                    items = logLevelOptions,
                    selectedIndex = logLevelValues.indexOf(logLevel.value).coerceAtLeast(0),
                    onSelectedIndexChange = { onLogLevelChange(logLevelValues[it]) }
                )
                SwitchPreference(
                    title = stringResource(R.string.hide_desktop_icon),
                    summary = stringResource(R.string.hide_desktop_icon_summary),
                    checked = desktopIconHidden.value,
                    onCheckedChange = { onDesktopIconHiddenChange(it) }
                )
            }
        }

        item {
            Card(modifier = Modifier.padding(top = 12.dp)) {
                // Adaptive模式开关：控制耳机自适应降噪模式的启用状态
                SwitchPreference(
                    title = stringResource(R.string.adaptive_mode),
                    summary = stringResource(R.string.adaptive_mode_summary),
                    checked = adaptiveMode.value,
                    onCheckedChange = { onAdaptiveModeChange(it) }
                )
                SwitchPreference(
                    title = stringResource(R.string.auto_game_mode),
                    checked = autoGameMode.value,
                    onCheckedChange = { onAutoGameModeChange(it) }
                )
                SwitchPreference(
                    title = stringResource(R.string.open_heytap),
                    summary = stringResource(R.string.open_heytap_summary),
                    checked = openHeyTap.value,
                    onCheckedChange = {
                        if (it) {
                            showHeyTapWarning.value = true
                        } else {
                            onOpenHeyTapChange(false)
                        }
                    }
                )
                BasicComponent(
                    title = stringResource(R.string.fake_device_id),
                    summary = stringResource(R.string.fake_device_id_summary)
                )
                TextField(
                    value = fakeDeviceId.value,
                    onValueChange = { onFakeDeviceIdChange(it.trim()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        }

        item {
            Card(modifier = Modifier.padding(top = 12.dp)) {
                BasicComponent(
                    title = stringResource(R.string.about),
                    summary = "OppoPods-Enhanced",
                    onClick = onOpenAbout
                )
            }
        }
    }

    OverlayDialog(
        title = stringResource(R.string.heytap_warning_title),
        summary = stringResource(R.string.heytap_warning),
        show = showHeyTapWarning.value,
        onDismissRequest = {
            showHeyTapWarning.value = false
        }
    ) {
        TextButton(
            text = stringResource(R.string.confirm),
            onClick = {
                showHeyTapWarning.value = false
                onOpenHeyTapChange(true)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColorsPrimary()
        )
    }
}

@Composable
fun AboutPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 12.dp,
            start = 12.dp,
            end = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card {
                BasicComponent(
                    title = "OppoPods-Enhanced",
                    summary = "https://github.com/1812z/OppoPods",
                    onClick = {
                        Intent(Intent.ACTION_VIEW).apply {
                            this.data = Uri.parse("https://github.com/1812z/OppoPods")
                            context.startActivity(this)
                        }
                    }
                )
                BasicComponent(
                    title = "OppoPods",
                    summary = "https://github.com/Leaf-lsgtky/OppoPods",
                    onClick = {
                        Intent(Intent.ACTION_VIEW).apply {
                            this.data = Uri.parse("https://github.com/Leaf-lsgtky/OppoPods")
                            context.startActivity(this)
                        }
                    }
                )
                BasicComponent(
                    title = stringResource(R.string.based_on),
                    summary = "HyperPods by Art_Chen"
                )
                BasicComponent(
                    title = "Github",
                    summary = "https://github.com/Art-Chen/HyperPods",
                    onClick = {
                        Intent(Intent.ACTION_VIEW).apply {
                            this.data = Uri.parse("https://github.com/Art-Chen/HyperPods")
                            context.startActivity(this)
                        }
                    }
                )
            }
        }
    }
}
