/*
 * Copyright (C) 2026 crDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.icons

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log

/**
 * Gate for the blurred themed icon background.
 *
 * The blur itself is not produced here. It is a real backdrop blur done by SurfaceFlinger: the
 * icon punches a transparent hole in the launcher window and registers a blur region, so what
 * gets blurred is whatever is genuinely behind the window. All of that lives in the launcher,
 * because it needs `com.android.internal` APIs and this library also builds against the public
 * SDK for SystemUI.
 *
 * This object therefore only answers "is the user asking for it", and is deliberately trivial.
 *
 * An earlier version of this feature read the wallpaper bitmap and blurred it by hand, offsetting
 * a shader per icon to fake a backdrop. That needed READ_WALLPAPER_INTERNAL, hand-written offset
 * maths, wallpaper parallax tracking and per-frame invalidation on scroll, and it silently did
 * nothing on live wallpapers. Letting SurfaceFlinger do it removes every one of those.
 */
object ThemedIconBackdrop {

    private const val TAG = "ThemedIconBackdrop"

    /** Secure setting gating the effect. */
    const val SETTING_KEY = "themed_icon_blur_enabled"

    private var initialised = false
    private var appContext: Context? = null

    @Volatile private var enabled = false

    /** True when the user has switched the effect on. */
    @JvmStatic
    val isEnabled: Boolean
        get() = enabled

    @JvmStatic
    fun init(context: Context) {
        synchronized(this) {
            if (initialised) return
            initialised = true
            appContext = context.applicationContext
        }
        refresh()
        runCatching {
            val ctx = appContext ?: return
            ctx.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(SETTING_KEY),
                false,
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) = refresh()
                },
            )
        }.onFailure { Log.w(TAG, "Could not observe $SETTING_KEY", it) }
    }

    /** Re-reads the setting. Safe to call from a settings observer. */
    @JvmStatic
    fun refresh() {
        val ctx = appContext ?: return
        enabled = runCatching {
            Settings.Secure.getInt(ctx.contentResolver, SETTING_KEY, 0) != 0
        }.getOrDefault(false)
    }
}
