package com.neuroverse.authapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette. Defined once here and referenced only from [Theme.kt] so a
 * rebrand is a one-file change.
 *
 * Kotlin note: Color(0xFF1F6FEB) is ARGB. The leading FF is full opacity — omit
 * it and you get a fully transparent colour, which is the single most common
 * "my UI is invisible" bug in Compose.
 */

// --- Light scheme --------------------------------------------------------
val BrandBlue        = Color(0xFF1F6FEB)   // primary actions
val BrandBlueDeep    = Color(0xFF0B4FC2)   // pressed state
val BrandSlate       = Color(0xFF3F4A5A)   // secondary
val SurfaceLight     = Color(0xFFFBFCFE)
val BackgroundLight  = Color(0xFFF4F6FA)
val ErrorRed         = Color(0xFFBA1A1A)
val SuccessGreen     = Color(0xFF1B7A3D)

// --- Dark scheme ---------------------------------------------------------
val BrandBlueLight   = Color(0xFF8AB4F8)
val BrandSlateLight  = Color(0xFFB7C0CD)
val SurfaceDark      = Color(0xFF14181F)
val BackgroundDark   = Color(0xFF0D1117)
val ErrorRedLight    = Color(0xFFFFB4AB)
val SuccessGreenLight = Color(0xFF7BD79C)
