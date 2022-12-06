package com.example.pr24_kargin

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var  outputDir: File
    private var imageCapture: ImageCapture? = null
    private lateinit var pv_camera: androidx.camera.view.PreviewView
    private lateinit var iV : ImageView

    companion object{
        private const val TAG = "CameraX"
        private const val  FILE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val  PERMISSION_CODE = 10
        private val PERMISSION = arrayOf(Manifest.permission.CAMERA)
    }

    private fun allPermissonGranted() = PERMISSION.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    private var imageView: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pv_camera = findViewById(R.id.pv_camera)
        iV = findViewById(R.id.imageView)
        if (allPermissonGranted()){
            startCamera()
        }else{
            ActivityCompat.requestPermissions(this, PERMISSION, PERMISSION_CODE )
        }
        outputDir = getOutputDir()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun getOutputDir():File{
    val mediaDir = externalMediaDirs.firstOrNull()?.absoluteFile.let{
        File(it, resources.getString(R.string.app_name)).apply {
            mkdir()
        }
    }
       return if (mediaDir != null && mediaDir.exists()) mediaDir
        else filesDir
    }

    fun onClick(view: View?) {
        Toast.makeText(this,"начало фотографирования", Toast.LENGTH_SHORT)
        takePhoto()
    }

    private fun takePhoto(){
        val imageCapture = imageCapture?:return
        val photoFile = File(outputDir, SimpleDateFormat(FILE_FORMAT, Locale.US)
            .format(System.currentTimeMillis()) + ".jpg")
        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOption ,
            ContextCompat.getMainExecutor(baseContext),  object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = Uri.fromFile(photoFile)
                    val msg = "Photo :$uri"
                    iV.setImageURI(uri)
                    Toast.makeText(baseContext, msg,Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(baseContext, "Ошибка сохранения: ${exception.message}",
                        Toast.LENGTH_SHORT).show()
                }
            } )
    }


    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                .also {
                    it.setSurfaceProvider(pv_camera.surfaceProvider)
                }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            imageCapture = ImageCapture.Builder().build()
            try{
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            }catch (e: Exception){
                Log.e(TAG,"Bind error", e)
            }
        }, ContextCompat.getMainExecutor(this))

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE){
            startCamera()
        }
        else{
            Toast.makeText(this,"Вы не дали разрешение на открытие камеры", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}