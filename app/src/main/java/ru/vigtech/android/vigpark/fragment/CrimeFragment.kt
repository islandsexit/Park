package ru.vigtech.android.vigpark.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import android.opengl.Visibility
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import ru.vigtech.android.vigpark.AliasZone
import ru.vigtech.android.vigpark.database.Crime
import ru.vigtech.android.vigpark.ui.EditTextWithDel
import ru.vigtech.android.vigpark.tools.PicturesUtils
import ru.vigtech.android.vigpark.R
import ru.vigtech.android.vigpark.api.ApiClient
import ru.vigtech.android.vigpark.viewmodel.CrimeDetailViewModel
import java.io.File
import java.util.*




class CrimeFragment : Fragment(){




    var isEdit = false

    private lateinit var crime: Crime
    private lateinit var titleField: EditTextWithDel

    private lateinit var iconFound: ImageView
    private lateinit var iconSend: ImageView
    private lateinit var textFound: TextView
    private lateinit var textSend: TextView
    private lateinit var workDetails: TextView
    private lateinit var workIcon: ImageView
    private lateinit var workText: TextView

   private lateinit var longlat: Button

    private lateinit var resend_fragment_activity: Button
    private lateinit var photoView: ImageView
    private lateinit var photoViewSmall: ImageView


    var lastEvent: FloatArray? = null
    var d = 0f
    private var isZoomAndRotate = false
    private var isOutSide = false
    private val NONE = 0
    private val DRAG = 1
    private val ZOOM = 2
    private var mode = NONE
    private val start = PointF()
    private val mid = PointF()
    var oldDist = 1f
    private var xCoOrdinate = 0f
    private  var yCoOrdinate = 0f

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)
        isEdit = false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)

        titleField = view.findViewById(R.id.crime_title) as EditTextWithDel
        workDetails = view.findViewById(R.id.crime_det)


        longlat = view.findViewById(R.id.longlat)

        photoView = view.findViewById(R.id.crime_photo) as ImageView
        photoViewSmall = view.findViewById(R.id.crime_photo_small) as ImageView
        photoViewSmall.visibility= View.VISIBLE

        iconFound = view.findViewById(R.id.crimefragment_icon_found)
        iconSend = view.findViewById(R.id.crimefragment_icon_send)
        textFound = view.findViewById(R.id.crimefragment_text_found)
        textSend = view.findViewById(R.id.crimefragment_text_send)
        workText = view.findViewById(R.id.crimefragment_text_worker)
        workIcon = view.findViewById(R.id.crimefragment_icon_worker)


        titleField.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if(!isEdit) {
                isEdit = true
                resend_fragment_activity.visibility = View.VISIBLE
                resend_fragment_activity.text = "Отправить изменения"
            }
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
                    titleField.getWindowToken(),
                    0
                )


                return@OnKeyListener true
            }
            false
        })


        resend_fragment_activity = view.findViewById(R.id.resend_fragment_activity) as Button

//        photoView.setOnTouchListener { v, event ->
//            v.bringToFront()
//            viewTransformation(v, event)
//            true
//        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val crimeId = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                crime?.let {
                    this.crime = crime
                    updateUI()
                }
            })
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {

            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {

            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                crime.title = sequence.toString()


            }


            override fun afterTextChanged(sequence: Editable?) {

            }
        }

        val titleWatcher_photo = object : TextWatcher {

            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // This space intentionally left blank
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                crime.img_path = sequence.toString()
            }


            override fun afterTextChanged(sequence: Editable?) {
                // This one too
            }
        }


        titleField.addTextChangedListener(titleWatcher)



//        if(crime.send || crime.found){
//            resend_fragment_activity.visibility = View.GONE
//
//        }

        resend_fragment_activity.setOnClickListener{
           val img_64 = PicturesUtils.getImg_64(crime)
            if(img_64 != ""){
                if(!isEdit) {
                    ApiClient.POST_img64(img_64, crime)
                    ApiClient.POST_img64(img_64, crime)

                }else{
                    crime.title = titleField.text.toString()
                    ApiClient.POST_img64_with_edited_text(img_64, "i", crime)
                }
                crime.date = Calendar.getInstance().time
                val fm: FragmentManager = requireActivity().supportFragmentManager
                fm.popBackStack()

        }}




    }

    override fun onStop() {
        super.onStop()
//        crimeDetailViewModel.saveCrime(crime)
    }



    private fun updateUI() {
        if (crime.info.count() >= 1 && crime.info != "null"){
            workDetails.text = crime.info
            workIcon.visibility = View.VISIBLE
            workText.visibility = View.VISIBLE

        }
        titleField.setText(crime.title)
        if(File(crime.img_path).exists() && crime.img_path != ""){
           var mybitmap = BitmapFactory.decodeFile(crime.img_path)
            try{
                if(!crime.Rect.isNullOrEmpty()) {
                    val myBitmapSmall =
                        Bitmap.createBitmap(
                            mybitmap, crime.Rect?.get(0)?.toInt()!!,
                            crime.Rect?.get(1)?.toInt()!!,
                            crime.Rect?.get(2)?.toInt()!!, crime.Rect?.get(3)?.toInt()!!
                        )
                    photoViewSmall.setImageBitmap(Bitmap.createScaledBitmap(myBitmapSmall, (myBitmapSmall.width*2.5).toInt(), (myBitmapSmall.height*2.5).toInt(), false))
                    photoViewSmall.visibility = View.VISIBLE

                }else{
                    photoViewSmall.visibility = View.INVISIBLE
                }
            }
            catch (e:Exception){
                photoViewSmall.visibility = View.INVISIBLE
            }

            photoView.setImageBitmap(Bitmap.createBitmap(mybitmap))
            photoView.visibility = View.VISIBLE
        }else{
            photoView.visibility = View.INVISIBLE
        }


        if(!crime.send){
            iconSend.visibility = View.VISIBLE
            textSend.visibility = View.VISIBLE
        }else if(!crime.found){
            iconFound.visibility = View.VISIBLE
            textFound.visibility = View.VISIBLE
        }

        longlat.setOnClickListener {
            if (crime.lat == 0.0){
                Toast.makeText(requireContext(), "Нет геоданных", Toast.LENGTH_SHORT).show()
            }
            else {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0?q=${crime.lat},${crime.lon}")
                )
                startActivity(intent)
            }
        }

//        val text = "Местоположение по <a href=\"https://yandex.ru/maps/?pt=${crime.lat},${crime.lon}&z=18&l=map\">ссылке</a>"
//        longlat.text = Html.fromHtml(text);
//        longlat.movementMethod = LinkMovementMethod.getInstance()
//        longlat.setLinkTextColor(Color.parseColor("blue"))


        }


    private fun viewTransformation(view: View, event: MotionEvent) {

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                Log.i("zoom", "${view.x}, ${event.rawY + yCoOrdinate}")
                xCoOrdinate = view.x - event.rawX
                yCoOrdinate = view.y - event.rawY
                start[event.x] = event.y
                isOutSide = false
                mode = DRAG
                lastEvent = null
                view.animate().x(view.x).y(view.y)
                    .setDuration(0).start()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    midPoint(mid, event)
                    mode = ZOOM
                }
                lastEvent = FloatArray(4)
                lastEvent!![0] = event.getX(0)
                lastEvent!![1] = event.getX(1)
                lastEvent!![2] = event.getY(0)
                lastEvent!![3] = event.getY(1)
                d = rotation(event)
            }
            MotionEvent.ACTION_UP -> {
                isZoomAndRotate = false
                if (mode === DRAG) {
                    val x = event.x
                    val y = event.y
                }
                isOutSide = true
                mode = NONE
                lastEvent = null
                mode = NONE
                lastEvent = null
            }
            MotionEvent.ACTION_OUTSIDE -> {
                isOutSide = true
                mode = NONE
                lastEvent = null
                mode = NONE
                lastEvent = null
            }
            MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                lastEvent = null
            }
            MotionEvent.ACTION_MOVE -> if (!isOutSide) {
                if (mode === DRAG) {
                    isZoomAndRotate = false
                    view.animate().x(event.rawX + xCoOrdinate).y(event.rawY + yCoOrdinate)
                        .setDuration(0).start()
                }
                if (mode === ZOOM && event.pointerCount == 2) {
                    val newDist1 = spacing(event)
                    if (newDist1 > 10f) {
                        val scale = newDist1 / oldDist * view.scaleX
                        view.scaleX = scale
                        view.scaleY = scale
                    }

                }
            }
        }
    }

    private fun rotation(event: MotionEvent): Float {
        val delta_x = (event.getX(0) - event.getX(1)).toDouble()
        val delta_y = (event.getY(0) - event.getY(1)).toDouble()
        val radians = Math.atan2(delta_y, delta_x)
        return Math.toDegrees(radians).toFloat()
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return Math.sqrt((x * x + y * y).toDouble()).toInt().toFloat()
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point[x / 2] = y / 2
    }


    companion object {
        private  val ARG_CRIME_ID = "crime_id"
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }


        }

    }


}