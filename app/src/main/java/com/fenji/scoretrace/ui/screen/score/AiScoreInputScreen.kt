package com.fenji.scoretrace.ui.screen.score

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.fenji.scoretrace.R
import com.fenji.scoretrace.data.remote.glm.dto.ScoreSheetType
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppToast
import com.fenji.scoretrace.util.DateUtils
import java.util.Date

/** 需要逐科录入的六科与各自满分（云南 3+1+2：语数外 150，其余 100）。 */
private val FORM_SUBJECTS = listOf(
    "语文" to 150.0,
    "数学" to 150.0,
    "英语" to 150.0,
    "物理" to 100.0,
    "化学" to 100.0,
    "生物" to 100.0,
)

/**
 * AI 录成绩页：手动录入考试名、日期、班级排名与各科分数，实时汇总总分并校验（分数不超过满分），
 * 保存后写入数据库并生成「新成绩已录入」通知。
 */
@Composable
fun AiScoreInputScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    autoPickImage: Boolean = false,
    viewModel: AiScoreInputViewModel = hiltViewModel(),
) {
    val parseHint by viewModel.parseHint.collectAsStateWithLifecycle()
    val parsedSheet by viewModel.parsedSheet.collectAsStateWithLifecycle()
    val loadingStage by viewModel.loadingStage.collectAsStateWithLifecycle()
    val pendingNameInput by viewModel.pendingNameInput.collectAsStateWithLifecycle()
    val subjectMismatch by viewModel.subjectMismatch.collectAsStateWithLifecycle()
    val reopenTypeSelector by viewModel.reopenTypeSelector.collectAsStateWithLifecycle()
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showTypeDialog by rememberSaveable { mutableStateOf(false) }
    var selectedSheetType by remember { mutableStateOf<ScoreSheetType?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val type = selectedSheetType
        if (uri != null && type != null) {
            pickedImageUri = uri
            viewModel.parseScoreImage(uri, type)
        }
    }

    // 内容审核（1301）失败 → 重新弹出类型选择
    LaunchedEffect(reopenTypeSelector) {
        if (reopenTypeSelector) {
            showTypeDialog = true
            viewModel.consumeReopenTypeSelector()
        }
    }
    // 从 AI 助手「从相册选图」进入时，直接弹出图片类型选择
    LaunchedEffect(autoPickImage) {
        if (autoPickImage) showTypeDialog = true
    }
    LaunchedEffect(parseHint) {
        parseHint?.let {
            AppToast.info(it)
            viewModel.clearParseHint()
        }
    }

    var examName by rememberSaveable { mutableStateOf("") }
    var examDateText by rememberSaveable { mutableStateOf(DateUtils.formatDate(Date())) }
    var classRankText by rememberSaveable { mutableStateOf("") }
    val scoreTexts = remember { mutableStateListOf(*Array(FORM_SUBJECTS.size) { "" }) }

    val entries = FORM_SUBJECTS.mapIndexed { index, (name, fullScore) ->
        ScoreEntry(name = name, score = scoreTexts[index].toDoubleOrNull() ?: 0.0, fullScore = fullScore)
    }
    val total = entries.sumOf { if (it.score > 0) it.score else 0.0 }
    val hasOverflow = entries.any { it.score > it.fullScore }
    val canSave = examName.isNotBlank() && !hasOverflow && entries.any { it.score > 0 }

    // 识别成功：把结果回填到表单（只覆盖有值的字段）
    LaunchedEffect(parsedSheet) {
        parsedSheet?.let { sheet ->
            if (sheet.examName.isNotBlank()) examName = sheet.examName
            if (sheet.examDate.isNotBlank()) examDateText = sheet.examDate
            sheet.classRank?.let { classRankText = it.toString() }
            sheet.scores.forEach { subject ->
                val index = FORM_SUBJECTS.indexOfFirst { it.first == subject.name }
                if (index >= 0) {
                    scoreTexts[index] = if (subject.score % 1.0 == 0.0) {
                        subject.score.toInt().toString()
                    } else {
                        subject.score.toString()
                    }
                }
            }
            AppToast.success("识别成功，请核对后保存")
            viewModel.clearParsedSheet()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AiScoreTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (pickedImageUri != null) {
                    SectionCard {
                        Text("成绩单图片", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
                        Spacer(modifier = Modifier.height(10.dp))
                        AsyncImage(
                            model = pickedImageUri,
                            contentDescription = "成绩单预览",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = loadingStage ?: "识别完成后请核对各科分数，必要时手动修正",
                            fontSize = 13.sp,
                            color = if (loadingStage != null) ScoreTraceColors.BrandPrimary else ScoreTraceColors.TextSecondaryLight,
                        )
                    }
                }

                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("考试信息", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ScoreTraceColors.BrandPrimary.copy(alpha = 0.12f))
                                .clickable { showTypeDialog = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_photo_camera),
                                contentDescription = "拍照或从相册选择",
                                tint = ScoreTraceColors.BrandPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = examName,
                        onValueChange = { examName = it },
                        label = { Text("考试名称") },
                        placeholder = { Text("如：高三10月月考") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = examDateText,
                        onValueChange = { examDateText = it },
                        label = { Text("考试日期（yyyy-MM-dd）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = classRankText,
                        onValueChange = { classRankText = it.filter(Char::isDigit) },
                        label = { Text("班级排名（可空）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                SectionCard {
                    Text("各科分数", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
                    Spacer(modifier = Modifier.height(4.dp))
                    FORM_SUBJECTS.forEachIndexed { index, (name, fullScore) ->
                        if (index > 0) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = ScoreTraceColors.CardBorderLight,
                            )
                        }
                        val score = scoreTexts[index].toDoubleOrNull() ?: 0.0
                        val overflow = score > fullScore
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = name,
                                fontSize = 15.sp,
                                color = ScoreTraceColors.TextPrimaryLight,
                                modifier = Modifier.width(52.dp),
                            )
                            OutlinedTextField(
                                value = scoreTexts[index],
                                onValueChange = { scoreTexts[index] = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                placeholder = { Text("0", fontSize = 14.sp) },
                                isError = overflow,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "/ ${fullScore.toInt()}",
                                fontSize = 13.sp,
                                color = if (overflow) ScoreTraceColors.ErrorRed else ScoreTraceColors.TextSecondaryLight,
                            )
                        }
                    }
                }

                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("总分", fontSize = 13.sp, color = ScoreTraceColors.TextSecondaryLight)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${total.toInt()}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = ScoreTraceColors.BrandPrimary,
                            )
                        }
                        Text(
                            text = if (hasOverflow) "有科目超过满分，请检查" else "各科分之和自动汇总",
                            fontSize = 12.sp,
                            color = if (hasOverflow) ScoreTraceColors.ErrorRed else ScoreTraceColors.TextTertiaryLight,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            SaveBar(
                enabled = canSave,
                onSave = {
                    viewModel.save(
                        examName = examName.trim(),
                        examDate = DateUtils.parseDate(examDateText) ?: Date(),
                        classRank = classRankText.toIntOrNull(),
                        entries = entries.filter { it.score > 0 },
                        onSaved = onSaved,
                    )
                },
            )
        }
    }

    if (showTypeDialog) {
        SheetTypeDialog(
            onSelect = { type ->
                selectedSheetType = type
                showTypeDialog = false
                imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onDismiss = { showTypeDialog = false },
        )
    }

    if (pendingNameInput) {
        NameInputDialog(
            onConfirm = viewModel::onNameSubmitted,
            onDismiss = viewModel::onCancelNameInput,
        )
    }

    subjectMismatch?.let { mismatch ->
        SubjectMismatchDialog(
            recognized = mismatch.recognized,
            current = mismatch.current,
            onConfirm = viewModel::onConfirmSubjectSwitch,
            onDismiss = viewModel::onCancelSubjectSwitch,
        )
    }
}

@Composable
private fun SheetTypeDialog(
    onSelect: (ScoreSheetType) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("请选择图片类型") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TypeOption(
                    title = "📄 个人成绩单",
                    subtitle = "单人多科成绩表，选完直接识别回填",
                    onClick = { onSelect(ScoreSheetType.PERSONAL) },
                )
                TypeOption(
                    title = "📋 班级排名表",
                    subtitle = "多人成绩表，需输入姓名后提取对应行",
                    onClick = { onSelect(ScoreSheetType.CLASS_RANKING) },
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun TypeOption(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ScoreTraceColors.BrandPrimary.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = ScoreTraceColors.TextSecondaryLight,
        )
    }
}

@Composable
private fun NameInputDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("检测到班级排名表") },
        text = {
            Column {
                Text(
                    text = "请输入你在表格中的姓名，将从表格中提取对应成绩。",
                    fontSize = 13.sp,
                    color = ScoreTraceColors.TextSecondaryLight,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    placeholder = { Text("请输入你的姓名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name) },
            ) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun SubjectMismatchDialog(
    recognized: List<String>,
    current: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("检测到选科不一致") },
        text = {
            Text(
                text = "识别到的科目为：${recognized.joinToString(" · ")}。\n" +
                    "你当前选科为：${current.joinToString(" · ")}。\n是否切换选科并回填？",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("切换并回填") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun AiScoreTopBar(onBack: () -> Unit) {
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
                .background(Color.White)
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
            text = "AI 录成绩",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        content()
    }
}

@Composable
private fun SaveBar(enabled: Boolean, onSave: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val shape = RoundedCornerShape(26.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(shape)
                    .background(
                        if (enabled) {
                            Brush.horizontalGradient(
                                listOf(ScoreTraceColors.BrandPrimary, ScoreTraceColors.AccentCyan),
                            )
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFFC3CBD8), Color(0xFFC3CBD8)))
                        },
                    )
                    .then(if (enabled) Modifier.clickable(onClick = onSave) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "保存成绩",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}
