package com.unidice.scanandcontrolexample.showimages

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.unidice.scanandcontrolexample.ExampleApplication
import com.unidice.sdk.api.AssetSpecialPurpose
import com.unidice.sdk.api.UnidiceController
import com.unidice.sdk.internal.UnidiceControllerBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

class DiceImage(
    var guid: String,
    var assetDirFilename: String,
    var specialPurpose: AssetSpecialPurpose
) {

}

// This is not part of the Unidice SDK.  It is a class which manages
// which images we use in our game and several other game actions such
// as pushing the images we desire to the unidice.
//
// No 'rolling rules' are pushed to the Unidice during this simple game. The effect
// is that whatever rolling rules were already on the Unidice when this game was launched
// will remain active.  How to set and control Rolling Rules is described in another example
// mini game.
//
class ShowImagesGameController {
    private val TAG = "ShowImagesGameController"

    private var diceDownloadJobQueue = ConcurrentLinkedQueue<DiceImage>()
    private lateinit var showImagesViewModel: ShowImagesViewModel
    private var downloadImageHandler = Handler(Looper.getMainLooper())
    private lateinit var downloadImageRunnable: Runnable

    // This small game uses static images that app holds in the /assets folder
    //
    // here are their guids
    //
    private var cyberrun_1_guid = "1000d9d340104a72911bf1bb228575d1"
    private var cyberrun_2_guid = "1000d9d340104a72911bf1bb228575d2"
    private var cyberrun_3_guid = "1000d9d340104a72911bf1bb228575d3"
    private var cyberrun_4_guid = "1000d9d340104a72911bf1bb228575d4"
    private var cyberrun_5_guid = "1000d9d340144a72911bf1bb228575d5"

    private var dating_1_guid = "1001d9d340104a72911bf1bb228575d1"
    private var dating_2_guid = "1001d9d340104a72911bf1bb228575d2"
    private var dating_3_guid = "1001d9d340104a72911bf1bb228575d3"
    private var dating_4_guid = "1001d9d340104a72911bf1bb228575d4"
    private var dating_5_guid = "1001d9d340144a72911bf1bb228575d5"
    private var dating_6_guid = "1001d9d340144a72911bf1bb228575d6"

    val imageList = mutableListOf<DiceImage>()
    val diceImageList = mutableListOf<DiceImage>()
    var numAssetsToLoad = 0

    fun init() {
        readyListOfAssetsForUnidice()
    }

    private fun readyListOfAssetsForUnidice() {
        // Dice images used in the game
        imageAdd(DiceImage( cyberrun_1_guid, "cyberrun_1.jpg",  AssetSpecialPurpose.NONE))
        imageAdd(DiceImage( cyberrun_2_guid, "cyberrun_2.jpg",  AssetSpecialPurpose.NONE))
        imageAdd(DiceImage( cyberrun_3_guid, "cyberrun_3.jpg",  AssetSpecialPurpose.NONE))
        imageAdd(DiceImage( cyberrun_4_guid, "cyberrun_4.jpg",  AssetSpecialPurpose.NONE))
        imageAdd(DiceImage( cyberrun_5_guid, "cyberrun_5.jpg",  AssetSpecialPurpose.NONE))

        imageAdd(DiceImage( dating_1_guid, "dating_1.jpg",  AssetSpecialPurpose.NONE))
        imageAdd(DiceImage( dating_2_guid, "dating_2.jpg",  AssetSpecialPurpose.NONE))
        imageAdd(DiceImage( dating_3_guid, "dating_3.jpg",  AssetSpecialPurpose.NONE))
        imageAdd(DiceImage( dating_4_guid, "dating_4.jpg",  AssetSpecialPurpose.NONE))
        imageAdd(DiceImage( dating_5_guid, "dating_5.jpg",  AssetSpecialPurpose.NONE))
        imageAdd(DiceImage( dating_6_guid, "dating_6.jpg",  AssetSpecialPurpose.NONE))
    }

    fun diceListHasImageInIt(image: DiceImage): Boolean {
        val targetGUID = image.guid

        diceImageList.forEach {
            if(it.guid == targetGUID) {
                return true
            }
        }

        return false
    }

    fun downloadQueueHasImageInIt(image: DiceImage): Boolean {
        val targetGUID = image.guid

        diceDownloadJobQueue.forEach {
            if(it.guid == targetGUID) {
                return true
            }
        }

        return false
    }

    fun imageAdd(image: DiceImage) {
        if (!diceListHasImageInIt(image)) {
            diceImageList.add(image)
        }

        if (!downloadQueueHasImageInIt(image)) {
            diceDownloadJobQueue.add(image)
        }
    }

    fun doesUnidiceHaveTheImagesWeNeed(): Boolean {
        val app = ExampleApplication.applicationContext() as ExampleApplication
        val controller = app.getUnidiceController()
        val unidiceModel = controller.getModel()

        if (diceImageList.size == 0) {
            throw Exception("Unidice doesn't have any images?  or the asset list was not loaded from it first?")
        }

        diceImageList.forEach {
            if (!unidiceModel.hasAssetGUID(it.guid)) {
                return false;
            }
        }

        return true
    }

    private fun getNextNeededDownload(): DiceImage? {
        if (diceDownloadJobQueue.size == 0){
            return null
        }

        val app = ExampleApplication.applicationContext() as ExampleApplication
        val controller = app.getUnidiceController()
        val unidiceModel = controller.getModel()

        while (diceDownloadJobQueue.size > 0) {
            val op = diceDownloadJobQueue.remove()
            if (!unidiceModel.hasAssetGUID(op.guid)) {
                // The Unidice doesn't have this asset yet.  Return it so we can push it.
                return op
            }
        }

        return null
    }

    // We're done downloading an asset to the unidice?
    // Then try to download a new one.
    private fun uploadFinishedCallback(successful: Boolean, b: Boolean) {

        // Update our progress graph
        GlobalScope.launch(Dispatchers.Main) {
            var pct = diceDownloadJobQueue.size.toFloat() / numAssetsToLoad.toFloat()     // 100 / 25
            pct = pct * 100.0f
            pct = 100.0f - pct
            showImagesViewModel.imageLoadPercentComplete.value = pct.toInt()
            Log.d(TAG, "Image push percent complete: " + pct.toInt())
            triggerAssetDownload()
        }
    }

    // Let's start send our images to the Unidice one by one
    //
    private fun triggerAssetDownload() {
        Log.d(TAG, "triggerAssetDownload entry")

        val neededJob = getNextNeededDownload()
        if (neededJob != null) {
            val app = ExampleApplication.applicationContext() as ExampleApplication

            downloadImageRunnable = Runnable() {

                GlobalScope.launch(Dispatchers.IO) {
                    Log.d(TAG, "triggerAssetDownload calling uploadImageToUnidice() ");
                    app.getUnidiceController()
                        .uploadImageToUnidice(neededJob.assetDirFilename, neededJob.guid, 240, 240,
                            neededJob.specialPurpose,
                            ::uploadFinishedCallback)
                }

            }
            downloadImageHandler.postDelayed(downloadImageRunnable, 10)
        } else {
            Log.d(TAG, "triggerAssetDownload: NON LEFT TO TRANSMIT");
            showImagesViewModel.imageLoadComplete.value = true
        }
    }

    // Attempt to send our list of images to the unidice
    //
    fun beginImagePush(showImagesViewModel: ShowImagesViewModel) {
        this.showImagesViewModel = showImagesViewModel

        readyListOfAssetsForUnidice()

        numAssetsToLoad = diceDownloadJobQueue.size
        triggerAssetDownload()

    }

    fun unidiceController() : UnidiceControllerBase {
        val app = ExampleApplication.applicationContext() as ExampleApplication
        return app.getUnidiceController()
    }

    fun showImageSet1() {
        val unidiceController = unidiceController()
        val unidiceModel = unidiceController.getModel()

        var assetId = unidiceModel.assetIDGivenGUIDOrDefault(cyberrun_1_guid, 0u)
        unidiceController.displayAssetOnScreens(assetId, UnidiceController.UNIDICE_SCREEN_1 )

        assetId = unidiceModel.assetIDGivenGUIDOrDefault(cyberrun_2_guid, 0u)
        unidiceController.displayAssetOnScreens(assetId, UnidiceController.UNIDICE_SCREEN_2 )

        assetId = unidiceModel.assetIDGivenGUIDOrDefault(cyberrun_3_guid, 0u)
        unidiceController.displayAssetOnScreens(assetId, UnidiceController.UNIDICE_SCREEN_3 )

        assetId = unidiceModel.assetIDGivenGUIDOrDefault(cyberrun_4_guid, 0u)
        unidiceController.displayAssetOnScreens(assetId, UnidiceController.UNIDICE_SCREEN_4 )

        assetId = unidiceModel.assetIDGivenGUIDOrDefault(cyberrun_5_guid, 0u)
        unidiceController.displayAssetOnScreens(assetId, UnidiceController.UNIDICE_SCREEN_5 )

        assetId = unidiceModel.assetIDGivenGUIDOrDefault(dating_1_guid, 0u)
        unidiceController.displayAssetOnScreens(assetId, UnidiceController.UNIDICE_SCREEN_6 )
    }

    fun showImageSet2() {
        val unidiceController = unidiceController()
        val unidiceModel = unidiceController.getModel()

        var assetId = unidiceModel.assetIDGivenGUIDOrDefault(dating_1_guid, 0u)
        unidiceController.displayAssetOnScreens(assetId, UnidiceController.UNIDICE_SCREEN_1 )

        assetId = unidiceModel.assetIDGivenGUIDOrDefault(dating_2_guid, 0u)
        unidiceController.displayAssetOnScreens(assetId, UnidiceController.UNIDICE_SCREEN_2 )

        assetId = unidiceModel.assetIDGivenGUIDOrDefault(dating_3_guid, 0u)
        unidiceController.displayAssetOnScreens(assetId, UnidiceController.UNIDICE_SCREEN_3 )

        assetId = unidiceModel.assetIDGivenGUIDOrDefault(dating_4_guid, 0u)
        unidiceController.displayAssetOnScreens(assetId, UnidiceController.UNIDICE_SCREEN_4 )

        assetId = unidiceModel.assetIDGivenGUIDOrDefault(dating_5_guid, 0u)
        unidiceController.displayAssetOnScreens(assetId, UnidiceController.UNIDICE_SCREEN_5 )

        assetId = unidiceModel.assetIDGivenGUIDOrDefault(cyberrun_1_guid, 0u)
        unidiceController.displayAssetOnScreens(assetId, UnidiceController.UNIDICE_SCREEN_6 )
    }



}