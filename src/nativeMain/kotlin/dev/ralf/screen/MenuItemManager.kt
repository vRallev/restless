package dev.ralf.screen

interface MenuItemManager {
  fun updateTitle(title: String)

  fun setManualMode(isManual: Boolean)

  suspend fun setupMenu()
}
