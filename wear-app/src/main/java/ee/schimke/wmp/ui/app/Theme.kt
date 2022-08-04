/*
 * Copyright 2022 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.schimke.wmp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val wearColorPalette: Colors = Colors(
    primary = Color(0xFF7FCFFF),
    primaryVariant = Color(0xFF3998D3),
    onPrimary = Color(0xFF003355),
    secondary = Color(0xFF6DD58C),
    secondaryVariant = Color(0xFF1EA446),
    onSecondary = Color(0xFF0A3818),
    surface = Color(0xFF303030),
    onSurface = Color(0xFFE3E3E3),
    onSurfaceVariant = Color(0xFFC4C7C5),
    background = Color.Black,
    onBackground = Color.White,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF370906)
)

@Composable
fun WearMediaPlayerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = wearColorPalette,
        content = content
    )
}
