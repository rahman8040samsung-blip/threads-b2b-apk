package com.example.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.composed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Android Jetpack Compose CSS Framework (CssTheme)
 * Replicates Tailwind CSS & Modern Web Design System styling tokens directly in Kotlin/Compose.
 * Designed specifically for a clean, secure, dark-mode professional B2B aesthetic.
 */
fun Modifier.cssSkeleton(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)
): Modifier = this.composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    val shimmerColors = if (isLightThemeMode) {
        listOf(
            Color(0xFFE0E0E0),
            Color(0xFFF0F0F0),
            Color(0xFFE0E0E0)
        )
    } else {
        listOf(
            Color(0xFF1E1E20),
            Color(0xFF2D2D31),
            Color(0xFF1E1E20)
        )
    }

    val brush = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x = translateAnim, y = translateAnim)
    )

    this.clip(shape).background(brush)
}

fun Modifier.cssCard(
    padding: Dp = 16.dp,
    cornerRadius: Dp = 12.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(CssTheme.bgCard)
    .border(BorderStroke(1.dp, CssTheme.borderMuted), RoundedCornerShape(cornerRadius))
    .padding(padding)

fun Modifier.cssInput(
    isFocused: Boolean = false,
    cornerRadius: Dp = 8.dp
): Modifier = this
    .heightIn(min = 54.dp)
    .border(
        BorderStroke(1.dp, if (isFocused) CssTheme.borderFocus else CssTheme.borderMuted),
        RoundedCornerShape(cornerRadius)
    )
    .background(CssTheme.bgCard, RoundedCornerShape(cornerRadius))

fun Modifier.cssOverlayBlur(
    cornerRadius: Dp = 12.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(Color(0xE0050505))
    .border(BorderStroke(1.dp, CssTheme.borderMuted), RoundedCornerShape(cornerRadius))

object CssTheme {

    // CSS Color Equivalents
    val bgOled: Color get() = if (isLightThemeMode) Color(0xFFFFFFFF) else Color(0xFF000000)         // bg-black
    val bgCard: Color get() = if (isLightThemeMode) Color(0xFFF5F5F7) else Color(0xFF0A0A0C)         // bg-zinc-950
    val bgGray: Color get() = if (isLightThemeMode) Color(0xFFEBEBEB) else Color(0xFF18181B)         // bg-zinc-900 (Secondary borders & inputs)
    val borderMuted: Color get() = if (isLightThemeMode) Color(0xFFD2D2D2) else Color(0xFF27272A)    // border-zinc-800 (Clean divider lines)
    val borderFocus: Color get() = Color(0xFF10B981)    // border-emerald-500 (Active focus accent)
    val textPrimary: Color get() = if (isLightThemeMode) Color(0xFF000000) else Color(0xFFFAFAFA)    // text-zinc-50 (Pristine display title)
    val textMuted: Color get() = if (isLightThemeMode) Color(0xFF707070) else Color(0xFF71717A)      // text-zinc-500 (Subheading/Caption)
    val accentSecure: Color get() = Color(0xFF10B981)   // text-emerald-400 (Legal and network verified status)
    val accentError: Color get() = Color(0xFFEF4444)    // text-red-500 (Alert prompts/errors)
    val accentBlue: Color get() = Color(0xFF3B82F6)     // text-blue-500 (Special highlights)

    // CSS Badge Pattern (Tailwind-like styled pill badges)
    @Composable
    fun CssBadge(
        text: String,
        badgeType: BadgeType = BadgeType.MUTED,
        modifier: Modifier = Modifier
    ) {
        val (bg, txt, border) = when (badgeType) {
            BadgeType.SECURE -> Triple(
                if (isLightThemeMode) Color(0xFFE6F4EA) else Color(0xFF064E3B),
                accentSecure,
                if (isLightThemeMode) Color(0xFFCEEAD6) else Color(0xFF047857)
            )
            BadgeType.ERROR -> Triple(
                if (isLightThemeMode) Color(0xFFFCE8E6) else Color(0xFF451A03),
                accentError,
                if (isLightThemeMode) Color(0xFFFAD2CF) else Color(0xFF7F1D1D)
            )
            BadgeType.BLUE -> Triple(
                if (isLightThemeMode) Color(0xFFE8F0FE) else Color(0xFF1E3A8A),
                accentBlue,
                if (isLightThemeMode) Color(0xFFD2E3FC) else Color(0xFF1D4ED8)
            )
            BadgeType.MUTED -> Triple(
                if (isLightThemeMode) Color(0xFFF0F0F0) else bgOled,
                textMuted,
                if (isLightThemeMode) Color(0xFFE0E0E0) else borderMuted
            )
        }

        Box(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(bg)
                .border(BorderStroke(0.5.dp, border), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text.uppercase(),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = txt,
                letterSpacing = 0.5.sp
            )
        }
    }

    enum class BadgeType {
        SECURE, ERROR, BLUE, MUTED
    }

    // CSS Button equivalents
    @Composable
    fun CssPrimaryButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        icon: (@Composable () -> Unit)? = null,
        enabled: Boolean = true
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .height(48.dp)
                .shadow(4.dp, RoundedCornerShape(8.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = textPrimary,
                contentColor = bgOled,
                disabledContainerColor = bgGray,
                disabledContentColor = textMuted
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxHeight()
            ) {
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }

    @Composable
    fun CssSecondaryButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        icon: (@Composable () -> Unit)? = null,
        enabled: Boolean = true
    ) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = textPrimary,
                disabledContentColor = textMuted
            ),
            border = BorderStroke(1.dp, borderMuted),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxHeight()
            ) {
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }

    // Modern "Callout Box" or alert banner suitable for enterprise security notification
    @Composable
    fun CssEnterpriseSecNotice(
        modifier: Modifier = Modifier,
        title: String = "SECURE ENCRYPTED NODE",
        description: String = "AES-256 session authenticated. Direct B2B handshake protocol active."
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isLightThemeMode) Color(0xFFE6F4EA) else Color(0xFF04140D)) // Deep forest dark green
                .border(BorderStroke(1.dp, borderFocus.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pulsing dot simulation
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentSecure)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = accentSecure,
                    letterSpacing = 1.sp
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = textMuted
                )
            }
        }
    }

    // CSS Code/Indicator tag block
    @Composable
    fun CssCodeIndicator(
        label: String,
        value: String,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(4.dp))
                .background(bgGray)
                .border(BorderStroke(1.dp, borderMuted), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "$label :",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = textMuted
            )
            Text(
                text = value,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }
    }

    // Modern High-Fidelity Skeleton layout for the Live Feed
    @Composable
    fun B2bFeedSkeleton(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(3) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = bgCard),
                    border = BorderStroke(0.5.dp, borderMuted)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .cssSkeleton(CircleShape)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(14.dp)
                                        .cssSkeleton()
                                )
                                Box(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(10.dp)
                                        .cssSkeleton()
                                )
                            }
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .cssSkeleton()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(14.dp)
                                    .cssSkeleton()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(14.dp)
                                    .cssSkeleton()
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(55.dp)
                                    .height(12.dp)
                                    .cssSkeleton()
                            )
                            Box(
                                modifier = Modifier
                                    .width(55.dp)
                                    .height(12.dp)
                                    .cssSkeleton()
                            )
                        }
                    }
                }
            }
        }
    }

    // Modern High-Fidelity Skeleton layout for the Merchant Profile
    @Composable
    fun B2bProfileSkeleton(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .cssSkeleton(CircleShape)
            )

            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(22.dp)
                    .cssSkeleton()
            )

            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(14.dp)
                    .cssSkeleton()
            )

            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(24.dp)
                    .cssSkeleton(RoundedCornerShape(6.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = bgCard),
                border = BorderStroke(0.5.dp, borderMuted)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(16.dp)
                            .cssSkeleton()
                    )

                    repeat(3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(12.dp)
                                    .cssSkeleton()
                            )
                            Box(
                                modifier = Modifier
                                    .width(130.dp)
                                    .height(12.dp)
                                    .cssSkeleton()
                            )
                        }
                    }
                }
            }
        }
    }

    // React-like Error Boundary equivalent in Jetpack Compose
    @Composable
    fun B2bErrorBoundary(
        viewName: String,
        isSimulatedError: Boolean,
        onReset: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        content()
    }
}
