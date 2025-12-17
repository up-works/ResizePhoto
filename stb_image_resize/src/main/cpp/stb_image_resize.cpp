#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <android/log.h>
#include <cstring>
#include <thread>
#include <vector>

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

    STBIR_RESIZE resize;
    stbir_resize_init(&resize, srcPixels, srcInfo.width, srcInfo.height, srcInfo.stride,
                      dstPixels, dstInfo.width, dstInfo.height, dstInfo.stride,
                      layout, datatype);
    stbir_set_filters(&resize, filterType, filterType);
    stbir_set_edgemodes(&resize, edge, edge);

    int num_threads = std::thread::hardware_concurrency();
    if (num_threads < 1) num_threads = 1;

    // Build samplers and get actual split count
    int split_count = stbir_build_samplers_with_splits(&resize, num_threads);

    if (split_count > 1) {
        std::vector<std::thread> threads;
        threads.reserve(split_count);

        for (int i = 0; i < split_count; ++i) {
            threads.emplace_back([&resize, i]() {
                stbir_resize_extended_split(&resize, i, 1);
            });
        }

        for (auto& t : threads) {
            t.join();
        }
    } else {
        // Fallback to single thread if splitting failed or not needed
        stbir_resize_extended_split(&resize, 0, 1);
    }

    stbir_free_samplers(&resize);

    // Unlock pixels
    AndroidBitmap_unlockPixels(env, srcBitmap);
    AndroidBitmap_unlockPixels(env, dstBitmap);

    return JNI_TRUE;
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
extern "C"
JNIEXPORT jint JNICALL
Java_org_onedroid_resizephoto_core_algorithm_StbImageResizer_getFilterBox(JNIEnv *env,jclass clazz) {
    return STBIR_FILTER_BOX;
}
extern "C"
JNIEXPORT jint JNICALL
Java_org_onedroid_resizephoto_core_algorithm_StbImageResizer_getFilterTriangle(JNIEnv *env,jclass clazz) {
    return STBIR_FILTER_TRIANGLE;
}
extern "C"
JNIEXPORT jint JNICALL
Java_org_onedroid_resizephoto_core_algorithm_StbImageResizer_getFilterPointSample(JNIEnv *env,jclass clazz) {
    return STBIR_FILTER_POINT_SAMPLE;
}

