package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Forced dark CRT terminal palette with vibrant phosphor green highlights
private val RetroTerminalColorScheme = darkColorScheme(
  primary = PhosphorGreen,
  onPrimary = TerminalBlack,
  primaryContainer = TerminalBorderGlow,
  onPrimaryContainer = PhosphorGreenBright,
  secondary = AmberTerminal,
  onSecondary = TerminalBlack,
  secondaryContainer = AmberDim,
  onSecondaryContainer = AmberTerminal,
  tertiary = CyanTerminal,
  onTertiary = TerminalBlack,
  background = TerminalBlack,
  onBackground = PhosphorGreen,
  surface = TerminalDarkBg,
  onSurface = PhosphorGreen,
  surfaceVariant = TerminalSurface,
  onSurfaceVariant = PhosphorGreenDim,
  outline = TerminalBorder,
  outlineVariant = PhosphorGreenDark,
  error = RedTerminal,
  onError = TerminalBlack
)

@Composable
fun BursaAbsurdTheme(
  content: @Composable () -> Unit
) {
  // Always use authentic 1980s green terminal aesthetic
  MaterialTheme(
    colorScheme = RetroTerminalColorScheme,
    typography = RetroTerminalTypography,
    content = content
  )
}
