package com.fittrack.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.Button
import com.fittrack.app.MainActivity

class WorkoutDayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWorkoutDayData(context)
        provideContent {
            FitTrackGlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(12.dp)
                ) {
                    Text(
                        "📋 Treino do dia",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        data.templateName ?: "Nenhum treino criado",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (data.templateName != null) {
                        Text(
                            "${data.exerciseCount} exercícios",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                    Button(
                        text = if (data.templateName != null) "▶ Iniciar treino" else "Abrir app",
                        onClick = actionStartActivity(
                            Intent(context, MainActivity::class.java).apply {
                                if (data.templateId != null) {
                                    action = MainActivity.ACTION_START_WORKOUT
                                    putExtra(MainActivity.EXTRA_TEMPLATE_ID, data.templateId)
                                }
                            }
                        ),
                        modifier = GlanceModifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

class WorkoutDayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WorkoutDayWidget()
}
