package com.pison.hello_world

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pison.hello_world.Application.Companion.spotifyAppRemote
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote


class MainActivity : AppCompatActivity() {

    private val clientId = "07714fc028664557b2d93001b86a45b0"
    private val redirectUri = "http://com.pison.hello_world/callback"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart(){
        super.onStart()
        // Set the connection parameters
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()
        Log.d("MainActivity", "Connected! Yay!")

        val listener = object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("MainActivity", "Connected! Yay!")
                // Now you can start interacting with App Remote
                connected()
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("MainActivity", throwable.message, throwable)
//                Log.d("Fuck you man...")
                // Something went wrong when attempting to connect! Handle errors here
            }
        }

        SpotifyAppRemote.connect(this, connectionParams, listener)
    }

    private fun connected() {
        // Then we will write some more code here.
        spotifyAppRemote.apply {
            this.playerApi.play("spotify:playlist:37i9dQZF1DX2sUQwD7tbmL")
        }
    }

    override fun onStop() {
        super.onStop()
        // Aaand we will finish off here.
    }
}

