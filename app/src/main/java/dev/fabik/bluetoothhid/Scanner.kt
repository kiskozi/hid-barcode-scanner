package dev.fabik.bluetoothhid

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.TorchState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.fabik.bluetoothhid.ui.*
import dev.fabik.bluetoothhid.ui.theme.Neutral95
import dev.fabik.bluetoothhid.utils.DeviceInfo
import dev.fabik.bluetoothhid.utils.PreferenceStore
import dev.fabik.bluetoothhid.utils.rememberPreferenceDefault

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Scanner(
    navController: NavController,
    currentDevice: BluetoothDevice?,
    onDisconnect: () -> Unit,
    sendText: (String) -> Unit
) {
    var currentBarcode by rememberSaveable { mutableStateOf<String?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    Scaffold(
        topBar = {
            ScannerAppBar(camera, onDisconnect, navController)
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            currentDevice?.let {
                SendToDeviceFAB(currentBarcode, sendText)
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            RequiresCameraPermission {
                CameraPreviewArea({ camera = it }) { value, send ->
                    currentBarcode = value
                    if (send) {
                        sendText(value)
                    }
                }
                BarcodeValue(currentBarcode)
            }
            DeviceInfoDialog(currentDevice)
        }
    }
}

@Composable
private fun CameraPreviewArea(
    onCameraReady: (Camera) -> Unit,
    onBarcodeDetected: (String, Boolean) -> Unit,
) {
    val context = LocalContext.current

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val playSound by rememberPreferenceDefault(PreferenceStore.PLAY_SOUND)

    val toneGenerator = remember {
        ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
    }

    val autoSend by rememberPreferenceDefault(PreferenceStore.AUTO_SEND)

    val vibrate by rememberPreferenceDefault(PreferenceStore.VIBRATE)

    CameraPreview(onCameraReady = onCameraReady) {
        onBarcodeDetected(it, autoSend)
        if (playSound) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 75)
        }
        if (vibrate) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }
}

@Composable
private fun BoxScope.BarcodeValue(currentBarcode: String?) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        Modifier
            .fillMaxHeight(0.3f)
            .fillMaxWidth()
            .align(Alignment.TopStart),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val copiedString = stringResource(R.string.copied_to_clipboard)

        currentBarcode?.let {
            val text = AnnotatedString(it, SpanStyle(Neutral95), ParagraphStyle(TextAlign.Center))
            ClickableText(
                text,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            ) {
                clipboardManager.setText(text)
                Toast.makeText(context, copiedString, Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Text(
                stringResource(R.string.scan_code_to_start),
                color = Neutral95,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SendToDeviceFAB(
    currentBarcode: String?,
    onClick: (String) -> Unit
) {
    currentBarcode?.let {
        ExtendedFloatingActionButton(onClick = { onClick(it) }) {
            Icon(Icons.Filled.Send, "Send")
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.send_to_device))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ScannerAppBar(
    camera: Camera?,
    onDisconnect: () -> Unit,
    navController: NavController
) {
    TopAppBar(title = { Text(stringResource(R.string.scanner)) }, actions = {
        camera?.let {
            if (it.cameraInfo.hasFlashUnit()) {
                ToggleFlashButton(it)
            }
        }
        IconButton(onDisconnect) {
            Icon(Icons.Default.BluetoothDisabled, "Disconnect")
        }
        Dropdown(navController)
    })
}

@Composable
fun ToggleFlashButton(camera: Camera) {
    val torchState by camera.cameraInfo.torchState.observeAsState()

    IconButton(onClick = {
        camera.cameraControl.enableTorch(
            when (torchState) {
                TorchState.OFF -> true
                else -> false
            }
        )
    }) {
        Icon(
            when (torchState) {
                TorchState.OFF -> Icons.Default.FlashOn
                else -> Icons.Default.FlashOff
            }, "Flash"
        )
    }
}

@Suppress("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.DeviceInfoDialog(device: BluetoothDevice?) {
    val dialogState = rememberDialogState()

    device?.let {
        ElevatedCard(
            onClick = {
                dialogState.open()
            },
            Modifier
                .padding(12.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, "Info")
                Text(stringResource(R.string.connected_with, it.name))
            }
        }

        InfoDialog(dialogState, stringResource(R.string.info), onDismiss = { close() }) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(stringResource(R.string.name) + ":", fontWeight = FontWeight.Bold)
                    Text(it.name)
                }

                item {
                    Text(stringResource(R.string.mac_address) + ":", fontWeight = FontWeight.Bold)
                    Text(it.address)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    item {
                        Text(stringResource(R.string.alias) + ":", fontWeight = FontWeight.Bold)
                        Text(it.alias ?: "")
                    }
                }

                item {
                    Text(stringResource(R.string.type) + ":", fontWeight = FontWeight.Bold)
                    Text(
                        when (it.type) {
                            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
                            BluetoothDevice.DEVICE_TYPE_LE -> "LE"
                            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
                            BluetoothDevice.DEVICE_TYPE_UNKNOWN -> "Unknown"
                            else -> "?"
                        } + " (${it.type})"
                    )
                }

                item {
                    Text(stringResource(R.string.clazz) + ":", fontWeight = FontWeight.Bold)
                    with(it.bluetoothClass.majorDeviceClass) {
                        val classString = remember(it) {
                            DeviceInfo.deviceClassString(this)
                        }
                        Text("$classString (${this})")
                    }
                }

                item {
                    Text(stringResource(R.string.services) + ":", fontWeight = FontWeight.Bold)
                    val serviceInfo = remember(it) {
                        DeviceInfo.deviceServiceInfo(it.bluetoothClass)
                    }
                    serviceInfo.forEach {
                        Text(it)
                    }
                }

                item {
                    Text(stringResource(R.string.uuids) + ":", fontWeight = FontWeight.Bold)
                    it.uuids.forEach {
                        Text(it.toString())
                    }
                }
            }
        }
    }
}