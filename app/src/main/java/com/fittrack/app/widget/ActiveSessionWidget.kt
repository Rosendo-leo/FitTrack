package com.fittrack.app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fittrack.app.MainActivity
import com.fittrack.app.ui.common.format

class ActiveSessionWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadActiveSessionData(context)
        provideContent {
            FitTrackGlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    if (data.active) {
                        Text(
                            "🏋️ Sessão ativa",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                        Text(
                            data.templateName,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            "${data.totalSets} séries · " +
                                data.weightUnit.format(data.totalVolume, decimals = 0),
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 13.sp
                            )
                        )
                    } else {
                        Text(
                            "🏋️ FitTrack",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                        Text(
                            "Nenhum treino em andamento",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 14.sp
                            )
                        )
                        Text(
                            "Toque para abrir",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

class ActiveSessionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ActiveSessionWidget()
}
