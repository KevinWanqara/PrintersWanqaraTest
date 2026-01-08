package com.example.printerswanqara.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.printerswanqara.ui.screens.LoginScreen
import com.example.printerswanqara.ui.screens.DomainValidationScreen
import com.example.printerswanqara.data.AppStorage
import com.example.printerswanqara.ui.theme.Primary
import com.example.printerswanqara.ui.theme.Secondary
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    logoResId: Int
) {
    val context = LocalContext.current
    val hasSession = remember { !AppStorage.getToken(context).isNullOrBlank() }
    var printerConfigured by remember { mutableStateOf(false) }
    val pageCount = if (hasSession) 3 else 5
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false
        ) { page ->
            AnimatedVisibility(
                visible = pagerState.currentPage == page,
                enter = fadeIn() + slideInHorizontally { it / 2 },
                exit = fadeOut() + slideOutHorizontally { -it / 2 }
            ) {
                when (page) {
                    0 -> WelcomeSlide(
                        onNext = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        logoResId = logoResId
                    )
                    1 -> PrinterSetupSlide(
                        onNext = { configured ->
                            printerConfigured = configured
                            if (hasSession) {
                                if (configured) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                } else {
                                    AppStorage.setOnboardingCompleted(context, true)
                                    onOnboardingComplete()
                                }
                            } else {
                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                            }
                        }
                    )
                    2 -> {
                        if (hasSession) {
                            SuccessSlide(
                                onNext = {
                                    AppStorage.setOnboardingCompleted(context, true)
                                    onOnboardingComplete()
                                }
                            )
                        } else {
                            SessionExplanationSlide(
                                onNext = { coroutineScope.launch { pagerState.animateScrollToPage(3) } }
                            )
                        }
                    }
                    3 -> {
                        LoginWithDomainSlide(
                            onLoginSuccess = {
                                if (printerConfigured) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(4) }
                                } else {
                                    AppStorage.setOnboardingCompleted(context, true)
                                    onOnboardingComplete()
                                }
                            },
                            logoResId = logoResId
                        )
                    }
                    4 -> {
                        SuccessSlide(
                            onNext = {
                                AppStorage.setOnboardingCompleted(context, true)
                                onOnboardingComplete()
                            }
                        )
                    }
                }
            }
        }

        // Page Indicator with animation
        Row(
            Modifier
                .height(50.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                val width by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 8.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "indicatorWidth"
                )
                val color by animateColorAsState(
                    targetValue = if (isSelected) Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    label = "indicatorColor"
                )
                
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .width(width)
                        .height(8.dp)
                )
            }
        }
    }
}

@Composable
fun WelcomeSlide(onNext: () -> Unit, logoResId: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")

    // 1. Logo Floating (Up/Down)
    val logoYOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoYOffset"
    )

    // 2. Logo Breathing (Scale)
    val logoScalePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScalePulse"
    )

    // Entrance State
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Entrance Animations
    val logoEntranceScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "logoScale"
    )

    val logoEntranceAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800),
        label = "logoAlpha"
    )

    // Text Animations (Staggered Slide Up)
    val titleOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 100f,
        animationSpec = tween(800, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "titleOffset"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, delayMillis = 300),
        label = "titleAlpha"
    )

    val descOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 100f,
        animationSpec = tween(800, delayMillis = 500, easing = FastOutSlowInEasing),
        label = "descOffset"
    )
    val descAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, delayMillis = 500),
        label = "descAlpha"
    )

    val buttonOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 100f,
        animationSpec = tween(800, delayMillis = 700, easing = FastOutSlowInEasing),
        label = "buttonOffset"
    )
    val buttonAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, delayMillis = 700),
        label = "buttonAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.height(250.dp) // Fixed height for animation area
        ) {
            // Highlight Glow
            Canvas(modifier = Modifier.size(220.dp).scale(logoScalePulse)) {
                 drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.3f),
                            Primary.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension / 2
                    ),
                    radius = size.minDimension / 2
                )
            }

            // Logo
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        scaleX = logoEntranceScale * logoScalePulse
                        scaleY = logoEntranceScale * logoScalePulse
                        translationY = logoYOffset
                        alpha = logoEntranceAlpha
                    },
                contentScale = ContentScale.Fit
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Bienvenido a la configuración guiada",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Primary,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.graphicsLayer {
                translationY = titleOffset
                alpha = titleAlpha
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Te ayudaremos a configurar tu impresora y acceder a tu cuenta en pocos minutos.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            ),
            modifier = Modifier.graphicsLayer {
                translationY = descOffset
                alpha = descAlpha
            }
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer {
                    translationY = buttonOffset
                    alpha = buttonAlpha
                },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("Comenzar", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun PrinterSetupSlide(onNext: (Boolean) -> Unit) {
    SimplifiedPrinterSetup(onNext = onNext)
}

@Composable
fun SessionExplanationSlide(onNext: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "yOffset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Animated background ring
            Canvas(modifier = Modifier.size(160.dp)) {
                drawCircle(
                    color = Primary.copy(alpha = 0.1f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 4.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                    )
                )
            }

            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationY = yOffset
                    ),
                shape = CircleShape,
                color = Primary.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Primary.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Sincronización de Cuenta",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Primary,
                textAlign = TextAlign.Center
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Para finalizar, necesitamos que inicies sesión. Esto permitirá sincronizar tus configuraciones de impresión y asegurar que todos tus documentos se emitan correctamente.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("Continuar al Login", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun LoginWithDomainSlide(onLoginSuccess: (String) -> Unit, logoResId: Int) {
    val context = LocalContext.current
    var showDomainValidation by remember { mutableStateOf(AppStorage.getRuc(context).isNullOrBlank()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = showDomainValidation,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "LoginTransition"
            ) { isDomainVisible ->
                if (isDomainVisible) {
                    DomainValidationScreen(
                        onDomainValidated = { showDomainValidation = false },
                        logoResId = logoResId
                    )
                } else {
                    LoginScreen(
                        onLoginSuccess = onLoginSuccess,
                        onDomainValidationRequested = { showDomainValidation = true },
                        logoResId = logoResId
                    )
                }
            }
        }
    }
}

@Composable
fun SuccessSlide(onNext: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1000),
        label = "alpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Confetti Explosion from bottom
        if (startAnimation) {
            ConfettiExplosion()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "breathing")
            val breathingScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "breathingScale"
            )

            val entranceScale by animateFloatAsState(
                targetValue = if (startAnimation) 1f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "entranceScale"
            )

            val finalScale = entranceScale * breathingScale

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                Surface(
                    modifier = Modifier.size(180.dp * finalScale),
                    shape = CircleShape,
                    color = Primary.copy(alpha = 0.1f)
                ) {}

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(100.dp * finalScale)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "¡Todo listo!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.graphicsLayer(alpha = alpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tu configuración ha sido completada con éxito. Ya puedes empezar a imprimir tus documentos.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.graphicsLayer(alpha = alpha)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer(alpha = alpha),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Comenzar a Imprimir", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

data class ConfettiData(
    val x0: Float,
    val y0: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float
)

@Composable
fun ConfettiExplosion(modifier: Modifier = Modifier) {
    val time = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        time.animateTo(
            targetValue = 5f,
            animationSpec = tween(5000, easing = LinearEasing)
        )
    }

    val particles = remember {
        val colors = listOf(Primary, Secondary, Color.Cyan, Color.Magenta, Color.Yellow)
        List(150) {
             ConfettiData(
                 x0 = 0.5f,
                 y0 = 1.1f,
                 vx = (Math.random() * 1.0 - 0.5).toFloat(),
                 vy = -(Math.random() * 0.8 + 0.6).toFloat(),
                 color = colors.random(),
                 size = (Math.random() * 15 + 5).toFloat()
             )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = time.value
        val g = 0.8f

        particles.forEach { p ->
            val x = (p.x0 + p.vx * t) * size.width
            val y = (p.y0 + p.vy * t + 0.5f * g * t * t) * size.height

            if (y < size.height + 100) {
                drawCircle(
                    color = p.color,
                    radius = p.size,
                    center = Offset(x, y)
                )
            }
        }
    }
}
