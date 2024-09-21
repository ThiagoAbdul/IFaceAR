package com.example.imageclassificationlivefeed.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.preferences.core.edit
import com.example.imageclassificationlivefeed.R
import com.example.imageclassificationlivefeed.data.services.PwadService
import com.example.imageclassificationlivefeed.dataStore
import com.example.imageclassificationlivefeed.userIdKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class RegisterDeviceActivity : AppCompatActivity() {

    private lateinit var etRegisterDevice: EditText
    private lateinit var btnRegisterDevice: Button
    private val pwadService: PwadService by inject()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_device)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etRegisterDevice = findViewById(R.id.et_register_device)
        btnRegisterDevice = findViewById(R.id.btn_register_device)

        btnRegisterDevice.setOnClickListener {
            val token = etRegisterDevice.text.toString()
            if(token.isEmpty()){
                // TODO
            }
            else{
                CoroutineScope(Dispatchers.Main).launch {
                    val response = pwadService.registerDevice(token)
                    dataStore.edit {
                        it[userIdKey] = response.id
                    }

                    Toast.makeText(this@RegisterDeviceActivity,
                        "Device registered",
                        Toast.LENGTH_LONG).show()

                    delay(1000)
//                    startActivity(Intent(this@RegisterDeviceActivity, ))
                }
            }
        }


    }
}