package com.yalantis.ucrop.model;


import com.yalantis.ucrop.R;

public enum ASPECT_RATIO {

    RATIO_ORIGINAL(0f, 0f, R.drawable.ic_aspect_ratio_original),
    RATIO_1_1(1f, 1f, R.drawable.ic_aspect_ratio_1_1),
    RATIO_3_4(3, 4, R.drawable.ic_aspect_ratio_3_4),
    RATIO_4_3(4, 3, R.drawable.ic_aspect_ratio_4_3),
    RATIO_2_3(2, 3, R.drawable.ic_aspect_ratio_2_3),
    RATIO_3_2(3, 2, R.drawable.ic_aspect_ratio_3_2),
    RATIO_9_16(9, 16, R.drawable.ic_aspect_ratio_9_16),
    RATIO_16_9(16, 9, R.drawable.ic_aspect_ratio_16_9);

    private final float x;
    private final float y;
    private final int drawable;

    ASPECT_RATIO(float x, float y, int drawable) {
        this.x = x;
        this.y = y;
        this.drawable = drawable;

    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getDrawable() {
        return drawable;
    }

    public static int getDrawableByWidthHeight(float width, float height) {
        for (ASPECT_RATIO aspectRatio : values()) {
            if (aspectRatio.x == width && aspectRatio.y == height) {
                return aspectRatio.drawable;
            }
        }
        return R.drawable.ic_aspect_ratio_original;
    }


}
