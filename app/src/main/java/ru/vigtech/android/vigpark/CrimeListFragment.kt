package ru.vigtech.android.vigpark

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Base64
import android.util.Log
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
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.vigtech.android.vigpark.api.ApiClient
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


private const val TAG = "CrimeListFragment"

class CrimeListFragment : Fragment(){


    lateinit var cameraxHelper: CameraxHelper


    var img64_full: String? = null

    private lateinit var menu: Menu



    private lateinit var crimeRecyclerView: RecyclerView
    private lateinit var mybitmap: Bitmap

    private lateinit var photoButton:ImageButton

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarToggle: ActionBarDrawerToggle
    private lateinit var navView: NavigationView

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




        cameraxHelper = CameraxHelper(
            caller = this,
            previewView =  view.findViewById(R.id.previewView),
            onPictureTaken = { file, uri ->
                Log.i("apptg", "Picture taken ${file.absolutePath} uri=$uri")
            },
            onError = { Log.e("APPTAG", "error") },
            builderPreview = Preview.Builder().setTargetResolution(android.util.Size(200,200)),
            builderImageCapture = ImageCapture.Builder().setTargetResolution(android.util.Size(1024,768)),
            filesDirectory = context?.filesDir

        )
        cameraxHelper.start()
        cameraxHelper.changeCamera()

        drawerLayout = view.findViewById(R.id.drawer_layout)


        actionBarToggle = ActionBarDrawerToggle(activity, drawerLayout, 0, 0)
        drawerLayout.addDrawerListener(actionBarToggle)

        actionBarToggle.syncState()



        // Call findViewById on the NavigationView
        navView = view.findViewById(R.id.navigation)

        crimeRecyclerView =
                view.findViewById(R.id.crime_recycler_view) as RecyclerView
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        photoButton = view.findViewById(R.id.camera_button) as ImageButton

        photoButton.setOnClickListener {
            try {
                cameraxHelper.takePicture()
            }
            catch (e: Exception){
                Log.e("PictureDemo", "Exception in take photo", e)
            }

        }






       //todo camera size cameraBridgeViewBase.setMaxFrameSize(1280, 720)






        //todo свайп
        val swipeHandler = object : SwipeToDeleteCallback(this.context) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                crimeRecyclerView.adapter?.notifyItemRemoved(viewHolder.adapterPosition)
                CoroutineScope(Dispatchers.Default).launch {
                   val crime = crimeListViewModel.getCrimeFromPosition(viewHolder.adapterPosition)
                    CrimeRepository.get().deleteCrime(crime)
                    //notifyItemRemoved(position) execute only once
                    //crimeListViewModel.deleteCrime(crime)
//                    crimeListViewModel.deleteCrime(crime)
                    val file = File(crime.img_path)
                    if (file.exists()) {
                        file.delete()
                    }
                    Log.i("AAAAAAAAAAPPPPPP_TAAAGGG", "Deleting ${crime.title}")
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
                R.id.ip_configuration ->{
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
                        // what ever you want to do with No option.
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
                    builder1.setMessage("Удалить все снимки?")
                    builder1.setCancelable(true)

                    builder1.setPositiveButton(
                        "Да",
                        DialogInterface.OnClickListener { dialog, id ->
                            try {
                                val lastCrimeDate = crimeListViewModel.crimeListLiveData.value?.first()?.date
                                crimeListViewModel.crimeListLiveData.observe(viewLifecycleOwner, Observer<List<Crime>>(){
                                    for(crime in it) {
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


                                    }})
                            } catch (e: Exception){
                                Toast.makeText(requireContext(), "Ничего не нашлось", Toast.LENGTH_SHORT).show()
                            }
                            dialog.cancel() })

                    builder1.setNegativeButton(
                        "Нет",
                        DialogInterface.OnClickListener { dialog, id ->

                            dialog.cancel() })

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
                crimeRecyclerView.visibility = View.GONE
            }
            override fun onDrawerOpened(drawerView: View) {
                crimeRecyclerView.visibility = View.GONE
            }

            override fun onDrawerClosed(drawerView: View) {
                crimeRecyclerView.visibility = View.VISIBLE
            }

            /**
             * Called when the drawer motion state changes. The new state will
             * be one of [.STATE_IDLE], [.STATE_DRAGGING] or [.STATE_SETTLING].
             */
            override fun onDrawerStateChanged(newState: Int) {
                if (newState == 1){
                    crimeRecyclerView.visibility = View.VISIBLE
                }
                Log.i("OFFSET", newState.toString())
            }
        })

        getIpFromShared()?.let { ApiClient.reBuildRetrofit(it) }

        return view
    }

    override fun onResume() {
        super.onResume()


    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        this.menu = menu
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.flash ->{
                if (cameraxHelper.cameraInfo?.torchState?.value == TorchState.ON) {
                    cameraxHelper.cameraControl?.enableTorch(false)
                    item.setIcon(R.drawable.ic_flash_off)
                } else {
                    cameraxHelper.cameraControl?.enableTorch(true)
                    item.setIcon(R.drawable.ic_flash_on)
                }
                true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    val selectImageFromGalleryResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            //todo image picker

            Log.i("Image_picker", uri.path.toString())
            try {
                var path_to_image = context?.filesDir
                val inputStream: InputStream? =
                    context?.getContentResolver()?.openInputStream(uri)
                val bOut2 = ByteArrayOutputStream()
                var bm = BitmapFactory.decodeStream(inputStream)
                bm = PicturesUtils.getResizedBitmap(bm, 720, 480)
                bm.compress(Bitmap.CompressFormat.JPEG, 50, bOut2)
                img64_full = Base64.encodeToString(bOut2.toByteArray(), Base64.DEFAULT)
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
                ApiClient.POST_img64(img64,img_path =  mFile3.path, img_plate_path = "None")
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

    private inner class CrimeHolder(view: View)
        : RecyclerView.ViewHolder(view), View.OnClickListener {

        private lateinit var crime: Crime

        private val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.crime_date)
        private val solvedImageView: ImageView = itemView.findViewById(R.id.crime_solved)
        private val sendIcon:ImageView = itemView.findViewById(R.id.connection)
        private val foundIcon:ImageView = itemView.findViewById(R.id.not_found)



        init {
            itemView.setOnClickListener(this)

        }

        @SuppressLint("SimpleDateFormat")
        fun bind(crime: Crime) {
            this.crime = crime
            titleTextView.text = this.crime.title
            dateTextView.text = SimpleDateFormat("dd-MM-yyyy HH:mm").format(this.crime.date)
//            var path_to_image = "/storage/emulated/0/${crime.img_path}"
            if(File(crime.img_path).exists() && crime.img_path != ""){
                mybitmap = BitmapFactory.decodeFile(crime.img_path)
                solvedImageView.setImageBitmap(Bitmap.createScaledBitmap(mybitmap, 120, 120, false))
                solvedImageView.setVisibility(View.VISIBLE)
            }else{
                solvedImageView.setVisibility(View.INVISIBLE)
            }
            if(!crime.send){
                sendIcon.setVisibility(View.VISIBLE)
                foundIcon.setVisibility(View.GONE)

            }
            else{
                sendIcon.setVisibility(View.GONE)
                if(!crime.found){
                    foundIcon.setVisibility(View.VISIBLE)
                }
                else{
                    foundIcon.setVisibility(View.GONE)
                }
            }






        }

        override fun onClick(v: View) {
            callbacks?.onCrimeSelected(crime.id)
//            if (crime.send) {
//                callbacks?.onCrimeSelected(crime.id)
//            }
//            else{
//               lifecycleScope.launch {
//                   delay(3000)
//                   ResendCrime(crime)
//               }
//            }
        }
    }

    private inner class CrimeAdapter(var crimes: List<Crime>)
        : RecyclerView.Adapter<CrimeHolder>() {

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
//            crimes.removeAt(position) //todo remove adapter
            notifyItemRemoved(position)
        }

        override fun getItemCount() = crimes.size
    }

    companion object{

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

    override fun onStart() {
        super.onStart()
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


    suspend fun ResendCrime(crime: Crime){
        //todo удалить строку, на которую нажал и коорая не отправилась
        if(File(crime.img_path).exists() && crime.img_path != "") {
            val bOut2 = ByteArrayOutputStream()
            var bm = BitmapFactory.decodeFile(crime.img_path)
            bm.compress(Bitmap.CompressFormat.JPEG, 100, bOut2)
            img64_full = Base64.encodeToString(bOut2.toByteArray(), Base64.DEFAULT)
            crime.date = Calendar.getInstance().time
            ApiClient.POST_img64(img64_full.toString(), crime)
        }
        else{
            Log.e("RESEND CRIME", "not deleted")
        }


    }



    private fun flash(camera: androidx.camera.core.Camera, item: MenuItem){
        camera.apply {
            if (cameraInfo.hasFlashUnit()) {
                item.isVisible = true
                    cameraControl.enableTorch(cameraInfo.torchState.value == TorchState.OFF)

            } else {
                item.isVisible = false
            }

            cameraInfo.torchState.observe(viewLifecycleOwner) { torchState ->
                if (torchState == TorchState.OFF) {
                    item.setIcon(R.drawable.ic_flash_off)
                } else {
                    item.setIcon(R.drawable.ic_flash_on)
                }
            }
        }
    }


    private fun getIpFromShared(): String? {
        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        var url = preferences.getString("ip", "")
        if (!url.equals("", ignoreCase = true)) {
            return url
        }
        else{
            return "http://95.182.74.37:1234/"
        }
    }

}

