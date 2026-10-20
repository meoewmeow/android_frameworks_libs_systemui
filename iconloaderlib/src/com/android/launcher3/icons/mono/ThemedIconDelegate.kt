/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.launcher3.icons.mono

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Rect
import com.android.launcher3.icons.BitmapInfo
import com.android.launcher3.icons.FastBitmapDrawable
import com.android.launcher3.icons.FastBitmapDrawableDelegate
import com.android.launcher3.icons.FastBitmapDrawableDelegate.DelegateFactory
import com.android.launcher3.icons.GraphicsUtils.getColorMultipliedFilter
import com.android.launcher3.icons.GraphicsUtils.resizeToContentSize
import com.android.launcher3.icons.IconShape
import com.android.launcher3.icons.R
import com.android.launcher3.icons.ThemedIconBackdrop

/** Drawing delegate handle monochrome themed app icons */
class ThemedIconDelegate(
    constantState: ThemedIconInfo,
    val bitmapInfo: BitmapInfo,
    val paint: Paint,
    private val host: FastBitmapDrawable? = null,
) : FastBitmapDrawableDelegate {

    private val colorFg = constantState.colorFg

    // The foreground/monochrome icon for the app
    private val monoIcon = constantState.mono
    private val monoPaint =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = getColorMultipliedFilter(colorFg, paint.colorFilter)
        }

    private val shapeBounds = Rect(0, 0, bitmapInfo.icon.width, bitmapInfo.icon.height)

    init {
        paint.color = constantState.colorBg
    }

    override fun drawContent(
        info: BitmapInfo,
        iconShape: IconShape,
        canvas: Canvas,
        bounds: Rect,
        paint: Paint,
    ) {
        // The shadow layer is a rim drawn under the flat themed fill. With the fill replaced by
        // a blur region there is nothing for it to sit under, so it reads as a hard border
        // around every icon.
        if (!hasBackdrop()) {
            canvas.drawBitmap(iconShape.shadowLayer, null, bounds, paint)
        }

        canvas.resizeToContentSize(bounds, iconShape.pathSize.toFloat()) {
            clipPath(iconShape.path)
            // When the blurred backdrop is on, the launcher has already punched a transparent
            // hole here and handed the region to SurfaceFlinger, which composites the real blur
            // behind the window. Filling with the flat themed colour would paint straight over
            // it, so the fill is skipped and only the glyph is drawn.
            if (!hasBackdrop()) {
                drawPaint(paint)
            }
            drawBitmap(monoIcon, null, shapeBounds, monoPaint)
        }
    }

    /** True when the host punched a blur region behind this icon for this frame. */
    private fun hasBackdrop() = ThemedIconBackdrop.isEnabled && host?.backdropActive == true

    override fun setAlpha(alpha: Int) {
        monoPaint.alpha = alpha
    }

    override fun updateFilter(filter: ColorFilter?) {
        monoPaint.colorFilter = getColorMultipliedFilter(colorFg, filter)
    }

    override fun isThemed() = true

    override fun getIconColor(info: BitmapInfo) = colorFg

    companion object {
        const val TAG: String = "ThemedIconDrawable"


        /** Get an int array representing background and foreground colors for themed icons */
        @JvmStatic
        fun getColors(context: Context): IntArray {
            val res = context.resources
            return intArrayOf(
                res.getColor(R.color.themed_icon_background_color),
                res.getColor(R.color.themed_icon_color),
                res.getColor(R.color.themed_icon_adaptive_background_color),
            )
        }
    }
}

class ThemedIconInfo(val mono: Bitmap, val colorBg: Int, val colorFg: Int) : DelegateFactory {

    override fun newDelegate(
        bitmapInfo: BitmapInfo,
        iconShape: IconShape,
        paint: Paint,
        host: FastBitmapDrawable,
    ) = ThemedIconDelegate(this, bitmapInfo, paint, host)
}
