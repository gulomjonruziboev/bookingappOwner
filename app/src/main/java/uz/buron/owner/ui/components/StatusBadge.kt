package uz.buron.owner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.buron.owner.ui.theme.ErrorRed
import uz.buron.owner.ui.theme.SuccessGreen
import uz.buron.owner.ui.theme.TextPrimary
import uz.buron.owner.ui.theme.WarningYellow
import uz.buron.owner.util.Constants
import uz.buron.owner.util.statusLabel
import uz.buron.owner.util.venueStatusLabel

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier,
    isVenueStatus: Boolean = false
) {
    val label = if (isVenueStatus) venueStatusLabel(status) else statusLabel(status)
    val (background, foreground) = badgeColors(status, isVenueStatus)

    Text(
        text = label,
        modifier = modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = foreground
    )
}

private fun badgeColors(status: String, isVenueStatus: Boolean): Pair<Color, Color> {
    return when (status) {
        "pending" -> WarningYellow.copy(alpha = 0.2f) to TextPrimary
        "confirmed", "approved" -> SuccessGreen.copy(alpha = 0.15f) to SuccessGreen
        "cancelled", "rejected" -> ErrorRed.copy(alpha = 0.15f) to ErrorRed
        else -> Color(0xFFE2E8F0) to TextPrimary
    }
}

@Composable
fun SessionChip(
    session: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = Constants.SESSION_LABELS[session] ?: session,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary
    )
}
