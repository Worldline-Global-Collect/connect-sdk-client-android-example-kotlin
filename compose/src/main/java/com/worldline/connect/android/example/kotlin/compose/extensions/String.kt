/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.compose.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun Any.convertToString(): String {
    return when (this) {
        is Int -> stringResource(id = this)
        is String -> this
        else -> "no supported value"
    }
}
