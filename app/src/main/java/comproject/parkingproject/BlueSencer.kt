package comproject.parkingproject

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import comproject.parkingproject.databinding.ActivityMainBinding
import java.io.IOException
import java.io.InputStream
import java.util.UUID

class BlueSencer : AppCompatActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var device: BluetoothDevice? = null
    private var inputStream: InputStream? = null
    private lateinit var binding: ActivityMainBinding

    companion object {
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val REQUEST_BLUETOOTH_PERMISSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 블루투스 어댑터 초기화
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // 연결하려는 블루투스 디바이스(아두이노 모듈) 선택
        device = bluetoothAdapter?.getRemoteDevice("00:00:00:00:00") // 아두이노 모듈의 MAC 주소 입력

        // 블루투스 권한 확인
        checkBluetoothPermissions()
    }

    private fun checkBluetoothPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_BLUETOOTH_PERMISSION
            )
        } else {
            connectToBluetooth() // 권한이 허용되면 연결
        }
    }

    private fun connectToBluetooth() {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            bluetoothSocket = device?.createRfcommSocketToServiceRecord(MY_UUID)
            bluetoothSocket?.connect()

            // 입력 스트림 설정
            inputStream = bluetoothSocket?.inputStream
            Thread(BluetoothDataReceiver()).start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // 아두이노로부터 데이터를 수신하는 스레드
    private inner class BluetoothDataReceiver : Runnable {
        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    // 아두이노로부터 데이터 읽기
                    bytes = inputStream?.read(buffer) ?: break
                    val data = String(buffer, 0, bytes)
                    runOnUiThread {
                        // UI 업데이트 (메인 스레드에서)
                        binding.distanceTextView.text = "Distance: ${data.trim()}" // 뷰 바인딩을 통해 UI 업데이트
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            // 블루투스 소켓 닫기
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_BLUETOOTH_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    connectToBluetooth() // 권한이 허용되면 연결
                } else {
                    // 권한 거부에 대한 처리
                }
            }
        }
    }
}
