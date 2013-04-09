package no.fiksgatami.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Roy Sindre Norangshol <roy.sindre 'at' norangshol.no>
 */
public class CommonUtil {
    private static String LOG_TAG = CommonUtil.class.getSimpleName();

    public static boolean isStringNullOrEmpty(String string) {
        return !(string!=null && !string.trim().equalsIgnoreCase(""));
    }

    private static Bitmap createThumbnail(final File photo, final int width) {
        byte[] result;
        try {
            FileInputStream fis = new FileInputStream(photo.getAbsoluteFile());
            Bitmap bm = BitmapFactory.decodeStream(fis);

            bm = Bitmap.createScaledBitmap(bm, width, width, false);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
            result = baos.toByteArray();
        } catch (IOException e) {
            Log.w(LOG_TAG, e.getLocalizedMessage(), e);
            return null;
        }
        return BitmapFactory.decodeByteArray(result, 0, result.length);
    }

    // TODO Lindhjem: Investigate what with thumbnail when phone enters/leaves portrait mode.
    public static void updateImage(final ImageView imagePreview, final String photouri, final int widthPixels, final Drawable default_image) {
        if (default_image == null) {
            throw new IllegalArgumentException("Param default_image cannot be null");
        }

        File photo = photouri == null ? null : new File(photouri);

        boolean set = false;
        if (photo != null && photo.canRead()) {
            Bitmap bm = CommonUtil.createThumbnail(photo, widthPixels);
            if (bm != null) {
                imagePreview.setImageBitmap(bm);
                set = true;
            }
        }
        if (!set) {
            imagePreview.setImageDrawable(default_image);
        }
    }
}
