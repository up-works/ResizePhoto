package org.onedroid.stb_image_resize

class NativeLib {

    /**
     * A native method that is implemented by the 'stb_image_resize' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'stb_image_resize' library on application startup.
        init {
            System.loadLibrary("stb_image_resize")
        }
    }
}