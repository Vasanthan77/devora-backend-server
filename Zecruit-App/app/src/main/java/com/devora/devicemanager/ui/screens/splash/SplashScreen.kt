package com.devora.devicemanager.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devora.devicemanager.R
import com.devora.devicemanager.ui.theme.DarkBgElevated
import com.devora.devicemanager.ui.theme.JetBrainsMono
import com.devora.devicemanager.ui.theme.PlusJakartaSans
import com.devora.devicemanager.ui.theme.PurpleBright
import com.devora.devicemanager.ui.theme.PurpleCore

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 2500,
                easing = LinearEasing
            )
        )
        onSplashFinished()
    }

    val darkBg = Color(0xFF0F0F14)
    val darkBg2 = Color(0xFF14141E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(darkBg, darkBg2)
                )
            )
    ) {
        // Center content
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo image
            Image(
                painter = painterResource(id = R.drawable.devora_nobg),
                contentDescription = "Devora Logo",
                modifier = Modifier.size(160.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            // "DEVORA" title
            Text(
                text = "DEVORA",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 52.sp,
                color = Color(0xFFF0F2F5)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Divider line
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PurpleCore, Color.Transparent)
                        )
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "Enterprise Device Manager",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = Color(0xFF8899A6)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "SECURE  ·  MONITOR  ·  MANAGE",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                color = Color(0xFF6B7D8D),
                letterSpacing = 2.sp
            )
        }

        // Bottom loading bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter)
                .padding(horizontal = 0.dp)
        ) {
            // Track
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBgElevated)
            )
            // Animated fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress.value)
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PurpleCore, PurpleBright)
                        )
                    )
            )
        }
    }
}
