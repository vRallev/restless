package dev.ralf.screen

import androidx.compose.runtime.Composable

interface Presenter {
  @Composable fun present(): Any
}
