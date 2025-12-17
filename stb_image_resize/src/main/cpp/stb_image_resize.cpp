#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <android/log.h>
#include <cstring>

#define STB_IMAGE_RESIZE_IMPLEMENTATION
#define STBIR_MALLOC(size, context) malloc(size)
#define STBIR_FREE(ptr, context) free(ptr)
#include "stb_image_resize.h"

#define LOG_TAG "STBResize"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jboolean JNICALL
Java_org_onedroid_resizephoto_core_algorithm_StbImageResizer_nativeResize(
        JNIEnv* env,
        jobject /* this */,
        jobject srcBitmap,
        jobject dstBitmap,
        jint filter
) {
    AndroidBitmapInfo srcInfo, dstInfo;
    void* srcPixels = nullptr;
    void* dstPixels = nullptr;

    // Get source bitmap info
    if (AndroidBitmap_getInfo(env, srcBitmap, &srcInfo) < 0) {
        LOGE("Failed to get source bitmap info");
        return JNI_FALSE;
    }

    // Get destination bitmap info
    if (AndroidBitmap_getInfo(env, dstBitmap, &dstInfo) < 0) {
        LOGE("Failed to get destination bitmap info");
        return JNI_FALSE;
    }

    // Only support RGBA_8888
    if (srcInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888 ||
        dstInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Unsupported bitmap format");
        return JNI_FALSE;
    }

    // Lock pixels
    if (AndroidBitmap_lockPixels(env, srcBitmap, &srcPixels) < 0) {
        LOGE("Failed to lock source pixels");
        return JNI_FALSE;
    }

    if (AndroidBitmap_lockPixels(env, dstBitmap, &dstPixels) < 0) {
        AndroidBitmap_unlockPixels(env, srcBitmap);
        LOGE("Failed to lock destination pixels");
        return JNI_FALSE;
    }

    // Perform resize using the extended API
    stbir_pixel_layout layout = STBIR_RGBA;
    stbir_datatype datatype = STBIR_TYPE_UINT8;
    stbir_edge edge = STBIR_EDGE_CLAMP;
    stbir_filter filterType = static_cast<stbir_filter>(filter);

    void* result = stbir_resize(
            srcPixels, srcInfo.width, srcInfo.height, srcInfo.stride,
            dstPixels, dstInfo.width, dstInfo.height, dstInfo.stride,
            layout, datatype,
            edge, filterType
    );

    // Unlock pixels
    AndroidBitmap_unlockPixels(env, srcBitmap);
    AndroidBitmap_unlockPixels(env, dstBitmap);

    // result is dstPixels on success, NULL on failure
    return (result != nullptr) ? JNI_TRUE : JNI_FALSE;
}


extern "C"
JNIEXPORT jint JNICALL
Java_org_onedroid_resizephoto_core_algorithm_StbImageResizer_getFilterDefault(JNIEnv *env,jclass clazz) {
    return STBIR_FILTER_DEFAULT;
}
extern "C"
JNIEXPORT jint JNICALL
Java_org_onedroid_resizephoto_core_algorithm_StbImageResizer_getFilterMitchell(JNIEnv *env,jclass clazz) {
    return STBIR_FILTER_MITCHELL;
}
extern "C"
JNIEXPORT jint JNICALL
Java_org_onedroid_resizephoto_core_algorithm_StbImageResizer_getFilterCubicBSpline(JNIEnv *env,jclass clazz) {
    return STBIR_FILTER_CUBICBSPLINE;
}
extern "C"
JNIEXPORT jint JNICALL
Java_org_onedroid_resizephoto_core_algorithm_StbImageResizer_getFilterCatmullRom(JNIEnv *env,jclass clazz) {
    return STBIR_FILTER_CATMULLROM;
}

