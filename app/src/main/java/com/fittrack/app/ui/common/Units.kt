package com.fittrack.app.ui.common

import androidx.compose.runtime.compositionLocalOf
import com.fittrack.app.data.preferences.DistanceUnit
import com.fittrack.app.data.preferences.UserPreferences
import com.fittrack.app.data.preferences.WeightUnit

/**
 * Preferências do usuário disponíveis em toda a árvore de composição
 * (fornecidas pelo MainActivity). Os dados continuam armazenados em kg/km;
 * a conversão acontece só na exibição e na entrada.
 */
val LocalUserPreferences = compositionLocalOf { UserPreferences() }

private const val KG_PER_LB = 0.45359237f
private const val KM_PER_MI = 1.609344f

val WeightUnit.suffix: String
    get() = if (this == WeightUnit.KG) "kg" else "lb"

fun WeightUnit.fromKg(kg: Float): Float =
    if (this == WeightUnit.KG) kg else kg / KG_PER_LB

fun WeightUnit.toKg(value: Float): Float =
    if (this == WeightUnit.KG) value else value * KG_PER_LB

/** Ex.: `format(82.5f)` → "82,5 kg" ou "181,9 lb". */
fun WeightUnit.format(kg: Float, decimals: Int = 1): String =
    "%.${decimals}f %s".format(fromKg(kg), suffix)

val DistanceUnit.suffix: String
    get() = if (this == DistanceUnit.KM) "km" else "mi"

fun DistanceUnit.fromKm(km: Float): Float =
    if (this == DistanceUnit.KM) km else km / KM_PER_MI

fun DistanceUnit.toKm(value: Float): Float =
    if (this == DistanceUnit.KM) value else value * KM_PER_MI

fun DistanceUnit.format(km: Float, decimals: Int = 1): String =
    "%.${decimals}f %s".format(fromKm(km), suffix)
