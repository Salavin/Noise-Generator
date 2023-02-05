package `in`.samlav.noisegenerator

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import `in`.samlav.noisegenerator.AudioHandlerConstants.STATE_FADING_IN
import `in`.samlav.noisegenerator.AudioHandlerConstants.STATE_FADING_OUT
import `in`.samlav.noisegenerator.AudioHandlerConstants.STATE_PLAYING
import `in`.samlav.noisegenerator.AudioHandlerConstants.STATE_STOPPED
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Class that handles playing the white or pink noise. The initial conditions for the object are set using the following properties:
 *
 * @property isPink whether the noise played will be pink or not.
 * @property vol the volume to play the audio at. Must be a value between 0 and 10 inclusive.
 * @property fadeIn time in ms for the audio to fade in.
 * @property fadeOut time in ms for the audio to fade out.
 * @property bufferSize the buffer size.
 * @constructor Create empty Audio handler
 */
class AudioHandler(var isPink: Boolean, var vol: Int, var fadeIn: Int, var fadeOut: Int, var bufferSize: Int)
{
    private lateinit var buffer: ByteArray
    private var isPlaying = false
    private var beginFade = false
    private var endFade = false
    private var stop = false
    private var audioTrack: AudioTrack? = null
    private var thread: Thread? = null
    var samplesFaded = 0
        private set

    /**
     * Used as the main interaction with the object. Handles whether to start, stop, or fade the audio.
     */
    fun toggle()
    {
        if (!(beginFade || endFade))
        {
            if (isPlaying)
            {
                endFade = true
            }
            else
            {
                start()
            }
        }
        else
        {
            stop()
        }
    }

    /**
     * Handles starting the audio. Initializes [audioTrack], and starts generating the [buffer].
     */
    private fun start()
    {
        if (audioTrack == null)
        {
            audioTrack = createAudioTrack()
        }
        if (audioTrack?.state == AudioTrack.STATE_INITIALIZED)
        {
            val newVol = ((vol.toFloat() / 10.0) * AudioTrack.getMaxVolume()).toFloat()
            audioTrack?.setVolume(newVol)
            isPlaying = true
            beginFade = true
            audioTrack?.play()
            thread = Thread {
                while (isPlaying)
                {
                    generateBuffer()
                    if (beginFade or endFade)
                    {
                        applyFade()
                    }
                    playSound()
                }
            }
            thread?.start()
        }
        else
        {
            // Handle the error, the audio track was not initialized
        }
    }

    /**
     * Handles stopping the audio, and resetting all variables.
     */
    private fun stop()
    {
        stop = false
        beginFade = false
        endFade = false
        isPlaying = false
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        samplesFaded = 0
    }

    /**
     * Creates the [AudioTrack] instance.
     *
     * @return the created [AudioTrack]
     */
    @SuppressLint("Range")
    private fun createAudioTrack(): AudioTrack
    {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(AudioHandlerConstants.SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        return AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setSessionId(AudioManager.AUDIO_SESSION_ID_GENERATE)
            .build()
    }

    /**
     * Function for generating the [buffer].
     *
     * Filtering was copied from http://www.cooperbaker.com/home/code/pink%20noise/
     * and modified to work with Kotlin.
     */
    private fun generateBuffer()
    {
        buffer = ByteArray(bufferSize * 2)

        if (isPink)
        {
            val whiteNoiseBuffer = DoubleArray(this.buffer.size)
            val pinkNoiseBuffer = ByteArray(this.buffer.size)

            for (i in whiteNoiseBuffer.indices)
            {
                whiteNoiseBuffer[i] = (Random.nextDouble(2.0) - 1)
            }

            val filters = Pink()

            for (i in whiteNoiseBuffer.indices)
            {
                var pink = 0.0
                for (j in filters.a.indices)
                {
                    // filter the white noise
                    filters.y[j] = filters.a[j] * whiteNoiseBuffer[i] + ( 1.0 - filters.a[j] ) * filters.y[j]

                    // apply gain and accumulate filtered noise
                    pink += filters.y[j] * filters.g[j]
                }
                pinkNoiseBuffer[i] = (pink * (127.0 / 0.17)).toInt().toByte()
            }
            buffer = pinkNoiseBuffer
        }
        else
        {
            buffer = ByteArray(this.buffer.size) { (Random.nextInt(256) - 128).toByte() }
        }
    }

    /**
     * Applies a fade to the audio by scaling the samples within the [buffer] linearly according to the user's preference.
     */
    private fun applyFade()
    {
        if (beginFade)
        {
            val samplesToFade = ((fadeIn / 1000.0) * AudioHandlerConstants.SAMPLE_RATE).roundToInt()
            var i = 0
            while ((samplesFaded < samplesToFade) && (i++ < buffer.size))
            {
                buffer[samplesFaded % buffer.size] = (buffer[samplesFaded % buffer.size] * (samplesFaded.toFloat() / samplesToFade)).toInt().toByte()
                samplesFaded++
            }
            if (samplesFaded >= samplesToFade)
            {
                beginFade = false
                samplesFaded = 0
            }
        }
        else
        {
            val samplesToFade = ((fadeOut / 1000.0) * AudioHandlerConstants.SAMPLE_RATE).roundToInt()
            var i = 0
            while ((samplesFaded < samplesToFade) && (i++ < buffer.size))
            {
                buffer[samplesFaded % buffer.size] = (buffer[samplesFaded % buffer.size] * (1 - (samplesFaded.toFloat() / samplesToFade))).toInt().toByte()
                samplesFaded++
            }
            if (samplesFaded >= samplesToFade)
            {
                endFade = false
                stop = true
                samplesFaded = 0
            }
        }
    }

    /**
     * Takes the [buffer] generated by [generateBuffer] and writes it to the [audioTrack] instance.
     */
    private fun playSound()
    {
        try
        {
            if (audioTrack?.write(
                    this.buffer,
                    0,
                    this.buffer.size,
                    AudioTrack.WRITE_BLOCKING
                ) == AudioTrack.ERROR_INVALID_OPERATION
            )
            {
                Thread.sleep(10)
            }
        } catch (_: java.lang.IllegalStateException) { }
        if (stop)
        {
            stop()
        }
    }

    /**
     * Set the volume for this [AudioHandler].
     *
     * @param vol an [Integer] value representing the volume to set. Range should be between 0 and 10 inclusive.
     */
    fun setVolume(vol: Int)
    {
        if ((vol > 10) || (vol < 0))
        {
            throw IllegalArgumentException()
        }
        else if (audioTrack != null && audioTrack?.state == AudioTrack.STATE_INITIALIZED)
        {
            val newVol = ((vol.toFloat() / 10.0) * AudioTrack.getMaxVolume()).toFloat()
            audioTrack?.setVolume(newVol)
        }
        this.vol = vol
    }

    /**
     * Gets the state of this [AudioHandler].
     *
     * @return one of the four [String] states defined in [AudioHandlerConstants]
     */
    fun getState(): String
    {
        if (beginFade)
        {
            return STATE_FADING_IN
        }
        if (endFade)
        {
            return STATE_FADING_OUT
        }
        if (isPlaying)
        {
            return STATE_PLAYING
        }
        return STATE_STOPPED
    }

    init
    {
        if ((vol > 10) || (vol < 0))
        {
            throw IllegalArgumentException()
        }
    }
}

/**
 * Audio handler constants
 *
 * @constructor Create empty Audio handler constants
 */
object AudioHandlerConstants
{
    const val STATE_STOPPED = "stopped"
    const val STATE_PLAYING = "playing"
    const val STATE_FADING_IN = "fading_in"
    const val STATE_FADING_OUT = "fading_out"
    const val SAMPLE_RATE = 44100
}