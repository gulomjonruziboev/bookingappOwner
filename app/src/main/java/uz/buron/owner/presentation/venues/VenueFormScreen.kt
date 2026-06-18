package uz.buron.owner.presentation.venues

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import uz.buron.owner.R
import uz.buron.owner.ui.components.FormPhoneField
import uz.buron.owner.ui.components.FormTextField
import uz.buron.owner.ui.components.PrimaryButton
import uz.buron.owner.util.Constants
import uz.buron.owner.util.ImageUtils

private const val MAX_VENUE_IMAGES = 10

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VenueFormScreen(
    venueId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VenueFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var regionExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(venueId) {
        venueId?.let { viewModel.loadVenue(it) }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(uiState.fieldErrors) {
        if (uiState.fieldErrors.isNotEmpty()) {
            uiState.fieldErrors.values.firstOrNull()?.let { snackbarHostState.showSnackbar(it) }
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    fun onImagesPicked(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        val accepted = uris.filter { ImageUtils.isSupportedImage(context, it) }
        if (accepted.isNotEmpty()) {
            viewModel.addImages(accepted)
        }
        if (accepted.size < uris.size) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.image_format_error))
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = PickMultipleVisualMedia(MAX_VENUE_IMAGES)
    ) { uris -> onImagesPicked(uris) }

    fun launchImagePicker() {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (venueId == null) {
                            stringResource(R.string.new_venue)
                        } else {
                            stringResource(R.string.edit_venue)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoadingVenue) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FormTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = stringResource(R.string.venue_name),
                error = uiState.fieldErrors["name"]
            )
            FormTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = stringResource(R.string.venue_description),
                minLines = 3,
                error = uiState.fieldErrors["description"]
            )
            FormTextField(
                value = uiState.address,
                onValueChange = viewModel::updateAddress,
                label = stringResource(R.string.venue_address),
                error = uiState.fieldErrors["address"]
            )

            ExposedDropdownMenuBox(
                expanded = regionExpanded,
                onExpandedChange = { regionExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.region,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.venue_region)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(regionExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    isError = uiState.fieldErrors["region"] != null,
                    supportingText = uiState.fieldErrors["region"]?.let { { Text(it) } }
                )
                ExposedDropdownMenu(
                    expanded = regionExpanded,
                    onDismissRequest = { regionExpanded = false }
                ) {
                    Constants.UZBEK_REGIONS.forEach { region ->
                        DropdownMenuItem(
                            text = { Text(region) },
                            onClick = {
                                viewModel.updateRegion(region)
                                regionExpanded = false
                            }
                        )
                    }
                }
            }

            FormTextField(
                value = uiState.district,
                onValueChange = viewModel::updateDistrict,
                label = stringResource(R.string.venue_district),
                error = uiState.fieldErrors["district"]
            )
            FormPhoneField(
                digits = uiState.phoneDigits,
                onDigitsChange = viewModel::updatePhoneDigits,
                label = stringResource(R.string.field_phone),
                error = uiState.fieldErrors["phone"]
            )
            FormTextField(
                value = uiState.pricePerSession,
                onValueChange = viewModel::updatePrice,
                label = stringResource(R.string.venue_price),
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                error = uiState.fieldErrors["price"]
            )
            FormTextField(
                value = uiState.capacity,
                onValueChange = viewModel::updateCapacity,
                label = stringResource(R.string.venue_capacity),
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                error = uiState.fieldErrors["capacity"]
            )
            FormTextField(
                value = uiState.mapLink,
                onValueChange = viewModel::updateMapLink,
                label = stringResource(R.string.venue_map_link)
            )

            Text(stringResource(R.string.venue_images), style = MaterialTheme.typography.titleMedium)
            uiState.fieldErrors["images"]?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.keptImageUrls.forEach { url ->
                    Box {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { viewModel.removeKeptImage(url) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                uiState.newImageUris.forEach { uri ->
                    Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { viewModel.removeNewImage(uri) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                IconButton(onClick = { launchImagePicker() }) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = stringResource(R.string.venue_images))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            PrimaryButton(
                text = if (uiState.isLoading) {
                    stringResource(R.string.saving)
                } else {
                    stringResource(R.string.save)
                },
                onClick = { viewModel.submit(onSaved) },
                enabled = !uiState.isLoading
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
