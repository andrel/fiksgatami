package no.fiksgatami.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import no.fiksgatami.utils.CommonUtil;

import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;

/**
 * Button for taking picture.
 * <p/>
 * This button will not be {@code GONE} if the device does not have a camera.
 */
public class CameraButton extends Button {

    private static final String LOG_TAG = CameraButton.class.getSimpleName();

    public CameraButton(final Context context) {
        super(context);
        setVisibility(hasCamera());
    }

    public CameraButton(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        setVisibility(hasCamera());
    }

    public CameraButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setVisibility(hasCamera());
    }

    private int hasCamera() {
        boolean hasCamera = CommonUtil.isIntentAvailable(getContext(), ACTION_IMAGE_CAPTURE);
        return hasCamera ? VISIBLE : GONE;
    }

}
