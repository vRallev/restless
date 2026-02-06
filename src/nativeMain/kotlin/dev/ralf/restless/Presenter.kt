package dev.ralf.restless

import androidx.compose.runtime.Composable

interface Presenter {
  @Composable fun present(): Any
}
