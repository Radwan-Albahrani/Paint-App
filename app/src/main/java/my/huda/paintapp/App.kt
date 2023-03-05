package my.huda.paintapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import my.huda.paintapp.screens.MainScreen

class App : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MainScreen::class.java)
        startActivity(intent)
    }
}