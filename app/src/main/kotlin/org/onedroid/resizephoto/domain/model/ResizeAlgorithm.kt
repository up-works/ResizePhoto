package org.onedroid.resizephoto.domain.model

enum class ResizeAlgorithm {
    BITMAP_SCALING,
    LANCZOS,
    STB_MITCHELL,
    STB_CUBIC_BSPLINE,
    STB_CATMULL_ROM,
    STB_BOX,
    STB_TRIANGLE,
    STB_POINT_SAMPLE
}