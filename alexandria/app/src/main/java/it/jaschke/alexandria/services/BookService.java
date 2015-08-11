package it.jaschke.alexandria.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import it.jaschke.alexandria.MainActivity;
import it.jaschke.alexandria.R;
import it.jaschke.alexandria.data.AlexandriaContract;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class BookService extends IntentService {

    private final String LOG_TAG = BookService.class.getSimpleName();

    public static final String GET_BOOK = "it.jaschke.alexandria.services.action.GET_BOOK";
    public static final String SHOW_BOOK = "it.jaschke.alexandria.services.action.SHOW_BOOK";
    public static final String SHOW_AUTHORS = "it.jaschke.alexandria.services.action.SHOW_AUTHORS";
    public static final String SHOW_CATEGORIES = "it.jaschke.alexandria.services.action.SHOW_CATEGORIES";
    public static final String FETCH_BOOK = "it.jaschke.alexandria.services.action.FETCH_BOOK";
    public static final String DELETE_BOOK = "it.jaschke.alexandria.services.action.DELETE_BOOK";

    public static final String EAN = "it.jaschke.alexandria.services.extra.EAN";

    Handler mHandler;

    public BookService() {
        super("Alexandria");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (GET_BOOK.equals(action)) {
                final String ean = intent.getStringExtra(EAN);
                getBook(ean);
            } else if (FETCH_BOOK.equals(action)) {
                final String ean = intent.getStringExtra(EAN);
                fetchBook(ean);
            } else if (DELETE_BOOK.equals(action)) {
                final String ean = intent.getStringExtra(EAN);
                deleteBook(ean);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void deleteBook(String ean) {
        if(ean!=null) {
            getContentResolver().delete(AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)), null, null);
        }
    }

    /**
     * Find a book and use a broadcaster to send it to the main thread
     * @param ean The isbn of the book to search
     */
    private void getBook(String ean) {
        if(ean.length()!=13){
            return;
        }

        Cursor bookEntry = getContentResolver().query(
                AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        if(bookEntry.getCount()>0){
            bookEntry.close();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.duplicate_book_msg, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        bookEntry.close();

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String bookJsonString = null;

        try {
            final String FORECAST_BASE_URL = "https://www.googleapis.com/books/v1/volumes?";
            final String QUERY_PARAM = "q";

            final String ISBN_PARAM = "isbn:" + ean;

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, ISBN_PARAM)
                    .build();

            URL url = new URL(builtUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
            }

            if (buffer.length() == 0) {
                return;
            }
            bookJsonString = buffer.toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error ", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }

        }

        final String ITEMS = "items";

        final String VOLUME_INFO = "volumeInfo";

        final String TITLE = "title";
        final String SUBTITLE = "subtitle";
        final String AUTHORS = "authors";
        final String DESC = "description";
        final String CATEGORIES = "categories";
        final String IMG_URL_PATH = "imageLinks";
        final String IMG_URL = "thumbnail";

        try {
            JSONObject bookJson = null;
            try {
                bookJson = new JSONObject(bookJsonString);
            } catch (NullPointerException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), R.string.no_connection_msg, Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            JSONArray bookArray;
            if(bookJson.has(ITEMS)){
                bookArray = bookJson.getJSONArray(ITEMS);
            }else{
                Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
                messageIntent.putExtra(MainActivity.MESSAGE_KEY,getResources().getString(R.string.not_found));
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
                return;
            }

            JSONObject bookInfo = ((JSONObject) bookArray.get(0)).getJSONObject(VOLUME_INFO);

            String title = bookInfo.getString(TITLE);

            String subtitle = "";
            if(bookInfo.has(SUBTITLE)) {
                subtitle = bookInfo.getString(SUBTITLE);
            }

            String desc="";
            if(bookInfo.has(DESC)){
                desc = bookInfo.getString(DESC);
            }

            String imgUrl = "";
            if(bookInfo.has(IMG_URL_PATH) && bookInfo.getJSONObject(IMG_URL_PATH).has(IMG_URL)) {
                imgUrl = bookInfo.getJSONObject(IMG_URL_PATH).getString(IMG_URL);
            }

            writeBackBook(ean, title, subtitle, desc, imgUrl, false);

            if(bookInfo.has(AUTHORS)) {
                writeBackAuthors(ean, bookInfo.getJSONArray(AUTHORS), false);
            }
            if(bookInfo.has(CATEGORIES)){
                writeBackCategories(ean,bookInfo.getJSONArray(CATEGORIES), false);
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error ", e);
        }
    }

    /**
     * Handle action fetchBook in the provided background thread with the provided
     * parameters.
     */
    private void fetchBook(String ean) {

        if(ean.length()!=13){
            return;
        }

        Cursor bookEntry = getContentResolver().query(
                AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        if(bookEntry.getCount()>0){
            bookEntry.close();
            return;
        }

        bookEntry.close();

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String bookJsonString = null;

        try {
            final String FORECAST_BASE_URL = "https://www.googleapis.com/books/v1/volumes?";
            final String QUERY_PARAM = "q";

            final String ISBN_PARAM = "isbn:" + ean;

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, ISBN_PARAM)
                    .build();

            URL url = new URL(builtUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
            }

            if (buffer.length() == 0) {
                return;
            }
            bookJsonString = buffer.toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error ", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }

        }

        final String ITEMS = "items";

        final String VOLUME_INFO = "volumeInfo";

        final String TITLE = "title";
        final String SUBTITLE = "subtitle";
        final String AUTHORS = "authors";
        final String DESC = "description";
        final String CATEGORIES = "categories";
        final String IMG_URL_PATH = "imageLinks";
        final String IMG_URL = "thumbnail";

        try {
            JSONObject bookJson = null;
            try {
                bookJson = new JSONObject(bookJsonString);
            } catch (NullPointerException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), R.string.no_connection_msg, Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            JSONArray bookArray;
            if(bookJson.has(ITEMS)){
                bookArray = bookJson.getJSONArray(ITEMS);
            }else{
                Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
                messageIntent.putExtra(MainActivity.MESSAGE_KEY,getResources().getString(R.string.not_found));
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
                return;
            }

            JSONObject bookInfo = ((JSONObject) bookArray.get(0)).getJSONObject(VOLUME_INFO);

            String title = bookInfo.getString(TITLE);

            String subtitle = "";
            if(bookInfo.has(SUBTITLE)) {
                subtitle = bookInfo.getString(SUBTITLE);
            }

            String desc="";
            if(bookInfo.has(DESC)){
                desc = bookInfo.getString(DESC);
            }

            String imgUrl = "";
            if(bookInfo.has(IMG_URL_PATH) && bookInfo.getJSONObject(IMG_URL_PATH).has(IMG_URL)) {
                imgUrl = bookInfo.getJSONObject(IMG_URL_PATH).getString(IMG_URL);
            }

            writeBackBook(ean, title, subtitle, desc, imgUrl, true);

            if(bookInfo.has(AUTHORS)) {
                writeBackAuthors(ean, bookInfo.getJSONArray(AUTHORS), true);
            }
            if(bookInfo.has(CATEGORIES)){
                writeBackCategories(ean,bookInfo.getJSONArray(CATEGORIES), true);
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error ", e);
        }
    }

    private void writeBackBook(String ean, String title, String subtitle, String desc, String imgUrl, boolean add) {
        if (add) {
            ContentValues values= new ContentValues();
            values.put(AlexandriaContract.BookEntry._ID, ean);
            values.put(AlexandriaContract.BookEntry.TITLE, title);
            values.put(AlexandriaContract.BookEntry.IMAGE_URL, imgUrl);
            values.put(AlexandriaContract.BookEntry.SUBTITLE, subtitle);
            values.put(AlexandriaContract.BookEntry.DESC, desc);
            getContentResolver().insert(AlexandriaContract.BookEntry.CONTENT_URI,values);
        } else {
            Intent intent = new Intent(SHOW_BOOK);
            intent.putExtra(AlexandriaContract.BookEntry._ID, ean);
            intent.putExtra(AlexandriaContract.BookEntry.TITLE, title);
            intent.putExtra(AlexandriaContract.BookEntry.IMAGE_URL, imgUrl);
            intent.putExtra(AlexandriaContract.BookEntry.SUBTITLE, subtitle);
            intent.putExtra(AlexandriaContract.BookEntry.DESC, desc);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            Log.v(LOG_TAG, "sending broadcast");
        }
    }

    private void writeBackAuthors(String ean, JSONArray jsonArray, boolean add) throws JSONException {
        if (add) {
            ContentValues values= new ContentValues();
            for (int i = 0; i < jsonArray.length(); i++) {
                values.put(AlexandriaContract.AuthorEntry._ID, ean);
                values.put(AlexandriaContract.AuthorEntry.AUTHOR, jsonArray.getString(i));
                getContentResolver().insert(AlexandriaContract.AuthorEntry.CONTENT_URI, values);
                values= new ContentValues();
            }
        } else {
            Intent intent = new Intent(SHOW_AUTHORS);
            ArrayList<String> authors = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                authors.add(jsonArray.getString(i));
            }
            intent.putStringArrayListExtra(AlexandriaContract.AuthorEntry.AUTHOR, authors);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }

    private void writeBackCategories(String ean, JSONArray jsonArray, boolean add) throws JSONException {
        if (add) {
            ContentValues values= new ContentValues();
            for (int i = 0; i < jsonArray.length(); i++) {
                values.put(AlexandriaContract.CategoryEntry._ID, ean);
                values.put(AlexandriaContract.CategoryEntry.CATEGORY, jsonArray.getString(i));
                getContentResolver().insert(AlexandriaContract.CategoryEntry.CONTENT_URI, values);
                values= new ContentValues();
            }
        } else {
            Intent intent = new Intent(SHOW_CATEGORIES);
            ArrayList<String> categories = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                categories.add(jsonArray.getString(i));
            }
            intent.putStringArrayListExtra(AlexandriaContract.CategoryEntry.CATEGORY, categories);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }
 }