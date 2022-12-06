package com.example.demo

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.demo.util.StudentCardService
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var iTcpService: ITcpService? = null

    private val intentService: Intent by lazy { Intent(this, StudentCardService::class.java) }

    //一般信息：
    //XX,YYYYYYYYYYYYYYY,V2,HHMMSS,S,latitude,D,longitude,G,speed,direction,DDMMYY,tracker _status#
    //*WT,868976030203477,V2,151744,92,1,56,0,A,2250.2308,N,11391.6231,E,0.11,237,170721,FFFFDFFF#
    private var v2Signal = ""

    //确认信息：
    //XX,YYYYYYYYYYYYYYY,V4,CMD,seq,para,HHMMSS,S,latitude,D,longitude,G,speed,direction,DDMMYY,tracker _status#
    //*WT,868976030203477,V4,D1,150042,150102,A,2250.2304,N,11391.6088,E,0.18,0,460,1,0,1,9519,102264332,64,64,0,58,170721,FDFFFFFF#
    private var v4Signal = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        start_tcp.setOnClickListener {
            //bindService(mServiceIntent, conn, Context.BIND_AUTO_CREATE)
            startService(intentService)
        }

        send_tcp.setOnClickListener {
            try {
                Log.d(StudentCardService.TAG, "iTcpService是否为空：$iTcpService")
                //                if (iTcpService == null) {
                //                    Toast.makeText(this@MainActivity, "没有连接，可能是服务器已断开", Toast.LENGTH_SHORT).show()
                //                } else {
                //                    iTcpService?.sendMessage("===============Send===============")
                //                }
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }

        stop_tcp.setOnClickListener {
            //            TcpTask.sharedCenter().disconnect()
            stopService(intentService)
        }
        TcpTask.sharedCenter().setConnectedCallback {
            Log.d("TcpTask", "v2Signal:$v2Signal")
            TcpTask.sharedCenter().send(v2Signal.toByteArray())
        }
        TcpTask.sharedCenter().setDisconnectedCallback {

        }
        TcpTask.sharedCenter().setReceivedCallback {

        }
//        start_tcp.animate()
//            .alpha(1f)
//            .translationY(0f)
//            .setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN)
//            .setStartDelay(delay)
//            .setDuration(DOZE_ANIMATION_ELEMENT_DURATION);

        val cn = ComponentName("com.android.calendar","com.android.calendar.AllInOneActivity");

    }


    override fun onStart() {
        super.onStart()

    }

    override fun onPause() {
        super.onPause()
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(){
        // 获取系统LocationManager服务
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        // 从GPS获取最近的定位信息
//        val location: Location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        // 查找到服务信息
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE // 高精度

        criteria.isAltitudeRequired = false
        criteria.isBearingRequired = false
        criteria.isCostAllowed = true
        criteria.powerRequirement = Criteria.POWER_LOW // 低功耗

        val provider = locationManager.getBestProvider(criteria, true) // 获取GPS信息

        val location = locationManager.getLastKnownLocation(provider) // 通过GPS获取位置

        Log.d("TcpService", "location.latitude:${location.latitude}")
        Log.d("TcpService", "location.longitude:${location.longitude}")

        // 设置每2秒获取一次GPS的定位信息
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,2000,8f,object :LocationListener{
            override fun onLocationChanged(location: Location?) {
                Log.d("TcpService", "onLocationChanged:$location")
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                TODO("Not yet implemented")
            }

            override fun onProviderEnabled(provider: String?) {
                TODO("Not yet implemented")
            }

            override fun onProviderDisabled(provider: String?) {
                TODO("Not yet implemented")
            }

        })
    }

    private val conn: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) { // 未连接为空
            iTcpService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) { // 已连接
            iTcpService = ITcpService.Stub.asInterface(service)
        }
    }

}