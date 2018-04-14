package com.rapsealk.voicemotion

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private val PERMISSION_CODE = 0x1010

    private var mAudioRecord: AudioRecord? = null
    private val mRecorderBPP = 16
    private val mAudioSampleRate = 16000
    private val mAudioChannel = AudioFormat.CHANNEL_IN_MONO
    private val mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private val mAudioSource = MediaRecorder.AudioSource.MIC

    // private var mMediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private lateinit var fileName: String
    private var mBufferSizeInBytes: Int = 0
    private lateinit var mBuffer: ByteArray

    private val EXT_PCM = "pcm"
    private val EXT_WAV = "wav"

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

        // set AudioRecord
        mBufferSizeInBytes = AudioRecord.getMinBufferSize(mAudioSampleRate, mAudioChannel, mAudioEncoding)
        assert(mBufferSizeInBytes != AudioRecord.ERROR_BAD_VALUE, {
            toast("Unable to get minimum buffer size (sample rate: $mAudioSampleRate)")
            return finish()
        })

        mAudioRecord = AudioRecord(mAudioSource, mAudioSampleRate, mAudioChannel, mAudioEncoding, mBufferSizeInBytes)

        // var buffer: ByteArray
        mAudioRecord?.let {
            assert(it.state == AudioRecord.STATE_INITIALIZED, {
                toast("Unable to initialize AudioRecord")
                it.release()
                return finish()
            })
            mBuffer = ByteArray(mBufferSizeInBytes)
            it.startRecording()
        }

        button_record.setOnClickListener {

            isRecording = isRecording.not()
            Log.d(TAG, "isRecording: $isRecording")

            if (isRecording) {

                button_record.setImageResource(R.drawable.ic_microphone)
                button_record.setBackgroundResource(R.color.colorPrimaryLight)
                text_status.text = getString(R.string.recording_on)

                Thread({

                    val directory = File("${Environment.getExternalStorageDirectory().absolutePath}/Kaubrain")
                    if (directory.exists().not()) directory.mkdir()

                    fileName = "${directory.path}/${System.currentTimeMillis()}"
                    val fOutputStream: FileOutputStream
                    try {
                        fOutputStream = FileOutputStream("$fileName.$EXT_PCM")
                        // writeWavHeader(fOutputStream, mAudioChannel.toShort(), mAudioSampleRate, mAudioEncoding.toShort())
                    }
                    catch (e: IOException) {
                        e.printStackTrace();
                        return@Thread
                    }

                    // mAudioRecord?.startRecording()

                    while ((mAudioRecord != null) and isRecording) {
                        Log.d(TAG, "mAudioRecord: $mAudioRecord and isRecording: $isRecording")
                        val size = mAudioRecord?.read(mBuffer, 0, mBuffer.size)
                        Log.d(TAG, "size: $size")
                        if (size == AudioRecord.ERROR_INVALID_OPERATION || size == AudioRecord.ERROR_BAD_VALUE) continue;
                        try {
                            fOutputStream.write(mBuffer, 0, mBuffer.size)
                        }
                        catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }

                    Log.d(TAG, "close output stream")
                    try {
                        fOutputStream.close()
                    }
                    catch (e: IOException) {
                        e.printStackTrace()
                    }

                    runOnUiThread {
                        toast(getString(R.string.message_recording_done))

                        // updateWavHeader(File("$fileName.$EXT_PCM"))
                        copyWavFile()
                    }

                }).start()
            }
            else {
                button_record.setImageResource(R.drawable.ic_microphone_off)
                button_record.setBackgroundResource(R.color.colorAccentLight)
                text_status.text = getString(R.string.recording_off)
            }

            /* TODO("permission")

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
            */
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

    /*
    private fun startRecording() {

        mAudioRecord = AudioRecord(mAudioSource, mAudioSampleRate, mAudioChannel, mAudioEncoding, mBufferSizeInBytes)

        mAudioRecord?.let {
            assert(it.state == AudioRecord.STATE_INITIALIZED, {
                return@let
            })

            it.startRecording()

            isRecording = true

            Thread({

            })
        }
    }
    */

    private fun copyWavFile() {
        val fInputStream: FileInputStream
        val fOutputStream: FileOutputStream

        // var totalAudioLength = 0L
        //var totalDataLength = totalAudioLength + 36
        val longSampleRate = mAudioSampleRate.toLong()
        val channels = 1//2
        val byteRate = mRecorderBPP * mAudioSampleRate * 2/*channels*/ / 8

        val data = ByteArray(mBufferSizeInBytes)

        try {
            fInputStream = FileInputStream("$fileName.$EXT_PCM")
            fOutputStream = FileOutputStream("$fileName.$EXT_WAV")
            val totalAudioLength = fInputStream.channel.size()
            val totalDataLength = totalAudioLength + 36

            val byteHeader = ByteArray(44)
            // TODO("replace works to ByteArray.plus() method")
            "RIFF".toByteArray().forEachIndexed { index, byte -> byteHeader[index] = byte }
            for (i in 0 until 4) { byteHeader[i+4] = ((totalDataLength shr (i * 8)) and 0xff).toByte() }
            "WAVEfmt ".toByteArray().forEachIndexed { index, byte -> byteHeader[index+8] = byte }
            byteArrayOf(16, 0, 0, 0, 1, 0, channels.toByte(), 0).forEachIndexed { index, byte -> byteHeader[index+16] = byte }
            for (i in 0 until 4) { byteHeader[i+24] = ((longSampleRate shr (i * 8)) and 0xff).toByte() }
            for (i in 0 until 4) { byteHeader[i+28] = ((byteRate shr (i * 8)) and 0xff).toByte() }
            byteArrayOf(2 * 16 / 8, 0, mRecorderBPP.toByte(), 0).forEachIndexed { index, byte -> byteHeader[index+32] = byte }
            "data".toByteArray().forEachIndexed { index, byte -> byteHeader[index+36] = byte }
            for (i in 0 until 4) { byteHeader[i+40] = ((totalAudioLength shr (i * 8)) and 0xff).toByte() }

            fOutputStream.write(byteHeader, 0, 44)

            while (fInputStream.read(data) != -1) { fOutputStream.write(data) }

            fInputStream.close()
            fOutputStream.close()

            toast("WAV 파일이 저장되었습니다.")

            File("$fileName.$EXT_PCM").delete()
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun writeWavHeader(out: OutputStream, channels: Short, sampleRate: Int, bitDepth: Short) {
        val littleBytes = ByteBuffer.allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(channels)
                .putInt(sampleRate)
                .putInt(sampleRate * channels * (bitDepth / 8))
                .putShort((channels * (bitDepth / 8)).toShort())
                .putShort(bitDepth)
                .array()
        out.write(byteArrayOf(
                'R'.toByte(), 'I'.toByte(), 'F'.toByte(), 'F'.toByte(),
                0, 0, 0, 0,
                'W'.toByte(), 'A'.toByte(), 'V'.toByte(), 'E'.toByte(),
                'f'.toByte(), 'm'.toByte(), 't'.toByte(), ' '.toByte(),
                16, 0, 0, 0,
                1, 0,
                littleBytes[0], littleBytes[1],
                littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5],
                littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9],
                littleBytes[10], littleBytes[11],
                littleBytes[12], littleBytes[13],
                'd'.toByte(), 'a'.toByte(), 't'.toByte(), 'a'.toByte(),
                0, 0, 0, 0
        ))
    }

    @Throws(IOException::class)
    private fun updateWavHeader(wav: File) {
        val sizes = ByteBuffer.allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt((wav.length() - 8).toInt())
                .putInt((wav.length() - 44).toInt())
                .array()
        val randomAccessFile: RandomAccessFile
        try {
            randomAccessFile = RandomAccessFile(wav, "rw")
            randomAccessFile.seek(4)
            randomAccessFile.write(sizes, 0, 4)
            randomAccessFile.seek(40)
            randomAccessFile.write(sizes, 4, 4)

            randomAccessFile.close()
        }
        catch (e: IOException) {
            throw e
        }
    }
}

fun Activity.toast(message: String, duration: Int = Toast.LENGTH_SHORT, context: Context = applicationContext) {
    Toast.makeText(context, message, duration).show()
}