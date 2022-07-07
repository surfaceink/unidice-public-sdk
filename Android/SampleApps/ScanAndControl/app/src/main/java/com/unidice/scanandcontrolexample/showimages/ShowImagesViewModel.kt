package com.unidice.scanandcontrolexample.showimages

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ShowImagesViewModel : ViewModel() {
    var imageLoadComplete = MutableLiveData<Boolean>()
    var imageLoadPercentComplete = MutableLiveData<Int>()

}