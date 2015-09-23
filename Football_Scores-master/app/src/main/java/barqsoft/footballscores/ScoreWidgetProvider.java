package barqsoft.footballscores;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.caverock.androidsvg.SVG;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.SVG.SvgBitmapTranscoder;
import barqsoft.footballscores.SVG.SvgDecoder;

/**
 * Created by ncnov on 9/19/2015.
 */
public class ScoreWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for (final int id : appWidgetIds) {
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.score_widget);
            remoteViews.setOnClickPendingIntent(R.id.widget, pendingIntent);
            Date date = new Date(System.currentTimeMillis());
            SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
            String[] date2 = {mformat.format(date)};
            CursorLoader loader = new CursorLoader(context, DatabaseContract.scores_table.buildScoreWithDate(), null, null, date2, null);
            loader.registerListener(42, new Loader.OnLoadCompleteListener<Cursor>() {
                @Override
                public void onLoadComplete(Loader<Cursor> loader, Cursor data) {
                    if (data.getCount() > 0) {
                        if (!data.moveToFirst()) {
                            return;
                        }
                        while (data.getInt(scoresAdapter.COL_HOME_GOALS) != -1) {
                            if (!data.isLast()) {
                                data.moveToNext();
                            } else {
                                break;
                            }
                        }
                        String homeName = data.getString(scoresAdapter.COL_HOME);
                        String awayName = data.getString(scoresAdapter.COL_AWAY);
                        String score = Utilities.getScores(data.getInt(scoresAdapter.COL_HOME_GOALS), data.getInt(scoresAdapter.COL_AWAY_GOALS));
                        int homeCrestResource = Utilities.getTeamCrestByTeamName(homeName);
                        if (homeCrestResource == R.drawable.no_icon) {
                            final String url = Utilities.getTeamCrestUrl(homeName, context.getString(R.string.crestJson));
                            if (!"".equals(url)) {
                                Thread download = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            GenericRequestBuilder<Uri, InputStream, SVG, Bitmap> requestBuilder = Glide.with(context)
                                                    .using(Glide.buildStreamModelLoader(Uri.class, context), InputStream.class)
                                                    .from(Uri.class)
                                                    .as(SVG.class)
                                                    .transcode(new SvgBitmapTranscoder(), Bitmap.class)
                                                    .sourceEncoder(new StreamEncoder())
                                                    .cacheDecoder(new FileToStreamDecoder<SVG>(new SvgDecoder()))
                                                    .decoder(new SvgDecoder())
                                                    .animate(android.R.anim.fade_in);
                                            final Bitmap bitmap = requestBuilder
                                                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                                    .load(Uri.parse(url))
                                                    .into(50, 50)
                                                    .get();
                                            Handler handler = new Handler(Looper.getMainLooper());
                                            handler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    remoteViews.setImageViewBitmap(R.id.home_crest, bitmap);
                                                    appWidgetManager.updateAppWidget(id, remoteViews);
                                                }
                                            });
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                download.start();
                            }
                            remoteViews.setImageViewResource(R.id.home_crest, R.drawable.no_icon);
                        } else {
                            remoteViews.setImageViewResource(R.id.home_crest, homeCrestResource);
                        }
                        int awayCrestResource = Utilities.getTeamCrestByTeamName(awayName);
                        if (awayCrestResource == R.drawable.no_icon) {
                            final String url = Utilities.getTeamCrestUrl(awayName, context.getString(R.string.crestJson));
                            if (!"".equals(url)) {
                                Thread download = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            GenericRequestBuilder<Uri, InputStream, SVG, Bitmap> requestBuilder = Glide.with(context)
                                                    .using(Glide.buildStreamModelLoader(Uri.class, context), InputStream.class)
                                                    .from(Uri.class)
                                                    .as(SVG.class)
                                                    .transcode(new SvgBitmapTranscoder(), Bitmap.class)
                                                    .sourceEncoder(new StreamEncoder())
                                                    .cacheDecoder(new FileToStreamDecoder<SVG>(new SvgDecoder()))
                                                    .decoder(new SvgDecoder())
                                                    .animate(android.R.anim.fade_in);
                                            final Bitmap bitmap = requestBuilder
                                                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                                    .load(Uri.parse(url))
                                                    .into(50, 50)
                                                    .get();
                                            Handler handler = new Handler(Looper.getMainLooper());
                                            handler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    remoteViews.setImageViewBitmap(R.id.away_crest, bitmap);
                                                    appWidgetManager.updateAppWidget(id, remoteViews);
                                                }
                                            });
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                download.start();
                            }
                            remoteViews.setImageViewResource(R.id.away_crest, R.drawable.no_icon);
                        } else {
                            remoteViews.setImageViewResource(R.id.away_crest, awayCrestResource);
                        }
                        remoteViews.setTextViewText(R.id.home_name, homeName);
                        remoteViews.setTextViewText(R.id.away_name, awayName);
                        remoteViews.setTextViewText(R.id.score_textview, score);
                        appWidgetManager.updateAppWidget(id, remoteViews);
                        data.close();
                    }
                }
            });
            loader.startLoading();
        }
    }
}
