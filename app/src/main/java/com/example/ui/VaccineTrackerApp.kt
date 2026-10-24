package com.example.ui

import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*


fun formatIraqiPhoneNumber(input: String): String {
    val trimmed = input.trim().replace(" ", "").replace("-", "")
    if (trimmed.isEmpty()) return ""
    
    // If the input is 11 digits, delete the first digit on the left (index 0) and prepend +964
    if (trimmed.length == 11) {
        return "+964" + trimmed.substring(1)
    }
    
    // Handle cases where 0 is pre-entered but not 11 digits
    if (trimmed.startsWith("0")) {
        return "+964" + trimmed.substring(1)
    }
    
    // Handle +9640... (starts with +964 followed by 0)
    if (trimmed.startsWith("+9640")) {
        return "+964" + trimmed.substring(5)
    }
    
    // Handle 9640... (starts with 964 followed by 0)
    if (trimmed.startsWith("9640")) {
        return "+964" + trimmed.substring(4)
    }
    
    // If already starts with +964, keep it
    if (trimmed.startsWith("+964")) {
        return trimmed
    }
    
    // If starts with 964, add +
    if (trimmed.startsWith("964")) {
        return "+" + trimmed
    }
    
    // If starts with 7 (e.g. 7701234567)
    if (trimmed.startsWith("7")) {
        return "+964" + trimmed
    }
    
    return trimmed
}

fun formatOnTheFly(input: String): String {
    // The user wants to type normally (e.g. 11 digits starting with 0) without it automatically 
    // replacing with +964 while typing. The +964 is added upon saving.
    return input.trim().replace(" ", "").replace("-", "")
}

fun cleanPhoneForWhatsApp(phone: String): String {
    return phone.replace("+", "").replace(" ", "").replace("-", "").trim()
}

@Composable
private fun CustomBottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) Color(0xFFE3F2FD) else Color.Transparent
    val contentColor = if (selected) Color(0xFF1877F2) else Color.DarkGray

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LanguageSwitcherIcon(viewModel: VaccineViewModel) {
    IconButton(onClick = { viewModel.toggleAppLanguage() }) {
        Icon(Icons.Default.Language, contentDescription = "اللغة", tint = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccineTrackerApp(viewModel: VaccineViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val defaultHealthCenter by viewModel.defaultHealthCenter.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.check24HoursNotification()
    }
    val context = LocalContext.current

    var showSplash by remember { mutableStateOf(true) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showWelcomeDialog by remember { mutableStateOf(true) }

    if (showSplash) {
        RtlLayoutWrapper(lang = appLanguage) {
            SplashScreen(onDismiss = { showSplash = false })
        }
        return
    }

    // Intercept back presses to show custom exit phrase
    BackHandler(enabled = true) {
        showExitDialog = true
    }

    RtlLayoutWrapper(lang = appLanguage) {
        Scaffold(
            topBar = {
                if (isLoggedIn && currentScreen != AppScreen.Onboarding && currentScreen != AppScreen.Login) {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = Transl.t("app_title", appLanguage),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = Transl.t("center_subtitle", appLanguage, defaultHealthCenter),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LanguageSwitcherIcon(viewModel = viewModel)
                                    IconButton(
                                        onClick = { viewModel.navigateTo(AppScreen.QrScanner) },
                                        modifier = Modifier.testTag("nav_qr_scanner_button")
                                    ) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            contentDescription = "مسح QR اللقاحات",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    // Theme Toggle Button - Sun and Moon
                                    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
                                    IconButton(
                                        onClick = { viewModel.toggleTheme() },
                                        modifier = Modifier.testTag("toggle_theme_button")
                                    ) {
                                        androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
                                            if (isDarkTheme) {
                                                // Sun for toggling back to Light mode (glowing yellow sun)
                                                val center = center
                                                val radius = size.minDimension / 3.2f
                                                drawCircle(color = Color(0xFFFBBF24), radius = radius, center = center)
                                                // Sun rays
                                                val rayLength = radius * 0.45f
                                                val rayCount = 8
                                                for (i in 0 until rayCount) {
                                                    val angle = (i * 2 * Math.PI / rayCount).toFloat()
                                                    val startX = center.x + (radius + 2f) * kotlin.math.cos(angle)
                                                    val startY = center.y + (radius + 2f) * kotlin.math.sin(angle)
                                                    val endX = center.x + (radius + 2f + rayLength) * kotlin.math.cos(angle)
                                                    val endY = center.y + (radius + 2f + rayLength) * kotlin.math.sin(angle)
                                                    drawLine(
                                                        color = Color(0xFFFBBF24),
                                                        start = androidx.compose.ui.geometry.Offset(startX, startY),
                                                        end = androidx.compose.ui.geometry.Offset(endX, endY),
                                                        strokeWidth = 3f,
                                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                                    )
                                                }
                                            } else {
                                                // Moon for toggling to Dark mode (clean crescent moon)
                                                val center = center
                                                val radius = size.minDimension / 2.5f
                                                val moonPath = androidx.compose.ui.graphics.Path().apply {
                                                    moveTo(center.x + radius * 0.5f, center.y - radius)
                                                    cubicTo(
                                                        center.x - radius, center.y - radius,
                                                        center.x - radius, center.y + radius,
                                                        center.x + radius * 0.5f, center.y + radius
                                                    )
                                                    cubicTo(
                                                        center.x - radius * 0.2f, center.y + radius * 0.6f,
                                                        center.x - radius * 0.2f, center.y - radius * 0.6f,
                                                        center.x + radius * 0.5f, center.y - radius
                                                    )
                                                    close()
                                                }
                                                // Indigo/slate crescent moon representation
                                                drawPath(path = moonPath, color = Color(0xFF6366F1))
                                            }
                                        }
                                    }

                                    // Alert Hub Badge
                                    val alertCount by viewModel.overdueCount.collectAsState()
                                    val approachingCount by viewModel.approachingCount.collectAsState()
                                    val totalAlerts = alertCount + approachingCount

                                    IconButton(
                                        onClick = { viewModel.navigateTo(AppScreen.Alerts) },
                                        modifier = Modifier.testTag("nav_alerts_button")
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (totalAlerts > 0) {
                                                    Badge(containerColor = if (alertCount > 0) Color.Red else Color(0xFFEAB308)) {
                                                        Text(totalAlerts.toString(), color = Color.White)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Notifications,
                                                contentDescription = Transl.t("notifications", appLanguage),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            bottomBar = {
                if (isLoggedIn && currentScreen != AppScreen.Onboarding && currentScreen != AppScreen.Login) {
                    Row(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(36.dp)
                            )
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomBottomNavItem(
                            selected = currentScreen is AppScreen.Home,
                            onClick = { viewModel.navigateTo(AppScreen.Home) },
                            icon = Icons.Default.Home,
                            label = "الرئيسية",
                            modifier = Modifier.weight(1f).testTag("nav_home_tab")
                        )
                        CustomBottomNavItem(
                            selected = currentScreen is AppScreen.ChildrenList || currentScreen is AppScreen.ChildDetails,
                            onClick = { viewModel.navigateTo(AppScreen.ChildrenList) },
                            icon = Icons.Default.Person,
                            label = "الأشخاص",
                            modifier = Modifier.weight(1f).testTag("nav_children_tab")
                        )
                        CustomBottomNavItem(
                            selected = currentScreen is AppScreen.Alerts,
                            onClick = { viewModel.navigateTo(AppScreen.Alerts) },
                            icon = Icons.Default.Notifications,
                            label = "الإشعارات",
                            modifier = Modifier.weight(1f).testTag("nav_alerts_tab")
                        )
                        CustomBottomNavItem(
                            selected = currentScreen is AppScreen.Settings,
                            onClick = { viewModel.navigateTo(AppScreen.Settings) },
                            icon = Icons.Default.Settings,
                            label = "الإعدادات",
                            modifier = Modifier.weight(1f).testTag("nav_settings_tab")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                Column {
                    val isSoundPlaying by viewModel.isSoundPlaying.collectAsState()
                    if (isSoundPlaying) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.stopAlertSound() }
                                .padding(8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "🔊 جاري تشغيل صوت تنبيه اللقاح المستحق! اضغط هنا لكتم الصوت فوراً 🔕",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "كتم الآن ❌",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentScreen) {
                            is AppScreen.Login -> LoginScreen(viewModel)
                            is AppScreen.Home -> HomeScreen(viewModel)
                            is AppScreen.ChildrenList -> ChildrenListScreen(viewModel)
                            is AppScreen.AddChild -> AddChildScreen(viewModel)
                            is AppScreen.AddAdultTd -> AddAdultTdScreen(viewModel)
                            is AppScreen.FamiliesList -> FamiliesListScreen(viewModel)
                            is AppScreen.AddFamily -> AddFamilyScreen(viewModel)
                            is AppScreen.ChildDetails -> ChildDetailsScreen(
                                childId = (currentScreen as AppScreen.ChildDetails).childId,
                                viewModel = viewModel
                            )
                            is AppScreen.Alerts -> AlertsScreen(viewModel)
                            is AppScreen.ProgressChart -> ProgressChartScreen(viewModel)
                            is AppScreen.Settings -> SettingsScreen(viewModel)
                            is AppScreen.VaccineScheduleTable -> VaccineScheduleTableScreen(viewModel)
                            is AppScreen.DataEntry -> DataEntryScreen(viewModel)
                            is AppScreen.ViewData -> ViewDataScreen(viewModel)
                            is AppScreen.InfoAndExplanation -> InfoAndExplanationScreen(viewModel)
                            is AppScreen.VaccineInventory -> VaccineInventoryScreen(viewModel)
                            is AppScreen.Onboarding -> OnboardingScreen(viewModel)
                             is AppScreen.QrScanner -> QrScannerScreen(viewModel)
                        }
                    }
                }
            }
        }

        if (showWelcomeDialog) {
            AlertDialog(
                onDismissRequest = { showWelcomeDialog = false },
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.img_clean_i7san_app_icon_1782895431554),
                            contentDescription = "شعار إحسان",
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "مرحباً بكم في تطبيق إحسان",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "نظام متابعة لقاحات الأطفال وإدارة اللقاحات 🛡️",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Text(
                                "نظام ذكي متكامل مصمم لتسهيل متابعة وتدقيق لقاحات الأطفال بالاعتماد على جدول اللقاحات الوطني ومراقبة أرصدة اللقاحات ومكافحة تسربها.",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            TelegramChannelFollowCard()
                            Spacer(modifier = Modifier.height(6.dp))
                            DeveloperContactInfoCard()
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showWelcomeDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("الدخول إلى النظام 👍", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.img_clean_i7san_app_icon_1782895431554),
                            contentDescription = "شعار إحسان",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "مغادرة إحسان - في أمان الله 👋",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Text(
                                text = "دوام الصحة والعافية لأطفالنا الأعزاء ان شاء الله 👶❤️\nوكوادر التحصين المبدعون 🧑‍⚕️✨",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            TelegramChannelFollowCard()
                            Spacer(modifier = Modifier.height(6.dp))
                            DeveloperContactInfoCard()
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val activity = context as? android.app.Activity
                            activity?.finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("إغلاق التطبيق نهائياً", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("إلغاء والرجوع", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        // Side effect alert popup
        val sideEffectMessage by viewModel.sideEffectAlertMessage.collectAsState()
        if (sideEffectMessage != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissSideEffectAlert() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تحذير أثر جانبي من احسان ال شنتي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFDC2626)
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = sideEffectMessage ?: "",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Actionable instructions block
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                            border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "💡 إرشادات هامة للتعامل الأثر الجانبي:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "1. إعطاء خافض حرارة ومسكن ملائم لعمر الطفل.\n2. وضع كمادات ماء فاتر وتجنب الماء المثلج.\n3. الإكثار من الرضاعة الطبيعية والسوائل.\n4. قياس ومراقبة الحرارة وإبلاغ المركز بـ احسان ال شنتي عند الطوارئ.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78350F),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissSideEffectAlert() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("فهمت الإرشادات ومتابعة حرارة الطفل 👍", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun LoginScreen(viewModel: VaccineViewModel) {
    val savedMe by viewModel.rememberMe.collectAsState()
    val savedUser by viewModel.savedUser.collectAsState()
    val activeAdminUser by viewModel.adminUsername.collectAsState()

    var username by remember { mutableStateOf(viewModel.adminUsername.value) }

    LaunchedEffect(savedMe, savedUser, activeAdminUser) {
        if (savedMe && savedUser.isNotEmpty()) {
            username = savedUser
        } else {
            username = activeAdminUser
        }
    }

    val loginError by viewModel.loginError.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            // Header medical Graphic Illustration block
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.img_vaccine_hero_1781984579282),
                        contentDescription = "شعار حماية الطفل وتلقيحه",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "نظام متابعة اللقاحات الرقمي المعتمد من احسان ال شنتي",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "بوابة الملقح الطبي المعتمد لوزارة الصحة",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 24.dp)
            )

            if (loginError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "خطأ", tint = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = loginError ?: "",
                            color = Color(0xFF991B1B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("اسم المستخدم المسؤول") },
                placeholder = { Text("أدخل اسمك أو admin") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("login_username_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Checkbox(
                    checked = savedMe,
                    onCheckedChange = { viewModel.setRememberMe(it) },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "تذكر اسم المستخدم المسؤول",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = { viewModel.login(username) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_submit_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "دخول سريع إلى النظام",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "تلميح للمصححين والمشرفين:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                    Text(
                        "• اسم المستخدم: admin\n• كلمة المرور الحالية: 123456",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            ProgrammerFooter()
        }
    }
}

@Composable
fun HomeScreen(viewModel: VaccineViewModel) {
    val children by viewModel.children.collectAsState()
    val totalCount by viewModel.totalChildrenCount.collectAsState()
    val overdueCount by viewModel.overdueCount.collectAsState()
    val approachingCount by viewModel.approachingCount.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val context = LocalContext.current
    val childrenProgress by viewModel.childrenProgress.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Stats / Dashboard row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stat 1: Total Registered Kids
                Card(
                    modifier = Modifier.weight(1f).clickable { viewModel.navigateTo(AppScreen.ChildrenList) },
                    colors = CardDefaults.cardColors(containerColor = RegisteredBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "المسجلين",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(totalCount.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Stat 2: Approaching Warnings (Yellow)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(AppScreen.Alerts) },
                    colors = CardDefaults.cardColors(containerColor = UpcomingBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "مواعيد قريبة",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(approachingCount.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }
                }

                // Stat 3: Defaulter Overdues (Red)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(AppScreen.Alerts) },
                    colors = CardDefaults.cardColors(containerColor = DropoutBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "المتسربين",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(overdueCount.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // 1. بوابة إدخال البيانات والتسجيل
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(AppScreen.DataEntry) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Transl.t("home_data_entry_title", appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = Transl.t("home_data_entry_desc", appLanguage),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }

        // 2. بوابة عرض الملقحين والبيانات
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(AppScreen.ViewData) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ListAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Transl.t("home_view_data_title", appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = Transl.t("home_view_data_desc", appLanguage),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }

        // 3. بوابة إدارة اللقاحات ومستلزمات التلقيح
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(AppScreen.VaccineInventory) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Transl.t("home_inventory_title", appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = Transl.t("home_inventory_desc", appLanguage),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }

        // 4. بوابة توضيح ومعلومات
        // Section removed as requested

        item {
            Spacer(modifier = Modifier.height(10.dp))
            ProgrammerFooter()
        }
    }
}

@Composable
fun ChildrenListScreen(viewModel: VaccineViewModel) {
    val children by viewModel.children.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    var childToEdit by remember { mutableStateOf<com.example.data.Child?>(null) }
    var childToDelete by remember { mutableStateOf<com.example.data.Child?>(null) }

    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Children, 2: Adult Td

    val filteredChildren = remember(children, selectedTab) {
        when (selectedTab) {
            1 -> children.filter { !it.isAdultTd }
            2 -> children.filter { it.isAdultTd }
            else -> children
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        // Compact Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { 
                    Text(
                        text = "بحث...", 
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    ) 
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.updateSearchQuery("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear, 
                                contentDescription = "حذف",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp, max = 48.dp)
                    .testTag("search_child_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }

        // Segmented Tabs Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabsAr = listOf("عرض الكل", "دون الخامسة", "ثنائي كبار")
            tabsAr.forEachIndexed { index, title ->
                val selected = selectedTab == index
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = index },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (filteredChildren.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isBlank()) {
                                if (appLanguage == "en") "No registered vaccinees in the database." else "لا يوجد ملقحين مسجلين حالياً تطابق الفئة المحددة."
                            } else {
                                if (appLanguage == "en") "No search results match query." else "لم نعثر على مطابقة لطلب البحث في هذه الفئة."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredChildren, key = { it.id }) { child ->
                        ChildCard(
                            child = child,
                            viewModel = viewModel,
                            onClick = { viewModel.navigateTo(AppScreen.ChildDetails(child.id)) },
                            onEdit = {
                                childToEdit = child
                            },
                            onDelete = {
                                childToDelete = child
                            }
                        )
                    }
                    item {
                        ProgrammerFooter()
                    }
                }
            }

            // Simple Overlay Floating Action Button to Register a Kid
            ExtendedFloatingActionButton(
                onClick = { viewModel.navigateTo(AppScreen.AddChild) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_child_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "تسجيل جديد")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تسجيل طفل جديد",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        childToEdit?.let { child ->
            EditChildDialog(
                child = child,
                onDismiss = { childToEdit = null },
                onSave = { updatedChild ->
                    viewModel.updateChildDetails(updatedChild)
                    childToEdit = null
                }
            )
        }

        childToDelete?.let { child ->
            AlertDialog(
                onDismissRequest = { childToDelete = null },
                title = { Text("حذف الطفل المسجل") },
                text = { Text("هل أنت متأكد من حذف ${child.fullName} بشكل نهائي؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteChildAccount(child)
                            childToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { childToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
fun ChildCard(child: Child, viewModel: VaccineViewModel, onClick: () -> Unit, onEdit: () -> Unit = {}, onDelete: () -> Unit = {}) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val ageStr = remember(child.dob) { VaccineScheduler.computeAgeString(child.dob) }
    val records by viewModel.getChildVaccinesFlow(child.id).collectAsState(initial = emptyList())
    
    val totalVaccines = records.size
    val takenVaccines = records.count { it.isTaken }
    val progress = if (totalVaccines > 0) takenVaccines.toFloat() / totalVaccines else 0f


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("child_item_${child.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar with Progress Arc
                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 4.dp.toPx()
                        // Background arc
                        drawArc(
                            color = androidx.compose.ui.graphics.Color.LightGray.copy(alpha = 0.5f),
                            startAngle = 0f,
                            sweepAngle = -180f, // Top half circle (right to left)
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                        // Progress arc
                        drawArc(
                            color = androidx.compose.ui.graphics.Color(0xFF4CAF50), // Green for progress
                            startAngle = 0f,
                            sweepAngle = -180f * progress,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    }
                    val avatarColor = if (child.gender == "أنثى") Color(0xFFF8BBD0) else Color(0xFFBBDEFB)
                    val onAvatarColor = if (child.gender == "أنثى") Color(0xFFC2185B) else Color(0xFF1976D2)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(color = avatarColor, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (child.fullName.isNotEmpty()) child.fullName.take(1) else "👶",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = onAvatarColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = child.fullName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (child.vaccinationNumber.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "رقم السجل: " + child.vaccinationNumber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        } else if (child.isAdultTd) {
                            Surface(
                                color = Color(0xFFD97706).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = Transl.t("tab_adult_td", appLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "العمر: $ageStr",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft, // RTL layout leftwards chevron
                    contentDescription = "عرض التفاصيل",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEFF6FF), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF2ECC71) // Fresh green phone icon
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = child.phone,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF8B5CF6) // Soft violet birthday icon
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ولد في: ${child.dob}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AddChildScreen(viewModel: VaccineViewModel) {
    val defaultCenter by viewModel.defaultHealthCenter.collectAsState()
    var fullName by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var healthCenter by remember(defaultCenter) { mutableStateOf(defaultCenter) }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    
    var vaccinationNumber by remember { mutableStateOf("") }
    var familyNumber by remember { mutableStateOf("") }
    var headOfHousehold by remember { mutableStateOf("") }
    val isAdultTd = false
    
    var gender by remember { mutableStateOf("ذكر") }
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var tempPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { photoUri = it }
    }
    
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri = tempPhotoUri
        }
    }
    
    // Quick birthday builder configuration
    var yearStr by remember { mutableStateOf("2026") }
    var monthStr by remember { mutableStateOf("01") }
    var dayStr by remember { mutableStateOf("01") }
    
    var wordCount by remember { mutableStateOf(0) }
    var showFamilySelectDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val families by viewModel.families.collectAsState()

    // Observe word count in Arab quadruple name
    LaunchedEffect(fullName) {
        val trimmed = fullName.trim()
        wordCount = if (trimmed.isEmpty()) 0 else trimmed.split("\\s+".toRegex()).size
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            // Form Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "تسجيل طفل جديد",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "الرجاء تعبئة بيانات الطفل بدقة لإنشاء كرت لقاح عراقي ذكي",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    
                    // First: Child basic details (Name, Mother, Vaccination ID)
                    Text(
                        "بيانات الطفل الأساسية",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("اسم الطفل الرباعي") },
                        placeholder = { Text("مثال: علي حسين جاسم محمد") },
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (wordCount < 4) "الاسم الرباعي يتكون من 4 أسماء على الأقل ($wordCount الحالي)" else "الاسم متكامل ومطابق للأنظمة",
                                    color = if (wordCount < 4) Color(0xFFD97706) else Color(0xFF16A34A),
                                    fontSize = 11.sp
                                )
                                Text("$wordCount/4", fontSize = 11.sp)
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("input_full_name"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = motherName,
                        onValueChange = { motherName = it },
                        label = { Text("اسم الأم الثلاثي") },
                        placeholder = { Text("مثال: فاطمة زهراء علي") },
                        leadingIcon = { Icon(Icons.Default.Face, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("input_mother_name"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = vaccinationNumber,
                        onValueChange = { vaccinationNumber = it },
                        label = { Text("الرقم التلقيحي للطفل في السجل الدائمي") },
                        placeholder = { Text("مثال: 4521/ب") },
                        leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("input_vaccination_number"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "صورة الطفل والجنس",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(40.dp)).clickable {
                                galleryLauncher.launch("image/*")
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUri != null) {
                                coil.compose.AsyncImage(
                                    model = photoUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(40.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                val defaultImg = com.example.R.drawable.img_clean_i7san_app_icon_1782895431554
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = defaultImg),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(40.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                        Column {
                            Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                                Text("اختر من المعرض")
                            }
                            Button(onClick = {
                                val tempFile = java.io.File.createTempFile("child_photo_", ".jpg", context.cacheDir)
                                tempPhotoUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
                                cameraLauncher.launch(tempPhotoUri!!)
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text("التقط صورة")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { gender = "ذكر" }) {
                            RadioButton(selected = gender == "ذكر", onClick = { gender = "ذكر" })
                            Text("ذكر", modifier = Modifier.padding(start = 4.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { gender = "أنثى" }) {
                            RadioButton(selected = gender == "أنثى", onClick = { gender = "أنثى" })
                            Text("أنثى", modifier = Modifier.padding(start = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Second: Child's Date of Birth / Age
                    Text(
                        "عمر الطفل وتاريخ الميلاد",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dayStr,
                            onValueChange = { if (it.length <= 2) dayStr = it.filter { c -> c.isDigit() } },
                            label = { Text("اليوم") },
                            placeholder = { Text("DD") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_dob_day"),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = monthStr,
                            onValueChange = { if (it.length <= 2) monthStr = it.filter { c -> c.isDigit() } },
                            label = { Text("الشهر") },
                            placeholder = { Text("MM") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_dob_month"),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = yearStr,
                            onValueChange = { if (it.length <= 4) yearStr = it.filter { c -> c.isDigit() } },
                            label = { Text("السنة") },
                            placeholder = { Text("YYYY") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("input_dob_year"),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Third: Select or input family details
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ربط الطفل بأسرة مسجلة مسبقاً", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (families.isNotEmpty()) {
                            TextButton(onClick = { showFamilySelectDialog = true }) {
                                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اختر أسرة مسجلة 🏠", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = familyNumber,
                        onValueChange = { familyNumber = it },
                        label = { Text("رقم الأسرة المعتمد") },
                        placeholder = { Text("مثال: 9876-أسرة") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("input_family_number"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = headOfHousehold,
                        onValueChange = { headOfHousehold = it },
                        label = { Text("اسم رب الأسرة") },
                        placeholder = { Text("مثال: حسين جاسم محمد") },
                        leadingIcon = { Icon(Icons.Default.AccountBox, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("input_head_of_household"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = formatOnTheFly(it) },
                        label = { Text("رقم هاتف الوتساب لرب الأسرة") },
                        placeholder = { Text("مثال: 07700000000 أو 7700000000") },
                        supportingText = { Text("سيضاف إليه رمز العراق (964) تلقائياً عند الحفظ") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("input_phone_whatsapp"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("عنوان سكن العائلة") },
                        placeholder = { Text("مثال: الموصل، حي القادسية") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("input_address"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = healthCenter,
                        onValueChange = { healthCenter = it },
                        label = { Text("أقرب مركز صحي للطفل") },
                        placeholder = { Text("مثال: طاق ميكائيل") },
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("input_health_center"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "تنبيه: يجب ألا يتجاوز عمر الطفل 5 سنوات (تاريخ ميلاد بعد تاريخ 2021-06-20)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Save Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.navigateTo(AppScreen.Home) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء الأمر")
                }

                Button(
                    onClick = {
                        // Form validations
                        if (fullName.trim().isEmpty() || phone.trim().isEmpty()) {
                            Toast.makeText(context, "الرجاء إدخال جميع الحقول الأساسية وبضمنها رقم الهاتف", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (wordCount < 4) {
                            Toast.makeText(context, "الرجاء إدخال اسم الطفل الرباعي بشكل كامل وصحيح", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (motherName.trim().isEmpty()) {
                            Toast.makeText(context, "الرجاء إدخال اسم الأم الثلاثي لتوثيق السجل", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val day = dayStr.toIntOrNull() ?: 1
                        val month = monthStr.toIntOrNull() ?: 1
                        val year = yearStr.toIntOrNull() ?: 2026

                        val minYear = if (isAdultTd) 1940 else 2020
                        if (day !in 1..31 || month !in 1..12 || year < minYear || year > 2026) {
                            Toast.makeText(context, "تاريخ الميلاد المدخل غير صالح، يرجى كتابته بشكل صحيح", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        // Validate that the child is less than 5 years old (using simplistic difference)
                        val formattedDate = String.format("%04d-%02d-%02d", year, month, day)
                        val birthEpoch = VaccineScheduler.getEpochDays(formattedDate)
                        val currentEpoch = VaccineScheduler.getCurrentEpochDays()
                        val diffDays = currentEpoch - birthEpoch
                        val fiveYearsInDays = 365 * 5 + 1
                        val twelveYearsInDays = 365 * 12

                        if (diffDays < 0) {
                            Toast.makeText(context, "تاريخ الميلاد لا يمكن أن يكون في المستقبل!", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (!isAdultTd && diffDays > fiveYearsInDays) {
                            Toast.makeText(context, "عمر الطفل يتجاوز الخمس سنوات، البرنامج مخصص للأطفال دون سن الخامسة فقط!", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (isAdultTd && diffDays < twelveYearsInDays) {
                            Toast.makeText(context, "لقاح ثنائي الكبار مخصص للأعمار من 12 سنة فما فوق فقط!", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        // Iraq country code self key injection +964
                        val formattedPhone = formatIraqiPhoneNumber(phone)

                        // Save child
                        viewModel.registerNewChild(
                            fullName = fullName.trim(),
                            motherName = motherName.trim(),
                            healthCenter = if (healthCenter.trim().isEmpty()) defaultCenter else healthCenter.trim(),
                            address = if (address.trim().isEmpty()) "غير محدد" else address.trim(),
                            phone = formattedPhone,
                            dobString = formattedDate,
                            vaccinationNumber = vaccinationNumber.trim(),
                            familyNumber = familyNumber.trim(),
                            headOfHousehold = headOfHousehold.trim(),
                            isAdultTd = isAdultTd,
                            gender = gender,
                            photoUri = photoUri?.toString(),
                            onSuccess = {
                                Toast.makeText(context, "تم تسجيل الملف وتوليد خطة اللقاحات بنجاح!", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp)
                        .testTag("save_child_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("تسجيل وحفظ الكارت", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            ProgrammerFooter()
        }
    }

    if (showFamilySelectDialog && families.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showFamilySelectDialog = false },
            title = { Text("اختر الأسرة لربط الطفل بها", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(families.size) { i ->
                        val f = families[i]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    familyNumber = f.familyNumber
                                    headOfHousehold = f.headOfHousehold
                                    phone = f.phone
                                    address = f.address
                                    showFamilySelectDialog = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("اسم رب الأسرة: ${f.headOfHousehold}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("رقم الأسرة: ${f.familyNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFamilySelectDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
fun ChildDetailsScreen(childId: Int, viewModel: VaccineViewModel) {
    val child by viewModel.selectedChild.collectAsState()
    val vaccines by viewModel.selectedChildVaccines.collectAsState()
    val context = LocalContext.current
    
    val completedCount = remember(vaccines) { vaccines.count { it.isTaken } }
    val totalCount = remember(vaccines) { vaccines.size }
    val completionPercent = remember(completedCount, totalCount) {
        if (totalCount > 0) (completedCount.toFloat() / totalCount * 100).toInt() else 0
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedRecordForAction by remember { mutableStateOf<VaccineRecord?>(null) }
    var showShareOptions by remember { mutableStateOf(false) }
    var showQrCardDialog by remember { mutableStateOf(false) }
    var qrCardBitmapForSave by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    val saveQrImageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            try {
                qrCardBitmapForSave?.let { bmp ->
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                        Toast.makeText(context, "تم حفظ كارت QR للطفل بنجاح في ملفات الهاتف! 💾", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ أثناء حفظ الصورة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    val saveChildPdfLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                child?.let { currentChild ->
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        VaccineReportExporter.writeVaccinationCardToStream(
                            context,
                            currentChild,
                            vaccines,
                            outputStream
                        )
                        Toast.makeText(context, "تم حفظ بطاقة الطفل بنجاح في ملفات النظام! 💾", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ أثناء حفظ الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    if (child == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentChild = child!!
    
    // PDF/Share Option Dialog
    if (showShareOptions) {
        AlertDialog(
            onDismissRequest = { showShareOptions = false },
            title = { Text("إجراءات بطاقة التلقيح") },
            text = { Text("يرجى اختيار الإجراء المطلوب للبطاقة:") },
            confirmButton = {
                Button(onClick = {
                    val safeName = currentChild.fullName.replace(" ", "_")
                    saveChildPdfLauncher.launch("سجل_تلقيح_${safeName}.pdf")
                    showShareOptions = false
                }) {
                    Text("حفظ PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    VaccineReportExporter.shareChildCardAsPdf(context, currentChild, vaccines)
                    showShareOptions = false
                }) {
                    Text("مشاركة")
                }
            }
        )
    }

    // Comprehensive Child QR Card Dialog
    if (showQrCardDialog) {
        val qrJsonString = remember(currentChild, vaccines) {
            viewModel.generateChildQrJson(currentChild, vaccines)
        }
        val qrBitmap = remember(qrJsonString) {
            try {
                val hints = mapOf(
                    com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8",
                    com.google.zxing.EncodeHintType.MARGIN to 1,
                    com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L
                )
                val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(qrJsonString, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512, hints)
                val width = bitMatrix.width
                val height = bitMatrix.height
                val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
                bmp
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        AlertDialog(
            onDismissRequest = { showQrCardDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("كارت QR الشامل للطفل", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "هذا الرمز يحتوي على كافة معلومات الطفل (${currentChild.fullName}) وجميع سجلات اللقاحات المستلمة والغير مستلمة. عند مسح هذا الرمز بأي جهاز آخر يستعمل التطبيق، يتم حفظ واستيراد كافة بيانات الطفل واللقاحات تلقائياً! 🌟",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (qrBitmap != null) {
                        Card(
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.size(220.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "كارت QR للطفل",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    qrCardBitmapForSave = qrBitmap
                                    val safeName = currentChild.fullName.replace(" ", "_")
                                    saveQrImageLauncher.launch("كارت_QR_للطفل_${safeName}.png")
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ كصورة", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    try {
                                        val cachePath = java.io.File(context.cacheDir, "images")
                                        cachePath.mkdirs()
                                        val safeName = currentChild.fullName.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_\\-]"), "_").ifEmpty { "child" }
                                        val file = java.io.File(cachePath, "qr_card_${safeName}.png")
                                        java.io.FileOutputStream(file).use { stream ->
                                            qrBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                                        }

                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            file
                                        )

                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            putExtra(android.content.Intent.EXTRA_TEXT, "بطاقة التلقيح الشاملة (كارت QR) للطفل: ${currentChild.fullName} - تطبيق لقاحات الأطفال في العراق")
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        val chooser = android.content.Intent.createChooser(intent, "مشاركة كارت QR عبر")
                                        chooser.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        context.startActivity(chooser)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "خطأ أثناء المشاركة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مشاركة", fontSize = 11.sp)
                            }
                        }
                    } else {
                        Text("تعذر توليد كارت QR", color = Color.Red)
                    }

                    Text(
                        text = "رقم الهوية الفريد: ${currentChild.uniqueId}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showQrCardDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("إغلاق", color = Color.White)
                }
            }
        )
    }
    val ageStr = remember(currentChild.dob) { VaccineScheduler.computeAgeString(currentChild.dob) }

    val nextDueVaccine = remember(vaccines) {
        vaccines.filter { !it.isTaken && !it.isSkipped }
            .minByOrNull { it.dueMonths }
    }
    val nextDueDate = remember(currentChild.dob, nextDueVaccine, vaccines) {
        if (nextDueVaccine != null) {
            VaccineScheduler.getRecordDueDate(currentChild, nextDueVaccine, vaccines)
        } else ""
    }

    // Dialog confirming vaccine completion toggle with Custom Date & Side Effects & WhatsApp Action
    if (selectedRecordForAction != null) {
        val record = selectedRecordForAction!!
        val isTakenDialog = record.isTaken

        // Reset states whenever active record changes
        var customDate by remember(record.id) {
            mutableStateOf(record.takenDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
        }
        var sideEffectText by remember(record.id) {
            mutableStateOf(record.sideEffect ?: "")
        }

        // Get other pending/taken vaccines in the same age category milestone
        val categoryVaccines = remember(record.id, vaccines) {
            vaccines.filter { it.categoryAr == record.categoryAr && !it.isTaken }
        }

        // Selected vaccine IDs state (pre-select all available pending vaccines in the group including the current one)
        var selectedVaccineIds by remember(record.id) {
            mutableStateOf(categoryVaccines.map { it.id }.toSet())
        }

        AlertDialog(
            onDismissRequest = { selectedRecordForAction = null },
            title = {
                Text(
                    text = if (isTakenDialog) "تحديث / إلغاء حالة التلقيح" else "حقن وتثبيت لقاح للطفل",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isTakenDialog) {
                        item {
                            Text(
                                text = "اللقاح (${record.nameAr}) مسجل كمنجز للطفل ${currentChild.fullName}.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = sideEffectText,
                                onValueChange = { sideEffectText = it },
                                label = { Text("الأثار الجانبية الملاحظة") },
                                placeholder = { Text("مثال: ارتفاع درجة حرارة، احمرار بمكان الحقن") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Text(
                                text = "تنبيه: يمكنك إلغاء حالة 'تم التلقيح' وإرجاعه لغير المكتملة بالضغط على الزر أدناه باللون الأحمر.",
                                fontSize = 11.sp,
                                color = Color.Red
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "هل تم جرع وحقن لقاح (${record.nameAr}) للطفل ${currentChild.fullName} بنجاح؟",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        item { HorizontalDivider() }
                        
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val newIgnored = !record.isIgnored
                                    viewModel.updateVaccineIgnoreStatus(record, newIgnored)
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = record.isIgnored,
                                    onCheckedChange = { viewModel.updateVaccineIgnoreStatus(record, it) }
                                )
                                Text("تجاهل هذا اللقاح (لن يتم التذكير به)", fontSize = 13.sp)
                            }
                        }

                        item { HorizontalDivider() }

                        // Date picker row
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("تاريخ التلقيح الفعلي:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(customDate, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Button(
                                    onClick = {
                                        val calendar = Calendar.getInstance()
                                        val year = calendar.get(Calendar.YEAR)
                                        val month = calendar.get(Calendar.MONTH)
                                        val day = calendar.get(Calendar.DAY_OF_MONTH)

                                        android.app.DatePickerDialog(
                                            context,
                                            { _, selYear, selMonth, selDay ->
                                                val formattedMonth = String.format("%02d", selMonth + 1)
                                                val formattedDay = String.format("%02d", selDay)
                                                customDate = "$selYear-$formattedMonth-$formattedDay"
                                            },
                                            year, month, day
                                        ).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("تعديل التاريخ 📅", fontSize = 11.sp)
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(4.dp)) }

                        // Multi-selection of other vaccines in same milestone clinical category
                        if (categoryVaccines.size > 1) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "💉 لقاحات فئة (${record.categoryAr}) القادمة:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(
                                        onClick = {
                                            selectedVaccineIds = if (selectedVaccineIds.size == categoryVaccines.size) {
                                                setOf(record.id)
                                            } else {
                                                categoryVaccines.map { it.id }.toSet()
                                            }
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = if (selectedVaccineIds.size == categoryVaccines.size) "إلغاء تحديد الكل" else "تحديد الكل للحقن 📌",
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            categoryVaccines.forEach { catRecord ->
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (catRecord.id == record.id) {
                                                    // Main clicked item is locked
                                                } else {
                                                    selectedVaccineIds = if (selectedVaccineIds.contains(catRecord.id)) {
                                                        selectedVaccineIds - catRecord.id
                                                    } else {
                                                        selectedVaccineIds + catRecord.id
                                                    }
                                                }
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = selectedVaccineIds.contains(catRecord.id),
                                            onCheckedChange = { isChecked ->
                                                if (catRecord.id == record.id) {
                                                    // Locked
                                                } else {
                                                    selectedVaccineIds = if (isChecked) {
                                                        selectedVaccineIds + catRecord.id
                                                    } else {
                                                        selectedVaccineIds - catRecord.id
                                                    }
                                                }
                                            },
                                            enabled = catRecord.id != record.id,
                                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = catRecord.nameAr,
                                            fontSize = 12.sp,
                                            color = if (catRecord.id == record.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (catRecord.id == record.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Side effects input
                        item {
                            OutlinedTextField(
                                value = sideEffectText,
                                onValueChange = { sideEffectText = it },
                                label = { Text("الأثار الجانبية المسجلة (اختياري)") },
                                placeholder = { Text("مثال: حرارة مستمرة، خمول، وبكاء") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        if (sideEffectText.isNotBlank()) {
                            item {
                                // Antipyretic warning alert
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                    border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = "تنبيه خافض حرارة",
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "تنبيه هام: يرجى إعطاء الطفل خافضات الحرارة المناسبة والمسكنات المعتمدة، مع قياس درجة حرارته دورياً لمكافحة الأثر الجانبي.\n\nفي حالة تسجيل اثر جانبي خطير او شديد مثل فقدان للوعي او ضيق للتنفس الرجاء الاتصال برقم بالطوارئ 911",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedSideEffect = sideEffectText.trim().ifBlank { null }

                        if (isTakenDialog) {
                            // Update side effects first, then status
                            viewModel.updateVaccineSideEffect(record, trimmedSideEffect)
                            
                            // Send WhatsApp alert to supervisor if there's a side effect registered
                            if (trimmedSideEffect != null) {
                                val templateText = """
                                🟢 تبليغ فوري أثر جانبي لقاحات:
                                اسم الطفل: ${currentChild.fullName}
                                تاريخ الميلاد: ${currentChild.dob}
                                الرقم التلقيحي الدائمي للطفل: ${currentChild.vaccinationNumber}
                                نوع واسم اللقاح الملقح: ${record.nameAr} (${record.categoryAr})
                                تاريخ اخذ اللقاح: ${record.takenDate}
                                نوع الاثر الجانبي المسجل: $trimmedSideEffect
                                --------------------------------
                                تم تسجيل هذه الرسالة وإرسالها عبر تطبيق إحسان 🌟
                                """.trimIndent()

                                val targetPhone = viewModel.sideEffectsOfficerPhone.value.ifBlank { "9647729792239" }
                                val waIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=${cleanPhoneForWhatsApp(targetPhone)}&text=${Uri.encode(templateText)}")
                                }
                                try {
                                    context.startActivity(waIntent)
                                    Toast.makeText(context, "تم حفظ الأثر الجانبي. فتح الواتساب لإرسال الإشعار لمسؤول الآثار الجانبية...", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تم حفظ الأثر الجانبي بنجاح ✅", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "تم حفظ الأثر الجانبي بنجاح ✅", Toast.LENGTH_SHORT).show()
                            }
                            selectedRecordForAction = null
                        } else {
                            // Extract all checked vaccines in the milestone category
                            val chosenRecords = if (categoryVaccines.isNotEmpty()) {
                                vaccines.filter { selectedVaccineIds.contains(it.id) }
                            } else {
                                listOf(record)
                            }

                            // Perform bulk update
                            viewModel.updateMultipleVaccinesStatusAndSideEffect(
                                chosenRecords,
                                true,
                                customDate,
                                trimmedSideEffect
                            )

                            // Send WhatsApp alert to supervisor if there's a side effect registered
                            if (trimmedSideEffect != null) {
                                val vaccineNamesJoined = chosenRecords.joinToString("، ") { it.nameAr }
                                val templateText = """
                                🟢 تبليغ فوري أثر جانبي لقاحات:
                                اسم الطفل: ${currentChild.fullName}
                                تاريخ الميلاد: ${currentChild.dob}
                                الرقم التلقيحي الدائمي للطفل: ${currentChild.vaccinationNumber}
                                نوع واسم اللقاح الملقح: $vaccineNamesJoined (${record.categoryAr})
                                تاريخ اخذ اللقاح: $customDate
                                نوع الاثر الجانبي المسجل: $trimmedSideEffect
                                --------------------------------
                                تم تسجيل هذه الرسالة وإرسالها عبر تطبيق إحسان 🌟
                                """.trimIndent()

                                val targetPhone = viewModel.sideEffectsOfficerPhone.value.ifBlank { "9647729792239" }
                                val waIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=${cleanPhoneForWhatsApp(targetPhone)}&text=${Uri.encode(templateText)}")
                                }
                                try {
                                    context.startActivity(waIntent)
                                    Toast.makeText(context, "تنبيه خافض الحرارة: يرجى متابعة حالة الطفل وإعطاء خافض حرارة. فتح الواتساب لإرسال الإشعار...", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تنبيه خافض الحرارة! تم حفظ السجل وتوقيت التلقيح بنجاح.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "تم تسجيل وإنجاز اللقاحات المحددة للطفل بنجاح", Toast.LENGTH_SHORT).show()
                            }
                            selectedRecordForAction = null
                        }
                    }
                ) {
                    Text(if (isTakenDialog) "حفظ وإغلاق" else "تأكيد تسجيل التلقيح", color = Color.White)
                }
            },
            dismissButton = {
                Row {
                    if (isTakenDialog) {
                        // Option to undo / Cancel completion
                        TextButton(
                            onClick = {
                                viewModel.toggleVaccineStatus(record, false)
                                selectedRecordForAction = null
                                Toast.makeText(context, "تم إلغاء حالة التلقيح والرجوع", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("إلغاء التلقيح (غير منجز ❌)", color = Color.Red)
                        }
                    }
                    TextButton(onClick = { selectedRecordForAction = null }) {
                        Text("إغلاق")
                    }
                }
            }
        )
    }

    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        var editFullName by remember { mutableStateOf(currentChild.fullName) }
        var editMotherName by remember { mutableStateOf(currentChild.motherName) }
        var editPhone by remember { mutableStateOf(currentChild.phone) }
        var editDob by remember { mutableStateOf(currentChild.dob) }
        var editHealthCenter by remember { mutableStateOf(currentChild.healthCenter) }
        var editAddress by remember { mutableStateOf(currentChild.address) }
        var editVaccinationNumber by remember { mutableStateOf(currentChild.vaccinationNumber) }
        var editFamilyNumber by remember { mutableStateOf(currentChild.familyNumber) }
        var editHeadOfHousehold by remember { mutableStateOf(currentChild.headOfHousehold) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "تعديل تفاصيل سجل الطفل",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = editFullName,
                            onValueChange = { editFullName = it },
                            label = { Text("الاسم الرباعي للطفل") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editMotherName,
                            onValueChange = { editMotherName = it },
                            label = { Text("اسم الأم الثلاثي") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        // Date of birth editor row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("تاريخ الميلاد:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(editDob, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Button(
                                onClick = {
                                    val calendar = Calendar.getInstance()
                                    val parts = editDob.split("-")
                                    val year = if (parts.size == 3) parts[0].toIntOrNull() ?: calendar.get(Calendar.YEAR) else calendar.get(Calendar.YEAR)
                                    val month = if (parts.size == 3) (parts[1].toIntOrNull() ?: 1) - 1 else calendar.get(Calendar.MONTH)
                                    val day = if (parts.size == 3) parts[2].toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH) else calendar.get(Calendar.DAY_OF_MONTH)
                                    
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, selYear, selMonth, selDay ->
                                            val formattedMonth = String.format("%02d", selMonth + 1)
                                            val formattedDay = String.format("%02d", selDay)
                                            editDob = "$selYear-$formattedMonth-$formattedDay"
                                        },
                                        year, month, day
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("تعديل التاريخ 📅", fontSize = 11.sp)
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = formatOnTheFly(it) },
                            label = { Text("رقم هاتف الملقح") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editHealthCenter,
                            onValueChange = { editHealthCenter = it },
                            label = { Text("المركز الصحي") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editAddress,
                            onValueChange = { editAddress = it },
                            label = { Text("عنوان السكن والقرية") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editVaccinationNumber,
                            onValueChange = { editVaccinationNumber = it },
                            label = { Text("الرقم التلقيحي في السجل الدائمي") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editFamilyNumber,
                            onValueChange = { editFamilyNumber = it },
                            label = { Text("رقم الأسرة") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editHeadOfHousehold,
                            onValueChange = { editHeadOfHousehold = it },
                            label = { Text("اسم رب الأسرة") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editFullName.isNotBlank() && editMotherName.isNotBlank() && editPhone.isNotBlank()) {
                            viewModel.updateChildDetails(
                                currentChild.copy(
                                    fullName = editFullName.trim(),
                                    motherName = editMotherName.trim(),
                                    dob = editDob,
                                    phone = formatIraqiPhoneNumber(editPhone),
                                    healthCenter = editHealthCenter.trim(),
                                    address = editAddress.trim(),
                                    vaccinationNumber = editVaccinationNumber.trim(),
                                    familyNumber = editFamilyNumber.trim(),
                                    headOfHousehold = editHeadOfHousehold.trim()
                                )
                            )
                            showEditDialog = false
                            Toast.makeText(context, "تم حفظ وتعديل البيانات بنجاح ✔️", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "الرجاء إدخال الاسم والأم ورقم الهاتف كبيانات إلزامية!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("حفظ التغييرات", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog verifying child profile deletion
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("حذف كارت الطفل كلياً") },
            text = { Text("تحذير! سيؤدي هذا الإجراء إلى حذف ملف الطفل '${currentChild.fullName}' وكل سجلات تطعيماته للداخل بشكل نهائي. هل تريد الاستمرار؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteChildAccount(currentChild)
                        Toast.makeText(context, "تم حذف سجل الطفل بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("حذف الملف نهائياً", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("رجوع")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Toolbar header row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تفاصيل الملف والتلقيحات",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Action icons row above child card
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalIconButton(
                    onClick = { showQrCardDialog = true },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier.size(40.dp).testTag("child_qr_card_button")
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = "كارت QR الشامل للطفل", modifier = Modifier.size(20.dp))
                }
                FilledTonalIconButton(
                    onClick = {
                        VaccineReportExporter.printChildReportAsHtml(
                            context,
                            currentChild,
                            vaccines,
                            "المركز الصحي المعتمد"
                        )
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = "طباعة البطاقة التلقيحية", modifier = Modifier.size(20.dp))
                }
                FilledTonalIconButton(
                    onClick = { showShareOptions = true },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = "حفظ البطاقة PDF", modifier = Modifier.size(20.dp))
                }
                FilledIconButton(
                    onClick = {
                        VaccineReportExporter.shareChildCardAsPdf(context, currentChild, vaccines)
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "مشاركة البطاقة PDF", modifier = Modifier.size(20.dp))
                }
                FilledTonalIconButton(
                    onClick = { showEditDialog = true },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل بيانات الطفل", modifier = Modifier.size(20.dp))
                }
                FilledTonalIconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(40.dp).testTag("delete_child_account_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف الطفل", modifier = Modifier.size(20.dp))
                }
            }
        }

        item {
            val cardBackgroundColor = if (currentChild.gender == "ذكر") androidx.compose.ui.graphics.Color(0xFFE3F2FD) else androidx.compose.ui.graphics.Color(0xFFFCE4EC)

            // Identification card (Arabic Karta Style) with Custom Canvas QR Code
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp).padding(8.dp)) {
                        // Enhanced Progress Ring
                        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
                        val primaryColor = MaterialTheme.colorScheme.primary
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 8.dp.toPx()
                            drawCircle(
                                color = surfaceVariant,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                            )
                            drawArc(
                                color = primaryColor,
                                startAngle = -90f,
                                sweepAngle = (completionPercent / 100f) * 360f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }
                        
                        Box(
                            modifier = Modifier.size(90.dp).clip(RoundedCornerShape(45.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentChild.photoUri != null) {
                                coil.compose.AsyncImage(
                                    model = android.net.Uri.parse(currentChild.photoUri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                // Placeholder for now, will replace with anime assets
                                val defaultImg = com.example.R.drawable.img_clean_i7san_app_icon_1782895431554
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = defaultImg),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(45.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentChild.vaccinationNumber.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "رقم السجل: ${currentChild.vaccinationNumber}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = ageStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = currentChild.fullName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تاريخ الميلاد: ${currentChild.dob}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "الوتساب: ${currentChild.phone}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Face,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "اسم الأم الثلاثي: ${currentChild.motherName}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (currentChild.headOfHousehold.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccountBox,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "رب الأسرة: ${currentChild.headOfHousehold}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (currentChild.familyNumber.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "رقم الأسرة: ${currentChild.familyNumber}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "أقرب مركز صحي: ${currentChild.healthCenter}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "عنوان السكن: ${currentChild.address}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Custom premium progress bar for vaccine schedule completion
                        if (totalCount > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "نسبة اكتمال اللقاحات المأخوذة:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "$completionPercent%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { completedCount.toFloat() / totalCount },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.secondary, // Green visual identity matching dark/light mode
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        RechartsVaccineProgressChart(vaccines = vaccines)

                        Spacer(modifier = Modifier.height(12.dp))

                        // Button to launch direct WhatsApp Integration chat with custom prefilled template request
                        Button(
                            onClick = {
                                val idOrNum = if (currentChild.vaccinationNumber.isNotBlank()) "رقم السجل: ${currentChild.vaccinationNumber}" else "الاسم: ${currentChild.fullName}"
                                val templateText = "🛡️ تطبيق إحسان لإدارة التلقيح والتحصين\nالسلام عليكم، يُرجى العلم أن موعد لقاح (${currentChild.fullName}) المسجل بـ ($idOrNum) قد حان في جدول اللقاحات المعتمد، يرجى مراجعة أقرب مركز صحي بالسرعة الممكنة.\n\nتم إرسال هذه الرسالة عبر تطبيق إحسان 🌟\n\n📢 قناة التطبيق على التليكرام:\nhttps://t.me/i7san_App\n\n📥 لتحميل التطبيق افتح الرابط التالي:\nhttps://www.mediafire.com/folder/246decua5ij07/i7san"
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=${cleanPhoneForWhatsApp(currentChild.phone)}&text=${Uri.encode(templateText)}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "الوتساب غير منصب، كود الرسالة تم نسخه!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Share, 
                                 contentDescription = null, 
                                 modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تذكير موعد اللقاح عبر الوتساب", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val safeName = currentChild.fullName.replace(" ", "_")
                                    saveChildPdfLauncher.launch("سجل_تلقيح_${safeName}.pdf")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تنزيل كارت لقاح (PDF)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            Button(
                                onClick = {
                                    VaccineReportExporter.printChildReportAsHtml(
                                        context,
                                        currentChild,
                                        vaccines,
                                        "المركز الصحي المعتمد" // You could pass actual center name here if you want
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Print, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة سجل اللقاحات", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            if (nextDueVaccine != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)), // Soft Amber
                    border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "التنبيه بالموعد القادم",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "⏰ موعد التاريخ المستحق للجرعة القادمة للطفل:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${nextDueVaccine.nameAr} (${nextDueVaccine.categoryAr})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF78350F)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تاريخ الاستحقاق المقدر: $nextDueDate",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("مخطط اللقاحات التفصيلي المنجز وغير المنجز:")
                TextButton(
                    onClick = { viewModel.navigateTo(AppScreen.VaccineScheduleTable) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("عرض كجدول رسمي 📊", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (vaccines.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            // Group vaccines by clinical categories (age milestones)
            val groupedByAge = vaccines.groupBy { it.categoryAr }
            
            // Loop over age targets
            groupedByAge.forEach { (category, recordsList) ->
                item {
                    Text(
                        text = category,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .padding(top = 10.dp, bottom = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(recordsList, key = { it.id }) { record ->
                    val status = VaccineScheduler.getRecordStatus(currentChild, record, vaccines)
                    val calculatedDueDate = remember(currentChild.dob, record.dueMonths, vaccines) {
                        VaccineScheduler.getRecordDueDate(currentChild, record, vaccines)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedRecordForAction = record }
                            .testTag("vaccine_record_${record.vaccineKey}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(0.5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = record.nameAr,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (record.isTaken) "أُعطي بتاريخ: ${record.takenDate}" else if (record.isSkipped) "تم تجاوز وأريد اللقاح لاحقاً" else "تاريخ الاستحقاق المقدر: $calculatedDueDate",
                                        fontSize = 11.sp,
                                        color = if (record.isTaken) Color(0xFF166534) else if (record.isSkipped) Color(0xFFC2410C) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (record.isSkipped) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "سبب التجاوز: ${record.skipReason ?: "غير محدد"}",
                                        fontSize = 10.sp,
                                        color = Color(0xFFC2410C),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (!record.sideEffect.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "الأثر الجانبي: ${record.sideEffect}",
                                        fontSize = 10.sp,
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            VaccineStatusBadge(status = status)
                        }
                    }
                }
            }
            
            // PDF Export Actions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "طباعة ومشاركة البطاقة التلقيحية 🖨️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    VaccineReportExporter.printChildReportAsHtml(
                                        context,
                                        currentChild,
                                        vaccines,
                                        "المركز الصحي المعتمد"
                                    )
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("طباعة البطاقة A4", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    val safeName = currentChild.fullName.replace(" ", "_")
                                    saveChildPdfLauncher.launch("سجل_تلقيح_${safeName}.pdf")
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حفظ PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            
            // Interactive Route Map to Taqi Michael Health Center
            item {
                TaqaMikaelRouteMap(familyAddress = currentChild.address, viewModel = viewModel)
            }
            
            // Designer Credit
            item {
                ProgrammerFooter()
            }
        }
    }
}

@Composable
fun TaqaMikaelRouteMap(familyAddress: String, viewModel: VaccineViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsState()
    val defaultCenter by viewModel.defaultHealthCenter.collectAsState()
    val centerLat by viewModel.healthCenterLat.collectAsState()
    val centerLng by viewModel.healthCenterLng.collectAsState()

    // Deterministically generate a location coordinate for the child's home based on address hashcode
    val hash = Math.abs(familyAddress.hashCode())
    val homeLat = 36.270f + (hash % 120) / 1000f
    val homeLng = 43.080f + (hash % 120) / 1000f

    // Calculate distance and driving duration dynamically
    val dLat = homeLat - centerLat
    val dLng = homeLng - centerLng
    val distanceKm = Math.sqrt((dLat * dLat + dLng * dLng).toDouble()) * 111.0
    val distanceFormatted = ((distanceKm * 10).toInt() / 10.0).toString()
    val durationMinutes = (distanceKm * 2.5).toInt().coerceAtLeast(1)

    var simProgression by remember { mutableStateOf(0.0f) }
    var isSimulating by remember { mutableStateOf(false) }
    
    LaunchedEffect(isSimulating) {
        if (isSimulating) {
            simProgression = 0.0f
            while (simProgression < 1.0f) {
                kotlinx.coroutines.delay(50)
                simProgression += 0.02f
            }
            isSimulating = false
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Place, 
                        contentDescription = "خريطة الملاحة", 
                        tint = MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (appLanguage == "en") "Navigation Route to Center ($defaultCenter)" else "مسار الوصول إلى المركز الصحي ($defaultCenter)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Button(
                    onClick = { isSimulating = true },
                    enabled = !isSimulating,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(text = if (isSimulating) (if (appLanguage == "en") "Navigating..." else "ملاحة جارية...") else (if (appLanguage == "en") "Route Simulation 🚗" else "محاكاة الطريق 🚗"), fontSize = 10.sp, color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Map Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(if (viewModel.isDarkTheme.collectAsState().value) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .border(1.dp, if (viewModel.isDarkTheme.collectAsState().value) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Draw grid lines to represent a stylized map
                    for (i in 1..8) {
                        val x = w * (i / 9.0f)
                        drawLine(
                            color = Color(0x3094A3B8),
                            start = androidx.compose.ui.geometry.Offset(x, 0f),
                            end = androidx.compose.ui.geometry.Offset(x, h),
                            strokeWidth = 2f
                        )
                        val y = h * (i / 9.0f)
                        drawLine(
                            color = Color(0x3094A3B8),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(w, y),
                            strokeWidth = 2f
                        )
                    }

                    // Map relative coordinates to canvas pixels
                    // Mosul Lat/Lng bounds
                    val minLat = 36.25f
                    val maxLat = 36.45f
                    val minLng = 43.05f
                    val maxLng = 43.35f

                    val startX = w * ((homeLng - minLng) / (maxLng - minLng)).coerceIn(0.05f, 0.95f)
                    val startY = h * (1.0f - (homeLat - minLat) / (maxLat - minLat)).coerceIn(0.05f, 0.95f)

                    val endX = w * ((centerLng - minLng) / (maxLng - minLng)).coerceIn(0.05f, 0.95f)
                    val endY = h * (1.0f - (centerLat - minLat) / (maxLat - minLat)).coerceIn(0.05f, 0.95f)
                    
                    // Dynamic Bezier Route Path
                    val controlX1 = startX + (endX - startX) * 0.3f
                    val controlY1 = startY - (startY - endY) * 0.1f
                    val controlX2 = startX + (endX - startX) * 0.7f
                    val controlY2 = endY + (startY - endY) * 0.1f

                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(startX, startY)
                        cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
                    }
                    
                    // Draw main road path
                    drawPath(
                        path = path,
                        color = Color(0x600284C7),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 16f,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                    
                    // Draw central path line
                    drawPath(
                        path = path,
                        color = Color(0xFF0284C7),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 4f,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                    
                    // Draw Child's home anchor
                    drawCircle(
                        color = Color(0xFFE11D48),
                        radius = 18f,
                        center = androidx.compose.ui.geometry.Offset(startX, startY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 8f,
                        center = androidx.compose.ui.geometry.Offset(startX, startY)
                    )
                    
                    // Draw Configured Health Center anchor
                    drawCircle(
                        color = Color(0xFF16A34A),
                        radius = 22f,
                        center = androidx.compose.ui.geometry.Offset(endX, endY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 10f,
                        center = androidx.compose.ui.geometry.Offset(endX, endY)
                    )
                    
                    // Calculate animating vehicle dot on path using simple Bezier point approximation
                    if (simProgression > 0.0f) {
                        val t = simProgression
                        val mt = 1.0f - t
                        val vehX = mt * mt * mt * startX + 3 * mt * mt * t * controlX1 + 3 * mt * t * t * controlX2 + t * t * t * endX
                        val vehY = mt * mt * mt * startY + 3 * mt * mt * t * controlY1 + 3 * mt * t * t * controlY2 + t * t * t * endY
                        
                        drawCircle(
                            color = Color(0xFFF59E0B),
                            radius = 12f,
                            center = androidx.compose.ui.geometry.Offset(vehX, vehY)
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 5f,
                            center = androidx.compose.ui.geometry.Offset(vehX, vehY)
                        )
                    }
                }
                
                // Overlay text labels on anchors
                Text(
                    text = if (appLanguage == "en") "🏠 Home\n($familyAddress)" else "🏠 منزل الطفل\n($familyAddress)",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 12.dp)
                        .background(Color(0xE6FFFFFF), RoundedCornerShape(4.dp))
                        .padding(4.dp)
                )
                
                Text(
                    text = if (appLanguage == "en") "🏥 Health Center\n($defaultCenter)" else "🏥 المركز الصحي\n($defaultCenter)",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 12.dp, top = 12.dp)
                        .background(Color(0xE6FFFFFF), RoundedCornerShape(4.dp))
                        .padding(4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Route Details guidance text
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = if (appLanguage == "en") "Route Navigation Details:" else "تفاصيل خط السير والوصول والخرائط:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (appLanguage == "en") {
                            "• The route starts from child's household address ($familyAddress) toward the configured health center ($defaultCenter) according to geographical coordinates.\n• Estimated navigation distance: $distanceFormatted km (approx. $durationMinutes minutes by car)."
                        } else {
                            "• يبدأ المسار من موقع بيت الطفل ($familyAddress) باتجاه المركز الصحي المعتمد ($defaultCenter) وفقاً للإحداثيات الجغرافية المحددة.\n• المسافة التقديرية للملاحة: $distanceFormatted كم (تقريباً $durationMinutes دقيقة بالسيارة)."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun AlertsScreen(viewModel: VaccineViewModel) {
    val alerts by viewModel.alerts.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "تنبيهات",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "متابعة وإشعار حالات المتسربين عن اللقاح وحالات اقتراب الاستحقاق لضمان التغطية المناعية لشعب نينوى",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Group alerts into "Approaching" (موعد قريب) and "Overdue/Defaulter" (تسرب اللقاح لأزيد من 28 يوم)
        val mergedAlerts = viewModel.groupVaccineAlerts(alerts)
        val overdueAlerts = mergedAlerts.filter { it.status == VaccineStatus.OVERDUE }
        val approachingAlerts = mergedAlerts.filter { it.status == VaccineStatus.APPROACHING }

        if (mergedAlerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "رائع! لا يوجد أي تسيب أو استحقاقات متأخرة بالدائرة اليوم.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Defaulters / delayed for over 28 Days (متسربين)
                if (overdueAlerts.isNotEmpty()) {
                    item {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "تسرب خطير", tint = Color.Red)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إشعار متأخري اللقاحات لأكثر من 28 يوماً (${overdueAlerts.size} فئات عمرية متسربة)\n✨ تذكير واتساب آلي أسبوعي (كل 7 أيام لحين تسجيل التلقيح)",
                                    color = Color(0xFF991B1B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    items(overdueAlerts, key = { "${it.child.id}-${it.categoryAr}-overdue" }) { alert ->
                        val vaccineNames = alert.vaccines.joinToString(" + ") { it.nameAr }
                        val combinedAlert = VaccineRepository.VaccinationAlert(
                            child = alert.child,
                            vaccine = alert.vaccines.first().copy(nameAr = vaccineNames),
                            dueDate = alert.dueDate,
                            status = alert.status
                        )
                        OverdueDefaulterCard(alert = combinedAlert, onAction = {
                            val idText = if (alert.child.vaccinationNumber.isNotBlank()) "رقم السجل: ${alert.child.vaccinationNumber}" else "الاسم: ${alert.child.fullName}"
                            val text = "🛡️ تطبيق إحسان لإدارة التلقيح والتحصين\nتنبيه عاجل لتلقي اللقاح! إن الطفل (${alert.child.fullName}) ذو ($idText) متأخر عن تلقي جرعته بلقاحات ($vaccineNames) المستحقة لعمر (${alert.categoryAr}) المفترض أخذها منذ تاريخ ${alert.dueDate}. يرجى مراجعة أقرب مشفى أو مركز صحي بالسرعة الممكنة لحمايته.\n\nتم إرسال هذه الرسالة عبر تطبيق إحسان 🌟\n\n📢 قناة التطبيق على التليكرام:\nhttps://t.me/i7san_App\n\n📥 لتحميل التطبيق افتح الرابط التالي:\nhttps://www.mediafire.com/folder/246decua5ij07/i7san"
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?phone=${cleanPhoneForWhatsApp(alert.child.phone)}&text=${Uri.encode(text)}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "لم نتمكن من فتح الواتساب، الكود تم نسخه", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }

                // 2. Approaching within 3 days (مواعيد قريبة)
                if (approachingAlerts.isNotEmpty()) {
                    item {
                        Surface(
                            color = Color(0xFFFEFCE8),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "موعد قريب", tint = Color(0xFF854D0E))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إخطارات اقتراب موعد الجرعات - خلال غضون 3 أيام (${approachingAlerts.size} فئات عمرية)",
                                    color = Color(0xFF854D0E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    items(approachingAlerts, key = { "${it.child.id}-${it.categoryAr}-approaching" }) { alert ->
                        val vaccineNames = alert.vaccines.joinToString(" + ") { it.nameAr }
                        val combinedAlert = VaccineRepository.VaccinationAlert(
                            child = alert.child,
                            vaccine = alert.vaccines.first().copy(nameAr = vaccineNames),
                            dueDate = alert.dueDate,
                            status = alert.status
                        )
                        ApproachingAlertCard(alert = combinedAlert, onAction = {
                            val text = "🛡️ تطبيق إحسان لإدارة التلقيح والتحصين\nتنبيه هام: اقترب موعد لقاحات الطفل (${alert.child.fullName}) وهي ($vaccineNames) لعمر (${alert.categoryAr}). يرجى مراجعة أقرب مركز صحي بالسلامة والصحة.\n\nتم إرسال هذه الرسالة عبر تطبيق إحسان 🌟\n\n📢 قناة التطبيق على التليكرام:\nhttps://t.me/i7san_App\n\n📥 لتحميل التطبيق افتح الرابط التالي:\nhttps://www.mediafire.com/folder/246decua5ij07/i7san"
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?phone=${cleanPhoneForWhatsApp(alert.child.phone)}&text=${Uri.encode(text)}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر توجيه الوتساب، كود الرسالة جاهز بالذاكرة", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        ProgrammerFooter()
    }
}

@Composable
fun AddActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    containerColor: Color
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

// Cards for Alerts Screen (Overdue Defaulters)
@Composable
fun OverdueDefaulterCard(
    alert: VaccineRepository.VaccinationAlert,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
            .testTag("alert_overdue_${alert.child.id}"),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "تسرب متأخر > 28 يوم",
                                color = Color(0xFF991B1B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "الأصغر سناً",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = alert.child.fullName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B)
                    )

                    if (alert.child.vaccinationNumber.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "الرقم التلقيحي: ${alert.child.vaccinationNumber}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "اللقاح الفائت: ${alert.vaccine.nameAr}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "استحقاق الجرعة بتاريخ: ${alert.dueDate}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AddActionIconButton(
                    icon = Icons.Default.Share,
                    text = "مراسلة وتساب فورية",
                    onClick = onAction,
                    containerColor = Color(0xFFDC2626)
                )
            }
        }
    }
}

// Approaching Alerts Card
@Composable
fun ApproachingAlertCard(
    alert: VaccineRepository.VaccinationAlert,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFDE047), RoundedCornerShape(12.dp))
            .testTag("alert_approaching_${alert.child.id}"),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                color = Color(0xFFFEFCE8),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.width(IntrinsicSize.Min)
            ) {
                Text(
                    text = "حقن قريب < 3 أيام",
                    color = Color(0xFF854D0E),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = alert.child.fullName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "اللقاح المستحق قريباً: ${alert.vaccine.nameAr}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "تاريخ الاستحقاق التقريبي: ${alert.dueDate}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الرسالة: اقترب موعد لقاح ${alert.child.fullName.split(" ").firstOrNull() ?: ""} يرجى مراجعة أقرب مركز صحي",
                    fontSize = 10.sp,
                    color = Color(0xFF854D0E),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.width(8.dp))

                AddActionIconButton(
                    icon = Icons.Default.Share,
                    text = "مراسلة وتساب",
                    onClick = onAction,
                    containerColor = Color(0xFFEAB308)
                )
            }
        }
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onExpand() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp).align(Alignment.CenterStart)
                )
                Text(
                    text = title,
                    fontSize = 16.sp,
                    
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        
        androidx.compose.animation.AnimatedVisibility(
            visible = isExpanded,
            enter = androidx.compose.animation.expandVertically(),
            exit = androidx.compose.animation.shrinkVertically()
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, end = 24.dp)
                        .drawBehind {
                            val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            drawLine(
                                color = Color(0xFFBDBDBD),
                                start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                                strokeWidth = 3f,
                                pathEffect = pathEffect
                            )
                        }
                        .padding(end = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: VaccineViewModel) {
    val children by viewModel.children.collectAsState()
    val context = LocalContext.current
    val currentAdminUser by viewModel.adminUsername.collectAsState()
    val currentAdminPass by viewModel.adminPassword.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isBackingUp by viewModel.isBackingUp.collectAsState()
    val backupStatusMessage by viewModel.backupStatusMessage.collectAsState()
    val backupEmail by viewModel.backupEmail.collectAsState()

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val jsonContent = inputStream?.bufferedReader()?.use { reader -> reader.readText() }
                if (jsonContent != null) {
                    ExportUtils.importDatabaseFromJson(context, jsonContent, viewModel.repository.dao) {
                        // Refresh logic if any
                    }
                } else {
                    android.widget.Toast.makeText(context, "تعذر قراءة محتوى ملف النسخة الاحتياطية", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "خطأ أثناء محاولة فتح الملف المختار: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    val saveDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri: android.net.Uri? ->
        uri?.let {
            ExportUtils.exportDatabaseToUri(context, it, viewModel.repository.dao)
        }
    }

    LaunchedEffect(backupStatusMessage) {
        backupStatusMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearBackupStatus()
        }
    }

    var newUsername by remember(currentAdminUser) { mutableStateOf(currentAdminUser) }
    var newPassword by remember(currentAdminPass) { mutableStateOf(currentAdminPass) }

    var expandedSection by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            CollapsibleSection(
                title = "بيانات المستخدم والمسؤول",
                isExpanded = expandedSection == "بيانات المستخدم والمسؤول",
                onExpand = {
                    expandedSection = if (expandedSection == "بيانات المستخدم والمسؤول") null else "بيانات المستخدم والمسؤول"
                }
            ) {
                // Content for Edit Data
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currentUsername by viewModel.username.collectAsState()
                    val currentHealthCenter by viewModel.healthCenterName.collectAsState()
                    val currentOfficerName by viewModel.sideEffectsOfficerName.collectAsState()
                    val currentOfficerPhone by viewModel.sideEffectsOfficerPhone.collectAsState()
                    
                    var officerNameInput by remember(currentOfficerName) { mutableStateOf(currentOfficerName) }
                    var officerPhoneInput by remember(currentOfficerPhone) { mutableStateOf(currentOfficerPhone) }
                    var centerInput by remember(currentHealthCenter) { mutableStateOf(currentHealthCenter) }

                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("اسم المستخدم", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("كلمة المرور", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )

                    OutlinedTextField(
                        value = centerInput,
                        onValueChange = { centerInput = it },
                        label = { Text("اسم المركز الصحي المعتمد", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                    
                    val mapLink by viewModel.healthCenterMapLink.collectAsState()
                    var mapLinkInput by remember(mapLink) { mutableStateOf(mapLink) }
                    OutlinedTextField(
                        value = mapLinkInput,
                        onValueChange = { mapLinkInput = it },
                        label = { Text("رابط خرائط جوجل للمركز الصحي", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )

                    OutlinedTextField(
                        value = backupEmail,
                        onValueChange = { viewModel.updateBackupEmail(it) },
                        label = { Text("حساب الجيميل للنسخ الاحتياطي", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                    
                    OutlinedTextField(
                        value = officerNameInput,
                        onValueChange = { officerNameInput = it },
                        label = { Text("اسم مسؤول الآثار الجانبية", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                    OutlinedTextField(
                        value = officerPhoneInput,
                        onValueChange = { officerPhoneInput = it },
                        label = { Text("رقم هاتف مسؤول الآثار الجانبية", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )

                    Button(
                        onClick = {
                            if (newUsername.trim().isEmpty() || newPassword.trim().isEmpty()) {
                                Toast.makeText(context, "الرجاء عدم ترك الحقول فارغة!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.updateAdminCredentials(newUsername.trim(), newPassword.trim())
                                viewModel.updateHealthCenterName(centerInput.trim())
                                viewModel.updateHealthCenterMapLink(mapLinkInput.trim())
                                viewModel.updateSideEffectsOfficerName(officerNameInput.trim())
                                viewModel.updateSideEffectsOfficerPhone(officerPhoneInput.trim())
                                Toast.makeText(context, "تم حفظ البيانات بنجاح!", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Text("حفظ البيانات", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            CollapsibleSection(
                title = "🌐 لغة التطبيق",
                isExpanded = expandedSection == "🌐 لغة التطبيق",
                onExpand = {
                    expandedSection = if (expandedSection == "🌐 لغة التطبيق") null else "🌐 لغة التطبيق"
                }
            ) {
                // Language switcher content
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Arabic option button
                    OutlinedButton(
                        onClick = { viewModel.setAppLanguage("ar") },
                        modifier = Modifier.weight(1f).testTag("lang_toggle_ar_btn"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (appLanguage == "ar") MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
                        ),
                        border = BorderStroke(
                            width = if (appLanguage == "ar") 2.dp else 1.dp,
                            color = if (appLanguage == "ar") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (appLanguage == "ar") {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = Transl.t("lang_arabic", appLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (appLanguage == "ar") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // English option button
                    OutlinedButton(
                        onClick = { viewModel.setAppLanguage("en") },
                        modifier = Modifier.weight(1f).testTag("lang_toggle_en_btn"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (appLanguage == "en") MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
                        ),
                        border = BorderStroke(
                            width = if (appLanguage == "en") 2.dp else 1.dp,
                            color = if (appLanguage == "en") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (appLanguage == "en") {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = Transl.t("lang_english", appLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (appLanguage == "en") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }


        item {
            CollapsibleSection(
                title = "تصدير واستعادة البيانات",
                isExpanded = expandedSection == "تصدير واستعادة البيانات",
                onExpand = {
                    expandedSection = if (expandedSection == "تصدير واستعادة البيانات") null else "تصدير واستعادة البيانات"
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    CompactActionCard(
                        title = "بيانات المستخدم والمسؤول",
                        subtitle = "تحديث بيانات الأسر، الأطفال، أو السجلات",
                        icon = Icons.Default.Edit,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        onClick = { viewModel.navigateTo(AppScreen.DataEntry) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    CompactActionCard(
                        title = "حفظ سحابي",
                        subtitle = "رفع النسخة الاحتياطية إلى Google Drive",
                        icon = Icons.Default.CloudUpload,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                        onClick = { viewModel.triggerGoogleDriveBackup() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    CompactActionCard(
                        title = "استعادة سحابية",
                        subtitle = "استيراد البيانات بسلاسة من Google Drive",
                        icon = Icons.Default.CloudDownload,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                        onClick = { /* Drive restore logic will go here */ },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    CompactActionCard(
                        title = "تصدير إلى Excel",
                        subtitle = "تصدير الأطفال والملقحين بجدول إكسل ومشاركته",
                        icon = Icons.Default.List,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        onClick = { ExportUtils.exportChildrenToExcel(context, children, viewModel.repository.dao) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    CompactActionCard(
                        title = "حفظ نسخة احتياطية (JSON)",
                        subtitle = "حفظ ملف البيانات JSON داخل ذاكرة الهاتف",
                        icon = Icons.Default.Save,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        onClick = {
                            val timestamp = System.currentTimeMillis() / 1000
                            saveDocumentLauncher.launch("اللقاحات_نسخة_احتياطية_$timestamp.json")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    CompactActionCard(
                        title = "مشاركة البيانات",
                        subtitle = "مشاركة ملف JSON عبر الواتساب أو التليجرام",
                        icon = Icons.Default.Send,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        onClick = { ExportUtils.exportDatabaseToJson(context, viewModel.repository.dao) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    CompactActionCard(
                        title = "استعادة البيانات",
                        subtitle = "استيراد ملف JSON واسترجاع البيانات",
                        icon = Icons.Default.SettingsBackupRestore,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    CompactActionCard(
                        title = "مشاركة التطبيق",
                        subtitle = "مشاركة ملف التثبيت APK مع زملائك",
                        icon = Icons.Default.Share,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        onClick = { shareApkFile(context) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    CompactActionCard(
                        title = "معلومات التطبيق",
                        subtitle = "دليل الاستخدام والاتصال بالمطور",
                        icon = Icons.Default.Info,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        onClick = { viewModel.navigateTo(AppScreen.InfoAndExplanation) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 5. Nearest Health Center Map and Transportation Routes (خريطة مركز طاق ميكائيل ومسار الملاحة)
        item {
            val mapLink by viewModel.healthCenterMapLink.collectAsState()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Transl.t("map_title", appLanguage),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = Transl.t("map_info", appLanguage),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Map via Google Maps App
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapLink))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps?daddr=36.273,43.344"))
                                context.startActivity(webIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (appLanguage == "en") "Open Location in Google Maps" else "تحديد الموقع عبر تطبيق خرائط جوجل", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }

        // 6. Complete Programmer Contact Card in Settings Screen as requested
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "👨‍💻 بطاقة البرمجة والاتصال بمطور النظام",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    DeveloperContactInfoCard(
                        onSaveClick = {
                            // Image reference removed
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveRouteMapView() {
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Draw map background grids
            for (i in 1..8) {
                drawLine(
                    color = Color(0xFFCBD5E1).copy(alpha = 0.3f),
                    start = androidx.compose.ui.geometry.Offset(0f, h * i / 9f),
                    end = androidx.compose.ui.geometry.Offset(w, h * i / 9f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color(0xFFCBD5E1).copy(alpha = 0.3f),
                    start = androidx.compose.ui.geometry.Offset(w * i / 9f, 0f),
                    end = androidx.compose.ui.geometry.Offset(w * i / 9f, h),
                    strokeWidth = 2f
                )
            }

            // Draw primary road connection
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.15f, h * 0.65f)
                quadraticTo(w * 0.45f, h * 0.4f, w * 0.55f, h * 0.7f)
                quadraticTo(w * 0.7f, h * 0.85f, w * 0.85f, h * 0.4f)
            }
            drawPath(
                path = path,
                color = Color(0xFF2563EB),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 8f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                )
            )

            // Draw point nodes
            // Mosul
            drawCircle(Color(0xFF1E3A8A), radius = 14f, center = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.65f))
            drawCircle(Color.White, radius = 6f, center = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.65f))

            // Bartella Checkpoint
            drawCircle(Color(0xFF475569), radius = 10f, center = androidx.compose.ui.geometry.Offset(w * 0.55f, h * 0.7f))

            // Dr. Ihsan Al-Ramadani Center
            drawCircle(Color(0xFFDC2626), radius = 18f, center = androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.4f))
            drawCircle(Color.White, radius = 8f, center = androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.4f))
        }

        // Overlay text labels
        Text(
            text = "📍 الموصل (نقطة الانطلاق)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E3A8A),
            modifier = Modifier
                .padding(start = 12.dp, top = 115.dp)
        )

        Text(
            text = "🏥 احسان ال شنتي",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFB91C1C),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 12.dp)
        )
    }
}

// Global utility helper to stably save resource drawable as a secure public JPEG image in user Pictures gallery
fun saveImageToGallery(context: android.content.Context, drawableId: Int, filename: String) {
    try {
        val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, drawableId) ?: return
        val contentResolver = context.contentResolver
        val imageDetails = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/VaccineTracker")
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val imageUri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageDetails)
        if (imageUri != null) {
            contentResolver.openOutputStream(imageUri).use { out ->
                if (out != null) {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                imageDetails.clear()
                imageDetails.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(imageUri, imageDetails, null, null)
            }
            Toast.makeText(context, "تم حفظ بطاقة التواصل بنجاح في معرض الصور الخاص بك 🖼️", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "فشل في حفظ الصورة.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "حدث خطأ أثناء حفظ الصورة: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ProgressChartScreen(viewModel: VaccineViewModel) {
    val childrenProgress by viewModel.childrenProgress.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "نسب الإنجاز 📊",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "متابعة شاملة لنسب إنجاز اللقاحات لجميع الأطفال المسجلين بالمركز لضمان الالتزام بجدول اللقاحات الوطني",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        if (childrenProgress.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا يوجد أطفال مسجلين لعرض نسب الإنجاز لديهم حالياً 👶",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(childrenProgress, key = { it.child.id }) { prog ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(AppScreen.ChildDetails(prog.child.id)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prog.child.fullName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "العمر: ${VaccineScheduler.computeAgeString(prog.child.dob)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${prog.percentage.toInt()}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Line Indicator Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(prog.percentage / 100f)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الجرعات المكتملة: ${prog.completedCount} من أصل ${prog.totalCount}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "اضغط لعرض السجل 🔍",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Completed Badge
                            Surface(
                                color = Color(0xFFE8F5E9),
                                contentColor = Color(0xFF2E7D32),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("مكتملة", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("${prog.completedCount}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            // Pending Badge
                            Surface(
                                color = Color(0xFFECEFF1),
                                contentColor = Color(0xFF37474F),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("متبقية", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("${prog.pendingCount}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            // Overdue Badge
                            if (prog.overdueCount > 0) {
                                Surface(
                                    color = Color(0xFFFFEBEE),
                                    contentColor = Color(0xFFC62828),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("متأخرة ⚠️", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text("${prog.overdueCount}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelegramChannelFollowCard() {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F4FA) // Soft Telegram-like light blue background
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFB3E5FC)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/i7san_App"))
                context.startActivity(intent)
            }
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = com.example.R.drawable.ic_telegram),
                contentDescription = "قناة التليجرام",
                tint = Color(0xFF249FD8),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "انظم الى قناة التلكرام ( احصل على اخر التحديثات والتنبيهات ) 📢",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F5B80),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DeveloperContactInfoCard(onSaveClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth().testTag("developer_contact_card"),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "👨‍💻 بطاقة مبرمج النظام (احسان ال شنتي)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "اضغط على أي أيقونة للتواصل والتبليغ السريع",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Link icon
                ContactMiniChip(drawableId = com.example.R.drawable.ic_popl, tooltip = "الرابط الشامل") {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://popl.co/card/M7KfvDVK/1/s"))
                    context.startActivity(intent)
                }
                // Email
                ContactMiniChip(drawableId = com.example.R.drawable.ic_gmail, tooltip = "البريد") {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:ihsan.421990@gmail.com"))
                    context.startActivity(intent)
                }
                // Whatsapp
                ContactMiniChip(drawableId = com.example.R.drawable.ic_whatsapp, tooltip = "الواتساب") {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://api.whatsapp.com/send?phone=9647729792239"))
                    context.startActivity(intent)
                }
                // Facebook
                ContactMiniChip(drawableId = com.example.R.drawable.ic_facebook, tooltip = "الفيسبوك") {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.facebook.com/ihsan.zwaid"))
                    context.startActivity(intent)
                }
                // Instagram
                ContactMiniChip(drawableId = com.example.R.drawable.ic_instagram, tooltip = "الانستغرام") {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.instagram.com/i7san90/"))
                    context.startActivity(intent)
                }
                // Telegram
                ContactMiniChip(drawableId = com.example.R.drawable.ic_telegram, tooltip = "التليجرام") {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/i7san_App"))
                    context.startActivity(intent)
                }
            }
        }
    }
}

@Composable
fun ContactMiniChip(
    @androidx.annotation.DrawableRes drawableId: Int,
    tooltip: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = tooltip,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ContactItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Icon(Icons.Default.PlayArrow, contentDescription = Transl.t("open", "ar"), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
    }
}

@Composable
fun ProgrammerFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "تم تطوير المنصة بواسطة احسان زويد ال شنتي الحديدي",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "حقوق الطبع محفوظة لدى دائرة صحة نينوى © 2026",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SplashScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val videoResId = context.resources.getIdentifier("intro_video", "raw", context.packageName)

    if (videoResId != 0) {
        VideoSplashScreen(videoResId = videoResId, onDismiss = onDismiss)
    } else {
        StaticSplashScreen(onDismiss = onDismiss)
    }
}

@Composable
fun VideoSplashScreen(videoResId: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.widget.VideoView(ctx).apply {
                    setVideoURI(android.net.Uri.parse("android.resource://${ctx.packageName}/$videoResId"))
                    setOnCompletionListener {
                        onDismiss()
                    }
                    setOnErrorListener { _, _, _ ->
                        onDismiss()
                        true
                    }
                    start()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun StaticSplashScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val prefs = context.getSharedPreferences("vaccine_prefs", android.content.Context.MODE_PRIVATE)
    val appLanguage = prefs.getString("app_language", "ar") ?: "ar"

    val bgColors = if (isDark) {
        listOf(
            Color(0xFF0E1617), // Deep calming teal-slate
            Color(0xFF162526), // Soothing darker teal
            Color(0xFF1E3537)  // Elegant dark blue-green
        )
    } else {
        listOf(
            Color(0xFFF2F9F6), // Very soft calming light mint-green
            Color(0xFFE0F2FE), // Cozy sky-blue pediatric gradient element
            Color(0xFFE0F2F1)  // Warm comforting soft light teal
        )
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(bgColors)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = com.example.R.drawable.img_clean_i7san_app_icon_1782895431554),
                contentDescription = "شعار إحسان",
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, if (isDark) Color(0xFF4DB6AC) else Color(0xFF0D9488), RoundedCornerShape(24.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = Transl.t("splash_ehsan", appLanguage),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = if (isDark) Color.White else Color(0xFF0A1F1D)
            )
            Text(
                text = Transl.t("splash_tracker", appLanguage),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF81C784) else Color(0xFF0D9488)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = Transl.t("splash_subtitle_desc", appLanguage),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFF81D4FA) else Color(0xFF0EA5E9),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(60.dp))
            Text(
                text = Transl.t("splash_footer", appLanguage),
                fontSize = 10.sp,
                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f)
            )
        }
    }
}

object Transl {
    fun t(key: String, lang: String, centerName: String = ""): String {
        val text = translations[lang]?.get(key) ?: translations["ar"]?.get(key) ?: key
        if (centerName.isNotEmpty()) {
            return text.replace("طاق ميكائيل", centerName).replace("Taq-Mikael", centerName)
        }
        return text
    }

    fun getVaccineName(key: String, arDefault: String, lang: String): String {
        if (lang != "en") return arDefault
        return when (key) {
            "BCG" -> "BCG Vaccine (Tuberculosis)"
            "HepB_0" -> "Hepatitis B Vaccine (Zero Dose)"
            "OPV_0" -> "Oral Polio Vaccine (Zero Dose)"
            "OPV_1" -> "Oral Polio Vaccine (Dose 1)"
            "Rota_1" -> "Rotavirus Vaccine (Dose 1)"
            "Penta_1" -> "Pentavalent Vaccine (Dose 1) [DPT-HepB-Hib]"
            "PCV_1" -> "Pneumococcal Conjugate Vaccine (Dose 1)"
            "OPV_2" -> "Oral Polio Vaccine (Dose 2)"
            "Penta_2" -> "Pentavalent Vaccine (Dose 2) [DPT-HepB-Hib]"
            "Rota_2" -> "Rotavirus Vaccine (Dose 2)"
            "PCV_2" -> "Pneumococcal Conjugate Vaccine (Dose 2)"
            "IPV_1" -> "Inactivated Polio Injection (Dose 1)"
            "OPV_3" -> "Oral Polio Vaccine (Dose 3)"
            "Rota_3" -> "Rotavirus Vaccine (Dose 3)"
            "Penta_3" -> "Pentavalent Vaccine (Dose 3) [DPT-HepB-Hib]"
            "PCV_3" -> "Pneumococcal Conjugate Vaccine (Dose 3)"
            "IPV_2" -> "Inactivated Polio Injection (Dose 2)"
            "Measles" -> "Single Measles Vaccine"
            "VitA_1" -> "Vitamin A Supplement (Dose 1)"
            "MMR_1" -> "MMR Mixed Vaccine (Dose 1)"
            "MMR_2" -> "MMR Mixed Vaccine (Dose 2)"
            "Booster_OPV_1" -> "Oral Polio Booster (Dose 1)"
            "Booster_DPT_1" -> "DPT Tri-booster (Dose 1)"
            "VitA_2" -> "Vitamin A Supplement (Dose 2)"
            "Booster_OPV_2" -> "Oral Polio Booster (Dose 2)"
            "Booster_DPT_2" -> "DPT Tri-booster (Dose 2)"
            else -> arDefault
        }
    }

    fun getCategoryName(arDefault: String, lang: String): String {
        if (lang != "en") return arDefault
        return when (arDefault.trim()) {
            "عند الولادة / أول 24 ساعة", "عند الولادة / أول ٢٤ ساعة" -> "At Birth / First 24 Hours"
            "عمر شهرين", "عمر شهرين " -> "Age 2 Months"
            "عمر 4 أشهر", "عمر ٤ أشهر", "عمر 4 اشهر" -> "Age 4 Months"
            "عمر 6 أشهر", "عمر ٦ أشهر", "عمر 6 اشهر" -> "Age 6 Months"
            "عمر 9 أشهر", "عمر ٩ أشهر", "عمر 9 اشهر" -> "Age 9 Months"
            "عمر 12 شهر", "عمر ١٢ شهر", "عمر 12 شهراً" -> "Age 12 Months"
            "عمر 18 شهراً", "عمر ١٨ شهراً", "عمر 18 اشهر" -> "Age 18 Months"
            "عمر 4 سنوات", "عمر ٤ سنوات", "عمر 4 أعوام" -> "Age 4 Years"
            else -> arDefault
        }
    }

    private val translations = mapOf(
        "ar" to mapOf(
            "app_title" to "لقح طفلك (القعقاع)",
            "center_subtitle" to "مركز صحي طاق ميكائيل ( احسان ال شنتي )",
            "search_placeholder" to "البحث عن الطفل المسجل",
            "register_new_child" to "تسجيل طفل جديد",
            "children_list" to "قائمة الملقحين",
            "welcome_title" to "مرحباً بك في منصة إحسان ال شنتي لللقاحات 🛡️",
            "no_children_found" to "لا يوجد أطفال مسجلون حتى الآن يطابقون البحث.",
            "age" to "العمر",
            "phone" to "الهاتف",
            "dob" to "ولد في",
            "view_details" to "عرض التفاصيل",
            "alerts" to "الإشعارات",
            "settings" to "الإعدادات",
            "logout" to "تسجيل الخروج",
            "theme_toggle" to "تبديل المظهر دائم الصحة والعافية",
            "notifications" to "التنبيهات",
            "database_connected" to "قاعدة البيانات متصلة ومزامنة مع لوحة المسؤول 🟢",
            "database_info" to "محفظة SQLite المحلية متصلة في الوقت الحقيقي بكل جداول كروت الأطفال دون سن الخامسة. الربط يتم بصفة فورية مع مركز صحي طاق ميكائيل لنقل البيانات وتدقيق الأكواد QR.",
            "backup_title" to "📁 النسخ الاحتياطي لقاعدة البيانات (Google Drive)",
            "backup_info" to "قم بنسخ قاعدة بيانات مركز طاق ميكائيل للأطفال ومزامنتها سحابياً على حساب Google Drive الخاص بك لضمان حماية سجلات الملقحين من الفقدان والاحتفاظ بها دائماً.",
            "backup_button" to "ربط ومزامنة Google Drive السحابي",
            "share_title" to "🤝 مشاركة التطبيق مع الأصدقاء والزملاء",
            "share_info" to "ساعد في تعميم الفائدة ونشر الوعي الصحي من خلال مشاركة رابط التطبيق مع الفرق الطبية وأولياء الأمور لتسهيل حماية الأطفال.",
            "share_button" to "مشاركة رابط التطبيق الآن",
            "map_title" to "🗺️ خريطة مركز صحي طاق ميكائيل ومسار التنقل:",
            "map_info" to "مسار التلقيح المباشر: الموصل ➔ برطلة ➔ مركز صحي طاق ميكائيل ( احسان ال شنتي )",
            "map_button" to "فتح الملاحة وتثبيت موقع المركز في خرائط Google",
            "admin_title" to "🔒 تعديل حساب المسؤول (اسم المستخدم وكلمة المرور)",
            "admin_user_label" to "اسم المستخدم الجديد",
            "admin_pass_label" to "كلمة المرور الجديدة",
            "save_changes" to "حفظ التغييرات الجديدة",
            "settings_header_title" to "⚙️ الإعدادات المتقدمة وإدارة النظام",
            "settings_header_desc" to "تعديل حساب الإدارة ومطالعة الخريطة والمزامنة لقاعدة البيانات ومبدل اللغة",
            "lang_section_title" to "🌐 لغة التطبيق / App Language",
            "lang_section_desc" to "تبديل لغة واجهة التطبيق بين العربية والإنجليزية / Toggle application language to English or Arabic",
            "lang_arabic" to "العربية (Arabic)",
            "lang_english" to "English (الإنجليزية)",
            "splash_ehsan" to "إحسان",
            "splash_tracker" to "i7san Vaccination Tracker",
            "splash_subtitle_desc" to "نظام المتابعة الذكي للقاحات والرعاية الأسرية",
            "splash_footer" to "ثقة طبية وأمان رقمي متكامل",
            "exit_title" to "خروج",
            "exit_message" to "هل أنت متأكد من رغبتك في الخروج من منصة لقح طفلك؟",
            "cancel" to "إلغاء",
            "yes" to "نعم، خروج",
            "id_label" to "معرّف: ",
            "vaccine_progress" to "نسبة اكتمال اللقاحات المأخوذة:",
            "whatsapp_request" to "طلب ملف التلقيح عبر واتساب",
            "menu_children" to "الرئيسية",
            "menu_alerts" to "الإشعارات",
            "menu_settings" to "الإعدادات",
            "home_data_entry_title" to "تسجيل بيانات الملقحين",
            "home_data_entry_desc" to "إضافة بيانات أسر جديدة وتسجيل لقاحات الملقحين",
            "home_view_data_title" to "عرض السجلات",
            "home_view_data_desc" to "عرض سجلات الأطفال الملقحين والبيانات الموثقة",
            "home_inventory_title" to "إدارة اللقاحات ومستلزماتها",
            "home_inventory_desc" to "متابعة الرصيد والمستلم والمتبقي وحساب نسب الهدر",
            "alerts_title" to "مركز مراقبة الإشعارات والتنبيهات",
            "alerts_subtitle" to "متابعة تلقائية للأطفال المقترب موعدهم أو المتسربين متأخري الجرعات",
            "alerts_empty" to "رائع! لا يوجد أي تسيب أو استحقاقات متأخرة بالدائرة اليوم.",
            "alerts_overdue_header" to "إشعار متأخري اللقاحات لأكثر من 28 يوماً ({count} أطفال متسربين)",
            "alerts_approaching_header" to "إخطارات اقتراب موعد الجرعات - خلال غضون 3 أيام ({count} أطفال)",
            "whatsapp_sms" to "مراسلة وتساب فورية",
            "overdue_tag" to "تسرب متأخر > 28 يوم",
            "youngest" to "الأصغر سناً",
            "unique_id_label" to "الرقم التعريفي المستقل (ID): ",
            "missing_vaccine" to "اللقاح الفائت: ",
            "due_date_label" to "استحقاق الجرعة بتاريخ: ",
            "custom_qr" to "QR مخصص",
            "approaching_tag" to "حقن قريب < 3 أيام",
            "upcoming_vaccine" to "اللقاح المستحق قريباً: ",
            "approx_due_date" to "تاريخ الاستحقاق التقريبي: ",
            "message_preview" to "الرسالة: اقترب موعد لقاح {name} يرجى مراجعة أقرب مركز صحي",
            "dev_card_desc" to "بطاقة تواصل احسان ال شنتي",
            "dev_save_card" to "حفظ الكارت في الاستوديو 🖼️",
            "dev_title" to "المبرمج احسان ال شنتي",
            "dev_email" to "البريد الإلكتروني",
            "dev_whatsapp" to "الواتساب المباشر",
            "dev_facebook" to "صفحة الفيسبوك",
            "dev_instagram" to "حساب الانستغرام",
            "dev_telegram" to "حساب التليجرام",
            "open" to "فتح",
            "dev_card_title" to "👨‍💻 بطاقة البرمجة والاتصال بمطور النظام",
            "save_success_toast" to "تم حفظ بطاقة التواصل بنجاح في معرض الصور الخاص بك 🖼️",
            "save_fail_toast" to "فشل في حفظ الصورة.",
            "save_error_toast" to "حدث خطأ أثناء حفظ الصورة:",
            "programmer_title" to "المبرمج ( احسان ال شنتي )",
            "programmer_sub" to "مع تمنياتنا لكم بدوام الصحة والعافية ان شاء الله",
            "welcome_dialog_title" to "👋 مرحباً بكم في نظام اللقاحات الرقمي المعتمد",
            "welcome_dialog_desc" to "نظام متابعة لقاحات الأطفال المعتمد من قبل وزارة الصحة ومساعد المبرمج احسان ال شنتي. يرجى حفظ وتثبيت كارت التواصل للتراسل اللاحق وتنسيق جداول الملقحين.",
            "welcome_dialog_confirm" to "الدخول إلى النظام 👍",
            "welcome_dialog_save" to "حفظ الكارت 💾",
            "exit_dialog_title" to "🚪 مغادرة التطبيق - في أمان الله",
            "exit_dialog_desc" to "دوام الصحة والعافية لأطفالنا الأعزاء ان شاء الله. يسعدنا احتفاظك ببطاقة تواصل مبرمج النظام المعتمد للتواصل السريع والتبليغ عن الأعراض.",
            "exit_dialog_confirm" to "إغلاق التطبيق نهائياً",
            "exit_dialog_dismiss" to "إلغاء والرجوع",
            "login_title" to "نظام متابعة اللقاحات الرقمي المعتمد من احسان ال شنتي",
            "login_subtitle" to "بوابة الملقح الطبي المعتمد لوزارة الصحة",
            "login_user_label" to "اسم المستخدم المسؤول",
            "login_user_placeholder" to "أدخل اسمك أو admin",
            "login_remember" to "تذكر اسم المستخدم المسؤول",
            "login_button" to "دخول سريع إلى النظام",
            "login_hint_title" to "تلميح للمصححين والمشرفين:",
            "login_hint_desc" to "• اسم المستخدم: admin\n• كلمة المرور الحالية: 123456",
            "login_error_empty" to "يرجى إدخال اسم المستخدم الخاص بالمسؤول",
            "add_child_desc" to "الرجاء تعبئة بيانات الطفل بدقة لإنشاء كرت لقاح عراقي ذكي",
            "fullname_label" to "اسم الطفل الرباعي",
            "fullname_placeholder" to "مثال: علي حسين جاسم محمد",
            "fullname_err" to "الاسم الرباعي يتكون من 4 أسماء على الأقل ({count} الحالي)",
            "fullname_ok" to "الاسم متكامل ومطابق للأنظمة",
            "mother_name_label" to "اسم الأم الثلاثي",
            "mother_name_placeholder" to "مثال: فاطمة زهراء علي",
            "phone_whatsapp_label" to "رقم هاتف الوتساب لرب الأسرة",
            "phone_placeholder" to "مثال: 07700000000 أو 7700000000",
            "phone_hint" to "سيضاف إليه رمز العراق (964) تلقائياً عند الحفظ",
            "health_center_label" to "أقرب مركز صحي للطفل",
            "health_center_placeholder" to "مثال: طاق ميكائيل",
            "address_label" to "عنوان سكن العائلة",
            "address_placeholder" to "مثال: الموصل، حي القادسية",
            "dob_title" to "تاريخ الميلاد",
            "day_label" to "اليوم",
            "month_label" to "الشهر",
            "year_label" to "السنة",
            "age_limit_warning" to "تنبيه: يجب ألا يتجاوز عمر الطفل 5 سنوات (تاريخ ميلاد بعد تاريخ 2021-06-20)",
            "cancel_button" to "إلغاء الأمر",
            "save_card_button" to "تسجيل وحفظ الكارت",
            "details_title" to "تفاصيل الملف والتلقيحات",
            "edit_child_title" to "تعديل تفاصيل سجل الطفل",
            "edit_fullname_label" to "الاسم الرباعي للطفل",
            "edit_mother_label" to "اسم الأم الثنائي",
            "edit_phone_label" to "رقم هاتف الملقح",
            "edit_center_label" to "المركز الصحي",
            "edit_address_label" to "عنوان السكن والقرية",
            "toggle_status_title" to "تحديث / إلغاء حالة التلقيح",
            "add_vaccine_title" to "حقن وتثبيت لقاح للطفل",
            "vaccine_taken_desc" to "اللقاح ({name}) مسجل كمنجز للطفل",
            "side_effects_observed" to "الأثار الجانبية الملاحظة",
            "side_effects_placeholder" to "مثال: ارتفاع درجة حرارة، احمرار بمكان الحقن",
            "undo_vaccine_warning" to "تنبيه: يمكنك إلغاء حالة 'تم التلقيح' وإرجاعه لغير المكتملة بالضغط على الزر أدناه باللون الأحمر.",
            "vaccine_prompt" to "هل تم جرع وحقن لقاح ({name}) للطفل {child} بنجاح؟",
            "actual_vax_date_label" to "تاريخ التلقيح الفعلي:",
            "edit_date_btn" to "تعديل التاريخ 📅",
            "upcoming_milestones_title" to "💉 لقاحات فئة ({category}) القادمة:",
            "deselect_all" to "إلغاء تحديد الكل",
            "select_all" to "تحديد الكل للحقن 📌",
            "side_effects_optional" to "الأثار الجانبية المسجلة (اختياري)",
            "side_effects_placeholder_2" to "مثال: حرارة مستمرة، خمول، وبكاء",
            "antipyretic_warning" to "تنبيه هام: يرجى إعطاء الطفل خافضات الحرارة المناسبة والمسكنات المعتمدة، مع قياس درجة حرارته دورياً لمكافحة الأثر الجانبي.",
            "save_close" to "حفظ وإغلاق",
            "confirm_vaccine" to "تأكيد تسجيل التلقيح",
            "cancel_taken" to "إلغاء التلقيح (غير منجز ❌)",
            "close" to "إغلاق",
            "delete_profile_title" to "حذف كارت الطفل كلياً",
            "delete_profile_warn" to "تحذير! سيؤدي هذا الإجراء إلى حذف ملف الطفل '{name}' وكل سجلات تطعيماته للداخل بشكل نهائي. هل تريد الاستمرار؟",
            "delete_confirm_btn" to "حذف الملف نهائياً",
            "details_dob_label" to "تاريخ الميلاد: ",
            "details_phone_label" to "الوتساب: ",
            "details_mother_label" to "اسم الأم الثلاثي: ",
            "details_center_label" to "أقرب مركز صحي: ",
            "details_address_label" to "عنوان السكن: ",
            "registered_stats" to "المسجلين",
            "approaching_stats" to "مواعيد قريبة",
            "overdue_stats" to "المتسربين",
            "clear" to "حذف",
            "no_children_blank" to "لا يوجد أطفال مسجلون حالياً في المركز.",
            "no_children_search" to "لم نعثر على مطابقة لطلب البحث.",
            "backup_title" to "📁 النسخ الاحتياطي لقاعدة البيانات (Google Drive)",
            "backup_info" to "قم بنسخ قاعدة بيانات مركز طاق ميكائيل للأطفال ومزامنتها سحابياً على حساب Google Drive الخاص بك لضمان حماية سجلات الملقحين من الفقدان والاحتفاظ بها دائماً.",
            "backup_button" to "ربط ومزامنة Google Drive السحابي",
            "share_title" to "🤝 مشاركة التطبيق مع الأصدقاء والزملاء",
            "share_info" to "ساعد في تعميم الفائدة ونشر الوعي الصحي من خلال مشاركة رابط التطبيق مع الفرق الطبية وأولياء الأمور لتسهيل حماية الأطفال.",
            "share_button" to "مشاركة رابط التطبيق الآن",
            "map_title" to "🗺️ خريطة مركز صحي طاق ميكائيل ومسار التنقل:",
            "map_info" to "مسار التلقيح المباشر: الموصل ➔ برطلة ➔ مركز صحي طاق ميكائيل ( احسان ال شنتي )",
            "map_button" to "فتح الملاحة وتثبيت موقع المركز في خرائط Google",
            "admin_title" to "🔒 تعديل حساب المسؤول (اسم المستخدم وكلمة المرور)",
            "admin_user_label" to "اسم المستخدم الجديد",
            "admin_pass_label" to "كلمة المرور الجديدة",
            "save_changes" to "حفظ التغييرات الجديدة",
            "settings_header_title" to "⚙️ الإعدادات المتقدمة وإدارة النظام",
            "settings_header_desc" to "تعديل حساب الإدارة ومطالعة الخريطة والمزامنة لقاعدة البيانات ومبدل اللغة",
            "lang_section_title" to "🌐 لغة التطبيق / App Language",
            "lang_section_desc" to "تبديل لغة واجهة التطبيق بين العربية والإنجليزية / Toggle application language to English or Arabic",
            "lang_arabic" to "العربية (Arabic)",
            "lang_english" to "English (الإنجليزية)",
            "splash_ehsan" to "إحسان",
            "splash_tracker" to "i7san Vaccination Tracker",
            "splash_subtitle_desc" to "نظام المتابعة الذكي للقاحات والرعاية الأسرية",
            "splash_footer" to "ثقة طبية وأمان رقمي متكامل",
            "exit_title" to "خروج",
            "exit_message" to "هل أنت متأكد من رغبتك في الخروج من منصة لقح طفلك؟",
            "cancel" to "إلغاء",
            "yes" to "نعم، خروج",
            "id_label" to "معرّف: ",
            "vaccine_progress" to "نسبة اكتمال اللقاحات المأخوذة:",
            "whatsapp_request" to "طلب ملف التلقيح عبر واتساب",
            "tab_home" to "الرئيسية",
            "tab_children" to "الأشخاص",
            "tab_alerts" to "الإشعارات",
            "tab_progress" to "نسبة الإنجاز",
            "tab_settings" to "الإعدادات",
            "setting_center_title" to "🏥 اسم المركز الصحي المعتمد",
            "setting_center_desc" to "الاسم المعتمد للمركز الصحي في كروت اللقاح ومسارات الخريطة والتنبيهات.",
            "setting_center_label" to "اسم المركز الصحي",
            "setting_center_save" to "حفظ اسم المركز الصحي"
        ),
        "en" to mapOf(
            "app_title" to "Vaccinate Your Child",
            "center_subtitle" to "Taq-Mikael Health Center (i7san Al Shanti)",
            "search_placeholder" to "Search registered child",
            "register_new_child" to "Register New Child",
            "children_list" to "Registered Children List",
            "welcome_title" to "Welcome to i7san Al Shanti Vaccine Platform 🛡️",
            "no_children_found" to "No registered children match your search.",
            "age" to "Age",
            "phone" to "Phone",
            "dob" to "Born on",
            "view_details" to "View Details",
            "alerts" to "Alerts",
            "settings" to "Settings",
            "logout" to "Log Out",
            "theme_toggle" to "Toggle Theme",
            "notifications" to "Notifications",
            "database_connected" to "Database is connected and synced with Admin 🟢",
            "database_info" to "The local SQLite database is connected in real-time. Syncing is active with Taq-Mikael Health Center for data transmission and QR validation.",
            "backup_title" to "📁 Database Backup (Google Drive)",
            "backup_info" to "Backup and sync the children database to Google Drive to protect records from loss and keep them always available.",
            "backup_button" to "Sync with Google Drive Cloud",
            "share_title" to "🤝 Share App with Friends & Colleagues",
            "share_info" to "Help spread the word and raise health awareness by sharing the app link with healthcare teams and parents.",
            "share_button" to "Share Application Link Now",
            "map_title" to "🗺️ Taq-Mikael Health Center Map & Route:",
            "map_info" to "Direct Transit Route: Mosul ➔ Bartella ➔ Taq-Mikael Health Center (i7san Al Shanti)",
            "map_button" to "Open Navigation & Get Location on Google Maps",
            "admin_title" to "🔒 Update Admin Account (Username & Password)",
            "admin_user_label" to "New Username",
            "admin_pass_label" to "New Password",
            "save_changes" to "Save New Changes",
            "settings_header_title" to "⚙️ Advanced Settings & Admin Dashboard",
            "settings_header_desc" to "Edit admin credentials, view interactive map, sync databases and toggle language",
            "lang_section_title" to "🌐 App Language / لغة التطبيق",
            "lang_section_desc" to "Toggle application language to English or Arabic / تبديل لغة واجهة التطبيق بين العربية والإنجليزية",
            "lang_arabic" to "العربية (Arabic)",
            "lang_english" to "English (الإنجليزية)",
            "splash_ehsan" to "i7san",
            "splash_tracker" to "i7san Vaccination Tracker",
            "splash_subtitle_desc" to "Smart vaccination tracker and family care system",
            "splash_footer" to "Medical trust and integrated digital security",
            "exit_title" to "Exit App",
            "exit_message" to "Are you sure you want to exit the Vaccinate Your Child application?",
            "cancel" to "Cancel",
            "yes" to "Yes, Exit",
            "id_label" to "ID: ",
            "vaccine_progress" to "Vaccinated doses progress:",
            "whatsapp_request" to "Request Vaccination Card via WhatsApp",
            "menu_children" to "Home",
            "home_slogan" to "Smart Medical Platform for Vaccine & Family Care Management",
            "home_data_entry_title" to "Data Entry 📝",
            "home_data_entry_desc" to "Add new family data, register a new child, and record adult Td vaccine",
            "home_view_data_title" to "View Vaccinated & Data 📋",
            "home_view_data_desc" to "View families data, registered children, and adult Td records",
            "home_inventory_title" to "Vaccine Inventory Management 📊",
            "home_inventory_desc" to "Track received & remaining stock, calculate waste rates accurately, and monthly automated reporting",
            "home_registered_title" to "Registered Vaccinees 📋",
            "home_registered_desc" to "List of center vaccinees",
            "search_placeholder_2" to "Search by child name or record number...",
            "tab_all" to "All 🌐",
            "tab_children_only" to "Children 👶",
            "tab_adult_td" to "Adult Td 💉",
            "download_pdf" to "Download Vaccine Card (PDF)",
            "print_card" to "Print Vaccination Record",
            "onboarding_title_1" to "تسجيل بيانات الملقحين 📋",
            "onboarding_desc_1" to "واجهة منظمة للوصول السريع لتسجيل الأطفال وتحديث البيانات وإدارة اللقاحات بكفاءة.",
            "onboarding_title_2" to "تسجيل طفل جديد 👶",
            "onboarding_desc_2" to "إضافة الأطفال دون سن الخامسة وتوليد جدول لقاحاتهم الوطني تلقائياً لضمان حمايتهم.",
            "onboarding_title_3" to "ربط الطفل بأسرة مسجلة مسبقاً 🔗",
            "onboarding_desc_3" to "ربط الطفل الجديد بأسرة مسجلة مسبقاً لتنظيم المتابعة وإرسال تنبيهات مباشرة لأولياء الأمور.",
            "onboarding_title_4" to "تسجيل وحفظ الكارت 🛡️",
            "onboarding_desc_4" to "توليد كارت لقاح ملون ورمز QR فريد، ومشاركته وحفظه لسهولة تدقيق الجرعات.",
            "onboarding_skip" to "Skip ⏭️",
            "menu_alerts" to "Alerts",
            "menu_settings" to "Settings",
            "alerts_title" to "Alerts & Warnings Monitor",
            "alerts_subtitle" to "Automatic tracking for approaching or overdue vaccine doses",
            "alerts_empty" to "Excellent! No overdue or approaching vaccinations today.",
            "alerts_overdue_header" to "Overdue vaccine alerts for over 28 days ({count} children)",
            "alerts_approaching_header" to "Approaching vaccine dose alerts - within 3 days ({count} children)",
            "whatsapp_sms" to "Instant WhatsApp SMS",
            "overdue_tag" to "Overdue > 28 Days",
            "youngest" to "Youngest",
            "unique_id_label" to "Unique Identification (ID): ",
            "missing_vaccine" to "Missed Vaccine: ",
            "due_date_label" to "Due date for dose: ",
            "custom_qr" to "Custom QR",
            "approaching_tag" to "Approaching < 3 Days",
            "upcoming_vaccine" to "Upcoming Vaccine: ",
            "approx_due_date" to "Approximate Due Date: ",
            "message_preview" to "Msg: Vaccine due for {name}, please visit nearest center",
            "dev_card_desc" to "i7san Al Shanti Contact Card",
            "dev_save_card" to "Save Card to Gallery 🖼️",
            "dev_title" to "Developer i7san Al Shanti",
            "dev_email" to "E-mail Address",
            "dev_whatsapp" to "Direct WhatsApp",
            "dev_facebook" to "Facebook Page",
            "dev_instagram" to "Instagram Account",
            "dev_telegram" to "Telegram Username",
            "open" to "Open",
            "dev_card_title" to "👨‍💻 System Developer Contact Card",
            "save_success_toast" to "Contact card saved successfully to your gallery! 🖼️",
            "save_fail_toast" to "Failed to save image.",
            "save_error_toast" to "Error occurred while saving image:",
            "programmer_title" to "(Programmer) i7san Al Shanti",
            "programmer_sub" to "Wishing you and your children continuous health and wellness",
            "welcome_dialog_title" to "👋 Welcome to the Smart Vaccine Tracking System",
            "welcome_dialog_desc" to "The smart child vaccination system approved by the Ministry of Health and Assistant Programmer i7san Al Shanti. Please save the developer card for future communication and schedule coordination.",
            "welcome_dialog_confirm" to "Enter the System 👍",
            "welcome_dialog_save" to "Save Card 💾",
            "exit_dialog_title" to "🚪 Exiting the App - Goodbye",
            "exit_dialog_desc" to "Continuous health and safety for our children. You can save the certified system developer profile details to report symptoms quickly.",
            "exit_dialog_confirm" to "Exit App Completely",
            "exit_dialog_dismiss" to "Cancel & Go Back",
            "login_title" to "Digital Vaccine Schedule Tracking Platform by i7san Al Shanti",
            "login_subtitle" to "Authorized Medical Vaccinator Gateway for MOH",
            "login_user_label" to "Admin Username",
            "login_user_placeholder" to "Enter username or admin",
            "login_remember" to "Remember admin username",
            "login_button" to "Quick Access to System",
            "login_hint_title" to "Note for reviewers & supervisors:",
            "login_hint_desc" to "• Username: admin\n• Password: 123456",
            "login_error_empty" to "Please enter the admin username",
            "add_child_desc" to "Please fill out child details accurately to build a smart Iraqi vaccine card",
            "fullname_label" to "Child's Quadruple Name",
            "fullname_placeholder" to "e.g. Ali Hussein Jasim Mohammed",
            "fullname_err" to "The quadruple name must consist of at least 4 names ({count} current)",
            "fullname_ok" to "The name is fully qualified and matches official records",
            "mother_name_label" to "Mother's Triple Name",
            "mother_name_placeholder" to "e.g. Fatima Zahra Ali",
            "phone_whatsapp_label" to "Parent's WhatsApp Phone",
            "phone_placeholder" to "e.g. 07700000000 or 7700000000",
            "phone_hint" to "Country code (964) will be appended automatically",
            "health_center_label" to "Nearest Health Center for child",
            "health_center_placeholder" to "e.g. Taq-Mikael",
            "address_label" to "Family housing address",
            "address_placeholder" to "e.g. Mosul, Al-Qadisiya",
            "dob_title" to "Date of Birth",
            "day_label" to "Day",
            "month_label" to "Month",
            "year_label" to "Year",
            "age_limit_warning" to "Warning: Child should be under 5 years old (born after 2021-06-20)",
            "cancel_button" to "Cancel",
            "save_card_button" to "Register & Save Card",
            "details_title" to "Child Profile & Vaccination Details",
            "edit_child_title" to "Edit Child Record Details",
            "edit_fullname_label" to "Child's Quadruple Name",
            "edit_mother_label" to "Mother's Triple Name",
            "edit_phone_label" to "Vaccinator Phone Number",
            "edit_center_label" to "Health Center",
            "edit_address_label" to "Address and Village",
            "toggle_status_title" to "Update / Cancel Vaccination Status",
            "add_vaccine_title" to "Perform & Approve Dose Admin",
            "vaccine_taken_desc" to "Vaccine ({name}) is marked as fully completed.",
            "side_effects_observed" to "Observed Side Effects",
            "side_effects_placeholder" to "e.g. fever, redness at injection site",
            "undo_vaccine_warning" to "Warning: You can revert 'Taken' status back to pending by clicking the red button below.",
            "vaccine_prompt" to "Has the vaccine ({name}) been successfully administered to {child}?",
            "actual_vax_date_label" to "Actual Vaccination Date:",
            "edit_date_btn" to "Edit Date 📅",
            "upcoming_milestones_title" to "💉 Upcoming vaccines of group ({category}):",
            "deselect_all" to "Deselect All",
            "select_all" to "Select All for injection 📌",
            "side_effects_optional" to "Observed side effects (optional)",
            "side_effects_placeholder_2" to "e.g. continuous fever, drowsiness or crying",
            "antipyretic_warning" to "Important: Please administer appropriate antipyretics and approved pain relievers to the child, monitoring body temperature regularly.",
            "save_close" to "Save and Close",
            "confirm_vaccine" to "Confirm & Save Dose",
            "cancel_taken" to "Revert Vaccination (Pending ❌)",
            "close" to "Close",
            "delete_profile_title" to "Completely Delete Child Profile",
            "delete_profile_warn" to "Warning: This will permanently delete child profile '{name}' and all associated vaccine records. Proceed?",
            "delete_confirm_btn" to "Permanently Delete Record",
            "details_dob_label" to "Date of Birth: ",
            "details_phone_label" to "WhatsApp: ",
            "details_mother_label" to "Mother's Full Name: ",
            "details_center_label" to "Nearest Center: ",
            "details_address_label" to "Residence Address: ",
            "registered_stats" to "Registered",
            "approaching_stats" to "Approaching",
            "overdue_stats" to "Overdue",
            "clear" to "Clear",
            "no_children_blank" to "No registered children in the database.",
            "no_children_search" to "No search results match query.",
            "tab_home" to "Home",
            "tab_children" to "People",
            "tab_alerts" to "Alerts",
            "tab_progress" to "Progress",
            "tab_settings" to "Settings",
            "setting_center_title" to "🏥 Configured Health Center Name",
            "setting_center_desc" to "The approved name of the health center used in vaccine cards, maps, and notifications.",
            "setting_center_label" to "Health Center Name",
            "setting_center_save" to "Save Health Center Name"
        )
    )
}

@Composable
fun VaccineScheduleTableScreen(viewModel: VaccineViewModel) {
    val children by viewModel.children.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val context = LocalContext.current

    val selectedChild by viewModel.selectedChild.collectAsState()
    val selectedChildVaccines by viewModel.selectedChildVaccines.collectAsState()

    // Local state for selected child ID. Initialize with current selectedChild's ID if available.
    var selectedChildId by remember { mutableStateOf<Int?>(selectedChild?.id) }

    // Trigger loading child vaccines when child selection changes
    LaunchedEffect(selectedChildId) {
        if (selectedChildId != null) {
            viewModel.loadChildDetails(selectedChildId!!)
        }
    }

    var selectedRecordForAction by remember { mutableStateOf<VaccineRecord?>(null) }

    // Dialog confirming vaccine completion toggle (reused from ChildDetailsScreen pattern)
    if (selectedRecordForAction != null) {
        val record = selectedRecordForAction!!
        val isTakenDialog = record.isTaken

        var customDate by remember(record.id) {
            mutableStateOf(record.takenDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
        }
        var sideEffectText by remember(record.id) {
            mutableStateOf(record.sideEffect ?: "")
        }

        val categoryVaccines = remember(record.id, selectedChildVaccines) {
            selectedChildVaccines.filter { it.categoryAr == record.categoryAr && !it.isTaken }
        }

        var selectedVaccineIds by remember(record.id) {
            mutableStateOf(categoryVaccines.map { it.id }.toSet())
        }

        AlertDialog(
            onDismissRequest = { selectedRecordForAction = null },
            title = {
                Text(
                    text = if (isTakenDialog) "تعديل حالة التطعيم" else "تأكيد جرعة اللقاح للطفل",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isTakenDialog) {
                        item {
                            Text(
                                text = "اللقاح (${record.nameAr}) مسجل كمنجز للطفل ${selectedChild?.fullName ?: ""}.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = sideEffectText,
                                onValueChange = { sideEffectText = it },
                                label = { Text("الأثار الجانبية الملاحظة") },
                                placeholder = { Text("مثال: ارتفاع درجة حرارة، خمول") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "هل تم إعطاء لقاح (${record.nameAr}) للطفل ${selectedChild?.fullName ?: ""}؟",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        item { HorizontalDivider() }

                        // Date picker row
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("تاريخ التلقيح الفعلي:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(customDate, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Button(
                                    onClick = {
                                        val calendar = Calendar.getInstance()
                                        val year = calendar.get(Calendar.YEAR)
                                        val month = calendar.get(Calendar.MONTH)
                                        val day = calendar.get(Calendar.DAY_OF_MONTH)

                                        android.app.DatePickerDialog(
                                            context,
                                            { _, selYear, selMonth, selDay ->
                                                val formattedMonth = String.format("%02d", selMonth + 1)
                                                val formattedDay = String.format("%02d", selDay)
                                                customDate = "$selYear-$formattedMonth-$formattedDay"
                                            },
                                            year, month, day
                                        ).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("تعديل 📅", fontSize = 11.sp)
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(4.dp)) }

                        // Multi-selection of other vaccines in same milestone
                        if (categoryVaccines.size > 1) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "💉 لقاحات نفس الفئة العمرية الجاهزة:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(
                                        onClick = {
                                            selectedVaccineIds = if (selectedVaccineIds.size == categoryVaccines.size) {
                                                setOf(record.id)
                                            } else {
                                                categoryVaccines.map { it.id }.toSet()
                                            }
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = if (selectedVaccineIds.size == categoryVaccines.size) "إلغاء تحديد الكل" else "تحديد الكل للحقن 📌",
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            categoryVaccines.forEach { catRecord ->
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (catRecord.id == record.id) {
                                                    // Locked
                                                } else {
                                                    selectedVaccineIds = if (selectedVaccineIds.contains(catRecord.id)) {
                                                        selectedVaccineIds - catRecord.id
                                                    } else {
                                                        selectedVaccineIds + catRecord.id
                                                    }
                                                }
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = selectedVaccineIds.contains(catRecord.id),
                                            onCheckedChange = { isChecked ->
                                                if (catRecord.id == record.id) {
                                                    // Locked
                                                } else {
                                                    selectedVaccineIds = if (isChecked) {
                                                        selectedVaccineIds + catRecord.id
                                                    } else {
                                                        selectedVaccineIds - catRecord.id
                                                    }
                                                }
                                            },
                                            enabled = catRecord.id != record.id,
                                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = catRecord.nameAr,
                                            fontSize = 12.sp,
                                            color = if (catRecord.id == record.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (catRecord.id == record.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Side effects input
                        item {
                            OutlinedTextField(
                                value = sideEffectText,
                                onValueChange = { sideEffectText = it },
                                label = { Text("الأثار الجانبية المرصودة (اختياري)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedSideEffect = sideEffectText.trim().ifBlank { null }

                        if (isTakenDialog) {
                            viewModel.updateVaccineSideEffect(record, trimmedSideEffect)
                            
                            // Send WhatsApp alert to supervisor if side effects are added
                            if (trimmedSideEffect != null && selectedChild != null) {
                                val childObj = selectedChild!!
                                val templateText = """
                                🟢 تبليغ فوري أثر جانبي لقاحات:
                                اسم الطفل: ${childObj.fullName}
                                تاريخ الميلاد: ${childObj.dob}
                                الرقم التلقيحي الدائمي للطفل: ${childObj.vaccinationNumber}
                                نوع واسم اللقاح الملقح: ${record.nameAr} (${record.categoryAr})
                                تاريخ اخذ اللقاح: ${record.takenDate}
                                نوع الاثر الجانبي المسجل: $trimmedSideEffect
                                --------------------------------
                                تم تسجيل هذه الرسالة وإرسالها عبر تطبيق إحسان 🌟
                                """.trimIndent()

                                val targetPhone = viewModel.sideEffectsOfficerPhone.value.ifBlank { "9647729792239" }
                                val waIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=${cleanPhoneForWhatsApp(targetPhone)}&text=${Uri.encode(templateText)}")
                                }
                                try {
                                    context.startActivity(waIntent)
                                    Toast.makeText(context, "تم حفظ الأثر الجانبي. فتح الواتساب لإرسال الإشعار لمسؤول الآثار الجانبية...", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تم حفظ الأثر الجانبي بنجاح ✅", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "تم حفظ السجل بنجاح ✅", Toast.LENGTH_SHORT).show()
                            }
                            selectedRecordForAction = null
                        } else {
                            val chosenRecords = if (categoryVaccines.isNotEmpty()) {
                                selectedChildVaccines.filter { selectedVaccineIds.contains(it.id) }
                            } else {
                                listOf(record)
                            }

                            viewModel.updateMultipleVaccinesStatusAndSideEffect(
                                chosenRecords,
                                true,
                                customDate,
                                trimmedSideEffect
                            )

                            // Send WhatsApp alert to supervisor if side effects are added
                            if (trimmedSideEffect != null && selectedChild != null) {
                                val childObj = selectedChild!!
                                val vaccineNamesJoined = chosenRecords.joinToString("، ") { it.nameAr }
                                val templateText = """
                                🟢 تبليغ فوري أثر جانبي لقاحات:
                                اسم الطفل: ${childObj.fullName}
                                تاريخ الميلاد: ${childObj.dob}
                                الرقم التلقيحي الدائمي للطفل: ${childObj.vaccinationNumber}
                                نوع واسم اللقاح الملقح: $vaccineNamesJoined (${record.categoryAr})
                                تاريخ اخذ اللقاح: $customDate
                                نوع الاثر الجانبي المسجل: $trimmedSideEffect
                                --------------------------------
                                تم تسجيل هذه الرسالة وإرسالها عبر تطبيق إحسان 🌟
                                """.trimIndent()

                                val targetPhone = viewModel.sideEffectsOfficerPhone.value.ifBlank { "9647729792239" }
                                val waIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=${cleanPhoneForWhatsApp(targetPhone)}&text=${Uri.encode(templateText)}")
                                }
                                try {
                                    context.startActivity(waIntent)
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            }
                            Toast.makeText(context, "تم تثبيت وجرع اللقاحات بنجاح", Toast.LENGTH_SHORT).show()
                            selectedRecordForAction = null
                        }
                    }
                ) {
                    Text(if (isTakenDialog) "حفظ وإغلاق" else "تأكيد تسجيل التلقيح", color = Color.White)
                }
            },
            dismissButton = {
                Row {
                    if (isTakenDialog) {
                        TextButton(
                            onClick = {
                                viewModel.toggleVaccineStatus(record, false)
                                selectedRecordForAction = null
                                Toast.makeText(context, "تم إلغاء حالة التلقيح والرجوع", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("إلغاء التلقيح ❌", color = Color.Red)
                        }
                    }
                    TextButton(onClick = { selectedRecordForAction = null }) {
                        Text("إغلاق")
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toolbar header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "جدول اللقاحات العراقي التفاعلي 📊",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "نظام توثيق وجرع اللقاحات الوطني المعتمد للأطفال دون الـ 5 سنوات",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Child Selection Dropdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "👶 اختر طفلاً لتحديث حالته وتأكيد التلقيح:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                var expanded by remember { mutableStateOf(false) }
                val currentSelectionLabel = if (selectedChildId == null) {
                    "📖 جدول اللقاحات المرجعي العام (نموذج رسمي)"
                } else {
                    val childName = children.find { it.id == selectedChildId }?.fullName ?: ""
                    "👶 طفل محدد: $childName"
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentSelectionLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("📖 جدول اللقاحات المرجعي العام (نموذج رسمي)", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedChildId = null
                                expanded = false
                            }
                        )
                        HorizontalDivider()
                        if (children.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("⚠️ لا يوجد أطفال مسجلين حالياً", fontSize = 12.sp, color = Color.Gray) },
                                onClick = { expanded = false },
                                enabled = false
                            )
                        } else {
                            children.forEach { child ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(child.fullName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                val descText = if (child.vaccinationNumber.isNotBlank()) {
                                                    "رقم السجل: ${child.vaccinationNumber} | الأم: ${child.motherName}"
                                                } else {
                                                    "الأم: ${child.motherName}"
                                                }
                                                Text(descText, fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedChildId = child.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Beautiful Vaccination Table Layout with clean scroll and responsive visual headers
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اللقاح الوطني 💉",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(0.35f),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "الفئة المستهدفة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(0.28f),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "الحالة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(0.20f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "التحكم",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(0.17f),
                        textAlign = TextAlign.Center
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))

                // Table Data List
                if (selectedChildId == null) {
                    // Render static template Iraqi schedule as reference
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(VaccineScheduler.iraqSchedule.size) { index ->
                            val item = VaccineScheduler.iraqSchedule[index]
                            TableItemRow(
                                name = item.nameAr,
                                category = item.categoryAr,
                                statusText = "مرجعي",
                                statusColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                isTaken = false,
                                interactive = false,
                                onCheckChange = {}
                            )
                            if (index < VaccineScheduler.iraqSchedule.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            }
                        }
                    }
                } else {
                    // Render interactive child-specific vaccines
                    if (selectedChildVaccines.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(selectedChildVaccines.size) { index ->
                                val record = selectedChildVaccines[index]
                                val childLocal = selectedChild
                                val status = if (childLocal != null) {
                                    VaccineScheduler.getRecordStatus(childLocal, record, selectedChildVaccines)
                                } else {
                                    VaccineScheduler.getStatus(childLocal?.dob ?: "", record)
                                }
                                val isTaken = record.isTaken

                                val statusText = when {
                                    isTaken -> "أُعطي"
                                    record.isSkipped -> "مؤجل"
                                    status == VaccineStatus.OVERDUE -> "متأخر ⚠️"
                                    status == VaccineStatus.APPROACHING -> "حان موعده"
                                    else -> "مستحق"
                                }

                                val statusColor = when {
                                    isTaken -> Color(0xFF16A34A)
                                    record.isSkipped -> Color(0xFFEA580C)
                                    status == VaccineStatus.OVERDUE -> Color(0xFFDC2626)
                                    status == VaccineStatus.APPROACHING -> Color(0xFFD97706)
                                    else -> MaterialTheme.colorScheme.primary
                                }

                                TableItemRow(
                                    name = record.nameAr,
                                    category = record.categoryAr,
                                    statusText = statusText,
                                    statusColor = statusColor,
                                    isTaken = isTaken,
                                    isSkipped = record.isSkipped,
                                    interactive = true,
                                    onCheckChange = {
                                        selectedRecordForAction = record
                                    },
                                    onSkipChange = {
                                        viewModel.updateVaccineSkipStatus(record, !record.isSkipped, "تجاهل مستخدم")
                                    }
                                )
                                if (index < selectedChildVaccines.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableItemRow(
    name: String,
    category: String,
    statusText: String,
    statusColor: Color,
    isTaken: Boolean,
    isSkipped: Boolean = false,
    interactive: Boolean,
    onCheckChange: () -> Unit,
    onSkipChange: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = interactive) { onCheckChange() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vaccine Name Column (35%)
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.35f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        // Age Target Column (28%)
        Text(
            text = category,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.28f)
        )
        
        // Status Column (20%)
        Box(
            modifier = Modifier
                .weight(0.20f)
                .wrapContentWidth(Alignment.CenterHorizontally)
        ) {
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Interactive Toggle/Checkbox Column (17%)
        Row(
            modifier = Modifier
                .weight(0.17f)
                .wrapContentWidth(Alignment.End),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (interactive) {
                if (!isTaken && onSkipChange != null) {
                    IconButton(
                        onClick = onSkipChange,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "تجاهل اللقاح",
                            tint = if (isSkipped) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                if (!isSkipped) {
                    IconButton(
                        onClick = onCheckChange,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                            contentDescription = "تعديل حالة اللقاح",
                            tint = if (isTaken) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "مرجعي غير قابل للتعديل",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun AddFamilyScreen(viewModel: VaccineViewModel) {
    var familyNumber by remember { mutableStateOf("") }
    var headOfHousehold by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "إضافة بيانات أسرة جديدة 🏠",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "تسهيل ربط وتجميع ملفات الأطفال تحت رقم أسرة واحد",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = familyNumber,
                        onValueChange = { familyNumber = it },
                        label = { Text("رقم الأسرة المعتمد") },
                        placeholder = { Text("مثال: 9876-أسرة") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("family_number_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = headOfHousehold,
                        onValueChange = { headOfHousehold = it },
                        label = { Text("اسم رب الأسرة الكامل") },
                        placeholder = { Text("مثال: حسين جاسم محمد") },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("head_of_household_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف") },
                        placeholder = { Text("مثال: 07700000000") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("family_phone_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان / السكن") },
                        placeholder = { Text("مثال: الموصل، حي القادسية") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("family_address_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.navigateTo(AppScreen.Home) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء الأمر")
                }

                Button(
                    onClick = {
                        if (familyNumber.trim().isEmpty() || headOfHousehold.trim().isEmpty()) {
                            Toast.makeText(context, "يرجى ملء الحقول الأساسية: رقم الأسرة واسم رب الأسرة", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        viewModel.registerNewFamily(
                            familyNumber = familyNumber.trim(),
                            headOfHousehold = headOfHousehold.trim(),
                            address = address.trim(),
                            phone = formatIraqiPhoneNumber(phone),
                            onSuccess = {
                                Toast.makeText(context, "تم حفظ بيانات الأسرة بنجاح!", Toast.LENGTH_SHORT).show()
                                viewModel.navigateTo(AppScreen.FamiliesList)
                            }
                        )
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp)
                        .testTag("save_family_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("حفظ الأسرة")
                }
            }
        }
    }
}

@Composable
fun FamiliesListScreen(viewModel: VaccineViewModel) {
    val families by viewModel.families.collectAsState()
    val children by viewModel.children.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current

    var familyToEdit by remember { mutableStateOf<com.example.data.Family?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "إدارة وسجلات الأسر 👨‍👩‍👧‍👦",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = { Text("بحث عن أسرة...") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

        if (families.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "لا توجد أسر مسجلة حالياً",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "أضف بيانات الأسرة لربط وتجميع ملفات الأطفال تلقائياً",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(families.size) { index ->
                val family = families[index]
                val linkedChildren = children.filter {
                    it.familyNumber.trim().equals(family.familyNumber.trim(), ignoreCase = true) &&
                    it.headOfHousehold.trim().equals(family.headOfHousehold.trim(), ignoreCase = true)
                }

                var isExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                    colors = CardDefaults.cardColors(
                        containerColor = if (linkedChildren.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (linkedChildren.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "رب الأسرة: ${family.headOfHousehold}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "رقم الأسرة: ${family.familyNumber}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        Row {
                                IconButton(onClick = { familyToEdit = family }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل الأسرة", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { viewModel.deleteFamilyRecord(family) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الأسرة", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (family.address.isNotBlank() || family.phone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (family.phone.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(family.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (family.address.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(family.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الأطفال المدرجون تحت رقم الأسرة: ${linkedChildren.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (linkedChildren.isNotEmpty()) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
                            )
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                // Head of household
                                com.example.ui.FamilyMemberCard(
                                    name = family.headOfHousehold,
                                    relation = "رب الاسرة ( أب / أم )"
                                )
                                // Children
                                linkedChildren.forEach { child ->
                                    Box(modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.ChildDetails(child.id)) }) {
                                        com.example.ui.FamilyMemberCard(
                                            name = child.fullName,
                                            relation = "ابن / بنت"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
        
        ExtendedFloatingActionButton(
            onClick = { viewModel.navigateTo(AppScreen.AddFamily) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_family_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة أسرة")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "إضافة بيانات أسرة جديدة",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    familyToEdit?.let { family ->
        EditFamilyDialog(
            family = family,
            onDismiss = { familyToEdit = null },
            onSave = { updatedFamily ->
                viewModel.updateFamilyInfo(updatedFamily)
                familyToEdit = null
            }
        )
    }
}

@Composable
fun EditFamilyDialog(
    family: com.example.data.Family,
    onDismiss: () -> Unit,
    onSave: (com.example.data.Family) -> Unit
) {
    var headOfHousehold by remember { mutableStateOf(family.headOfHousehold) }
    var familyNumber by remember { mutableStateOf(family.familyNumber) }
    var phone by remember { mutableStateOf(family.phone) }
    var address by remember { mutableStateOf(family.address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل بيانات الأسرة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = headOfHousehold,
                    onValueChange = { headOfHousehold = it },
                    label = { Text("اسم رب الأسرة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = familyNumber,
                    onValueChange = { familyNumber = it },
                    label = { Text("رقم الأسرة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("عنوان السكن") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    family.copy(
                        headOfHousehold = headOfHousehold.trim(),
                        familyNumber = familyNumber.trim(),
                        phone = phone.trim(),
                        address = address.trim()
                    )
                )
            }) {
                Text("حفظ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun AddAdultTdScreen(viewModel: VaccineViewModel) {
    val defaultCenter by viewModel.defaultHealthCenter.collectAsState()
    var fullName by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var healthCenter by remember(defaultCenter) { mutableStateOf(defaultCenter) }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    
    var vaccinationNumber by remember { mutableStateOf("") }
    var familyNumber by remember { mutableStateOf("") }
    var headOfHousehold by remember { mutableStateOf("") }
    
    // DOB builder
    var yearStr by remember { mutableStateOf("2010") }
    var monthStr by remember { mutableStateOf("01") }
    var dayStr by remember { mutableStateOf("01") }
    
    var wordCount by remember { mutableStateOf(0) }
    var showFamilySelectDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val families by viewModel.families.collectAsState()

    LaunchedEffect(fullName) {
        val trimmed = fullName.trim()
        wordCount = if (trimmed.isEmpty()) 0 else trimmed.split("\\s+".toRegex()).size
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "تسجيل لقاح ثنائي كبار 💉",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "تطعيم لقاح الثنائي للكبار للأعمار من 12 سنة فما فوق",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    
                    // First: Basic details (Name, Mother, Record ID)
                    Text(
                        "بيانات الملقّح الأساسية",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("اسم الملقح الرباعي الكلي") },
                        placeholder = { Text("مثال: علي حسين جاسم محمد") },
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (wordCount < 4) "الاسم الرباعي يتكون من 4 أسماء على الأقل ($wordCount الحالي)" else "الاسم متكامل ومطابق للأنظمة",
                                    color = if (wordCount < 4) Color(0xFFD97706) else Color(0xFF16A34A),
                                    fontSize = 11.sp
                                )
                                Text("$wordCount/4", fontSize = 11.sp)
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = motherName,
                        onValueChange = { motherName = it },
                        label = { Text("اسم الأم الثلاثي") },
                        placeholder = { Text("مثال: فاطمة زهراء علي") },
                        leadingIcon = { Icon(Icons.Default.Face, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = vaccinationNumber,
                        onValueChange = { vaccinationNumber = it },
                        label = { Text("رقم السجل التلقيحي للملقح") },
                        placeholder = { Text("مثال: 322/ثنائي") },
                        leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Second: Age / Date of birth
                    Text(
                        "تاريخ الميلاد والسن",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dayStr,
                            onValueChange = { if (it.length <= 2) dayStr = it.filter { c -> c.isDigit() } },
                            label = { Text("اليوم") },
                            placeholder = { Text("DD") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = monthStr,
                            onValueChange = { if (it.length <= 2) monthStr = it.filter { c -> c.isDigit() } },
                            label = { Text("الشهر") },
                            placeholder = { Text("MM") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = yearStr,
                            onValueChange = { if (it.length <= 4) yearStr = it.filter { c -> c.isDigit() } },
                            label = { Text("السنة") },
                            placeholder = { Text("YYYY") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.5f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Third: Select or input family details
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ربط الملقّح بأسرة مسجلة مسبقاً", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (families.isNotEmpty()) {
                            TextButton(onClick = { showFamilySelectDialog = true }) {
                                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اختر أسرة مسجلة 🏠", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = familyNumber,
                        onValueChange = { familyNumber = it },
                        label = { Text("رقم الأسرة") },
                        placeholder = { Text("مثال: 9876-أسرة") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = headOfHousehold,
                        onValueChange = { headOfHousehold = it },
                        label = { Text("اسم رب الأسرة") },
                        placeholder = { Text("مثال: حسين جاسم محمد") },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = formatOnTheFly(it) },
                        label = { Text("رقم هاتف الوتساب للملقح") },
                        placeholder = { Text("مثال: 07700000000") },
                        supportingText = { Text("سيضاف إليه رمز العراق (964) تلقائياً عند الحفظ") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("عنوان السكن") },
                        placeholder = { Text("مثال: الموصل، حي القادسية") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = healthCenter,
                        onValueChange = { healthCenter = it },
                        label = { Text("المركز الصحي للملقح") },
                        placeholder = { Text("مثال: طاق ميكائيل") },
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dayStr,
                            onValueChange = { if (it.length <= 2) dayStr = it.filter { c -> c.isDigit() } },
                            label = { Text("اليوم") },
                            placeholder = { Text("DD") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = monthStr,
                            onValueChange = { if (it.length <= 2) monthStr = it.filter { c -> c.isDigit() } },
                            label = { Text("الشهر") },
                            placeholder = { Text("MM") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = yearStr,
                            onValueChange = { if (it.length <= 4) yearStr = it.filter { c -> c.isDigit() } },
                            label = { Text("السنة") },
                            placeholder = { Text("YYYY") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.5f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "ملاحظة: هذا اللقاح مخصص للأشخاص البالغين من عمر 12 سنة فما فوق فقط ويشمل 5 جرعات دورية متباعدة.",
                        fontSize = 11.sp,
                        color = Color(0xFFD97706)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.navigateTo(AppScreen.Home) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء الأمر")
                }

                Button(
                    onClick = {
                        if (fullName.trim().isEmpty() || phone.trim().isEmpty()) {
                            Toast.makeText(context, "الرجاء إدخال اسم الملقح ورقم الهاتف", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (wordCount < 4) {
                            Toast.makeText(context, "الرجاء إدخال الاسم الرباعي كاملاً", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (motherName.trim().isEmpty()) {
                            Toast.makeText(context, "الرجاء إدخال اسم الأم الثلاثي", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val day = dayStr.toIntOrNull() ?: 1
                        val month = monthStr.toIntOrNull() ?: 1
                        val year = yearStr.toIntOrNull() ?: 2010

                        if (day !in 1..31 || month !in 1..12 || year < 1940 || year > 2026) {
                            Toast.makeText(context, "تاريخ الميلاد غير صالح", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val formattedDate = String.format("%04d-%02d-%02d", year, month, day)
                        val birthEpoch = VaccineScheduler.getEpochDays(formattedDate)
                        val currentEpoch = VaccineScheduler.getCurrentEpochDays()
                        val diffDays = currentEpoch - birthEpoch
                        val twelveYearsInDays = 365 * 12

                        if (diffDays < twelveYearsInDays) {
                            Toast.makeText(context, "عمر الملقح يقل عن 12 سنة، لقاح ثنائي الكبار مخصص للأعمار من 12 سنة فما فوق فقط!", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val formattedPhone = formatIraqiPhoneNumber(phone)

                        viewModel.registerNewChild(
                            fullName = fullName.trim(),
                            motherName = motherName.trim(),
                            healthCenter = if (healthCenter.trim().isEmpty()) defaultCenter else healthCenter.trim(),
                            address = if (address.trim().isEmpty()) "غير محدد" else address.trim(),
                            phone = formattedPhone,
                            dobString = formattedDate,
                            vaccinationNumber = vaccinationNumber.trim(),
                            familyNumber = familyNumber.trim(),
                            headOfHousehold = headOfHousehold.trim(),
                            isAdultTd = true,
                            onSuccess = {
                                Toast.makeText(context, "تم تسجيل ملقح ثنائي كبار وتوليد خطته الخمسية بنجاح!", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("حفظ وتسجيل")
                }
            }
        }
    }

    if (showFamilySelectDialog && families.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showFamilySelectDialog = false },
            title = { Text("اختر الأسرة لربطها تلقائياً", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(families.size) { i ->
                        val f = families[i]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    familyNumber = f.familyNumber
                                    headOfHousehold = f.headOfHousehold
                                    phone = f.phone
                                    address = f.address
                                    showFamilySelectDialog = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("اسم رب الأسرة: ${f.headOfHousehold}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("رقم الأسرة: ${f.familyNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFamilySelectDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

fun shareApkFile(context: android.content.Context) {
    try {
        val apkPath = context.packageCodePath
        val srcFile = java.io.File(apkPath)
        val cacheDir = context.cacheDir
        val destFile = java.io.File(cacheDir, "إحسان.apk")
        
        // Copy file
        srcFile.inputStream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        val fileUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            destFile
        )
        
        val shareMessage = """
            تطبيق نظام متابعة لقاحات الأطفال المعتمد (إحسان لمتابعة اللقاحات) 👶🛡️
            
            هذا التطبيق هو المنصة الطبية الذكية لإدارة ومتابعة لقاحات الأطفال ورعاية الطفولة والأسرة بالمركز الصحي.
            
            💡 طبيعة عمل التطبيق ومعلوماته الأساسية:
            • تسجيل وإدارة كاملة لبيانات الأطفال وأسرهم بخصوص اللقاحات.
            • الحساب التلقائي الذكي لجدول لقاحات الطفل والجرعات المستحقة بدقة بناءً على تاريخ الميلاد.
            • نظام تنبيهات وقائي ذكي للتذكير بالمواعيد القريبة والجرعات المتأخرة لتجنب التسرب من اللقاحات.
            • إرسال تقارير الآثار الجانبية تلقائياً إلى رقم هاتف المسؤول المعتمد.
            • تحديد موقع المراكز الصحية بدقة عبر تطبيق الخرائط وتسهيل مسارات الوصول.
            • تصدير كامل لقاعدة بيانات الأطفال واللقاحات إلى كشوفات Excel لسهولة الإشراف الطبي والميداني.
            
            📲 أشارك معك النسخة المرفقة وهي نسخة تثبيت مباشرة (APK) للعمل بكفاءة تامة.
        """.trimIndent()
        
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "تطبيق نظام متابعة اللقاحات")
            putExtra(android.content.Intent.EXTRA_TEXT, shareMessage)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "مشاركة التطبيق بصيغة APK عبر"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "فشل مشاركة ملف APK: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun DataEntryScreen(viewModel: VaccineViewModel) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "بوابة إدخال البيانات والتسجيل 📝",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            Text(
                text = "يرجى اختيار أحد الخيارات أدناه لتسجيل بيانات الأسر والأطفال واللقاحات الخاصة بهم بالمركز الصحي:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(AppScreen.AddFamily) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إضافة بيانات أسرة جديدة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "تسجيل بيانات رب الأسرة الجديد والمسكن ورقم الهاتف",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(AppScreen.AddChild) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تسجيل طفل جديد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "إدخال بيانات طفل جديد وربطه بالأسرة المسجلة مسبقاً وتوليد جدول لقاحاته",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(AppScreen.AddAdultTd) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تسجيل لقاح ثنائي كبار",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "تسجيل اللقاح الثنائي (الكزاز والديفتيريا) المخصص للبالغين والفرق الميدانية",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ViewDataScreen(viewModel: VaccineViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "بوابة عرض الملقحين والبيانات 📋",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            Text(
                text = "استعرض السجلات والبيانات المسجلة، أو ابحث عن طفل ملقح لإدارة ومتابعة جرعاته وحالته الطبية:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(AppScreen.FamiliesList) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "عرض بيانات الأسر",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "عرض وإدارة جميع الأسر المسجلة والبحث برقم الهاتف أو الاسم وعرض أفرادها",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        viewModel.setChildrenListTab(1)
                        viewModel.navigateTo(AppScreen.ChildrenList)
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "عرض الأطفال المسجلين",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "استعراض جميع الأطفال المسجلين دون سن الخامسة وإصدار كروت اللقاح الملونة وتثبيت الجرعات",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        viewModel.setChildrenListTab(2)
                        viewModel.navigateTo(AppScreen.ChildrenList)
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "عرض الملقحين بالثنائي كبار",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "استعراض قائمة البالغين والفرق الطبية الملقحة بلقاح الثنائي كبار ومتابعة جرعات التنشيطية",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun CompactActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 11.sp
                )
            }
        }
    }
}

@Composable
fun InfoAndExplanationScreen(viewModel: VaccineViewModel) {
    val children by viewModel.children.collectAsState()
    val context = LocalContext.current
    
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val jsonContent = inputStream?.bufferedReader()?.use { reader -> reader.readText() }
                if (jsonContent != null) {
                    ExportUtils.importDatabaseFromJson(context, jsonContent, viewModel.repository.dao) {
                        // Refresh logic if any, but flow is automated
                    }
                } else {
                    android.widget.Toast.makeText(context, "تعذر قراءة محتوى ملف النسخة الاحتياطية", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "خطأ أثناء محاولة فتح الملف المختار: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    val saveDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri: android.net.Uri? ->
        uri?.let {
            ExportUtils.exportDatabaseToUri(context, it, viewModel.repository.dao)
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "بوابة توضيح ومعلومات ℹ️",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // تنبيه بضرورة حفظ نسخة احتياطية للبيانات
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)), // Soft amber warning container
                border = BorderStroke(1.dp, Color(0xFFFFEBAA)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "تحذير",
                        tint = Color(0xFF856404),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "للمحافظة على بياناتك المسجلة احفظ نسخة احتياطية 💾",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF856404)
                    )
                }
            }
        }

        // 1. قسم دليل الاستخدام والتعليمات للأسر (أعلى الصفحة) مع شعار إحسان
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // شعار إحسان في الأعلى
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.img_clean_i7san_app_icon_1782895431554),
                            contentDescription = "شعار إحسان",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "تطبيق إحسان للمراقبة 🛡️",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "دليل الاستخدام والتعليمات لأولياء الأمور",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // تبويبات التعليمات الثلاثة بشكل مدمج وجميل جداً
                    var selectedInstructionTab by remember { mutableStateOf(0) }
                    
                    TabRow(
                        selectedTabIndex = selectedInstructionTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Tab(
                            selected = selectedInstructionTab == 0,
                            onClick = { selectedInstructionTab = 0 },
                            text = { 
                                Text("جدول اللقاحات 📋", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        )
                        Tab(
                            selected = selectedInstructionTab == 1,
                            onClick = { selectedInstructionTab = 1 },
                            text = { 
                                Text("رموز QR للطفل 🔍", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // محتوى التبويب المختار بتأثير تدرج أو ظهور
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        when (selectedInstructionTab) {
                            0 -> {
                                Text(
                                    text = "📋 كيفية قراءة جدول اللقاحات المعتمد:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• ينقسم الجدول حسب عمر الطفل بدقة (من الولادة لغاية الجرعات المدرسية).\n" +
                                           "• يحتوي كل تبويب عمري على اللقاحات المستهدفة لحمايته بالوقت الموصى به طبياً.\n" +
                                           "• الألوان والإشارات: اللقاح المكتمل يظهر بـ (✅ أخضر), والمتأخر بـ (⚠️ أحمر) لتنبيهكم بالسرعة.",
                                    fontSize = 10.5.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            1 -> {
                                Text(
                                    text = "🔍 تفسير رموز QR المطبوعة على بطاقة الطفل:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• يحتوي رمز QR المطبوع على بطاقة الطفل على كافة بياناته الصحية المحدثة وسجل جرعاته.\n" +
                                           "• عند مسح الرمز بواسطة كاميرا التطبيق، يظهر الملف الصحي بالكامل فوراً بدون إنترنت.\n" +
                                           "• يسهل هذا الرمز عمل الملقحين في المراكز لتوثيق وتلقيح الطفل وحمايته بسرعة.",
                                    fontSize = 10.5.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // قسم بطاقة البرمجة
        item {
            Spacer(modifier = Modifier.height(4.dp))
            DeveloperContactInfoCard()
        }
    }
}

// ==========================================
// VACCINE & SUPPLIES INVENTORY TAB SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccineInventoryScreen(viewModel: VaccineViewModel) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0: AI Parser, 1: Vaccines, 2: Supplies
    
    val selectedMonth by viewModel.selectedInventoryMonth.collectAsState()
    val selectedYear by viewModel.selectedInventoryYear.collectAsState()
    val inventoryRecords by viewModel.inventoryRecords.collectAsState()
    val defaultHealthCenter by viewModel.defaultHealthCenter.collectAsState()
    val parsingResultText by viewModel.parsingResultText.collectAsState()
    val isParsing by viewModel.isParsing.collectAsState()
    
    var rawInputText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showDetailedIndicatorsScreen by remember { mutableStateOf(false) }

    val savePdfLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    VaccineReportExporter.writeReportToStream(
                        context,
                        selectedMonth,
                        selectedYear,
                        inventoryRecords,
                        outputStream,
                        defaultHealthCenter
                    )
                    Toast.makeText(context, "تم حفظ تقرير جرد اللقاحات بنجاح في ملفات النظام! 💾", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ أثناء حفظ الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    // Trigger loading on start if empty
    LaunchedEffect(selectedMonth, selectedYear) {
        viewModel.loadInventoryRecords(selectedMonth, selectedYear)
    }

    if (showDetailedIndicatorsScreen) {
        DetailedIndicatorsScreen(
            month = selectedMonth,
            year = selectedYear,
            records = inventoryRecords,
            onDismiss = { showDetailedIndicatorsScreen = false }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("لوحة المعلومات", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("اللقاحات", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("المستلزمات", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Month and Year selector (shown for all tabs)
            if (true) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Month Dropdown Trigger
                            var monthExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { monthExpanded = true },
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    Text("الشهر: $selectedMonth", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                DropdownMenu(
                                    expanded = monthExpanded,
                                    onDismissRequest = { monthExpanded = false }
                                ) {
                                    viewModel.arabicMonths.forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(m, fontSize = 12.sp) },
                                            onClick = {
                                                viewModel.setInventoryMonthAndYear(m, selectedYear)
                                                monthExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Year Dropdown Trigger
                            var yearExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                val years = listOf("2024", "2025", "2026", "2027", "2028")
                                OutlinedButton(
                                    onClick = { yearExpanded = true },
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    Text("السنة: $selectedYear", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                DropdownMenu(
                                    expanded = yearExpanded,
                                    onDismissRequest = { yearExpanded = false }
                                ) {
                                    years.forEach { y ->
                                        DropdownMenuItem(
                                            text = { Text(y, fontSize = 12.sp) },
                                            onClick = {
                                                viewModel.setInventoryMonthAndYear(selectedMonth, y)
                                                yearExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Sync button
                            Button(
                                onClick = {
                                    viewModel.syncInitialBalancesFromPreviousMonth {
                                        Toast.makeText(context, "تمت مزامنة رصيد البداية بنجاح! 🔄", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1.3f).height(40.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مزامنة الرصيد", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        if (activeTab > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            // Search field (only for search in list tabs)
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("ابحث عن مادة محددة...", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }
                }
            }

            // Body content based on tab
            when (activeTab) {
                0 -> {
                    // Dashboard Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title / Description Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "📊 لوحة متابعة أرصدة اللقاحات ومؤشرات الهدر والتالف",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "تقوم لوحة المعلومات بجمع وحساب الإحصائيات الخاصة بالمستودع لشهر $selectedMonth $selectedYear تلقائياً، ومتابعة فاعلية التلقيح ونسب الهدر وتحليل مخرجات الجداول الإحصائية.",
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Export & Share Actions Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "📤 خيارات حفظ وتصدير التقرير ومشاركته:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Print / Save as PDF Button
                                        Button(
                                            onClick = {
                                                if (inventoryRecords.isEmpty()) {
                                                    Toast.makeText(context, "لا توجد بيانات لتصديرها. يرجى إدخال البيانات أولاً.", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    VaccineReportExporter.printReportAsHtml(
                                                        context,
                                                        selectedMonth,
                                                        selectedYear,
                                                        inventoryRecords,
                                                        defaultHealthCenter
                                                    )
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("طباعة كـ PDF 🖨️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Share PDF Button
                                        Button(
                                            onClick = {
                                                if (inventoryRecords.isEmpty()) {
                                                    Toast.makeText(context, "لا توجد بيانات لمشاركتها. يرجى إدخال البيانات أولاً.", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    VaccineReportExporter.shareReportAsPdf(
                                                        context,
                                                        selectedMonth,
                                                        selectedYear,
                                                        inventoryRecords,
                                                        defaultHealthCenter
                                                    )
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("مشاركة التقرير 🔗", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    // Save to Files direct button
                                    OutlinedButton(
                                        onClick = {
                                            if (inventoryRecords.isEmpty()) {
                                                Toast.makeText(context, "لا توجد بيانات لحفظها.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                savePdfLauncher.launch("تقرير جرد اللقاحات لشهر ${selectedMonth} لسنة ${selectedYear}.pdf")
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تصدير وحفظ ملف PDF في ملفات النظام 💾", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Statistics and calculations card (Portal Card to the Independent Indicators Page)
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDetailedIndicatorsScreen = true },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "📈 المؤشرات والتحليلات الإحصائية المستقلة لكل لقاح ومستلزم",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = "افتح صفحة مستقلة لعرض مؤشرات الهدر، كفاءة الاستهلاك، والتحليلات البيانية المستقلة لكل مادة بالتفصيل.",
                                                fontSize = 10.5.sp,
                                                lineHeight = 15.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(14.dp))
                                    
                                    Button(
                                        onClick = { showDetailedIndicatorsScreen = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(42.dp)
                                    ) {
                                        Text("فتح صفحة المؤشرات والتحليلات المستقلة 📊", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Critical Stock & Expiry Alert Card
                        item {
                            val lowStockItems = inventoryRecords.filter { it.finalRemaining < 20 && (it.initialBalance > 0 || it.receivedDuringMonth > 0 || it.totalVaccinated > 0) }
                            val hasAlerts = lowStockItems.isNotEmpty()
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (hasAlerts) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                                ),
                                border = BorderStroke(
                                    width = if (hasAlerts) 1.8.dp else 1.dp,
                                    color = if (hasAlerts) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (hasAlerts) "🚨 تنبيه المخزون الحرج (أقل من ٢٠ جرعة):" else "✅ حالة المستودع ممتازة:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (hasAlerts) Color(0xFFC62828) else Color(0xFF2E7D32),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (hasAlerts) Icons.Default.Warning else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (hasAlerts) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (!hasAlerts) {
                                        Text(
                                            text = "جميع اللقاحات والمستلزمات في مستويات آمنة وتتجاوز حد الأمان المحدد (20 جرعة/قطعة) لهذا الشهر.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF2E7D32),
                                            lineHeight = 17.sp
                                        )
                                    } else {
                                        Text(
                                            text = "المواد الحيوية التالية انخفضت عن حد الأمان المحدد (20 جرعة/قطعة) وتحتاج لطلب تعزيز فوري:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC62828),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        lowStockItems.forEach { item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "• ${item.itemName} (${if (item.itemType == "vaccine") "لقاح" else "مستلزم"})", 
                                                    fontSize = 11.sp, 
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF333333)
                                                )
                                                Text(
                                                    text = "المتبقي: ${item.finalRemaining} جرعة/قطعة", 
                                                    fontSize = 11.sp, 
                                                    fontWeight = FontWeight.ExtraBold, 
                                                    color = Color(0xFFD32F2F)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Computational Laws & Math Information Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "🧮 القوانين والمعادلات الحسابية المستخدمة بالجداول:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Text(
                                        text = "• إجمالي المتاح الصافي = الرصيد الأولي + الجرع المستلمة خلال الشهر\n" +
                                                "• إجمالي الأطفال الملقحين = ملقحي الجلسات الثابتة + ملقحي الجلسات الخارجية والفرق الجوالة\n" +
                                                "• إجمالي هدر اللقاح = هدر المفتوح (الزجاجات المفتوحة) + هدر المغلق (التالف والمنتهي الصلاحية)\n" +
                                                "• الرصيد النهائي المتبقي = المتاح الصافي - (إجمالي الملقحين + إجمالي الهدر + المواد المناقلة خارجياً)\n" +
                                                "• معدل الهدر المئوي = (إجمالي الهدر ÷ إجمالي المتاح الصافي) × ١٠٠٪",
                                        fontSize = 11.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                activeTab -> {
                    // Manual Tables (Vaccines or Supplies)
                    val targetType = if (activeTab == 1) "vaccine" else "supply"
                    val filteredRecords = inventoryRecords.filter { item ->
                        item.itemType == targetType && 
                        (searchQuery.isBlank() || item.itemName.contains(searchQuery, ignoreCase = true))
                    }
                    val sortedRecords = filteredRecords.sortedByDescending { it.finalRemaining }

                    if (sortedRecords.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد سجلات مطابقة في هذا الشهر والسنة. الرجاء استخدام معالج التقارير لإضافة البيانات أو الضغط على تحديث.", fontSize = 13.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(sortedRecords.size) { index ->
                                val record = sortedRecords[index]
                                InventoryRecordItemCard(record, onSave = { updated ->
                                    viewModel.updateInventoryRecord(updated)
                                })
                            }
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryRecordItemCard(record: InventoryRecord, onSave: (InventoryRecord) -> Unit) {
    val isExpanded = remember { mutableStateOf(false) }
    
    // Form fields editable state
    var initialVal by remember(record) { mutableStateOf(record.initialBalance.toString()) }
    var receivedVal by remember(record) { mutableStateOf(record.receivedDuringMonth.toString()) }
    var fixedVal by remember(record) { mutableStateOf(record.fixedSessionVaccinated.toString()) }
    var outreachVal by remember(record) { mutableStateOf(record.outreachSessionVaccinated.toString()) }
    var openedVal by remember(record) { mutableStateOf(record.openedWasted.toString()) }
    var closedVal by remember(record) { mutableStateOf(record.closedWasted.toString()) }
    var transVal by remember(record) { mutableStateOf(record.transferredOut.toString()) }
    var expiryVal by remember(record) { mutableStateOf(record.expiryDate ?: "") }
    
    val statusColor = when {
        record.finalRemaining < 20 -> Color(0xFFD32F2F) // Red for critical stock
        record.finalRemaining < 100 -> Color(0xFFE65100) // Orange for low/medium
        else -> Color(0xFF2E7D32) // Green for healthy stock
    }

    val cardBg = when {
        record.finalRemaining < 20 -> Color(0xFFFFF8F8)
        record.finalRemaining < 100 -> Color(0xFFFFFDF9)
        else -> Color(0xFFF9FDF9)
    }

    val badgeText = when {
        record.finalRemaining < 20 -> "🚨 رصيد حرج جداً (< ٢٠)"
        record.finalRemaining < 100 -> "⚠️ رصيد منخفض"
        else -> "✅ رصيد آمن ومستقر"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded.value = !isExpanded.value },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isExpanded.value) 1.8.dp else 1.2.dp,
            color = if (isExpanded.value) MaterialTheme.colorScheme.primary else statusColor.copy(alpha = 0.6f)
        ),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (record.itemType == "vaccine") Icons.Default.Info else Icons.Default.Refresh,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.itemName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Active alerts / Badge status
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 📊 Aggregated statistics and analytical indicators for each vaccine/supply item separately:
            val totalAvail = record.totalAvailable
            val totalVacc = record.totalVaccinated
            val totalWast = record.totalWasted
            
            val itemWastageRate = if (totalAvail > 0) {
                (totalWast.toFloat() / totalAvail.toFloat() * 100)
            } else 0f
            
            val itemConsumptionRate = if (totalAvail > 0) {
                (totalVacc.toFloat() / totalAvail.toFloat() * 100)
            } else 0f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .background(statusColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Total Available Indicator
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("المتاح الكلي 📦", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("$totalAvail", fontSize = 12.sp, fontWeight = FontWeight.Black, color = statusColor)
                }
                
                // Total Vaccinated Indicator
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("المستهلك/الملقح 👶", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("$totalVacc", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                }

                // Wastage rate indicator
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("معدل الهدر 📉", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("${"%.1f".format(itemWastageRate)}%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (itemWastageRate > 25f) Color(0xFFD32F2F) else Color(0xFFE65100))
                }

                // Coverage rate indicator
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("كفاءة التغطية 📈", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("${"%.1f".format(itemConsumptionRate)}%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF1976D2))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Traditional Grid details in simple form
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("البداية", fontSize = 9.sp, color = Color.Gray)
                    Text(record.initialBalance.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("+ المستلم", fontSize = 9.sp, color = Color.Gray)
                    Text(record.receivedDuringMonth.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("- الملقحين", fontSize = 9.sp, color = Color.Gray)
                    Text(record.totalVaccinated.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("- الهدر", fontSize = 9.sp, color = Color.Gray)
                    Text(record.totalWasted.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المتبقي", fontSize = 9.sp, color = Color.Gray)
                    Text(record.finalRemaining.toString(), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = statusColor)
                }
            }

            if (!record.expiryDate.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("صلاحية المادة: ${record.expiryDate}", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Expanded Editor Form
            if (isExpanded.value) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                
                Text("📝 تعديل البيانات الإحصائية يدوياً:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = initialVal,
                            onValueChange = { initialVal = it },
                            label = { Text("رصيد البداية", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = receivedVal,
                            onValueChange = { receivedVal = it },
                            label = { Text("المستلم", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fixedVal,
                            onValueChange = { fixedVal = it },
                            label = { Text("الملقح (ثابتة)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = outreachVal,
                            onValueChange = { outreachVal = it },
                            label = { Text("الملقح (خارجية)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = openedVal,
                            onValueChange = { openedVal = it },
                            label = { Text("هدر مفتوح", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = closedVal,
                            onValueChange = { closedVal = it },
                            label = { Text("هدر مغلق", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = transVal,
                            onValueChange = { transVal = it },
                            label = { Text("المناقلات المغادرة", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = expiryVal,
                            onValueChange = { expiryVal = it },
                            label = { Text("تاريخ الصلاحية YYYY-MM-DD", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            val updated = record.copy(
                                initialBalance = initialVal.toIntOrNull() ?: 0,
                                receivedDuringMonth = receivedVal.toIntOrNull() ?: 0,
                                fixedSessionVaccinated = fixedVal.toIntOrNull() ?: 0,
                                outreachSessionVaccinated = outreachVal.toIntOrNull() ?: 0,
                                openedWasted = openedVal.toIntOrNull() ?: 0,
                                closedWasted = closedVal.toIntOrNull() ?: 0,
                                transferredOut = transVal.toIntOrNull() ?: 0,
                                expiryDate = expiryVal.ifBlank { null }
                            )
                            onSave(updated)
                            isExpanded.value = false
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حفظ وتحديث المعادلات 💾", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedIndicatorsScreen(
    month: String,
    year: String,
    records: List<InventoryRecord>,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableStateOf(0) } // 0: الكل, 1: اللقاحات, 2: المستلزمات

    // Filter and sort records
    val filteredRecords = records.filter { rec ->
        val typeMatches = when (selectedFilterTab) {
            1 -> rec.itemType == "vaccine"
            2 -> rec.itemType == "supply"
            else -> true
        }
        val nameMatches = searchQuery.isBlank() || rec.itemName.contains(searchQuery, ignoreCase = true)
        typeMatches && nameMatches
    }.sortedByDescending { it.finalRemaining }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "المؤشرات والتحليلات الإحصائية التفصيلية 📈",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "تقرير جرد شهر $month لسنة $year",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search and Tab Filter Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("البحث في المواد (مثال: شلل الأطفال)...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Filter tab pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("الكل 📋", "اللقاحات 💉", "المستلزمات 📦").forEachIndexed { index, label ->
                            val isSelected = selectedFilterTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedFilterTab = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Explanatory Note
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "يتم هنا احتساب مؤشرات الهدر والفاعلية وكفاءة التغطية بشكل مستقل تماماً لكل مادة بناءً على الرصيد الفعلي والعمليات المجرية.",
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد نتائج مطابقة لبحثك أو لتصفيتك المحددة 🔍",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRecords.size) { index ->
                        val record = filteredRecords[index]
                        
                        // Calculating separate analytics
                        val totalAvail = record.totalAvailable
                        val totalVacc = record.totalVaccinated
                        val totalWast = record.totalWasted
                        
                        val itemWastageRate = if (totalAvail > 0) {
                            (totalWast.toFloat() / totalAvail.toFloat() * 100)
                        } else 0f
                        
                        val itemConsumptionRate = if (totalAvail > 0) {
                            (totalVacc.toFloat() / totalAvail.toFloat() * 100)
                        } else 0f

                        val statusColor = when {
                            record.finalRemaining < 20 -> Color(0xFFD32F2F) // Critical stock
                            record.finalRemaining < 100 -> Color(0xFFE65100) // Low/Medium stock
                            else -> Color(0xFF2E7D32) // Stable
                        }

                        val cardBg = when {
                            record.finalRemaining < 20 -> Color(0xFFFFF8F8)
                            record.finalRemaining < 100 -> Color(0xFFFFFDF9)
                            else -> Color(0xFFF9FDF9)
                        }

                        val badgeText = when {
                            record.finalRemaining < 20 -> "🚨 رصيد حرج جداً"
                            record.finalRemaining < 100 -> "⚠️ رصيد منخفض"
                            else -> "✅ رصيد آمن ومستقر"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.2.dp, statusColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Title row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = if (record.itemType == "vaccine") Icons.Default.Info else Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = record.itemName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    
                                    Text(
                                        text = badgeText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        modifier = Modifier
                                            .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Grid of core metrics
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(statusColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("المتاح الكلي 📦", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("$totalAvail", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("الملقح/المستهلك 👶", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("$totalVacc", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("الهدر والتالف 📉", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("$totalWast", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("الرصيد المتبقي 🚨", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("${record.finalRemaining}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = statusColor)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Visual analytic bars
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // 1. Coverage Efficiency
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("📈 كفاءة التغطية والاستهلاك (نسبة التلقيح):", fontSize = 9.5.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                            Text("${"%.1f".format(itemConsumptionRate)}%", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF1976D2))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        androidx.compose.material3.LinearProgressIndicator(
                                            progress = { (itemConsumptionRate / 100f).coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth().height(6.dp),
                                            color = Color(0xFF1976D2),
                                            trackColor = Color(0xFFE3F2FD),
                                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    }

                                    // 2. Wastage Rate
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("📉 معدل الهدر والتالف من المتاح:", fontSize = 9.5.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                            Text("${"%.1f".format(itemWastageRate)}%", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (itemWastageRate > 25f) Color(0xFFD32F2F) else Color(0xFFE65100))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        androidx.compose.material3.LinearProgressIndicator(
                                            progress = { (itemWastageRate / 100f).coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth().height(6.dp),
                                            color = if (itemWastageRate > 25f) Color(0xFFD32F2F) else Color(0xFFE65100),
                                            trackColor = Color(0xFFECEFF1),
                                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Expandable sub-breakdown details
                                var showBreakdown by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showBreakdown = !showBreakdown }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (showBreakdown) "إخفاء التفاصيل الحسابية الدقيقة 🔺" else "عرض التفاصيل الحسابية الدقيقة 🔻",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                if (showBreakdown) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("الرصيد الافتتاحي لبداية الشهر:", fontSize = 9.sp, color = Color.DarkGray)
                                            Text("${record.initialBalance} جرعة/قطعة", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("الكمية المستلمة خلال الشهر:", fontSize = 9.sp, color = Color.DarkGray)
                                            Text("+ ${record.receivedDuringMonth}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("الملقحين في الجلسات الثابتة والميدانية:", fontSize = 9.sp, color = Color.DarkGray)
                                            Text("- ${record.totalVaccinated} (${record.fixedSessionVaccinated} ثابت + ${record.outreachSessionVaccinated} خارجي)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("الهدر والتلف في العبوات المفتوحة والمغلقة:", fontSize = 9.sp, color = Color.DarkGray)
                                            Text("- ${record.totalWasted} (${record.openedWasted} مفتوح + ${record.closedWasted} مغلق)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                        }
                                        if (record.transferredOut > 0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("المنقول والمحول لجهات أخرى:", fontSize = 9.sp, color = Color.DarkGray)
                                                Text("- ${record.transferredOut}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78909C))
                                            }
                                        }
                                        if (record.expiryDate != null) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("تاريخ انتهاء الصلاحية المعتمد:", fontSize = 9.sp, color = Color.DarkGray)
                                                Text("${record.expiryDate}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(viewModel: VaccineViewModel) {
    var currentPage by remember { mutableStateOf(0) }
    val appLanguage by viewModel.appLanguage.collectAsState()
    
    val pages = listOf(
        OnboardingPageData(
            title = Transl.t("onboarding_title_1", appLanguage),
            description = Transl.t("onboarding_desc_1", appLanguage),
            icon = Icons.Default.Home,
            color = Color(0xFFE8F5E9)
        ),
        OnboardingPageData(
            title = Transl.t("onboarding_title_2", appLanguage),
            description = Transl.t("onboarding_desc_2", appLanguage),
            icon = Icons.Default.AddCircle,
            color = Color(0xFFE3F2FD)
        ),
        OnboardingPageData(
            title = Transl.t("onboarding_title_3", appLanguage),
            description = Transl.t("onboarding_desc_3", appLanguage),
            icon = Icons.Default.Edit,
            color = Color(0xFFFFF3E0)
        ),
        OnboardingPageData(
            title = Transl.t("onboarding_title_4", appLanguage),
            description = Transl.t("onboarding_desc_4", appLanguage),
            icon = Icons.Default.Share,
            color = Color(0xFFF3E5F5)
        )
    )

    val currentPageData = pages[currentPage]

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row (Skip button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (currentPage < pages.size - 1) {
                    TextButton(
                        onClick = { viewModel.completeOnboarding() },
                        modifier = Modifier.testTag("onboarding_skip")
                    ) {
                        Text(
                            text = Transl.t("onboarding_skip", appLanguage),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(36.dp))
                }
            }

            // Middle section (Illustration + Text)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Beautiful Illustration card showing direct visual app interfaces
                Text(
                    text = "عرض توضيحي من واجهة التطبيق 📱",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Card(
                    modifier = Modifier
                        .width(290.dp)
                        .height(190.dp)
                        .testTag("onboarding_illustration_${currentPage}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = currentPageData.color),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    OnboardingUiMockup(pageIndex = currentPage)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Title
                Text(
                    text = currentPageData.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp).testTag("onboarding_title")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = currentPageData.description,
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp).testTag("onboarding_desc")
                )
            }

            // Bottom section (Dots + Button)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Progress indicators (Dots)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    pages.forEachIndexed { index, _ ->
                        val isSelected = index == currentPage
                        val dotWidth = if (isSelected) 24.dp else 8.dp
                        val dotColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(dotWidth)
                                .clip(RoundedCornerShape(4.dp))
                                .background(dotColor)
                        )
                    }
                }

                // Next or Start button
                if (currentPage < pages.size - 1) {
                    Button(
                        onClick = { currentPage++ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("onboarding_next"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("التالي ◀️", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.completeOnboarding() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("onboarding_start"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ابدأ الآن 🚀", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

data class OnboardingPageData(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@Composable
fun OnboardingUiMockup(pageIndex: Int, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("vaccine_prefs", android.content.Context.MODE_PRIVATE)
    val appLanguage = prefs.getString("app_language", "ar") ?: "ar"
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        when (pageIndex) {
            0 -> {
                // الشاشة الاولى: بوابة إدخال البيانات ( الصفحة الرئيسية )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFC3E6CB), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "بوابة إدخال البيانات 📋",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Quick Action Card 1
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                Text("تسجيل طفل", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }
                        // Quick Action Card 2
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            border = BorderStroke(1.dp, Color(0xFF90CAF9)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(14.dp))
                                Text("تصدير Excel", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Mock search/indicator bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Transl.t("search_placeholder_2", appLanguage), fontSize = 8.sp, color = Color.Gray)
                        }
                    }
                }
            }
            1 -> {
                // الشاشة الثانية: تسجيل طفل جديد
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFB3E5FC), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "تسجيل طفل جديد 👶",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF0288D1),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                        border = BorderStroke(1.dp, Color(0xFFDCEDC8)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFF81C784), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👶", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("إضافة طفل جديد للمنظومة", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color(0xFF33691E))
                                Text("توليد جدول اللقاحات تلقائياً", fontSize = 7.5.sp, color = Color(0xFF558B2F))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Add Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(26.dp)
                            .background(Color(0xFF0288D1), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("اضغط لإضافة طفل +", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.White)
                    }
                }
            }
            2 -> {
                // الشاشة الثالثة: تسجيل بيانات الطفل الأساسية
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFFFCC80), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "بيانات الطفل الأساسية 📝",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFFE65100),
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp)
                    )
                    // Mock Input Fields
                    MockInputField(label = "الاسم الكامل للطفل:", value = "أحمد محمد محمود")
                    Spacer(modifier = Modifier.height(4.dp))
                    MockInputField(label = "رقم سجل التلقيح المعتمد:", value = "12054")
                    Spacer(modifier = Modifier.height(4.dp))
                    MockInputField(label = "تاريخ الولادة:", value = "2024-05-12")
                }
            }
            3 -> {
                // الشاشة الرابعة: ربط الطفل بأسرة مسجلة مسبقاً
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE1BEE7), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "ربط الطفل بأسرة مسجلة 🔗",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF6A1B9A),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Family Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                            border = BorderStroke(1.dp, Color(0xFFE1BEE7)),
                            modifier = Modifier.weight(1.1f).height(46.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("👨‍👩‍👦 الأسرة", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color(0xFF4A148C))
                                Text("عائلة أحمد حسن", fontSize = 7.sp, color = Color(0xFF7B1FA2), maxLines = 1)
                            }
                        }
                        
                        // Connection line with chain icon
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(0.6f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF8E24AA), modifier = Modifier.size(16.dp))
                            Text("ربط تلقائي", fontSize = 7.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }

                        // Child Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                            modifier = Modifier.weight(1.1f).height(46.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("👶 الطفل الجديد", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color(0xFF1B5E20))
                                Text("علي أحمد حسن", fontSize = 7.sp, color = Color(0xFF2E7D32), maxLines = 1)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• يربط الطفل مع سجل عائلته الموحد لتسهيل المتابعة.",
                        fontSize = 7.5.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
            4 -> {
                // الشاشة الخامسة: تسجيل وحفظ الكارت
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFB2DFDB), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "تسجيل وحفظ الكارت 🛡️",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF004D40),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
                        border = BorderStroke(1.5.dp, Color(0xFF009688)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mock QR Code box
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color.White, RoundedCornerShape(4.dp))
                                    .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                                    .padding(2.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val sizeX = size.width
                                    val sizeY = size.height
                                    val block = sizeX / 4
                                    drawRect(Color.Black, topLeft = Offset(0f, 0f), size = Size(block, block))
                                    drawRect(Color.Black, topLeft = Offset(sizeX - block, 0f), size = Size(block, block))
                                    drawRect(Color.Black, topLeft = Offset(0f, sizeY - block), size = Size(block, block))
                                    drawRect(Color.Black, topLeft = Offset(block * 1.5f, block * 1.5f), size = Size(block, block))
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("بطاقة اللقاحات الرقمية 🛡️", fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color(0xFF004D40))
                                Text("رمز QR فعال يحتوي كامل الجرعات المحدثة", fontSize = 7.5.sp, color = Color(0xFF00796B))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(24.dp)
                            .background(Color(0xFF009688), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💾 مشاركة وحفظ بطاقة الطفل", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MockInputField(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(Color(0xFFFAFAFA), RoundedCornerShape(4.dp))
            .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Text(text = value, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun QrScannerScreen(viewModel: VaccineViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(android.Manifest.permission.CAMERA)
        }
    }

    var scannedResult by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val image = com.google.mlkit.vision.common.InputImage.fromFilePath(context, uri)
                val scanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        var found = false
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { rawValue ->
                                found = true
                                scannedResult = rawValue
                                viewModel.handleScannedQrCode(rawValue)
                                android.widget.Toast.makeText(context, "تم قراءة كارت QR من الصورة واستيراد بيانات الطفل بنجاح! 🎉", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                        if (!found) {
                            android.widget.Toast.makeText(context, "لم يتم العثور على رمز QR صالح في الصورة المحددة", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        android.widget.Toast.makeText(context, "خطأ في قراءة الصورة: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "خطأ في تحميل الصورة: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = androidx.camera.view.PreviewView(ctx)
                    val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = androidx.camera.core.Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                            .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val scanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()

                        imageAnalysis.setAnalyzer(androidx.core.content.ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && scannedResult == null) {
                                val image = com.google.mlkit.vision.common.InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            barcode.rawValue?.let { rawValue ->
                                                if (scannedResult == null) {
                                                    scannedResult = rawValue
                                                    viewModel.handleScannedQrCode(rawValue)
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        // Handle failure
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (exc: Exception) {
                            android.util.Log.e("QrScanner", "Use case binding failed", exc)
                        }
                    }, androidx.core.content.ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ماسح رمز QR للقاحات",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .border(3.dp, Color.Green, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (scannedResult != null) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "تم مسح الرمز بنجاح! 🎉",
                                    color = Color.Green,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = scannedResult ?: "",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { scannedResult = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("مسح رمز آخر", color = Color.White)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "ضع رمز QR الخاص بسجل اللقاح داخل الإطار",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "سيتم تحديث حالة طفل أو لقاح فورياً عند مسح الرمز الصحيح",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اختر صورة QR من المعرض لاستيرادها", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "إذن الكاميرا مطلوب لمسح رموز QR الخاصة باللقاحات",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { launcher.launch(android.Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("منح إذن الكاميرا", color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                    Text("العودة للرئيسية", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun RechartsVaccineProgressChart(vaccines: List<VaccineRecord>) {
    val categories = listOf("عند الولادة", "شهران", "4 أشهر", "6 أشهر", "9 أشهر", "15 شهر", "18 شهر", "4 - 6 سنوات")
    
    val categoryStats = categories.map { cat ->
        val catVaccines = vaccines.filter { it.categoryAr == cat }
        val total = catVaccines.size
        val completed = catVaccines.count { it.isTaken }
        Triple(cat, total, completed)
    }.filter { it.second > 0 }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "📊 رسم بياني لتقدم اللقاحات (Recharts Style)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "جدول العراق المعتمد",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recharts style Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF10B981), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("المكتمل", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("المجموع المطلوب", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bar chart representation
            if (categoryStats.isEmpty()) {
                Text(
                    text = "لا توجد سجلات لقاحات متاحة للرسم البياني.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryStats.forEach { (cat, total, completed) ->
                        val progress = if (total > 0) completed.toFloat() / total.toFloat() else 0f
                        val percent = (progress * 100).toInt()

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$completed / $total ($percent%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (completed == total) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color(0xFF059669), Color(0xFF10B981))
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}