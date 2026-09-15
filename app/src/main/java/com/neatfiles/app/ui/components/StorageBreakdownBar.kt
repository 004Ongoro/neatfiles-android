package com.neatfiles.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neatfiles.app.core.model.CategoryStat
import com.neatfiles.app.core.util.Formatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StorageBreakdownBar(
    categoryStats: List<CategoryStat>,
    totalBytes: Long,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Segmented Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (categoryStats.isEmpty() || totalBytes == 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                categoryStats.forEach { stat ->
                    val weight = (stat.totalBytes.toFloat() / totalBytes).coerceAtLeast(0.01f)
                    val animatedWeight by animateFloatAsState(
                        targetValue = weight,
                        animationSpec = tween(600),
                        label = "bar_weight"
                    )
                    Box(
                        modifier = Modifier
                            .weight(animatedWeight)
                            .height(14.dp)
                            .background(Color(stat.category.colorHex))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categoryStats.take(5).forEach { stat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(stat.category.colorHex))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${stat.category.displayName} (${Formatters.formatFileSize(stat.totalBytes)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
