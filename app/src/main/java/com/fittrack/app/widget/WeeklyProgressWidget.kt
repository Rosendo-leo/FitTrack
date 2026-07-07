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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fittrack.app.MainActivity

private val dayLetters = listOf("S", "T", "Q", "Q", "S", "S", "D")

class WeeklyProgressWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWeeklyData(context)
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
                    Text(
                        "Progresso semanal",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                    Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp)) {
                        data.weekDays.forEachIndexed { index, trained ->
                            Column(
                                modifier = GlanceModifier.defaultWeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    dayLetters[index],
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    if (trained) "✓" else "·",
                                    style = TextStyle(
                                        color = if (trained) GlanceTheme.colors.tertiary
                                        else GlanceTheme.colors.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                    Text(
                        "🔥 Streak: ${data.streakDays} " +
                            if (data.streakDays == 1) "dia" else "dias",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = GlanceModifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

class WeeklyProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeeklyProgressWidget()
}
