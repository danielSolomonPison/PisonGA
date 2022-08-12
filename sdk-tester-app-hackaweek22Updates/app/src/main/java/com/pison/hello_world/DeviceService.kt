package com.pison.hello_world

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.badoo.reaktive.disposable.CompositeDisposable
import com.badoo.reaktive.observable.flatMapIterable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.observable.subscribe
import com.badoo.reaktive.scheduler.mainScheduler
import com.pison.core.client.PisonRemoteClassifiedDevice
import com.pison.core.client.PisonRemoteDevice
import com.pison.core.client.monitorConnectedDevices
import com.pison.core.shared.haptic.*
import com.pison.core.shared.imu.EulerAngles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

const val SERVER_INITIALIZE_COMMAND = 1
private const val TAG = "Device Service"
private const val EULER_TAG = "EULER"
private const val GESTURES_TAG = "GESTURES"

private const val HAPTICCT = 5
private const val HAPTIC_PULSE = 2
private const val HAPTIC_BURST = 3
private const val HAPTIC_OFF = 0

private const val DURATION_MS_DEFAULT = 245
private const val INTENSITY_DEFAULT = 100
private const val NUMBER_DEFAULT_Unlock = 3
private const val NUMBER_DEFAULT_lock = 1



@Suppress("DEPRECATION")
@ExperimentalStdlibApi
class DeviceService: Service(){
    private var pisonRemoteDevice: PisonRemoteDevice? = null

    private var masterDisposable = CompositeDisposable()

//    private val _gestureReceived = MutableLiveData<String>()
//    val gestureReceived: LiveData<String>
//        get() = _gestureReceived

    private val _eulerReceived = MutableLiveData<EulerAngles>()
    val eulerReceived: LiveData<EulerAngles>
        get() = _eulerReceived

    private val _errorReceived = MutableLiveData<Throwable>()
    val errorReceived: LiveData<Throwable>
        get() = _errorReceived

    private var wakeword = false
    private var isPlay = false
    private var spot = false
    private var prev = ""

    companion object {
        private const val ACTION_START_SERVICE = "action_start_service"
        private const val ACTION_STOP_SERVICE = "action_stop_service"

        fun getStartIntent(context: Context): Intent {
            return with(Intent(context, DeviceService::class.java)) {
                action = ACTION_START_SERVICE
                println("INTENT STARTING*************")
                this
            }
        }

        fun getStopIntent(context: Context): Intent {
            return with(Intent(context, DeviceService::class.java)) {
                action = ACTION_STOP_SERVICE
                this
            }
        }
    }
    private fun sendHaptic(hapticCommandCode: Int, durationMs: Int, intensity: Int, numberBursts: Int) {
        GlobalScope.launch {
            val haptic = getHapticCommand(
                hapticCommandCode,
                durationMs,
                intensity,
                numberBursts
            )
            Log.d(
                TAG,
                "send Haptic with $hapticCommandCode, $durationMs, $intensity, $numberBursts resulting in $haptic"
            )
            pisonRemoteDevice?.sendHaptic(haptic)
        }
    }

    private fun getHapticCommand(
        hapticCommandCode: Int,
        durationMs: Int,
        intensity: Int,
        number: Int
    ): HapticCommand {
        return when (hapticCommandCode) {
            HAPTICCT -> HapticOnCommand(intensity)
            HAPTIC_PULSE -> HapticPulseCommand(intensity, durationMs)
            HAPTIC_BURST -> HapticBurstCommand(intensity, durationMs, number)
            else -> HapticOffCommand
        }
    }

    private fun moniter() {
        Log.d(TAG, "connect called")
        val deviceDisposable = Application.sdk.monitorConnectedDevices().observeOn(mainScheduler).subscribe(
            onNext = { pisonDevice ->
                Log.d(TAG, "$TAG SUCCESS")
                inTakeData(pisonDevice)
                pisonRemoteDevice = pisonDevice
            },
            onError = { throwable ->
                Log.d(TAG, "$TAG Error:$throwable")
                _errorReceived.postValue(throwable)
            }
        )
        masterDisposable.add(deviceDisposable)
    }

    private fun inTakeData(pisonRemoteDevice: PisonRemoteClassifiedDevice){
        monitorGestures(pisonRemoteDevice)
        monitorEuler(pisonRemoteDevice)
    }
    override fun onCreate() {
        super.onCreate()
        moniter()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposeDisposables()
    }


    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private fun monitorGestures(pisonRemoteDevice: PisonRemoteClassifiedDevice) {
        val gestureDisposable =
            pisonRemoteDevice.monitorFrameTags().flatMapIterable { it }.observeOn(mainScheduler).subscribe(
                onNext = { gesture ->
                    Log.d(TAG, "$GESTURES_TAG $gesture")
//                    print(gesture)
                    if ((gesture == "SHAKE_N_FHEH" || gesture == "SHAKE_N_TEH") && (prev == gesture || prev == "")){
//                    if (gesture == "SHAKE_N_FHEH" || gesture == "SHAKE_N_TEH"){
                        if(wakeword){
                            sendHaptic(
                                HAPTIC_BURST,
                                DURATION_MS_DEFAULT,
                                INTENSITY_DEFAULT,
                                NUMBER_DEFAULT_lock
                            )
                        }else{
                            sendHaptic(
                                HAPTIC_BURST,
                                DURATION_MS_DEFAULT,
                                INTENSITY_DEFAULT,
                                NUMBER_DEFAULT_Unlock
                            )
                        }
                        wakeword = wakeword == false
                    }
                    if (wakeword) {
                        if ((gesture == "FHEH_SWIPE_RIGHT" || gesture == "TEH_SWIPE_RIGHT") && (prev == gesture)) {
                            Application.spotifyAppRemote.playerApi.skipNext()
                        } else if ((gesture == "FHEH_SWIPE_LEFT" || gesture == "TEH_SWIPE_LEFT") && (prev == gesture)) {
                            Application.spotifyAppRemote.playerApi.skipPrevious()
                        } else if (gesture == "DEBOUNCE_LDA_INEH" && (prev == gesture)) {
                            isPlay = if(isPlay){
                                Application.spotifyAppRemote.playerApi.pause()
                                false
                            } else{
                                Application.spotifyAppRemote.playerApi.resume()
                                true
                            }
                        }
//                        else if ((gesture == "DEBOUNCE_LDA_FHEH" || gesture == "DEBOUNCE_LDA_TEH") && (prev == gesture)) {
                        else if (gesture == "DEBOUNCE_LDA_FHEH" || gesture == "DEBOUNCE_LDA_TEH") {
                            if (!spot) {
                                Application.spotifyAppRemote.playerApi.toggleShuffle()
                                Application.spotifyAppRemote.playerApi.play("spotify:playlist:37i9dQZF1DX2sUQwD7tbmL")
                                spot = true
                            }
                        } else {
                            println("other")
                        }
                    }
                    prev = gesture
                    print(gesture)
                    print(prev)
                    println("butt")
                },
                onError = { throwable ->
                    Log.d(TAG, "ERROR $GESTURES_TAG $throwable")
                    _errorReceived.postValue(throwable)
                }
            )
        masterDisposable.add(gestureDisposable)
    }

    private fun monitorEuler(pisonRemoteDevice: PisonRemoteClassifiedDevice) {
        val eulerDisposable =
            pisonRemoteDevice.monitorEulerAngles().observeOn(mainScheduler).subscribe(
                onNext = { euler ->
                    Log.d(TAG, "$EULER_TAG $euler")
                    _eulerReceived.postValue(euler)
                },
                onError = { throwable ->
                    Log.d(TAG, " $EULER_TAG ERROR $throwable")
                    _errorReceived.postValue(throwable)
                }
            )
        masterDisposable.add(eulerDisposable)
    }
    private fun disposeDisposables() {
        Log.d(TAG, "disposed of all disposables")
        masterDisposable.clear(dispose = true)
    }
}