package barqsoft.footballscores;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.Rect;
import android.util.Log;

import com.caverock.androidsvg.SVG;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by yehya khaled on 3/3/2015.
 */
public class Utilities
{
    public static final int BUNDESLIGA1 = 394;
    public static final int BUNDESLIGA2 = 395;
    public static final int LIGUE1 = 396;
    public static final int LIGUE2 = 397;
    public static final int PREMIER_LEAGUE = 398;
    public static final int PRIMERA_DIVISION = 399;
    public static final int SEGUNDA_DIVISION = 400;
    public static final int SERIE_A = 401;
    public static final int PRIMEIRA_LIGA = 402;
    public static final int BUNDESLIGA3 = 403;
    public static final int EREDIVISIE = 404;
    public static final int CHAMPIONS_LEAGUE = 405;

    public static String getLeague(Context context, int league_num)
    {
        switch (league_num)
        {
            case BUNDESLIGA1 : return context.getString(R.string.Bundesliga1);
            case BUNDESLIGA2 : return context.getString(R.string.Bundesliga2);
            case LIGUE1 : return context.getString(R.string.Ligue1);
            case LIGUE2 : return context.getString(R.string.Ligue2);
            case PREMIER_LEAGUE : return context.getString(R.string.PremierLeague);
            case PRIMERA_DIVISION : return context.getString(R.string.PrimeraDivision);
            case SEGUNDA_DIVISION : return context.getString(R.string.SegundaDivision);
            case SERIE_A : return context.getString(R.string.SerieA);
            case PRIMEIRA_LIGA : return context.getString(R.string.PrimeiraLiga);
            case BUNDESLIGA3 : return context.getString(R.string.Bundesliga3);
            case EREDIVISIE : return context.getString(R.string.Eredivisie);
            case CHAMPIONS_LEAGUE : return context.getString(R.string.UEFAChampionsLeague);
            default: return context.getString(R.string.not_known_league);
        }
    }
    public static String getMatchDay(Context context, int match_day, int league_num)
    {
        if(league_num == CHAMPIONS_LEAGUE)
        {
            if (match_day <= 6)
            {
                return context.getString(R.string.matchday_GSM6);
            }
            else if(match_day == 7 || match_day == 8)
            {
                return context.getString(R.string.matchday_FKR);
            }
            else if(match_day == 9 || match_day == 10)
            {
                return context.getString(R.string.matchday_QF);
            }
            else if(match_day == 11 || match_day == 12)
            {
                return context.getString(R.string.matchday_SF);
            }
            else
            {
                return context.getString(R.string.matchday_F);
            }
        }
        else
        {
            return context.getString(R.string.matchday_MD) + String.valueOf(match_day);
        }
    }

    public static String getScores(int home_goals,int awaygoals)
    {
        if(home_goals < 0 || awaygoals < 0)
        {
            return " - ";
        }
        else
        {
            return String.valueOf(home_goals) + " - " + String.valueOf(awaygoals);
        }
    }

    public static int getTeamCrestByTeamName (String teamname)
    {
        if (teamname==null){return R.drawable.no_icon;}
        switch (teamname)
        {
            case "Arsenal London FC" : return R.drawable.arsenal;
            case "Manchester United FC" : return R.drawable.manchester_united;
            case "Swansea City" : return R.drawable.swansea_city_afc;
            case "Leicester City" : return R.drawable.leicester_city_fc_hd_logo;
            case "Everton FC" : return R.drawable.everton_fc_logo1;
            case "West Ham United FC" : return R.drawable.west_ham;
            case "Tottenham Hotspur FC" : return R.drawable.tottenham_hotspur;
            case "West Bromwich Albion" : return R.drawable.west_bromwich_albion_hd_logo;
            case "Sunderland AFC" : return R.drawable.sunderland;
            case "Stoke City FC" : return R.drawable.stoke_city;
            default: return R.drawable.no_icon;
        }
    }
    public static String getTeamCrestUrl(String team, String crestsJson) {
        String url = "";
        try {
            JSONObject object = new JSONObject(crestsJson);
            team = team.replace(" ", "");
            team = team.replace(".", "");
            url = object.getString(team);
        } catch (JSONException e) {
            Log.v("Utilities", "No image for " + team);
        }
        return url;
    }
}
