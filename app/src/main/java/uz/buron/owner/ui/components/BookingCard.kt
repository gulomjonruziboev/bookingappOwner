package uz.buron.owner.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.buron.owner.R
import uz.buron.owner.domain.model.Booking
import uz.buron.owner.util.DateUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookingCard(
    booking: Booking,
    onConfirm: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPending = booking.status == "pending"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.clientName,
                    style = MaterialTheme.typography.titleMedium
                )
                StatusBadge(status = booking.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${booking.clientPhone}"))
                    context.startActivity(intent)
                },
                modifier = Modifier.padding(start = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Qo'ng'iroq qilish",
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(text = booking.clientPhone)
            }

            Text(
                text = DateUtils.formatDisplayDate(booking.date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                booking.sessions.forEach { session ->
                    SessionChip(session = session)
                }
            }

            if (isPending && (onConfirm != null || onCancel != null)) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onConfirm != null) {
                        OutlinedButton(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.confirm_booking))
                        }
                    }
                    if (onCancel != null) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.cancel_booking))
                        }
                    }
                }
            }
        }
    }
}
