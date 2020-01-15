package com.example.takephoto;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 作者 : pengjiaqi
 * 邮箱 : pengjiaqi@richinfo.cn
 * 日期 : 2020/1/10 14:50
 * 功能 :
 */
public class SelectPhotoUtil {

    //系统拍照
    private static int REQUEST_SYSTEM_TAKE_PICTURE = 1;
    //相册
    private static int REQUEST_SYSTEM_GALLERY = 2;
    //系统裁剪
    private static int REQUEST_SYSTEM_ZOOM = 3;


    private Boolean mIsCrop = false;
    private IOnImageResult mOnResult;
    private Activity mActivity;

    //图片文件
    private File file;

    //单例
    private volatile static SelectPhotoUtil instance = null;

    // 私有化构造方法
    private SelectPhotoUtil(Activity activity) {
        mActivity = activity;
    }

    public static SelectPhotoUtil getInstance(Activity activity) {
        if (instance == null) {
            synchronized (SelectPhotoUtil.class) {
                if (instance == null) {
                    instance = new SelectPhotoUtil(activity);
                }
            }

        }
        return instance;
    }


    /**
     * 是否开启裁剪
     */
    public void setIsCrop(Boolean isCrop) {
        mIsCrop = isCrop;
    }


    /**
     * 图片最终回调的监听
     *
     * @param onResult
     */
    public void setIOnImageResult(IOnImageResult onResult) {
        mOnResult = onResult;
    }

    /**
     * 弹出选择拍照还是相册的选择框
     */
    public void showDialog() {
        new AlertDialog.Builder(mActivity).setItems(new CharSequence[]{"拍照", "从相册选择"},
                new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onClick(DialogInterface dialogInterface, int index) {
                        if (index == 0) {
                            //拍照
                            requestReadExternalPermission(REQUEST_SYSTEM_TAKE_PICTURE);
                        } else {
                            //从相册选择
                            requestReadExternalPermission(REQUEST_SYSTEM_GALLERY);
                        }

                    }
                }).create().show();

    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestReadExternalPermission(int requestCode) {
        if ((mActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)||
                (mActivity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)||
                (mActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
//            if (!mActivity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                mActivity.requestPermissions(new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
//            }
        } else {
            //权限同意了的话
            if (requestCode == REQUEST_SYSTEM_TAKE_PICTURE) {
                //去拍照
                getCamera();
            } else {
                //去相册
                getGallery();
            }
        }
    }

    private void getCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String imgName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg";
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), imgName);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(mActivity,
                        "com.example.myapplication.fileprovider", file));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } else {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            }

            mActivity.startActivityForResult(intent, REQUEST_SYSTEM_TAKE_PICTURE);
        } else {
            Toast.makeText(mActivity, "储存卡不可用！", Toast.LENGTH_SHORT).show();
        }

    }


    private void getGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        String imgName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg";
        file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), imgName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(mActivity,
                    "com.example.myapplication.fileprovider", file));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        mActivity.startActivityForResult(intent, REQUEST_SYSTEM_GALLERY);
    }

    /**
     * 选择图片和拍照回调
     */
    public void attachToActivityForResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_SYSTEM_TAKE_PICTURE) {
                if (mIsCrop) {
                    if (Build.VERSION.SDK_INT >= 24) {
                        getZoom(FileProvider.getUriForFile(mActivity,
                                "com.example.myapplication.fileprovider", file));
                    } else {
                        getZoom(Uri.fromFile(file));
                    }
                } else {
                    mOnResult.onResultFile(file);
                }

            } else if (requestCode == REQUEST_SYSTEM_GALLERY) {
                File imgUri = new File(GetImagePath.getPath(mActivity, data.getData()));
                if (mIsCrop) {
                    if (Build.VERSION.SDK_INT >= 24) {
                        Uri uriForFile = FileProvider.getUriForFile(mActivity,
                                "com.example.myapplication.fileprovider", imgUri);
                        getZoom(uriForFile);
                    } else {
                        getZoom(data.getData());
                    }
                } else {
                    mOnResult.onResultFile(imgUri);
                }

            } else if (requestCode == REQUEST_SYSTEM_ZOOM) {
                mOnResult.onResultFile(file);

            } else {
                throw new IllegalStateException("Unexpected value: " + requestCode);
            }
        }
    }


    /**
     * 图片裁剪
     */
    @SuppressLint("ObsoleteSdkInt")
    private void getZoom(Uri uri) {
        if (uri == null) {
            return;
        }
        Intent intent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri outPutUri = Uri.fromFile(file);
            intent.setDataAndType(uri, "image/*");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri);
            //去除默认的人脸识别，否则和剪裁匡重叠
            intent.putExtra("noFaceDetection", false);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            Uri outPutUri = Uri.fromFile(file);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                //这个方法是处理4.4以上图片返回的Uri对象不同的处理方法
                String url = GetImagePath.getPath(mActivity, uri);
                intent.setDataAndType(Uri.fromFile(new File(url)), "image/*");
            } else {
                intent.setDataAndType(uri, "image/*");
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri);
        } // 设置裁剪
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        //        intent.putExtra("aspectX", 1)
        //        intent.putExtra("aspectY", 1)
        // outputX outputY 是裁剪图片宽高
        //        intent.putExtra("outputX", 200)
        //        intent.putExtra("outputY", 200)
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG); // 图片格式
        mActivity.startActivityForResult(intent, REQUEST_SYSTEM_ZOOM);
    }

    /**
     * 权限回调
     */
    public void attrRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                             @NonNull int[] grantResults) {
        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //权限申请通过
            if (requestCode == REQUEST_SYSTEM_TAKE_PICTURE) {
                getCamera();
            } else if (requestCode == REQUEST_SYSTEM_GALLERY) {
                getGallery();
            }
        } else {
            //授权失败，去设置界面
            SystemPermissionUtil.GoToSetting(mActivity);
            Toast.makeText(mActivity, "请打开相机权限！", Toast.LENGTH_SHORT).show();
        }
    }

}


