package com.example.maptest.ai.map

import android.Manifest
import android.animation.LayoutTransition
import android.app.Activity
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.maptest.R
import com.example.maptest.databinding.FragmentMapBinding
import com.example.maptest.utils.drawableToBitmap
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.mylocation.FlagOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class MapFragment : Fragment() {

    private val viewModel by viewModels<MapViewModel>()
    private lateinit var binding: FragmentMapBinding
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private val compositeDisposable = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        initButtonsListener()
        initMap()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val neededPermissions = getNeededPermissions()
        requestNeededPermissions(neededPermissions)
        checkLocation { createMyLocationOverlay() }
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
        initSubscribes()
    }

    private fun initButtonsListener() {
        binding.mapZoomPlusButton.setOnClickListener {
            binding.map.controller.zoomIn()
        }

        binding.mapZoomMinusButton.setOnClickListener {
            binding.map.controller.zoomOut()
        }

        binding.mapMyLocationButton.setOnClickListener {
            checkLocation {
                myLocationOverlay?.let { mLocationOverlay ->
                    binding.map.controller.animateTo(mLocationOverlay.myLocation, 10.0, 500L);
                } ?: createMyLocationOverlay()
            }
        }
        binding.mapNextFlagButton.setOnClickListener {
            viewModel.activeFlagInfoNumber = viewModel.activeFlagInfoNumber + 1
            showFlagInfoBehavior()
        }
        binding.includeBottomSheet.bottomSheet.setOnClickListener {
            viewModel.activeFlagInfoNumber = 0
            hideFlagInfoBehavior()
        }
    }

    private fun initMap() {
        Configuration.getInstance().load(
            context,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        binding.map.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 4.0
            controller.setZoom(5.0)
        }
    }

    private fun getNeededPermissions(): Array<String> {
        return arrayListOf<String>().also {
            if (checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                it.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                it.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }.toTypedArray()
    }

    private fun requestNeededPermissions(permissions: Array<String>) {
        if (permissions.isNotEmpty()) {
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                it.forEach { entry ->
                    if (!entry.value) {
                        showMessagePermissionsNeeded()
                        return@forEach
                    }
                }
            }.launch(permissions)
        }
    }

    private fun initSubscribes() {
        val disposableFlags = viewModel.getFlags()
            .flatMap {
                Observable.fromIterable(it)
            }
            .observeOn(Schedulers.io())
            .flatMap { flag ->
                val avatarBitmap =
                    try {
                        Glide.with(requireContext()).asBitmap().load(flag.urlAvatar)
                            .submit()
                            .get()
                    } catch (e: Exception) {
                        AppCompatResources.getDrawable(requireContext(), R.drawable.ic_placeholder)
                            ?.let { drawableToBitmap(it) }
                    }
                val flagOverlay = FlagOverlay(
                    avatarBitmap,
                    GeoPoint(flag.latitude, flag.longitude)
                )
                flagOverlay.setClickListener {
                    if (viewModel.activeFlagInfoNumber == flag.id) {
                        viewModel.activeFlagInfoNumber = 0
                        hideFlagInfoBehavior()
                    } else {
                        viewModel.activeFlagInfoNumber = flag.id
                        showFlagInfoBehavior()
                    }
                }
                Observable.fromArray(flagOverlay)
            }
            .subscribe(
                {
                    binding.map.overlays.add(it)
                }, {}
            )

        val disposableActiveFlag = viewModel.getActiveFlagInfoNumberUpdate().subscribe { flag ->
            binding.includeBottomSheet.apply {
                nameText.text = flag.name
                dateText.text = flag.date
                timeText.text = flag.time
                gpsStateImageView.setImageDrawable(
                    if (flag.isGpsActive) {
                        AppCompatResources.getDrawable(requireContext(), R.drawable.ic_gps_on)
                    } else {
                        AppCompatResources.getDrawable(requireContext(), R.drawable.ic_gps_off)
                    }
                )
                Glide
                    .with(requireContext())
                    .load(flag.urlAvatar)
                    .circleCrop()
                    .into(avatarImageView)

            }
            binding.map.controller.animateTo(GeoPoint(flag.latitude, flag.longitude), null, 500L)
        }

        compositeDisposable.addAll(disposableFlags, disposableActiveFlag)
    }


    private fun createMyLocationOverlay() {
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), binding.map)
        myLocationOverlay?.let { mLocationOverlay ->
            mLocationOverlay.enableMyLocation()
            val myLocationBitmap =
                BitmapFactory.decodeResource(resources, R.drawable.ic_my_tracker_46dp)
            mLocationOverlay.setPersonIcon(myLocationBitmap)
            binding.map.overlays.add(mLocationOverlay)
        }
        myLocationOverlay?.runOnFirstFix {
            requireActivity().runOnUiThread {
                myLocationOverlay?.let { mLocationOverlay ->
                    binding.map.controller.animateTo(mLocationOverlay.myLocation, 10.0, 500L);
                }
            }
        }
    }


    private fun checkLocation(successCallback: () -> Unit) {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(requireActivity())
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            successCallback()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        1,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (sendEx: SendIntentException) {
                    Toast.makeText(context, exception.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            requestCode == 1 && resultCode == Activity.RESULT_OK -> {
                myLocationOverlay?.let { mLocationOverlay ->
                    binding.map.controller.animateTo(mLocationOverlay.myLocation, 10.0, 500L)
                } ?: createMyLocationOverlay()
            }
            requestCode == 1 && resultCode == Activity.RESULT_CANCELED -> {
                showMessagePermissionsNeeded()
            }
        }

    }

    private fun showFlagInfoBehavior() {
        val set = ConstraintSet()
        set.clone(binding.root)
        set.clear(R.id.include_bottom_sheet, ConstraintSet.TOP)
        set.connect(
            R.id.include_bottom_sheet,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            0
        )
        set.applyTo(binding.root)
    }

    private fun hideFlagInfoBehavior() {
        val set = ConstraintSet()
        set.clone(binding.root)
        set.clear(R.id.include_bottom_sheet, ConstraintSet.BOTTOM)
        set.connect(
            R.id.include_bottom_sheet,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            0
        )
        set.applyTo(binding.root)
    }

    private fun showMessagePermissionsNeeded() {
        Toast.makeText(
            context,
            "НУЖНЫ ВСЕ РАЗРЕШЕНИЯ ДЛЯ ПРАВИЛЬНОГО ФУНКЦИОНИРОВАНИЯ",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
        compositeDisposable.clear()
    }
}