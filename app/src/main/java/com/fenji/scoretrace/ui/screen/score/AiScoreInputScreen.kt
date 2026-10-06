package com.fenji.scoretrace.ui.screen.score

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.fenji.scoretrace.R
import com.fenji.scoretrace.data.remote.glm.dto.ScoreSheetType
import com.fenji.scoretrace.ui.component.score.SaveBar
import com.fenji.scoretrace.ui.component.score.ScoreEntryForm
import com.fenji.scoretrace.ui.component.score.ScoreInputTopBar
import com.fenji.scoretrace.ui.component.score.ScoreSectionCard
import com.fenji.scoretrace.ui.component.score.scoreFormHasOverflow
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppToast
import com.fenji.scoretrace.util.DateUtils
import java.io.File
import java.util.Date

/**
 * AI 录成绩页：顶部「上传成绩单」大按钮 → 拍照/相册 BottomSheet → AI 自动判断图片类型并识别 →
 * 表单自动回填，用户核对后保存。
 *
 * 自动判断失败、结果不确定或图片内容审核（1301）拒绝时，降级弹出「请选择图片类型」让用户手选。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScoreInputScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    autoPickImage: Boolean = false,
    viewModel: AiScoreInputViewModel = hiltViewModel(),
) {
    val formSubjects by viewModel.formSubjects.collectAsStateWithLifecycle()
    val parsedSheet by viewModel.parsedSheet.collectAsStateWithLifecycle()
    val loadingStage by viewModel.loadingStage.collectAsStateWithLifecycle()
    val outcome by viewModel.recognizeOutcome.collectAsStateWithLifecycle()
    val pendingNameInput by viewModel.pendingNameInput.collectAsStateWithLifecycle()
    val subjectMismatch by viewModel.subjectMismatch.collectAsStateWithLifecycle()
    val reopenTypeSelector by viewModel.reopenTypeSelector.collectAsStateWithLifecycle()
    val parseHint by viewModel.parseHint.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSourceSheet by rememberSaveable { mutableStateOf(false) }
    var showTypeDialog by rememberSaveable { mutableStateOf(false) }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            pickedImageUri = uri
            viewModel.onImagePicked(uri)
        }
    }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = cameraUri
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            pickedImageUri = uri
            viewModel.onImagePicked(uri)
        }
    }

    // 内容审核 / 自动判断失败 → 重新弹出类型选择（降级）
    LaunchedEffect(reopenTypeSelector) {
        if (reopenTypeSelector) {
            showTypeDialog = true
            viewModel.consumeReopenTypeSelector()
        }
    }
    // 从 AI 助手「从相册选图」进入时，直接拉起相册
    LaunchedEffect(autoPickImage) {
        if (autoPickImage) {
            galleryPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }
    LaunchedEffect(parseHint) {
        parseHint?.let {
            AppToast.info(it)
            viewModel.clearParseHint()
        }
    }

    var examName by rememberSaveable { mutableStateOf("") }
    var examDateMillis by rememberSaveable { mutableStateOf(Date().time) }
    var classRank by rememberSaveable { mutableStateOf("") }
    var gradeRank by rememberSaveable { mutableStateOf("") }
    val scoreTexts = remember { mutableStateMapOf<String, String>() }

    // 识别成功：把结果回填到表单（只覆盖有值的字段，支持动态科目）
    LaunchedEffect(parsedSheet) {
        parsedSheet?.let { sheet ->
            if (sheet.examName.isNotBlank()) examName = sheet.examName
            if (sheet.examDate.isNotBlank()) {
                DateUtils.parseDate(sheet.examDate)?.let { examDateMillis = it.time }
            }
            sheet.classRank?.let { classRank = it.toString() }
            sheet.gradeRank?.let { gradeRank = it.toString() }
            sheet.scores.forEach { subject ->
                scoreTexts[subject.name] = if (subject.score % 1.0 == 0.0) {
                    subject.score.toInt().toString()
                } else {
                    subject.score.toString()
                }
            }
            AppToast.success("识别成功，请核对后保存")
            viewModel.clearParsedSheet()
        }
    }

    val texts = formSubjects.map { scoreTexts[it.name].orEmpty() }
    val hasOverflow = scoreFormHasOverflow(formSubjects, texts)
    val entries = formSubjects.mapIndexed { index, subject ->
        ScoreEntry(
            name = subject.name,
            score = texts[index].toDoubleOrNull() ?: 0.0,
            fullScore = subject.fullScore,
        )
    }
    val canSave = examName.isNotBlank() && !hasOverflow && entries.any { it.score > 0 }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScoreInputTopBar(title = "AI 录成绩", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                UploadScoreCard(onClick = { showSourceSheet = true })

                if (pickedImageUri != null) {
                    RecognizePreviewCard(
                        imageUri = pickedImageUri!!,
                        loadingStage = loadingStage,
                        outcome = outcome,
                    )
                }

                ScoreEntryForm(
                    examName = examName,
                    onExamNameChange = { examName = it },
                    examDate = Date(examDateMillis),
                    onExamDateChange = { examDateMillis = it.time },
                    classRank = classRank,
                    onClassRankChange = { classRank = it },
                    gradeRank = gradeRank,
                    onGradeRankChange = { gradeRank = it },
                    subjects = formSubjects,
                    scoreTexts = texts,
                    onScoreChange = { index, value -> scoreTexts[formSubjects[index].name] = value },
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            SaveBar(
                enabled = canSave,
                onSave = {
                    viewModel.save(
                        examName = examName.trim(),
                        examDate = Date(examDateMillis),
                        classRank = classRank.toIntOrNull(),
                        gradeRank = gradeRank.toIntOrNull(),
                        entries = entries.filter { it.score > 0 },
                        onSaved = onSaved,
                    )
                },
            )
        }
    }

    if (showSourceSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSourceSheet = false },
            sheetState = sheetState,
            containerColor = ScoreTraceColors.CardBackgroundLight,
        ) {
            SourcePickerContent(
                onCamera = {
                    showSourceSheet = false
                    val uri = createTempImageUri(context)
                    cameraUri = uri
                    // ActivityResultContracts 不自动授予写入权限，需显式带 ClipData + flags，否则相机写入会被拒
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        putExtra(MediaStore.EXTRA_OUTPUT, uri)
                        clipData = ClipData.newRawUri(null, uri)
                        addFlags(
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                    runCatching { cameraLauncher.launch(intent) }
                        .onFailure { AppToast.error("无法打开相机") }
                },
                onGallery = {
                    showSourceSheet = false
                    galleryPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )
        }
    }

    if (showTypeDialog) {
        SheetTypeDialog(
            onSelect = { type ->
                showTypeDialog = false
                viewModel.parseWithType(type)
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

/** 顶部「上传成绩单」渐变大按钮。 */
@Composable
private fun UploadScoreCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(ScoreTraceColors.BrandPrimary, ScoreTraceColors.AccentCyan),
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo_camera),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "上传成绩单",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "拍照或从相册选择，AI 自动识别并录入",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}

/** 图片预览 + 识别状态反馈（识别中跳动动画 / 成功 / 失败）。 */
@Composable
private fun RecognizePreviewCard(
    imageUri: Uri,
    loadingStage: String?,
    outcome: RecognizeOutcome?,
) {
    ScoreSectionCard {
        Text(
            text = "成绩单图片",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
        Spacer(modifier = Modifier.height(10.dp))
        AsyncImage(
            model = imageUri,
            contentDescription = "成绩单预览",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(modifier = Modifier.height(10.dp))
        RecognizeStatus(loadingStage = loadingStage, outcome = outcome)
    }
}

@Composable
private fun RecognizeStatus(loadingStage: String?, outcome: RecognizeOutcome?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when {
            loadingStage != null -> {
                RecognizingDots()
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = loadingStage,
                    fontSize = 13.sp,
                    color = ScoreTraceColors.BrandPrimary,
                )
            }

            outcome == RecognizeOutcome.SUCCESS -> {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = ScoreTraceColors.SuccessGreen,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "识别完成，请核对",
                    fontSize = 13.sp,
                    color = ScoreTraceColors.SuccessGreen,
                )
            }

            outcome == RecognizeOutcome.FAILED -> {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = ScoreTraceColors.ErrorRed,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "未能识别，请手动填写",
                    fontSize = 13.sp,
                    color = ScoreTraceColors.ErrorRed,
                )
            }

            else -> Text(
                text = "识别完成后请核对各科分数，必要时手动修正",
                fontSize = 13.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }
    }
}

/** 三颗上下跳动的点，表示 AI 正在识别。 */
@Composable
private fun RecognizingDots() {
    val transition = rememberInfiniteTransition(label = "recognizing")
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val offsetY = transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 420, delayMillis = index * 140),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.5.dp)
                    .size(6.dp)
                    .graphicsLayer { translationY = -offsetY.value * 5f }
                    .clip(CircleShape)
                    .background(ScoreTraceColors.BrandPrimary),
            )
        }
    }
}

/** 「拍照 / 从相册选择」BottomSheet 内容。 */
@Composable
private fun SourcePickerContent(onCamera: () -> Unit, onGallery: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "上传成绩单",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ScoreTraceColors.TextPrimaryLight,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        SourceOption(
            iconRes = R.drawable.ic_photo_camera,
            tint = ScoreTraceColors.BrandPrimary,
            title = "拍照",
            subtitle = "用相机拍摄成绩单",
            onClick = onCamera,
        )
        SourceOption(
            iconRes = R.drawable.ic_image,
            tint = ScoreTraceColors.SuccessGreen,
            title = "从相册选择",
            subtitle = "选择已保存的成绩单图片",
            onClick = onGallery,
        )
    }
}

@Composable
private fun SourceOption(
    iconRes: Int,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
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

/** 在缓存目录建一个临时文件并返回可供相机写入的 content Uri。 */
private fun createTempImageUri(context: android.content.Context): Uri {
    val file = File(context.cacheDir, "score_sheet_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
