package com.creedon.cordova.plugin.photobrowser;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.creedon.androidphotobrowser.common.views.ImageOverlayView;

/**
 * _   _ _______   ________ _       _____   __
 * | \ | |_   _\ \ / /| ___ \ |     / _ \ \ / /
 * |  \| | | |  \ V / | |_/ / |    / /_\ \ V /
 * | . ` | | |  /   \ |  __/| |    |  _  |\ /
 * | |\  |_| |_/ /^\ \| |   | |____| | | || |
 * \_| \_/\___/\/   \/\_|   \_____/\_| |_/\_/
 * <p>
 * Created by jameskong on 10/8/2017.
 */

public class CustomeImageOverlayView extends ImageOverlayView {
    public CustomeImageOverlayView(Context context) {
        super(context);
    }

    public CustomeImageOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomeImageOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View init() {
        View view = super.init();
        etDescription.setEnabled(false);
        tvDescription.setEnabled(false);
        return view;
    }
}
