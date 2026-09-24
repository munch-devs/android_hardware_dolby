package com.aosp.dolby.geq.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aosp.dolby.geq.data.BandGain
import kotlin.math.roundToInt

@Composable
fun EqualizerBands(viewModel: EqualizerViewModel) {
    val preset by viewModel.preset.collectAsState()
    val bandGains = preset.bandGains

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thước đo dB bên trái
        BandGainDbScale()

        // Các dải tần số có thể vuốt ngang nếu màn hình nhỏ
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(bandGains.size) { index ->
                VerticalBandGainSlider(
                    bandGain = bandGains[index],
                    onValueChangeFinished = { newValue ->
                        viewModel.setGain(index, newValue)
                    }
                )
            }
        }
    }
}

@Composable
private fun BandGainDbScale() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(end = 12.dp, start = 16.dp, bottom = 48.dp, top = 24.dp), // Căn chỉnh khớp với thanh trượt
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        Text("+10", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("0", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("-10", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerticalBandGainSlider(
    bandGain: BandGain,
    onValueChangeFinished: (Int) -> Unit
) {
    // Trạng thái cục bộ để thanh trượt mượt mà khi vuốt
    var sliderValue by remember(bandGain.gain) { mutableFloatStateOf(bandGain.gain.toFloat()) }
    
    // Rút gọn tên tần số (ví dụ: 16000 -> 16k)
    val freqLabel = if (bandGain.band >= 1000) "${bandGain.band / 1000}k" else "${bandGain.band}"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(48.dp)
    ) {
        // Hiển thị giá trị gain hiện tại phía trên thanh trượt
        Text(
            text = "${sliderValue.roundToInt()}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (sliderValue == 0f) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Thanh trượt xoay dọc
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChangeFinished(sliderValue.roundToInt()) },
            valueRange = -10f..10f,
            steps = 19, // Bậc nhảy từng 1 dB
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    rotationZ = -90f
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .layout { measurable, constraints ->
                    // Đảo ngược chiều rộng và chiều cao do xoay 90 độ
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxWidth,
                        )
                    )
                    layout(placeable.height, placeable.width) {
                        placeable.place(-placeable.width / 2 + placeable.height / 2, -placeable.height / 2 + placeable.width / 2)
                    }
                },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        // Nhãn tần số phía dưới
        Text(
            text = freqLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}