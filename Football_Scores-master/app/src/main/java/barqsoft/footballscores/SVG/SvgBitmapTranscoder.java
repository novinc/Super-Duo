package barqsoft.footballscores.SVG;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;

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
        Bitmap bitmap = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawPicture(picture);
        return new SimpleResource<Bitmap>(bitmap);
    }

    @Override
    public String getId() {
        return "";
    }
}
