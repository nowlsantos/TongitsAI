package com.nowlsantos.tongitsai.domain.ai

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.nowlsantos.tongitsai.util.Logger
import java.nio.FloatBuffer

class OnnxAiEngine(private val context: Context) : AutoCloseable {
    private val tag = "OnnxAiEngine"
    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null

    init {
        try {
            env = OrtEnvironment.getEnvironment()
            val inputStream = context.assets.open("tongits_policy_model.onnx")
            val modelBytes = inputStream.readBytes()
            inputStream.close()
            session = env?.createSession(modelBytes)
            Logger.i(tag, "Successfully loaded ONNX model from assets.")
        } catch (e: Exception) {
            Logger.w(tag, "Failed to load ONNX model from assets (model might be missing during development): ${e.message}")
        }
    }

    fun isModelLoaded(): Boolean = session != null

    fun evaluate(inputTensor: FloatArray): FloatArray? {
        val currentEnv = env ?: return null
        val currentSession = session ?: return null
        try {
            val shape = longArrayOf(1, inputTensor.size.toLong())
            val tensor = OnnxTensor.createTensor(currentEnv, FloatBuffer.wrap(inputTensor), shape)
            
            val inputName = currentSession.inputNames.iterator().next()
            val inputs = mapOf(inputName to tensor)
            val results = currentSession.run(inputs)
            
            val outputTensor = results[0].value
            val probabilities = when (outputTensor) {
                is Array<*> -> (outputTensor[0] as FloatArray)
                is FloatArray -> outputTensor
                else -> floatArrayOf()
            }
            
            tensor.close()
            results.close()
            return probabilities
        } catch (e: Exception) {
            Logger.e(tag, "Error running ONNX inference: ${e.message}", e)
            return null
        }
    }

    override fun close() {
        try {
            session?.close()
            // Do not close global OrtEnvironment singleton to prevent native/telemetry finalizer crashes
        } catch (e: Exception) {
            Logger.e(tag, "Error closing ONNX engine: ${e.message}")
        }
    }
}
