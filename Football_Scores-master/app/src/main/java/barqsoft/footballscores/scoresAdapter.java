package barqsoft.footballscores;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.*;
import android.os.Process;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.caverock.androidsvg.SVG;

import java.io.InputStream;

import barqsoft.footballscores.SVG.SvgDecoder;
import barqsoft.footballscores.SVG.SvgDrawableTranscoder;

/**
 * Created by yehya khaled on 2/26/2015.
 */
public class scoresAdapter extends CursorAdapter
{
    GenericRequestBuilder<Uri, InputStream, SVG, PictureDrawable> requestBuilder;
    Handler mHandler;
    public static final int COL_HOME = 3;
    public static final int COL_AWAY = 4;
    public static final int COL_HOME_GOALS = 6;
    public static final int COL_AWAY_GOALS = 7;
    public static final int COL_DATE = 1;
    public static final int COL_LEAGUE = 5;
    public static final int COL_MATCHDAY = 9;
    public static final int COL_ID = 8;
    public static final int COL_MATCHTIME = 2;
    public double detail_match_id = 0;
    private String FOOTBALL_SCORES_HASHTAG = "#Football_Scores";
    public scoresAdapter(Context context,Cursor cursor,int flags)
    {
        super(context,cursor,flags);
        requestBuilder = Glide.with(context)
                .using(Glide.buildStreamModelLoader(Uri.class, context), InputStream.class)
                .from(Uri.class)
                .as(SVG.class)
                .transcode(new SvgDrawableTranscoder(), PictureDrawable.class)
                .sourceEncoder(new StreamEncoder())
                .cacheDecoder(new FileToStreamDecoder<SVG>(new SvgDecoder()))
                .decoder(new SvgDecoder())
                .animate(android.R.anim.fade_in)
                .error(R.drawable.no_icon);
        HandlerThread thread = new HandlerThread("imageloader", Process.THREAD_PRIORITY_LESS_FAVORABLE);
        thread.setDaemon(true);
        thread.start();
        mHandler = new Handler(thread.getLooper());
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent)
    {
        View mItem = LayoutInflater.from(context).inflate(R.layout.scores_list_item, parent, false);
        ViewHolder mHolder = new ViewHolder(mItem);
        mItem.setTag(mHolder);
        //Log.v(FetchScoreTask.LOG_TAG,"new View inflated");
        return mItem;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor)
    {
        final ViewHolder mHolder = (ViewHolder) view.getTag();
        final String crestJson = context.getString(R.string.crestJson);
        mHolder.home_name.setText(cursor.getString(COL_HOME));
        mHolder.away_name.setText(cursor.getString(COL_AWAY));
        mHolder.date.setText(cursor.getString(COL_MATCHTIME));
        mHolder.home_crest.setImageDrawable(null);
        mHolder.away_crest.setImageDrawable(null);
        if (ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL) {
            mHolder.score.setText(Utilities.getScores(cursor.getInt(COL_AWAY_GOALS), cursor.getInt(COL_HOME_GOALS)));
        } else {
            mHolder.score.setText(Utilities.getScores(cursor.getInt(COL_HOME_GOALS), cursor.getInt(COL_AWAY_GOALS)));
        }
        if (mHolder.score.getText() == " - ") {
            mHolder.score.setContentDescription("no score");
        }
        mHolder.match_id = cursor.getDouble(COL_ID);
        int homeCrest = Utilities.getTeamCrestByTeamName(
                cursor.getString(COL_HOME));
        if (homeCrest != R.drawable.no_icon) {
            mHolder.home_crest.setImageResource(homeCrest);
        } else {
            final String homeTeam = cursor.getString(COL_HOME);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    final String url = Utilities.getTeamCrestUrl(homeTeam, crestJson);
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!"".equals(url)) {
                                Uri uri = Uri.parse(url);
                                requestBuilder
                                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                        .load(uri)
                                        .into(mHolder.home_crest);
                            } else {
                                mHolder.home_crest.setImageResource(R.drawable.no_icon);
                            }
                        }
                    });
                }
            });
        }
        int awayCrest = Utilities.getTeamCrestByTeamName(
                cursor.getString(COL_AWAY));
        if (awayCrest != R.drawable.no_icon) {
            mHolder.away_crest.setImageResource(awayCrest);
        } else {
            final String awayTeam = cursor.getString(COL_AWAY);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    final String url = Utilities.getTeamCrestUrl(awayTeam, crestJson);
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!"".equals(url)) {
                                Uri uri = Uri.parse(url);
                                requestBuilder
                                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                        .load(uri)
                                        .into(mHolder.away_crest);
                            } else{
                                mHolder.away_crest.setImageResource(R.drawable.no_icon);
                            }
                        }
                    });
                }
            });
        }
        mHolder.home_crest.setContentDescription(mHolder.home_name.getText() + " crest");
        mHolder.away_crest.setContentDescription(mHolder.away_name.getText() + " crest");
        //Log.v(FetchScoreTask.LOG_TAG,mHolder.home_name.getText() + " Vs. " + mHolder.away_name.getText() +" id " + String.valueOf(mHolder.match_id));
        //Log.v(FetchScoreTask.LOG_TAG,String.valueOf(detail_match_id));
        LayoutInflater vi = (LayoutInflater) context.getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.detail_fragment, null);
        ViewGroup container = (ViewGroup) view.findViewById(R.id.details_fragment_container);
        if(mHolder.match_id == detail_match_id)
        {
            //Log.v(FetchScoreTask.LOG_TAG,"will insert extraView");

            container.addView(v, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                    , ViewGroup.LayoutParams.MATCH_PARENT));
            TextView match_day = (TextView) v.findViewById(R.id.matchday_textview);
            match_day.setText(Utilities.getMatchDay(cursor.getInt(COL_MATCHDAY),
                    cursor.getInt(COL_LEAGUE)));
            TextView league = (TextView) v.findViewById(R.id.league_textview);
            league.setText(Utilities.getLeague(cursor.getInt(COL_LEAGUE)));
            Button share_button = (Button) v.findViewById(R.id.share_button);
            share_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //add Share Action
                    context.startActivity(createShareForecastIntent(mHolder.home_name.getText() + " "
                            + mHolder.score.getText() + " " + mHolder.away_name.getText() + " "));
                }
            });
        }
        else
        {
            container.removeAllViews();
        }

    }
    public Intent createShareForecastIntent(String ShareText) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, ShareText + FOOTBALL_SCORES_HASHTAG);
        return shareIntent;
    }

}
