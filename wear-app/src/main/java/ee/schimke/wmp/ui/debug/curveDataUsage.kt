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

package ee.schimke.wmp.ui.debug

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.CurvedAlignment
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.material.Icon
import com.google.android.horologist.networks.data.NetworkStatus
import com.google.android.horologist.networks.data.NetworkType
import com.google.android.horologist.networks.data.Networks
import com.google.android.horologist.networks.data.Status

public fun CurvedScope.curveDataUsage(
    modifier: Modifier = Modifier,
    networkStatus: Networks,
) {
    networkStatus.networks.forEach {
        curvedComposable(radialAlignment = CurvedAlignment.Radial.Outer) {
            Icon(
                modifier = modifier
                    .size(12.dp),
                imageVector = it.type.icon,
                contentDescription = null,
                tint = it.tint(active = it.id == networkStatus.activeNetwork?.id)
            )
        }
    }
}

@Composable
public fun LinearDataUsage(
    networkStatus: Networks,
) {
    networkStatus.networks.forEach {
        Icon(
            modifier = Modifier.size(12.dp),
            imageVector = it.type.icon,
            contentDescription = null,
            tint = it.tint(active = it.id == networkStatus.activeNetwork?.id)
        )
    }
}

private fun NetworkStatus.tint(active: Boolean): Color {
    return if (!active && this.status == com.google.android.horologist.networks.data.Status.Available)
        Color.Blue
    else when (this.status) {
        is Status.Available -> Color.Green
        is Status.Losing -> Color.Yellow
        is Status.Lost -> Color.Gray
        is Status.Unknown -> Color.LightGray
    }
}

internal val NetworkType.icon
    get() = when (this) {
        is NetworkType.Wifi -> Icons.Filled.Wifi
        is NetworkType.Cellular -> Icons.Filled.SignalCellularAlt
        is NetworkType.Bluetooth -> Icons.Filled.Bluetooth
        else -> Icons.Filled.HelpOutline
    }
