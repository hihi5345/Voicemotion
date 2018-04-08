package com.rapsealk.voicemotion

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private val PERMISSION_CODE = 0x1010

    private var mMediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private lateinit var recordFilePath: String

    private val mFirebaseStorage by lazy { FirebaseStorage.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permissionsGranted = permissions.all { ContextCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED }

        if (permissionsGranted.not()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

            }
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_CODE)
        }

        button_record.setOnClickListener {
            // TODO("permission")

            if (isRecording) {
                button_record.setImageResource(R.drawable.ic_microphone_off)
                button_record.setBackgroundResource(R.color.colorAccentLight)
                text_status.text = getString(R.string.recording_off)
                mMediaRecorder?.let {
                    it.stop()
                    it.release()
                }
                mMediaRecorder = null

                toast(getString(R.string.message_recording_done))

                // check network
                val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = connectivityManager.activeNetworkInfo
                val isNetworkOn = ((activeNetwork != null) && activeNetwork.isConnectedOrConnecting)

                if (isNetworkOn.not()) return@setOnClickListener toast(getString(R.string.message_network_off))

                val isWifi = (activeNetwork.type == ConnectivityManager.TYPE_WIFI)

                if (isWifi.not()) return@setOnClickListener toast(getString(R.string.message_alert_not_wifi))

                // upload wav file to firebase storage
                val progressBar = ProgressBar(this)
                progressBar.isIndeterminate = true
                progressBar.visibility = ProgressBar.VISIBLE

                val uri = Uri.fromFile(File(recordFilePath))
                val uploadTask = mFirebaseStorage.getReference("wav/${recordFilePath.split("/").last()}").putFile(uri)

                uploadTask
                        .addOnFailureListener { exception ->
                            toast(exception.toString())
                            progressBar.visibility = ProgressBar.GONE
                        }
                        .addOnCompleteListener { task: Task<UploadTask.TaskSnapshot> ->
                            if (task.isSuccessful.not()) return@addOnCompleteListener toast(getString(R.string.message_uploading_failed))
                            val url = task.result.downloadUrl
                            toast("${getString(R.string.message_uploading_done)}\n$url")
                            progressBar.visibility = ProgressBar.GONE
                        }

            }
            else {
                button_record.setImageResource(R.drawable.ic_microphone)
                button_record.setBackgroundResource(R.color.colorPrimaryLight)
                text_status.text = getString(R.string.recording_on)
                mMediaRecorder = MediaRecorder()
                mMediaRecorder?.let {
                    it.setAudioSource(MediaRecorder.AudioSource.MIC)
                    it.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)

                    val directory = File("${Environment.getExternalStorageDirectory().absolutePath}/Kaubrain")
                    if (directory.exists().not()) directory.mkdir()

                    recordFilePath = "${directory.path}/${System.currentTimeMillis()}.wav"
                    it.setOutputFile(recordFilePath)
                    it.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

                    try {
                        it.prepare()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    it.start()
                }
            }

            isRecording = isRecording.not()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED}) {
                    toast("Permission granted.")
                }
                else finish()
            }
        }
    }
}

fun Activity.toast(message: String, duration: Int = Toast.LENGTH_SHORT, context: Context = applicationContext) {
    Toast.makeText(context, message, duration).show()
}