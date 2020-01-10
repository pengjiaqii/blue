package com.example.myapplication

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.takephoto.SelectPhotoUtil
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException


class MainActivity : AppCompatActivity() {

    private var selectPhotoUtil: SelectPhotoUtil? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selectPhotoUtil = SelectPhotoUtil.getInstance(this)

        //是否打开裁剪，默认为false，关闭状态
        selectPhotoUtil?.setIsCrop(true)

        selectPhotoUtil?.setIOnImageResult { file ->
            Log.d("jade", "文件File：$file")
            if (null != file) {
                image.setImageBitmap(getLocalBitmap(file))
            } else {
                image.setImageResource(R.mipmap.ic_launcher_round)
            }
        }


        showDialogTv.setOnClickListener {
            selectPhotoUtil?.showDialog()
        }


    }

    /**
     * 加载本地图片
     * @param file
     * @return
     */
    fun getLocalBitmap(file: File): Bitmap? {
        return try {
            val fis = FileInputStream(file)
            BitmapFactory.decodeStream(fis)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }

    }

    /**
     * 权限回调
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        selectPhotoUtil?.attrRequestPermissionsResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    /**
     * 拍照回调
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        selectPhotoUtil?.attachToActivityForResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }


}
