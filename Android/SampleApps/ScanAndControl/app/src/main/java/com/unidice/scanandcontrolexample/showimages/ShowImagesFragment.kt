package com.unidice.scanandcontrolexample.showimages

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.unidice.scanandcontrolexample.ExampleApplication
import com.unidice.scanandcontrolexample.MainActivity
import com.unidice.scanandcontrolexample.databinding.FragmentShowImagesBinding
import com.unidice.sdk.api.UnidiceController

// This fragment demonstates:
// - Requesting the unidice to tell us what images it has "loading the asset list"
// - Asking our game controller to push images to the unidice if needed
// - Responding to the event that the unidice has finished telling our app the images it has
// - Asking our game controller to display images on screens.

class ShowImagesFragment : Fragment() {

    private val TAG = "ShowImagesFragment"
    private var _binding: FragmentShowImagesBinding? = null
    private val binding get() = _binding!!
    private var desireSendingImagesToUnidice: Boolean = false
    private lateinit var showImagesViewModel: ShowImagesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentShowImagesBinding.inflate(inflater, container, false)

        showImagesViewModel = ViewModelProvider(requireActivity()).get(ShowImagesViewModel::class.java)

        // This will request a fresh description of assets the Unidice
        // has.  Your game only needs to do this once after a connection, or anytime
        // you have deleted or added images to the Unidice.
        //
        // Once the asset list is done being fetched, the unidiceModel.assetDetailLoadComplete
        // observable field will become set to true.
        //
        binding.sendImages.setOnClickListener {
            desireSendingImagesToUnidice = true
            showBusySpinner()

            // Ask the Unidice to tell us about the images it has
            //
            unidiceController().queueGetAssetList()
        }

        // React when the Unidice completes telling us about the images it has
        // In this game we either take no action, or we trigger a push of game
        // images to the Unidice.
        //
        val unidiceModel = unidiceController().getUnidiceModel()
        unidiceModel.assetDetailLoadComplete?.observe(viewLifecycleOwner) {
            if (unidiceModel.assetDetailLoadComplete.value == true) {
                Log.d(TAG, "completed unidice asset detail load")
                onAssetListLoadComplete()
            } else {
                Log.d(TAG, "beginning unidice asset detail load")
            }
        }

        // React when we're done pushing images to the Unidice
        //
        showImagesViewModel.imageLoadComplete.observe(viewLifecycleOwner) {
            if (showImagesViewModel.imageLoadComplete.value == true) {
                onImageLoadFinished()
            }
        }

        // React when our image upload percentage complete updates
        //
        binding.percentComplete.text = ""
        showImagesViewModel.imageLoadPercentComplete.observe(viewLifecycleOwner) {
            val percentInt = showImagesViewModel.imageLoadPercentComplete.value?:0
            binding.percentComplete.text = "Percent Complete: $percentInt%"
        }

        binding.showImageSet1.setOnClickListener {
            onShowImageSet1()
        }

        binding.showImageSet2.setOnClickListener {
            onShowImageSet2()
        }

        binding.eraseMyImages.setOnClickListener {
            onEraseAllGameImages()
        }

        hideBusySpinner()
        return binding.root
    }

    fun onImageLoadFinished() {
        hideBusySpinner()

        desireSendingImagesToUnidice = false
        // Ask the Unidice to tell us about the images it now has
        //
        unidiceController().queueGetAssetList()
    }

    // In this game example we may have asked for the asset list with the intention
    // of pushing game images to the Unidice once the list is done being loaded.
    // In a real game you'd probably store desireSendingImagesToUnidice in the view model
    // or save the value/state of desireSendingImagesToUnidice as the fragment is stopped or
    // started.  Such details are not done in this app for simplicity and clarity.
    //
    fun onAssetListLoadComplete() {
        if(desireSendingImagesToUnidice) {
            val gameController = (activity as MainActivity).getShowImagesGameController()

            if (gameController.doesUnidiceHaveTheImagesWeNeed()) {
                onImageLoadFinished()
            } else {
                gameController.beginImagePush(showImagesViewModel)
            }
        } else {
            hideBusySpinner()
        }
    }

    fun unidiceController() : UnidiceController {
        return (requireActivity().application as ExampleApplication).getUnidiceController()
    }

    fun showBusySpinner() {
        binding.progressBar.visibility = View.VISIBLE
    }

    fun hideBusySpinner() {
        binding.progressBar.visibility = View.GONE
    }

    fun onShowImageSet1() {
        val gameController = (activity as MainActivity).getShowImagesGameController()
        gameController.showImageSet1()
    }

    fun onShowImageSet2() {
        val gameController = (activity as MainActivity).getShowImagesGameController()
        gameController.showImageSet2()
    }

    fun onEraseAllGameImages() {
        showBusySpinner()
        unidiceController().queueEraseAllGameAssets()
        val mainHandler = Handler(Looper.getMainLooper()).postDelayed ({
            desireSendingImagesToUnidice = false
            // Ask the Unidice to tell us about the images it now has
            //
            unidiceController().queueGetAssetList()
        }, 5000)    //SDKTODO: monitor 'erase assets complete' message from Unidice.
    }

}