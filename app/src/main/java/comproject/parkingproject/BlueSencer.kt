package comproject.parkingproject

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import comproject.parkingproject.databinding.ActivityMainBinding
import java.io.IOException
import java.io.InputStream
import java.util.UUID

class BlueSencer : AppCompatActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var device: BluetoothDevice? = null
    private var inputStream: InputStream? = null
    private lateinit var binding: ActivityMainBinding // 뷰 바인딩 객체를 lateinit으로 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root) // 여기서 binding.root 사용

        // 블루투스 어댑터 초기화
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // 연결하려는 블루투스 디바이스(아두이노 모듈) 선택
        device = bluetoothAdapter?.getRemoteDevice("00:00:00:00:00") // 아두이노 모듈의 MAC 주소 입력
        try {
            // 소켓 생성 및 연결 시도
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
                    var a =1
                    // UI 업데이트 (메인 스레드에서)
                    runOnUiThread { binding.distanceTextView.text = "Distance: ${data.trim()}" } // 뷰 바인딩을 통해 UI 업데이트
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

    companion object {
        // UUID는 블루투스 모듈에 따라 다를 수 있음
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
