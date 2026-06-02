package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Bento Grid Theme Color Palette
val BentoBackground = Color(0xFF1C1B1F)      // Luxury dark background
val BentoOnBackground = Color(0xFFE6E1E5)    // Light silver/mauve text

val BentoPrimary = Color(0xFFD0BCFF)         // Bold highlight color (lavender glow)
val BentoOnPrimary = Color(0xFF381E72)       // Rich deep indigo for text on primary highlight

val BentoPrimaryContainer = Color(0xFF4A4458) // Dark orchid-gray container
val BentoOnPrimaryContainer = Color(0xFFE8DEF8) // Warm lavender accent label

val BentoSecondary = Color(0xFFE8DEF8)       // Secondary accent line/pills
val BentoOnSecondary = Color(0xFF2B2930)

val BentoSecondaryContainer = Color(0xFF2B2930) // Sleek dark card container surfaces
val BentoOnSecondaryContainer = Color(0xFFCAC4D0) // Muted label grey

val BentoSurface = Color(0xFF2B2930)        // Element surfaces
val BentoOnSurface = Color(0xFFE6E1E5)       // Regular text on surfaces

val BentoOutline = Color(0xFF49454F)         // Rigid card border strokes
val BentoOutlineVariant = Color(0xFFCAC4D0)  // Faint divider highlights

val BentoError = Color(0xFFF2B8B5)           // Error text
val BentoOnError = Color(0xFF601410)

// Backward compat compatibility fallback colors
val SurfaceGround = BentoBackground
val SurfaceElevated = BentoSecondaryContainer
val TextPrimary = BentoOnBackground
val TextSecondary = BentoOnSecondaryContainer
val PrimaryColor = BentoPrimary
val OnPrimary = BentoOnPrimary
val PrimaryContainer = BentoPrimaryContainer
val OnPrimaryContainer = BentoOnPrimaryContainer
val SecondaryColor = BentoSecondary
val OnSecondary = BentoOnSecondary
val SecondaryContainer = BentoSecondaryContainer
val OnSecondaryContainer = BentoOnSecondaryContainer
val TertiaryColor = Color(0xFF7E3000)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFA44100)
val OnTertiaryContainer = Color(0xFFFFD2BE)
val ErrorRed = BentoError
val OnError = BentoOnError
val ErrorContainer = Color(0xFF8C1D18)
val OnErrorContainer = Color(0xFFF9DEDC)
val OutlineColor = BentoOutline
val OutlineVariantColor = BentoOutlineVariant
val SurfaceGroundAlt = BentoBackground
val SurfaceDim = Color(0xFF141318)
val SurfaceBright = BentoBackground
val SurfaceContainerLowest = Color(0xFF141318)
val SurfaceContainerLow = Color(0xFF1C1D22)
val SurfaceContainer = BentoSecondaryContainer
val SurfaceContainerHigh = Color(0xFF36343B)
val SurfaceContainerHighest = Color(0xFF49454F)
val InverseSurface = Color(0xFFE6E1E5)
val InverseOnSurface = Color(0xFF313033)
val InversePrimaryColor = Color(0xFF6750A4)
