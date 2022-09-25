package dev.fabik.bluetoothhid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import dev.fabik.bluetoothhid.bt.BluetoothController
import dev.fabik.bluetoothhid.ui.Dropdown
import dev.fabik.bluetoothhid.utils.SystemBroadcastReceiver
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    navHostController: NavHostController,
    bluetoothController: BluetoothController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device List") },
                actions = {
                    Dropdown(navHostController)
                }
            )
        }) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            DeviceList(bluetoothController)
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceList(bluetoothController: BluetoothController) {
    val foundDevices = remember {
        mutableListOf<BluetoothDevice>()
    }

    var isScanning by remember {
        mutableStateOf(false)
    }

    SystemBroadcastReceiver(BluetoothAdapter.ACTION_DISCOVERY_STARTED) {
        Log.d("Discovery", "isDiscovering")
        isScanning = true
        foundDevices.clear()
    }

    SystemBroadcastReceiver(BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
        Log.d("Discovery", "FinishedDiscovering")
        isScanning = false
    }

    SystemBroadcastReceiver(BluetoothDevice.ACTION_FOUND) {
        val device: BluetoothDevice? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it!!.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE,
                    BluetoothDevice::class.java
                )
            } else {
                it!!.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

        device?.let { dev ->
            if (!foundDevices.contains(device)) {
                foundDevices.add(dev)
            }
        }

        Log.d("Discovery", "Found: $device")
    }

    var isRefreshing by remember {
        mutableStateOf(false)
    }

    var devices by remember {
        mutableStateOf(bluetoothController.pairedDevices())
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            devices = bluetoothController.pairedDevices()
            bluetoothController.scanDevices()
            delay(1000)
            isRefreshing = false
        }
    }

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    SwipeRefresh(state = swipeRefreshState, onRefresh = {
        isRefreshing = true
    }, indicator = { state, trigger ->
        SwipeRefreshIndicator(
            state = state,
            refreshTriggerDistance = trigger,
            scale = true,
            backgroundColor = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.small,
        )
    }) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Scanned devices")
            }

            if (isScanning) {
                item {
                    CircularProgressIndicator()
                }
            } else {
                if (foundDevices.isEmpty()) {
                    item {
                        Text(
                            "(Swipe from top to refresh)",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    items(foundDevices) { d ->
                        // TODO: add setting to show devices with no name
                        Device(d.name ?: "<unknown>", d.address) {
                            bluetoothController.connect(d)
                        }
                    }
                }
            }


            item {
                Text("Paired devices")
            }

            if (devices.isEmpty()) {
                item {
                    Text(
                        "(No paired devices found)",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                items(devices.toList()) {
                    Device(it.name, it.address) {
                        bluetoothController.connect(it)
                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Device(name: String, address: String, onClick: () -> Unit) {
    Card(
        onClick, modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(4.dp)) {
            Text(name)
            Text(address)
        }
    }
}