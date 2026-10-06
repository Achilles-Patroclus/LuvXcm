package com.fenji.scoretrace.ui.screen.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.fenji.scoretrace.BuildConfig
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppToast

/** 关于 ScoreTrace：Logo + 版本 + 简介 + 检查更新 / 开源许可 / 隐私政策 / 用户协议。 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    var legalDoc by remember { mutableStateOf<LegalDoc?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MineSubPageTopBar(title = "关于 ScoreTrace", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colorResource(R.color.ic_launcher_background)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "ScoreTrace",
                        modifier = Modifier.size(72.dp),
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("ScoreTrace", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
                Spacer(modifier = Modifier.height(4.dp))
                Text("版本 v${BuildConfig.VERSION_NAME}", fontSize = 13.sp, color = ScoreTraceColors.TextTertiaryLight)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "为 2027 届高考生而做的备考追踪工具",
                    fontSize = 14.sp,
                    color = ScoreTraceColors.TextSecondaryLight,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                AboutGroup {
                    AboutRow("检查更新", "当前已是最新版本") { AppToast.info("已是最新版本") }
                    AboutDivider()
                    AboutRow("开源许可", "查看使用的第三方开源库") { showLicenses = true }
                    AboutDivider()
                    AboutRow("隐私政策", "") { legalDoc = PRIVACY_POLICY }
                    AboutDivider()
                    AboutRow("用户协议", "") { legalDoc = USER_AGREEMENT }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "© 2026 ScoreTrace · Made with ❤ for 2027 届",
                    fontSize = 12.sp,
                    color = ScoreTraceColors.TextTertiaryLight,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showLicenses) {
        OpenSourceLicensesSheet(onDismiss = { showLicenses = false })
    }

    legalDoc?.let { doc ->
        AlertDialog(
            onDismissRequest = { legalDoc = null },
            title = { Text(doc.title) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(doc.body, fontSize = 13.sp, color = ScoreTraceColors.TextSecondaryLight, lineHeight = 20.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { legalDoc = null }) { Text("知道了") }
            },
        )
    }
}

@Composable
internal fun MineSubPageTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = ScoreTraceColors.TextPrimaryLight,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
    }
}

@Composable
private fun AboutGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        content()
    }
}

@Composable
private fun AboutRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = ScoreTraceColors.TextPrimaryLight,
            modifier = Modifier.weight(1f),
        )
        if (value.isNotEmpty()) {
            Text(text = value, fontSize = 13.sp, color = ScoreTraceColors.TextTertiaryLight)
        }
    }
}

@Composable
private fun AboutDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(start = 16.dp)
            .background(ScoreTraceColors.CardBorderLight),
    )
}

private data class LegalDoc(val title: String, val body: String)

private val PRIVACY_POLICY = LegalDoc(
    title = "隐私政策",
    body = "ScoreTrace 尊重并保护你的隐私。\n\n" +
        "1. 成绩、目标院校、选科、昵称、头像等数据仅保存在本机，不会上传到服务器。\n" +
        "2. 省份通过系统定位（粗定位）获取，仅用于本地展示，不做其他用途。\n" +
        "3. AI 对话内容会发送至你配置的大模型服务商以生成回复。\n" +
        "4. 卸载应用或使用「清除全部数据」即可删除本机全部数据。",
)

private val USER_AGREEMENT = LegalDoc(
    title = "用户协议",
    body = "欢迎使用 ScoreTrace。\n\n" +
        "1. 本应用为高考备考辅助工具，提供的分数线、院校与专业信息仅供参考，请以官方发布为准。\n" +
        "2. 请合理使用 AI 功能，勿输入违法违规内容。\n" +
        "3. 应用按现状提供，因使用本应用产生的任何后果由用户自行承担。",
)

/** 第三方开源库列表。 */
private data class OpenSourceLibrary(val name: String, val license: String)

private val OPEN_SOURCE_LIBRARIES = listOf(
    OpenSourceLibrary("Jetpack Compose", "Apache-2.0"),
    OpenSourceLibrary("AndroidX (Core / Lifecycle / Activity / Navigation)", "Apache-2.0"),
    OpenSourceLibrary("Material 3", "Apache-2.0"),
    OpenSourceLibrary("Hilt (Dagger)", "Apache-2.0"),
    OpenSourceLibrary("Room", "Apache-2.0"),
    OpenSourceLibrary("DataStore", "Apache-2.0"),
    OpenSourceLibrary("Retrofit", "Apache-2.0"),
    OpenSourceLibrary("OkHttp", "Apache-2.0"),
    OpenSourceLibrary("Gson", "Apache-2.0"),
    OpenSourceLibrary("Kotlin Coroutines", "Apache-2.0"),
    OpenSourceLibrary("Coil", "Apache-2.0"),
    OpenSourceLibrary("AndroidX Media3", "Apache-2.0"),
    OpenSourceLibrary("miuix-blur (yukonga)", "Apache-2.0"),
)

/** 开源许可弹层：LazyColumn 展示第三方库与许可证。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenSourceLicensesSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text("开源许可", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(listOf(ScoreTraceColors.BrandPrimary, ScoreTraceColors.AccentCyan)),
                    ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(OPEN_SOURCE_LIBRARIES) { lib ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = lib.name,
                            fontSize = 14.sp,
                            color = ScoreTraceColors.TextPrimaryLight,
                            modifier = Modifier.weight(1f),
                        )
                        Text(text = lib.license, fontSize = 12.sp, color = ScoreTraceColors.TextTertiaryLight)
                    }
                }
            }
        }
    }
}
