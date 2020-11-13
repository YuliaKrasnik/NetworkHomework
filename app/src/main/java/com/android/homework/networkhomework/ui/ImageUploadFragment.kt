package com.android.homework.networkhomework.ui

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.android.homework.networkhomework.R
import com.android.homework.networkhomework.networking.IApiImgur
import com.android.homework.networkhomework.networking.model.BodyRequest
import com.android.homework.networkhomework.networking.RetrofitBuilder
import com.android.homework.networkhomework.networking.model.ServerResponse
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class ImageUploadFragment : Fragment() {
    companion object {
        fun newInstance() = ImageUploadFragment()

        private const val PERMISSION_REQUEST_CODE_CAMERA = 101
        private const val PERMISSION_REQUEST_CODE_GALLERY = 102
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_OPEN_GALLERY = 2
        private const val KEY_SAVE_URI_IMAGE = "key_save_uri_image"
        private const val TAG_UPLOAD_IMAGE = "tag_upload_image"
    }

    private lateinit var imageView: ImageView
    private lateinit var btnCaptureImage: Button
    private lateinit var btnOpenImage: Button
    private lateinit var etTitleImage: EditText
    private lateinit var etDescriptionImage: EditText
    private lateinit var btnSendImage: ImageButton

    private var imageUri: Uri? = null
    private val apiService by lazy { RetrofitBuilder.buildService(IApiImgur::class.java) }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val fragmentLayout = inflater.inflate(R.layout.fragment_image_upload, container, false)
        initView(fragmentLayout)

        if (savedInstanceState != null) {
            val saveValue = savedInstanceState.getString(KEY_SAVE_URI_IMAGE)
            saveValue?.let { imageUri = Uri.parse(saveValue) }
            imageUri?.let { setImageInView(it) }
        }

        return fragmentLayout
    }

    private fun initView(fragmentLayout: View) {
        imageView = fragmentLayout.findViewById(R.id.image_view)
        btnCaptureImage = fragmentLayout.findViewById(R.id.btn_capture_image)
        etTitleImage = fragmentLayout.findViewById(R.id.et_title_image)
        etDescriptionImage = fragmentLayout.findViewById(R.id.et_description_image)
        btnSendImage = fragmentLayout.findViewById(R.id.btn_send_image)
        btnOpenImage = fragmentLayout.findViewById(R.id.btn_open_image)

        btnCaptureImage.setOnClickListener { captureImage() }
        btnSendImage.setOnClickListener { uploadImage() }
        btnOpenImage.setOnClickListener { selectImageFromGallery() }
    }

    private fun uploadImage() {
        imageUri?.let {
            val bytes = getBytesFromUri(it)
            val base64 = bytes?.let { it1 -> encodeInBase64(it1) }
            val fileName = createFileName(it)
            base64?.let { it3 ->
                val bodyRequest = BodyRequest(it3, fileName, etTitleImage.text.toString(), etDescriptionImage.text.toString())
                uploadImageService(bodyRequest)
            }
        }
    }

    private fun uploadImageService(bodyRequest: BodyRequest) {
        val call = apiService.uploadImage(bodyRequest)
        call.enqueue(object : retrofit2.Callback<ServerResponse> {
            override fun onResponse(call: Call<ServerResponse>, response: Response<ServerResponse>) {
                val serverResponse = response.body()
                if (serverResponse?.status == 200 && serverResponse.success) {
                    context?.let { showToast("Изображение успешно отправлено. Ссылка на него: ${serverResponse.data.link}", it) }
                    Log.i(TAG_UPLOAD_IMAGE, "Ссылка на изображение: ${serverResponse.data.link}")
                } else {
                    context?.let { showToast("Неуспешный запрос. Попробуйте еще раз", it) }
                }
            }

            override fun onFailure(call: Call<ServerResponse>, t: Throwable) {
                context?.let { showToast("Ошибка отправки", it) }
            }
        })
    }

    private fun createFileName(uri: Uri): String {
        val type = context?.contentResolver?.getType(uri)
        val typeName = type?.split('/')?.get(1)
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "$time.$typeName"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        imageUri?.let { outState.putString(KEY_SAVE_URI_IMAGE, imageUri.toString()) }
    }

    private fun captureImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermissionForCamera()) {
                openCamera()
            } else {
                requestPermissions(arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), PERMISSION_REQUEST_CODE_CAMERA)
            }
        } else {
            openCamera()
        }
    }

    private fun selectImageFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermissionForGallery()) {
                openGallery()
            } else {
                requestPermissions(arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                ), PERMISSION_REQUEST_CODE_GALLERY)
            }
        } else {
            openGallery()
        }
    }

    private fun getBytesFromUri(uri: Uri) = context?.contentResolver?.openInputStream(uri)?.readBytes()

    private fun encodeInBase64(bytes: ByteArray) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Base64.getEncoder().encodeToString(bytes)
    } else {
        android.util.Base64.encodeToString(bytes, 0)
    }

    private fun openCamera() {
        val resolver = context?.contentResolver
        val contentValues = ContentValues()
        imageUri = resolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_OPEN_GALLERY)
    }

    private fun showToast(text: String, context: Context) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    imageUri?.let { setImageInView(it) }
                }
                REQUEST_IMAGE_OPEN_GALLERY -> {
                    imageUri = data?.data
                    imageUri?.let { setImageInView(it) }
                }
            }
        }
    }

    private fun setImageInView(uri: Uri?) {
        imageView.setImageURI(null)
        imageView.setImageURI(uri)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    context?.let { showToast("Необходимо разрешение, чтобы открыть камеру", it) }
                }
            }
            PERMISSION_REQUEST_CODE_GALLERY -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    context?.let { showToast("Необходимо разрешение, чтобы открыть галерею", it) }
                }
            }
        }
    }

    private fun checkPermissionForCamera() = (ContextCompat.checkSelfPermission(
            context!!,
            android.Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(
            context!!,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED)

    private fun checkPermissionForGallery() = (ContextCompat.checkSelfPermission(
            context!!,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED)

}