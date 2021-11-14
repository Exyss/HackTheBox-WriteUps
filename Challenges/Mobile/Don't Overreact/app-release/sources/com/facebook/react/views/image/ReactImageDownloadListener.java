package com.facebook.react.views.image;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ForwardingDrawable;
import javax.annotation.Nullable;

public class ReactImageDownloadListener<INFO> extends ForwardingDrawable implements ControllerListener<INFO> {
    private static final int MAX_LEVEL = 10000;

    @Override // com.facebook.drawee.controller.ControllerListener
    public void onFailure(String str, Throwable th) {
    }

    @Override // com.facebook.drawee.controller.ControllerListener
    public void onFinalImageSet(String str, @Nullable INFO info, @Nullable Animatable animatable) {
    }

    @Override // com.facebook.drawee.controller.ControllerListener
    public void onIntermediateImageFailed(String str, Throwable th) {
    }

    @Override // com.facebook.drawee.controller.ControllerListener
    public void onIntermediateImageSet(String str, @Nullable INFO info) {
    }

    public void onProgressChange(int i, int i2) {
    }

    @Override // com.facebook.drawee.controller.ControllerListener
    public void onRelease(String str) {
    }

    @Override // com.facebook.drawee.controller.ControllerListener
    public void onSubmit(String str, Object obj) {
    }

    public ReactImageDownloadListener() {
        super(new EmptyDrawable());
    }

    /* access modifiers changed from: protected */
    @Override // com.facebook.drawee.drawable.ForwardingDrawable
    public boolean onLevelChange(int i) {
        onProgressChange(i, MAX_LEVEL);
        return super.onLevelChange(i);
    }

    private static final class EmptyDrawable extends Drawable {
        public void draw(Canvas canvas) {
        }

        public int getOpacity() {
            return -1;
        }

        public void setAlpha(int i) {
        }

        public void setColorFilter(ColorFilter colorFilter) {
        }

        private EmptyDrawable() {
        }
    }
}
