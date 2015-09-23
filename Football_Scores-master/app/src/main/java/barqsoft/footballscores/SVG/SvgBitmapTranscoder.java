package barqsoft.footballscores.SVG;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.Rect;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.caverock.androidsvg.SVG;

/**
 * Created by ncnov on 9/20/2015.
 */
public class SvgBitmapTranscoder implements ResourceTranscoder<SVG, Bitmap> {
    @Override
    public Resource<Bitmap> transcode(Resource<SVG> toTranscode) {
        SVG svg = toTranscode.get();
        Picture picture = svg.renderToPicture();
        Bitmap bitmap;
        int picWidth = picture.getWidth();
        int picHeight = picture.getHeight();
        if (picHeight > picWidth) {
            bitmap = Bitmap.createBitmap(picture.getHeight(), picture.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawRGB(255, 255, 255);
            int left = (bitmap.getWidth() - picWidth) / 2;
            int right = left + picWidth;
            canvas.drawPicture(picture, new Rect(left, 0, right, picHeight));
        } else if (picHeight < picWidth) {
            bitmap = Bitmap.createBitmap(picture.getWidth(), picture.getWidth(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawRGB(255, 255, 255);
            int top = (bitmap.getHeight() - picHeight) / 2;
            int bottom = top + picHeight;
            canvas.drawPicture(picture, new Rect(0, top, picWidth, bottom));
        } else {
            bitmap = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawRGB(255, 255, 255);
            canvas.drawPicture(picture);
        }
        return new SimpleResource<Bitmap>(bitmap);
    }

    @Override
    public String getId() {
        return "";
    }
}
