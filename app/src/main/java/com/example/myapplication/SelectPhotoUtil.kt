package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 作者 : pengjiaqi
 * 邮箱 : pengjiaqi@richinfo.cn
 * 日期 : 2019/12/31 17:25
 * 功能 :
 */

class SelectPhotoUtil(private val activity: Activity, private val isCrop: Boolean, private val onResult: OnResult) {

    interface OnResult {
        fun onResultFile(file: File)
    }

    companion object {
        //系统拍照
        private const val REQUEST_SYSTEM_TAKE_PICTURE = 1
        //相册
        private const val REQUEST_SYSTEM_GALLERY = 2
        //系统裁剪
        private const val REQUEST_SYSTEM_ZOOM = 3
    }


    var file: File? = null

    /**
     * 权限申请
     */
    @SuppressLint("NewApi")
    fun requestReadExternalPermission(requestCode: Int) {
        if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (!activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                activity.requestPermissions(arrayOf(Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE), requestCode)
            }
        } else {
            if (requestCode == REQUEST_SYSTEM_TAKE_PICTURE) {
                //去拍照
                getCamera()
            } else {
                //去相册
                getGallery()
            }
        }
    }

    /**
     * 拍照方式选择
     */
    fun showDialog() {
        AlertDialog.Builder(activity).setItems(arrayOf("拍照", "从相册选择")) { _, index ->
            if (index == 0) {
                //拍照
                requestReadExternalPermission(REQUEST_SYSTEM_TAKE_PICTURE)
            } else {
                //从相册选择
                requestReadExternalPermission(REQUEST_SYSTEM_GALLERY)
            }
        }.create().show()
    }

    /**
     * 拍照
     */
    private fun getCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val imgName = SimpleDateFormat("yyyyMMddHHmmss").format(Date()) + ".jpg"
            file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), imgName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(activity,
                        "com.example.myapplication.fileprovider", file!!))
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            } else {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file))
            }

            activity.startActivityForResult(intent, REQUEST_SYSTEM_TAKE_PICTURE)
        } else {
            Toast.makeText(activity, "储存卡不可用！", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 从相册选择
     */
    private fun getGallery() {
        var intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        val imgName = SimpleDateFormat("yyyyMMddHHmmss").format(Date()) + ".jpg"
        file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), imgName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(activity,
                    "com.example.myapplication.fileprovider", file!!))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        activity.startActivityForResult(intent, REQUEST_SYSTEM_GALLERY)
    }

    /**
     * 选择图片和拍照回调
     */
    fun attachToActivityForResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                //拍照回调
                REQUEST_SYSTEM_TAKE_PICTURE -> {
                    if (isCrop) {
                        if (Build.VERSION.SDK_INT >= 24) {
                            getZoom(FileProvider.getUriForFile(activity,
                                    "com.example.myapplication.fileprovider", file!!))
                        } else {
                            getZoom(Uri.fromFile(file!!))
                        }
                    } else {
                        onResult.onResultFile(file!!)
                    }
                }
                //相册选择回调
                REQUEST_SYSTEM_GALLERY -> {
                    var imgUri = File(GetImagePath.getPath(activity, data!!.data))
                    if (isCrop) {
                        if (Build.VERSION.SDK_INT >= 24) {
                            val dataUri: Uri = FileProvider.getUriForFile(activity,
                                    "com.example.myapplication.fileprovider", imgUri)
                            getZoom(dataUri)
                        } else {
                            getZoom(data.data!!)
                        }
                    } else {
                        onResult.onResultFile(imgUri)
                    }
                }
                //缩放裁剪回调
                REQUEST_SYSTEM_ZOOM -> {
                    onResult.onResultFile(file!!)
                }
            }
        }
    }

    /**
     * 图片裁剪
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun getZoom(uri: Uri) {
        if (uri == null) {
            return
        }
        val intent = Intent("com.android.camera.action.CROP")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val outPutUri = Uri.fromFile(file)
            intent.run {
                setDataAndType(uri, "image/*")
                putExtra(MediaStore.EXTRA_OUTPUT, outPutUri)
                //去除默认的人脸识别，否则和剪裁匡重叠
                putExtra("noFaceDetection", false)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        } else {
            val outPutUri = Uri.fromFile(file)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                //这个方法是处理4.4以上图片返回的Uri对象不同的处理方法
                val url = GetImagePath.getPath(activity, uri)
                intent.setDataAndType(Uri.fromFile(File(url)), "image/*")
            } else {
                intent.setDataAndType(uri, "image/*")
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri)
        } // 设置裁剪
        intent.putExtra("crop", "true")
        // aspectX aspectY 是宽高的比例
        //        intent.putExtra("aspectX", 1)
        //        intent.putExtra("aspectY", 1)
        // outputX outputY 是裁剪图片宽高
        //        intent.putExtra("outputX", 200)
        //        intent.putExtra("outputY", 200)
        intent.putExtra("return-data", false)
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG) // 图片格式
        activity.startActivityForResult(intent, REQUEST_SYSTEM_ZOOM)
    }


    /**
     * 权限回调
     */
    fun attrRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //权限申请通过
            when (requestCode) {
                REQUEST_SYSTEM_TAKE_PICTURE -> {
                    getCamera()
                }
                REQUEST_SYSTEM_GALLERY -> {
                    getGallery()
                }
            }
        } else {
            Toast.makeText(activity, "授权失败！", Toast.LENGTH_SHORT).show()
        }
    }


}