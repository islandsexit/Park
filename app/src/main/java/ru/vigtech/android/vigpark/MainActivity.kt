package ru.vigtech.android.vigpark

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Runnable
import ru.vigtech.android.vigpark.database.Crime
import ru.vigtech.android.vigpark.database.CrimeRepository
import ru.vigtech.android.vigpark.fragment.CrimeFragment
import ru.vigtech.android.vigpark.fragment.CrimeListFragment
import ru.vigtech.android.vigpark.tools.Diagnostics
import ru.vigtech.android.vigpark.tools.PicturesUtils
import java.util.*


class MainActivity : AppCompatActivity(), CrimeListFragment.Callbacks {

    lateinit var test_button: Button
    val crimeRepository = CrimeRepository.get()
    var position = false





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Diagnostics.context = this



//        ApiClient.buidAuthModule(this)



        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                val getpermission = Intent()
                getpermission.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(getpermission)
            }
        }



        val MY_CAMERA_REQUEST: Int = 2
        if (checkSelfPermission(
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE

                ), MY_CAMERA_REQUEST
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(
                        Uri.parse(
                            String.format(
                                "package:%s",
                                packageName
                            )
                        )
                    ), 1234
                )
            } else {
            }
        }

        //Storage Permission





        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {
            val fragment = CrimeListFragment.newInstance()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }




    }



    override fun onCrimeSelected(crimeId: UUID) {
        val fragment = CrimeFragment.newInstance(crimeId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }


    fun add_crime(view: View) {
        crimeRepository.addCrime(Crime())
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
            when (keyCode) {
                27 ->{
                    if (!position){
                        if (PicturesUtils.canCam){
                            val fm = supportFragmentManager
                            val fragment: CrimeListFragment? =
                                fm.findFragmentById(R.id.fragment_container) as CrimeListFragment?
                            fragment?.photoButton?.callOnClick()
                            fragment?.photoButton?.isPressed = true
                            Handler().postDelayed(Runnable { fragment?.photoButton?.isPressed = false }, 500)
                            }
                        else{
                            onBackPressed()
                        }
                        position = true
                        return true

                    }
                }
                KeyEvent.KEYCODE_MENU -> {

                    return true
                }
                KeyEvent.KEYCODE_SEARCH -> {

                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    onBackPressed()
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_UP ->
                    if (!position){
                        if (PicturesUtils.canCam){
                            val fm = supportFragmentManager
                            val fragment: CrimeListFragment? =
                                fm.findFragmentById(R.id.fragment_container) as CrimeListFragment?
                            fragment?.photoButton?.callOnClick()
                            fragment?.photoButton?.isPressed = true
                            Handler().postDelayed(Runnable { fragment?.photoButton?.isPressed = false }, 500)
                        }
                        else{
                            onBackPressed()
                        }
                        position = true
                        return true
                    }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    return false
                }
            }


        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        Log.i("App_tag", "$keyCode")

            when (keyCode) {
                27 ->{
                        position = false
                        return true
                    }

                KeyEvent.KEYCODE_MENU -> {

                    return true
                }
                KeyEvent.KEYCODE_SEARCH -> {

                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_UP ->{
                    position = false
                    return true
                }


                        KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    return false
                }
            }

        return super.onKeyDown(keyCode, event)
    }






}



