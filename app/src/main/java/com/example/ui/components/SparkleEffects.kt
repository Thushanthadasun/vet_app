package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.theme.LocalPastelColors
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class SparkleParticle(
    val id: Long,
    val origin: Offset,
    val angleRad: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val createdAtMs: Long,
)

@Stable
class SparkleController {
    internal val particles = mutableStateListOf<SparkleParticle>()
    private var nextId = 0L

    private val palette = listOf(
        Color(0xFFF6A5C4),
        Color(0xFFCBBDF8),
        Color(0xFF9AD2F7),
        Color(0xFFF4C86A),
        Color(0xFFFFD8E8),
    )

    fun burstAt(position: Offset, count: Int = 9) {
        val now = System.currentTimeMillis()
        repeat(count) { idx ->
            val baseAngle = (Math.PI * 2.0 * idx / count).toFloat()
            val jitter = Random.nextFloat() * 0.45f - 0.22f
            particles += SparkleParticle(
                id = nextId++,
                origin = position,
                angleRad = baseAngle + jitter,
                speed = 50f + Random.nextFloat() * 68f,
                size = 5.5f + Random.nextFloat() * 5.5f,
                color = palette[idx % palette.size],
                createdAtMs = now,
            )
        }
    }

    internal fun prune(nowMs: Long) {
        particles.removeAll { nowMs - it.createdAtMs > 600L }
    }
}

val LocalSparkleController = staticCompositionLocalOf { SparkleController() }

@Composable
fun PastelGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val pastel = LocalPastelColors.current
    val sparkleController = remember { SparkleController() }
    var frameClockMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val infiniteTransition = rememberInfiniteTransition(label = "aura_drift")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aura_phase",
    )

    LaunchedEffect(sparkleController.particles.size) {
        while (sparkleController.particles.isNotEmpty()) {
            frameClockMs = System.currentTimeMillis()
            sparkleController.prune(frameClockMs)
            delay(16L)
        }
    }

    CompositionLocalProvider(
        LocalSparkleController provides sparkleController
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            pastel.gradientStart,
                            pastel.gradientMid,
                            pastel.gradientEnd,
                        )
                    )
                )
                .pointerInput(sparkleController) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val down = event.changes.firstOrNull { it.pressed && !it.previousPressed }
                            if (down != null) {
                                sparkleController.burstAt(down.position, count = 7)
                            }
                        }
                    }
                }
        ) {
            // Dreamy glowing pastel aurora orbs + subtle ambient glints
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val roseCenter = Offset(w * (0.14f + 0.08f * drift), h * (0.12f + 0.04f * drift))
                val lavCenter = Offset(w * (0.86f - 0.07f * drift), h * (0.36f + 0.06f * drift))
                val skyCenter = Offset(w * (0.24f + 0.10f * drift), h * (0.82f - 0.05f * drift))

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(pastel.auraRose, Color.Transparent),
                        center = roseCenter,
                        radius = w * 0.62f,
                    ),
                    radius = w * 0.62f,
                    center = roseCenter,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(pastel.auraLavender, Color.Transparent),
                        center = lavCenter,
                        radius = w * 0.58f,
                    ),
                    radius = w * 0.58f,
                    center = lavCenter,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(pastel.auraSky, Color.Transparent),
                        center = skyCenter,
                        radius = w * 0.64f,
                    ),
                    radius = w * 0.64f,
                    center = skyCenter,
                )

                // Soft decorative 4-point ambient star glints
                val glints = listOf(
                    Offset(w * 0.84f, h * 0.10f) to 10f,
                    Offset(w * 0.10f, h * 0.48f) to 7f,
                    Offset(w * 0.90f, h * 0.72f) to 9f,
                )
                glints.forEachIndexed { idx, (pos, r) ->
                    val pulse = 0.35f + 0.35f * sin((drift * 3.14f) + idx * 1.4f).let { kotlin.math.abs(it) }
                    val star = Path().apply {
                        moveTo(pos.x, pos.y - r)
                        lineTo(pos.x + r * 0.28f, pos.y - r * 0.28f)
                        lineTo(pos.x + r, pos.y)
                        lineTo(pos.x + r * 0.28f, pos.y + r * 0.28f)
                        lineTo(pos.x, pos.y + r)
                        lineTo(pos.x - r * 0.28f, pos.y + r * 0.28f)
                        lineTo(pos.x - r, pos.y)
                        lineTo(pos.x - r * 0.28f, pos.y - r * 0.28f)
                        close()
                    }
                    drawPath(star, color = Color.White.copy(alpha = pulse))
                }
            }

            content()

            if (sparkleController.particles.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val now = frameClockMs
                    val originsSeen = HashSet<Long>()
                    sparkleController.particles.forEach { p ->
                        val t = ((now - p.createdAtMs) / 600f).coerceIn(0f, 1f)
                        val eased = 1f - (1f - t) * (1f - t)
                        val alpha = (1f - t).coerceIn(0f, 1f)

                        if (originsSeen.add(p.createdAtMs)) {
                            drawCircle(
                                color = p.color.copy(alpha = alpha * 0.35f),
                                radius = 14f + 42f * eased,
                                center = p.origin,
                                style = Stroke(width = 2.2f),
                            )
                        }

                        val dist = p.speed * eased
                        val cx = p.origin.x + cos(p.angleRad) * dist
                        val cy = p.origin.y + sin(p.angleRad) * dist
                        val r = p.size * (1f - 0.3f * t)

                        val fourPoint = Path().apply {
                            moveTo(cx, cy - r)
                            lineTo(cx + r * 0.30f, cy - r * 0.30f)
                            lineTo(cx + r, cy)
                            lineTo(cx + r * 0.30f, cy + r * 0.30f)
                            lineTo(cx, cy + r)
                            lineTo(cx - r * 0.30f, cy + r * 0.30f)
                            lineTo(cx - r, cy)
                            lineTo(cx - r * 0.30f, cy - r * 0.30f)
                            close()
                        }
                        rotate(degrees = t * 55f, pivot = Offset(cx, cy)) {
                            drawPath(path = fourPoint, color = p.color.copy(alpha = alpha * 0.92f))
                        }
                    }
                }
            }
        }
    }
}

private data class ConfettiPiece(
    val xFraction: Float,
    val speedFactor: Float,
    val swayAmplitude: Float,
    val swayPhase: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    val startDelayFraction: Float,
)

@Composable
fun PastelConfettiOverlay(
    modifier: Modifier = Modifier,
) {
    val colors = remember {
        listOf(
            Color(0xFFF6A5C4),
            Color(0xFFCBBDF8),
            Color(0xFF9AD2F7),
            Color(0xFFF4C86A),
            Color(0xFF9BE0C4),
        )
    }
    val pieces = remember {
        List(60) { i ->
            ConfettiPiece(
                xFraction = Random.nextFloat(),
                speedFactor = 0.65f + Random.nextFloat() * 0.7f,
                swayAmplitude = 18f + Random.nextFloat() * 34f,
                swayPhase = Random.nextFloat() * 6.28f,
                width = 10f + Random.nextFloat() * 10f,
                height = 6f + Random.nextFloat() * 8f,
                color = colors[i % colors.size],
                startDelayFraction = Random.nextFloat(),
            )
        }
    }

    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (true) {
            tick = System.currentTimeMillis() - start
            delay(16L)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val elapsedSec = tick / 1000f
        pieces.forEach { piece ->
            val cycle = ((elapsedSec * 0.24f * piece.speedFactor + piece.startDelayFraction) % 1.15f) - 0.1f
            val y = cycle * size.height
            val x = piece.xFraction * size.width +
                sin(elapsedSec * 2.4f + piece.swayPhase) * piece.swayAmplitude
            val rot = (elapsedSec * 120f * piece.speedFactor + piece.swayPhase * 50f) % 360f
            rotate(degrees = rot, pivot = Offset(x, y)) {
                drawRect(
                    color = piece.color.copy(alpha = 0.9f),
                    topLeft = Offset(x - piece.width / 2f, y - piece.height / 2f),
                    size = androidx.compose.ui.geometry.Size(piece.width, piece.height),
                )
            }
        }
    }
}
