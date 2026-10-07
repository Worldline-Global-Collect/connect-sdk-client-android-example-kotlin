/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.xml.utils.extentions

import android.content.Context
import android.os.Build
import java.util.*

fun Context.getCurrentLocale(): Locale {
    return this.resources.configuration.locales[0]
}
