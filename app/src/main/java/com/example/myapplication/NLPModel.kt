package com.example.myapplication

import org.tensorflow.lite.Interpreter
import android.content.Context
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.content.res.AssetFileDescriptor
import java.io.FileInputStream
import java.io.FileDescriptor
import java.io.IOException

class NLPModel(context: Context) {

    private var interpreter: Interpreter

    init {
        val model = loadModelFile(context)
        interpreter = Interpreter(model)
    }

    // Memuat file model .tflite dari folder assets
    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd("sentiment_model.tflite")
        val inputStream: FileInputStream = fileDescriptor.createInputStream()
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Fungsi untuk menjalankan model dan mendapatkan hasilnya
    fun runModel(inputData: ByteBuffer): String {
        val outputData = Array(1) { FloatArray(10) }  // Misalnya output berupa 10 kelas
        interpreter.run(inputData, outputData)
        // Proses hasil output, misalnya mengonversinya ke teks atau entitas tertentu
        return outputData[0].joinToString(", ")
    }
}
