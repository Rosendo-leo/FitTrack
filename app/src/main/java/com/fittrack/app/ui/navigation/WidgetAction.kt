package com.fittrack.app.ui.navigation

/** Atalho disparado a partir de um widget da home screen. */
sealed interface WidgetAction {
    data object RegisterWeight : WidgetAction
    data class StartWorkout(val templateId: Long?) : WidgetAction
}
