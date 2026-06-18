package uz.buron.owner.presentation.venues

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import uz.buron.owner.BuildConfig
import uz.buron.owner.ui.components.PrimaryButton
import uz.buron.owner.ui.components.StarRating
import uz.buron.owner.ui.components.StatusBadge
import uz.buron.owner.util.PriceFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueDetailScreen(
    venueId: String,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToReviews: (String) -> Unit,
    onNavigateToExternalBooking: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VenueDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val venue = uiState.venue

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(venue?.name ?: "To'yxona") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            venue == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error ?: "Topilmadi")
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val imageUrl = venue.images.firstOrNull()?.let { path ->
                        if (path.startsWith("http")) path else "${BuildConfig.UPLOADS_BASE_URL}$path"
                    }
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = venue.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(venue.name, style = MaterialTheme.typography.headlineSmall)
                        StatusBadge(status = venue.status, isVenueStatus = true)
                    }
                    StarRating(rating = venue.rating)
                    Text(
                        "${PriceFormatter.format(venue.pricePerSession)} / sessiya",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("${venue.region}, ${venue.district}")
                    Text(venue.address, style = MaterialTheme.typography.bodyMedium)
                    Text(venue.description, style = MaterialTheme.typography.bodyMedium)
                    Text("Sig'im: ${venue.capacity} kishi")
                    Text("Telefon: ${venue.phone}")
                    venue.mapLink?.let { link ->
                        TextButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                        }) {
                            Text("Xaritada ko'rish")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    PrimaryButton(text = "Tahrirlash", onClick = { onNavigateToEdit(venue.id) })
                    PrimaryButton(text = "Sharhlarni ko'rish", onClick = { onNavigateToReviews(venue.id) })
                    PrimaryButton(
                        text = "Tashqi bron qo'shish",
                        onClick = { onNavigateToExternalBooking(venue.id) }
                    )
                }
            }
        }
    }
}
