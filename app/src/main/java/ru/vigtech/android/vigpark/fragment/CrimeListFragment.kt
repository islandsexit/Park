package ru.vigtech.android.vigpark.fragment

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.util.Base64
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.vigtech.android.vigpark.R
import ru.vigtech.android.vigpark.api.ApiClient
import ru.vigtech.android.vigpark.camera.CameraxHelper
import ru.vigtech.android.vigpark.database.Crime
import ru.vigtech.android.vigpark.database.CrimeRepository
import ru.vigtech.android.vigpark.swipe.SwipeToDeleteCallback
import ru.vigtech.android.vigpark.swipe.SwipeToResendCallback
import ru.vigtech.android.vigpark.tools.PicturesUtils
import ru.vigtech.android.vigpark.tools.SizeHelper
import ru.vigtech.android.vigpark.viewmodel.Auth
import ru.vigtech.android.vigpark.viewmodel.CrimeListViewModel
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class CrimeListFragment : Fragment(),
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    com.google.android.gms.location.LocationListener {

    private val TAG = "CrimeListFragment"
    private lateinit var mSizeHelper: SizeHelper
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocation: Location? = null
    private var mLocationManager: LocationManager? = null
    private lateinit var mBottomSheetLayout: LinearLayout
    private lateinit var sheetBehavior: BottomSheetBehavior<View>
    private lateinit var header_Arrow_Image: ImageView
    private var mLocationRequest: LocationRequest? = null
    private val listener: com.google.android.gms.location.LocationListener? = null
    private val UPDATE_INTERVAL = (2 * 1000).toLong()  /* 10 secs */
    private val FASTEST_INTERVAL: Long = 2000 /* 2 sec */

    private val group1Id = 1

    var flashid = 111
    var zoneId = 112



    //TOAST
    private lateinit var toastInflater: LayoutInflater
    private lateinit var toastLayout: View
    private lateinit var toastImage: ImageView
    private lateinit var toastTextView: TextView
    private lateinit var toast: Toast

    var latLng: LatLng = LatLng(0.0, 0.0)

    private var locationManager: LocationManager? = null

    private val isLocationEnabled: Boolean
        get() {
            locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager!!.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        }


    lateinit var cameraxHelper: CameraxHelper


    var img64_full: String? = null

    private lateinit var menu: Menu
    private lateinit var viewModel: Auth


    private lateinit var crimeRecyclerView: RecyclerView
    private lateinit var mybitmap: Bitmap

    private lateinit var photoButton: ImageButton

    private lateinit var arrowImage: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarToggle: ActionBarDrawerToggle
    private lateinit var navView: NavigationView
    var zone: Int = 0


    private var adapter: CrimeAdapter? = CrimeAdapter(emptyList())
    val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeListViewModel::class.java)
    }


    private var callbacks: Callbacks? = null


    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as? Callbacks

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)
        mSizeHelper = SizeHelper()

        //Toast start
        toastInflater = layoutInflater
        toastLayout =
            toastInflater.inflate(R.layout.toast_layout, view.findViewById(R.id.toast_layout_root))
        toastImage = toastLayout.findViewById(R.id.image_toast)
        toastTextView = toastLayout.findViewById(R.id.text_toast)
        //Toast end
        mBottomSheetLayout = view.findViewById(R.id.bottom_layout);
        sheetBehavior = BottomSheetBehavior.from(mBottomSheetLayout);
        header_Arrow_Image = view.findViewById(R.id.arrow_list);

        header_Arrow_Image.setOnClickListener {
            if (sheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
            } else {
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
            }
        }

        sheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                header_Arrow_Image.rotation = slideOffset * 180
            }
        })


        cameraxHelper = CameraxHelper(
            caller = this,
            previewView = view.findViewById(R.id.previewView),
            onPictureTaken = { file, uri ->
                Log.i("apptg", "Picture taken ${file.absolutePath} uri=$uri")
            },
            onError = { Log.e("APPTAG", "error") },
            builderPreview = Preview.Builder().setTargetResolution(Size(200, 200)),
            builderImageCapture = ImageCapture.Builder().setTargetResolution(Size(1024, 768)),
            filesDirectory = context?.filesDir,
            latLng = latLng

        )

        cameraxHelper.start()
        cameraxHelper.changeCamera()

        drawerLayout = view.findViewById(R.id.drawer_layout)
        arrowImage = view.findViewById(R.id.arrow_list)

        actionBarToggle = ActionBarDrawerToggle(activity, drawerLayout, 0, 0)
        drawerLayout.addDrawerListener(actionBarToggle)

        actionBarToggle.syncState()


        // Call findViewById on the NavigationView
        navView = view.findViewById(R.id.navigation)

        crimeRecyclerView =
            view.findViewById(R.id.crime_recycler_view) as RecyclerView
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        photoButton = view.findViewById(R.id.camera_button) as ImageButton
//        crimeRecyclerView.setHasFixedSize(true)
        crimeRecyclerView.setItemViewCacheSize(50)

        photoButton.setOnClickListener {
            try {
                cameraxHelper.takePicture()
            } catch (e: Exception) {
                Log.e("PictureDemo", "Exception in take photo", e)
            }

        }


        //todo camera size cameraBridgeViewBase.setMaxFrameSize(1280, 720)


        //todo свайп
        val swipeHandler = object : SwipeToDeleteCallback(this.context) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                crimeRecyclerView.adapter?.notifyItemRemoved(viewHolder.position)
                CoroutineScope(Dispatchers.Default).launch {
                    val crime = crimeListViewModel.getCrimeFromPosition(viewHolder.position)

                    CrimeRepository.get().deleteCrime(crime)
                    val file = File(crime.img_path)
                    if (file.exists()) {
                        file.delete()
                    }
                    Log.i("AAAAAAAAAAPPPPPP_TAAAGGG", "Deleting ${viewHolder.position}")
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(crimeRecyclerView)

//todo resend swipe
        val swipeHandlerResend = object : SwipeToResendCallback(this.context) {
            @SuppressLint("NotifyDataSetChanged")
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                CoroutineScope(Dispatchers.Default).launch {
                    val crime = crimeListViewModel.getCrimeFromPosition(viewHolder.adapterPosition)
                    ResendCrime(crime)
                    Log.i("Swipe Resend", "Resend ${crime.title}")
                }
            }
        }
        val itemTouchHelperResend = ItemTouchHelper(swipeHandlerResend)
        itemTouchHelperResend.attachToRecyclerView(crimeRecyclerView)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.ip_configuration -> {
                    val alert = AlertDialog.Builder(requireContext())
                    val edittext = EditText(requireContext())
                    edittext.text = SpannableStringBuilder(getIpFromShared());
                    alert.setMessage(R.string.ip_сщташпгкфешщт)
                    alert.setTitle("Сервер")

                    alert.setView(edittext)

                    alert.setPositiveButton(
                        "Готово"
                    ) { dialog, whichButton -> //What ever you want to do with the value
                        val ip = edittext.text.toString()
                        val preferences: SharedPreferences =
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                        val editor = preferences.edit()
                        editor.putString("ip", ip)
                        editor.apply()
                        ApiClient.reBuildRetrofit(ip)
                    }

                    alert.setNegativeButton(
                        "Отмена"
                    ) { dialog, whichButton ->
                    }

                    alert.show()


                    true
                }

                R.id.new_crime -> {
                    pickPhoto()
                    true
                }



                R.id.delete_crimes -> {
                    val builder1: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                    val size = mSizeHelper.getFormatSize(
                        mSizeHelper.getFolderSize(context?.filesDir).toDouble()
                    )
                    builder1.setMessage("Удалить все снимки? \n Снимки занимают $size")
                    builder1.setCancelable(true)

                    builder1.setPositiveButton(
                        "Да",
                        DialogInterface.OnClickListener { dialog, id ->
                            try {
                                val lastCrimeDate =
                                    crimeListViewModel.crimeListLiveData.value?.first()?.date
                                crimeListViewModel.crimeListLiveData.observe(
                                    viewLifecycleOwner,
                                    Observer<List<Crime>>() {
                                        for (crime in it) {
                                            CoroutineScope(Dispatchers.Default).launch {
                                                if (crime.date <= lastCrimeDate) {
                                                    val file = File(crime.img_path)
                                                    if (file.exists()) {
                                                        file.delete()
                                                        crimeListViewModel.deleteCrime(crime)
                                                    } else {
                                                        this.cancel()
                                                    }
                                                }
                                            }


                                        }
                                    })
                            } catch (e: Exception) {
                                Toast.makeText(
                                    requireContext(),
                                    "Ничего не нашлось",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            dialog.cancel()
                        })

                    builder1.setNegativeButton(
                        "Нет",
                        DialogInterface.OnClickListener { dialog, id ->

                            dialog.cancel()
                        })

                    val alert11: AlertDialog = builder1.create()
                    alert11.show()


                    true
                }
                else -> {
                    false
                }
            }

        }


        drawerLayout.addDrawerListener(object : DrawerListener {
            /**
             * Called when a drawer's position changes.
             *
             * @param slideOffset The new offset of this drawer within its range, from 0-1
             * Example when you slide drawer from left to right, slideOffset will increase from 0 - 1 (0 when drawer closed and 1 when drawer display full)
             */
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
//                crimeRecyclerView.visibility = View.GONE
            }

            override fun onDrawerOpened(drawerView: View) {
                crimeRecyclerView.visibility = View.GONE
                arrowImage.visibility = View.GONE
            }

            override fun onDrawerClosed(drawerView: View) {
                crimeRecyclerView.visibility = View.VISIBLE
                arrowImage.visibility = View.VISIBLE
            }

            /**
             * Called when the drawer motion state changes. The new state will
             * be one of [.STATE_IDLE], [.STATE_DRAGGING] or [.STATE_SETTLING].
             */
            override fun onDrawerStateChanged(newState: Int) {
                if (newState == 1) {
                    crimeRecyclerView.visibility = View.VISIBLE
                    arrowImage.visibility = View.VISIBLE
                }
                Log.i("OFFSET", newState.toString())
            }
        })

        getIpFromShared()?.let { ApiClient.reBuildRetrofit(it) }



        mGoogleApiClient = GoogleApiClient.Builder(requireContext())
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()

        mLocationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        Log.d("gggg", "uooo");
        checkLocation()
        zone = getZoneFromShared()
        cameraxHelper.zone = zone


        viewModel = ViewModelProvider(this).get(Auth::class.java)
        viewModel.context = requireContext()
        viewModel.initViewModel()
        val authObserver = Observer<Int>{
            alertKey(it, viewModel)
        }





//        viewModel.authSuccess.value?.let { alertKey(it, viewModel) }

        viewModel.authSuccess.observe(viewLifecycleOwner, authObserver)
        ApiClient.authModel = viewModel
//        if (!viewModel.authSuccess.value!!){
//            alertKey(viewModel.authSuccess.value!!, viewModel)
//        }






        return view
    }

    private fun alertKey(isAuth: Int, AuthModel: Auth) {
        Log.i("AAAAAAAAAAAAAAAAAA", "$isAuth")
        if (isAuth==1 || isAuth==2) {
            val alert = AlertDialog.Builder(requireContext())
            val edittext = EditText(requireContext())
            edittext.hint = SpannableStringBuilder("xxx-xxx-xxx-xxx");
            alert.setMessage("Программное обеспечение защищено")
            alert.setTitle("Введите лицензионный ключ")

            alert.setView(edittext)

            alert.setPositiveButton(
                "Ок"
            ) { dialog, whichButton ->
                AuthModel.secureKey = edittext.text.toString()



                ApiClient.postAuthKeys()


                Log.i("AUUUUUUUUCTHHHHHH", "Api ${AuthModel.authSuccess.value!!}")

            }

            alert.setNegativeButton(
                "У меня нет ключа"
            ) { dialog, whichButton ->
                requireActivity().finish()
            }
            alert.setCancelable(false)
            alert.show()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        this.menu = menu

        menu.add(group1Id, flashid, flashid, "Фонарик").setIcon(R.drawable.ic_flash_off).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        val subZoneMenu = menu.addSubMenu(group1Id, zoneId, zoneId, "Выбор зоны").setIcon(R.drawable.ic_zone)
        subZoneMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)




        viewModel.listOfAlias.value?.forEachIndexed{ index, item ->
            subZoneMenu.add(2, index+1, index+1, item).setCheckable(true)
        }

        subZoneMenu.setGroupCheckable(2, true, true)
        try {
            selectMenu(zone-1, menu)
        }catch (e:Exception){

        }
        ApiClient.checkZone()
        val zoneObserver = Observer<Set<String>> {
            rebuildMenu(menu)
        }
        viewModel.listOfAlias.observe(viewLifecycleOwner, zoneObserver)
        try {
            selectMenu(zone-1, menu)
        }catch (e:Exception){

        }

    }

    fun rebuildMenu(menu: Menu){


        val subZoneMenu = menu.findItem(zoneId).subMenu
        subZoneMenu.clear()
        subZoneMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)


        viewModel.listOfAlias.value?.forEachIndexed{ index, item ->
            if(item!=""){
                subZoneMenu.add(2, index+1, index+1, item).setCheckable(true)
            }
        }
        try {
            subZoneMenu.getItem(getZoneFromShared()-1).isChecked = true
        }catch(e:Exception){

        }

        subZoneMenu.setGroupCheckable(2, true, true)
        adapter?.notifyDataSetChanged()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i("MENU", "${item}")



        return when (item.itemId) {

            flashid -> {
                if (cameraxHelper.cameraInfo?.torchState?.value == TorchState.ON) {
                    cameraxHelper.cameraControl?.enableTorch(false)
                    item.setIcon(R.drawable.ic_flash_off)
                } else {
                    cameraxHelper.cameraControl?.enableTorch(true)
                    item.setIcon(R.drawable.ic_flash_on)
                }
                true
            }
            zoneId ->{
                true
            }

            else ->{
                zoneChange(item.itemId, menu, item)
                return super.onOptionsItemSelected(item)
            }
        }
    }


    val selectImageFromGalleryResult =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                //todo image picker

                Log.i("Image_picker", uri.path.toString())
                try {
                    var path_to_image = context?.filesDir
                    val inputStream: InputStream? =
                        context?.getContentResolver()?.openInputStream(uri)
                    val bOut2 = ByteArrayOutputStream()
                    var bm = BitmapFactory.decodeStream(inputStream)
                    bm = PicturesUtils.getResizedBitmap(bm, 1024, 768)
                    bm.compress(Bitmap.CompressFormat.JPEG, 50, bOut2)
                    val mFile3 = File(
                        path_to_image,
                        UUID.randomUUID().toString() + "_" + ".jpg"
                    )
                    var fos2: FileOutputStream? = null
                    fos2 = FileOutputStream(mFile3)
                    Log.i("APP_LOG", mFile3.absolutePath)
                    bm.compress(Bitmap.CompressFormat.JPEG, 100, fos2)
                    val bOut = ByteArrayOutputStream()
                    bm.compress(Bitmap.CompressFormat.JPEG, 100, bOut)
                    val img64 = Base64.encodeToString(bOut.toByteArray(), Base64.DEFAULT)
                    ApiClient.POST_img64(
                        img64,
                        img_path = mFile3.path,
                        img_plate_path = "None",
                        zone = zone,
                        long = 0.0,
                        lat = 0.0
                    )
                } catch (e: IOException) {
                    Log.e("APP_LOG", "Exception in photoCallback", e)
                }

            }
        }

    private fun pickPhoto() {
        selectImageFromGalleryResult.launch("image/*")

    }

    private fun updateUI(crimes: List<Crime>) {
        adapter?.let {
            it.crimes = crimes
        } ?: run {
            adapter = CrimeAdapter(crimes)
        }
        crimeRecyclerView.adapter = adapter
    }

    private inner class CrimeHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener, View.OnLongClickListener {

        private lateinit var crime: Crime

        private val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.crime_date)
        private val solvedImageView: ImageView = itemView.findViewById(R.id.crime_solved)
        private val sendIcon: ImageView = itemView.findViewById(R.id.connection)
        private val foundIcon: ImageView = itemView.findViewById(R.id.not_found)
        private val infoDot: ImageView = itemView.findViewById(R.id.crime_info_dot)
        private val workerIcon: ImageView = itemView.findViewById(R.id.worker_info_ic)
        private val zone: TextView = itemView.findViewById(R.id.crime_zone)


        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)

        }

        @SuppressLint("SimpleDateFormat")
        fun bind(crime: Crime) {
            this.crime = crime
            workerIcon.visibility = View.GONE
            titleTextView.text = this.crime.title
            try {
                zone.text = viewModel.listOfAlias.value!!.elementAt(crime.Zone-1)
            }catch (e: Exception){
                zone.text = "xxx"
            }
            dateTextView.text = SimpleDateFormat("dd.MM HH:mm").format(this.crime.date)
//            var path_to_image = "/storage/emulated/0/${crime.img_path}"
            if (File(crime.img_path).exists() && crime.img_path != "") {
                mybitmap = BitmapFactory.decodeFile(crime.img_path)
                solvedImageView.setImageBitmap(Bitmap.createScaledBitmap(mybitmap, 120, 120, false))
                solvedImageView.visibility = View.VISIBLE
            } else {
                solvedImageView.visibility = View.INVISIBLE
            }
            if (!crime.send) {
                sendIcon.visibility = View.VISIBLE
                foundIcon.visibility = View.GONE

            } else {
                sendIcon.visibility = View.GONE
                if (!crime.found) {
                    foundIcon.visibility = View.VISIBLE
                } else {
                    foundIcon.visibility = View.GONE
                    if (crime.info.count() >= 2 && crime.info != "null") {
                        infoDot.visibility = View.VISIBLE
                    }
                }
            }



        }

        override fun onClick(v: View) {
            callbacks?.onCrimeSelected(crime.id)

        }

        override fun onLongClick(p0: View?): Boolean {
            toast = Toast(context)
            if (File(crime.img_path).exists() && crime.img_path != "") {
                var mybitmap = BitmapFactory.decodeFile(crime.img_path)
                if (!crime.Rect.isNullOrEmpty()) {
                    val myBitmapSmall =
                        Bitmap.createBitmap(
                            mybitmap, crime.Rect?.get(0)?.toInt()!!,
                            crime.Rect?.get(1)?.toInt()!!,
                            crime.Rect?.get(2)?.toInt()!!, crime.Rect?.get(3)?.toInt()!!
                        )
                    toastImage.setImageBitmap(Bitmap.createBitmap(myBitmapSmall))
                    toastTextView.text = crime.title
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.duration = Toast.LENGTH_LONG
                    toast.view = toastLayout
                    toast.show()
                    return true
                }
            }
            return false
        }
    }

    private inner class CrimeAdapter(var crimes: List<Crime>) :
        RecyclerView.Adapter<CrimeHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                : CrimeHolder {
            val layoutInflater = LayoutInflater.from(context)
            val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
            return CrimeHolder(view)
        }


        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = crimes[position]
            holder.bind(crime)
        }

        fun removeAt(position: Int) {
            notifyItemRemoved(position)
        }

        override fun getItemCount() = crimes.size
    }

    companion object {

        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }


    override fun onDestroy() {
        super.onDestroy()

    }


    override fun onPause() {
        super.onPause()

    }

    override fun onStop() {
        super.onStop()
        if (mGoogleApiClient!!.isConnected()) {
            mGoogleApiClient!!.disconnect()
        }
    }

    override fun onStart() {
        super.onStart()
        if (mGoogleApiClient != null) {
            mGoogleApiClient!!.connect()
        }

        crimeListViewModel.crimeListLiveData.observe(
            viewLifecycleOwner,
            Observer { crimes ->
                crimes?.let {
                    Log.i(TAG, "Got crimeLiveData ${crimes.size}")
                    updateUI(crimes)
                }
            }
        )
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }


    suspend fun ResendCrime(crime: Crime) {
        //todo удалить строку, на которую нажал и коорая не отправилась
        if (File(crime.img_path).exists() && crime.img_path != "") {
            val bOut2 = ByteArrayOutputStream()
            val bm = BitmapFactory.decodeFile(crime.img_path)
            bm.compress(Bitmap.CompressFormat.JPEG, 100, bOut2)
            img64_full = Base64.encodeToString(bOut2.toByteArray(), Base64.DEFAULT)
            crime.date = Calendar.getInstance().time
            ApiClient.POST_img64(img64_full.toString(), crime)
        } else {
            Log.e("RESEND CRIME", "not deleted")
        }


    }


    private fun getIpFromShared(): String? {
        val preferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        val url = preferences.getString("ip", "")
        if (!url.equals("", ignoreCase = true)) {
            return url
        } else {
            return "http://95.182.74.37:1234/"
        }
    }

    private fun getZoneFromShared(): Int {
        val preferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        val url = preferences.getString("zone", "1")
        if (!url.equals("", ignoreCase = true)) {
            return url!!.toInt()
        } else {
            return 1
        }
    }

    fun zoneChange(zon: Int, menu: Menu, item: MenuItem) {

        cameraxHelper.zone = zon
        zone = zon
        val preferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        val editor = preferences.edit()
        editor.putString("zone", zon.toString())
        editor.apply()

        selectMenu(zon, menu)

    }

    private fun selectMenu(zon: Int, menu: Menu) {
        val submenuZone = menu.findItem(zoneId).subMenu
        val item = submenuZone.findItem(zon)
        item.isChecked = true
    }

    override fun onConnected(p0: Bundle?) {


        if (androidx.core.app.ActivityCompat.checkSelfPermission(
                requireContext(),
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && androidx.core.app.ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        startLocationUpdates()
        if (mLocation == null) {
            startLocationUpdates()
        }
        if (mLocation != null) {
            Log.w("GPS", "lat-${mLocation!!.latitude}, long-${mLocation!!.longitude}")
        } else {
            Toast.makeText(context, "Не могу найти местоположение", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionSuspended(p0: Int) {
        mGoogleApiClient!!.connect()
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.i("GPS", "Ошибка, не могу найти местополежение " + p0.getErrorCode())
    }

    override fun onLocationChanged(p0: Location?) {

        val msg = "Обновляю местоположение: " +
                p0?.let { java.lang.Double.toString(it.latitude) } + "," +
                p0?.let { java.lang.Double.toString(it.longitude) }
        Log.i("GPS", msg)
        latLng = p0?.let { LatLng(it.latitude, p0.longitude) }!!
        cameraxHelper.latLng = latLng
    }

    protected fun startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(UPDATE_INTERVAL)
            .setFastestInterval(FASTEST_INTERVAL)
        // Request location updates

        if (androidx.core.app.ActivityCompat.checkSelfPermission(
                requireContext(),
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && androidx.core.app.ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Ищу местоположение", Toast.LENGTH_SHORT).show()
            return
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
            mGoogleApiClient,
            mLocationRequest, this
        )
        Log.d("GPS", "Нашел местоположение")
    }


    private fun checkLocation(): Boolean {
        if (!isLocationEnabled)
            showAlert()
        return isLocationEnabled
    }

    private fun showAlert() {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle("Enable Location")
            .setMessage("Использование вашего местоположения выключено'.\nВключите его " + "чтобы пользоваться приложением")
            .setPositiveButton("Настройки") { paramDialogInterface, paramInt ->
                val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(myIntent)
            }
            .setNegativeButton("Выйти") { paramDialogInterface, paramInt -> }
        dialog.show()
    }

}


