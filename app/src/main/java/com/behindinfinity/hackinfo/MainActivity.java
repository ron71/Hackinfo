package com.behindinfinity.hackinfo;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity{
    Map<Integer,String> articleTitles, articleUrls;
    ArrayList<Integer> articleIDs;
    SQLiteDatabase articleDB;
    ArrayList<String> titlesForList;
    ArrayList<String> urlList;
    ArrayAdapter<String> ad;

    // Creating a download task in background we will create a class extending 'AsyncTask' Class
    // NOTE : Any task which is going to take some time should be made using of different thread
    // other than main Thread

    public class DownloadTask extends AsyncTask<String, Void, String>{
        /*
         * AsyncTask<a,b,c>
         *     a : It is the type of object which we going to send to the class
         *       to instruct it what to do (Here we will be sending an url so we used String)
         *     b : It is the name of the method taht we will used to show the progress of this task.
         *       (Here we don't need to show, therefore we are using Void )
         *       For Example we can use a method showing progress bar to show the progress of
         *       download of a large file
         *     c : It is the type variable which will be returned by the task
         *       (Here we will be returning URL content in String)
         * */
        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url = null;
            HttpURLConnection httpURLConnection = null;
            try{
                url = new URL(urls[0]);
                httpURLConnection = (HttpURLConnection)url.openConnection();

                InputStream in = httpURLConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int c;
                while((c=reader.read())!=-1){
                    result+=String.valueOf((char)c);
                }

                // Now we get String of JSON object which can ce stored in Json Array
                JSONArray jsonArray = new JSONArray(result);

                // Now we can iterate through this array
                //Above we get almost 500 article id but we will use 20 of them.
                // It should to better to check the length or to array as some urls may contain less data

                int noOfData;
                if (jsonArray.length() > 20) {
                    noOfData = 200;
                } else {
                    noOfData = jsonArray.length();
                }

                articleDB.execSQL("DELETE FROM ARTICLE");

                for (int i = 0; i <noOfData; i++) {
             /*
                Log.i("ARTICLE ID : ", jsonArray.getString(i));
                Now we can download the contents of the each Article
                Example: https://hacker-news.firebaseio.com/v0/item/8863.json?print=pretty
                Above Url is for content of article no 8863
                We have to modify the url such that each article no is appended in between
                Now each article will run its own download thread
                */
                    String articleID = jsonArray.getString(i);
                   String urlString = "https://hacker-news.firebaseio.com/v0/item/" + articleID + ".json?print=pretty";
                    // NOw we will do same way retrieval as we did with articleId json file

                    url = new URL(urlString);
                    httpURLConnection = (HttpURLConnection)url.openConnection();

                    in = httpURLConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    c=0;
                    String articleInfo = "";
                    while((c=reader.read()) !=-1){
                        articleInfo += String.valueOf((char)c);
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                /*
                Above we got articles information in form of JSON Object
                Well we just want the title and the url for the news content
                */
                    String articleTitle = jsonObject.optString("title");
                    String articleUrl = jsonObject.optString("url");

                /*
                Instead of get String its better to use optString as it will return an empty
                String if key not exists
                */
//                Log.i("\tTITLE : ", articleTitle);
//                Log.i("\tURL : ", articleUrl);

                /*
                Now we can store data in a  two types of HashMaps or SparseArray(Same as dictionary
                 in Python), one for the article title
                consisting article ID(int) and title(String)
                Same way we will store URls and articleIDs in another HashMap.
                We will also need create a ArrayList to store the Article Ids
                 */
                    articleIDs.add(Integer.valueOf(articleID));
                    articleTitles.put(Integer.valueOf(articleID),articleTitle);
                    articleUrls.put(Integer.valueOf(articleID),articleUrl);


                    /*
                     *   Now we have them in the Data Structures so we can keep them in SQLDatabase
                     *   Now we will insert each item in table
                     *
                     */

                    // q = "INSERT INTO article (articleID, url, title)VALUES(" + articleID + ",'" +articleUrl + "','" + articleTitle + "')";
                    /*
                     *   Above written insert command is not good to use as we , dont know what would be the values of the variable.
                     *   For Example: if title conatins something like "I don't do it"
                     *   And also prevents from SQL injection codes
                     *   So we can see one apostrophie s in dont which can alter the statement
                     *
                     *   So its better to use prepared statements
                     */
                    String q = "INSERT INTO article (articleID, url, title)VALUES(?,?,?);";
                    SQLiteStatement statement = articleDB.compileStatement(q);
                    // Now we will put the variables in the prepared statements
                    statement.bindString(1,articleID); // Indexing starts from 1
                    statement.bindString(2, articleUrl);
                    statement.bindString(3, articleTitle);

                    statement.execute();

                    // before insertion we must delete the contents of the table, otherwise every tinme onCreate is called same data is inserted
                    // See line No- 125
                }
                Log.i("ARTICLE IDS : ",articleIDs.toString());
                Log.i("ARTICLE TITLES : ", articleTitles.toString());
                Log.i("ARTICLE URLS:", articleUrls.toString());



            }
            catch (Exception e){
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            // This method is called when the Thread completes its execution
            updateList();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView lv = findViewById(R.id._dynamicList);


        articleIDs = new ArrayList<Integer>();
        articleTitles = new HashMap<Integer, String>();
        articleUrls = new HashMap<Integer, String>();
        titlesForList = new ArrayList<String>();
        urlList = new ArrayList<String>();


        /*
         *      Now we can insert a list of Title on the Screen
         */

        ad = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,titlesForList);
        lv.setAdapter(ad);

        // Creating A Database

        articleDB = openOrCreateDatabase("ArticlesDB",0,null);
        // Creating Table
        String q = "CREATE TABLE IF NOT EXISTS article(" +
                "id INTEGER PRIMARY KEY,"+
                "articleID INTEGER,"+
                "url VARCHAR,"+
                "title VARCHAR,"+
                "content VARCHAR"+
                ");";


        try {
            articleDB.execSQL(q);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        updateList();       // Populating the List view





        String url = "https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty";
        // These URLS are from : https://github.com/HackerNews/API
        try {
            // don't forget to provide uses permissions
            DownloadTask task = new DownloadTask();
            task.execute(url);
            //  try to avoid task.execute(url).get() method, This will make the AsyncTask Thread to
            // run in the Main UI thread, which can cause time of loading issue


            //Following code are copied to DownloadTask class, because DownloadTask which is a Thread
//            was itself running in main UI thread Because of wihch the list was not populating until this async thread is complete


//            // Now we get String of JSON object which can ce stored in Json Array
//            JSONArray jsonArray = new JSONArray(result);
//
//            // Now we can iterate through this array
//            //Above we get almost 500 article id but we will use 20 of them.
//            // It should to better to check the length or to array as some urls may contain less data
//
//            int noOfData;
//            if (jsonArray.length() > 20) {
//                noOfData = 30;
//            } else {
//                noOfData = jsonArray.length();
//            }
//
//            articleDB.execSQL("DELETE FROM ARTICLE");
//
//            for (int i = 0; i <noOfData; i++) {
//             /*
//                Log.i("ARTICLE ID : ", jsonArray.getString(i));
//                Now we can download the contents of the each Article
//                Example: https://hacker-news.firebaseio.com/v0/item/8863.json?print=pretty
//                Above Url is for content of article no 8863
//                We have to modify the url such that each article no is appended in between
//                Now each article will run its own download thread
//                */
//                String articleID = jsonArray.getString(i);
//                url = "https://hacker-news.firebaseio.com/v0/item/" + articleID + ".json?print=pretty";
//
//                String articleInfo = new DownloadTask().execute(url).get();
//                JSONObject jsonObject = new JSONObject(articleInfo);
//
//                /*
//                Above we got articles information in form of JSON Object
//                Well we just want the title and the url for the news content
//                */
//                String articleTitle = jsonObject.optString("title");
//                String articleUrl = jsonObject.optString("url");
//
//                /*
//                Instead of get String its better to use optString as it will return an empty
//                String if key not exists
//                */
////                Log.i("\tTITLE : ", articleTitle);
////                Log.i("\tURL : ", articleUrl);
//
//                /*
//                Now we can store data in a  two types of HashMaps or SparseArray(Same as dictionary
//                 in Python), one for the article title
//                consisting article ID(int) and title(String)
//                Same way we will store URls and articleIDs in another HashMap.
//                We will also need create a ArrayList to store the Article Ids
//                 */
//                articleIDs.add(Integer.valueOf(articleID));
//                articleTitles.put(Integer.valueOf(articleID),articleTitle);
//                articleUrls.put(Integer.valueOf(articleID),articleUrl);
//
//
//                /*
//                 *   Now we have them in the Data Structures so we can keep them in SQLDatabase
//                 *   Now we will insert each item in table
//                 *
//                 */
//
//                // q = "INSERT INTO article (articleID, url, title)VALUES(" + articleID + ",'" +articleUrl + "','" + articleTitle + "')";
//                /*
//                *   Above written insert command is not good to use as we , dont know what would be the values of the variable.
//                *   For Example: if title conatins something like "I don't do it"
//                *   And also prevents from SQL injection codes
//                *   So we can see one apostrophie s in dont which can alter the statement
//                *
//                *   So its better to use prepared statements
//                 */
//                q = "INSERT INTO article (articleID, url, title)VALUES(?,?,?);";
//                SQLiteStatement statement = articleDB.compileStatement(q);
//                // Now we will put the variables in the prepared statements
//                statement.bindString(1,articleID); // Indexing starts from 1
//                statement.bindString(2, articleUrl);
//                statement.bindString(3, articleTitle);
//
//                statement.execute();
//
//                // before insertion we must delete the contents of the table, otherwise every tinme onCreate is called same data is inserted
//                // See line No- 125
//            }
//            Log.i("ARTICLE IDS : ",articleIDs.toString());
//            Log.i("ARTICLE TITLES : ", articleTitles.toString());
//            Log.i("ARTICLE URLS:", articleUrls.toString());
//

            /*
            *   To check that its stored or not using Projection Queries.
            *   Now we have to populate the arrayForList variable ArrayList
             */

        }
        catch (Exception e){
            e.printStackTrace();
        }


        // Creating list event handlers
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String urlToBeSentToWebview = urlList.get(i);

                Intent intent = new Intent(MainActivity.this, WebViewActivity.class);

                intent.putExtra("url",urlToBeSentToWebview);
                startActivity(intent);
            }
        });
    }

    public void updateList(){

        try {
            String q = "SELECT * FROM article ORDER BY articleID DESC;";

            Cursor c = articleDB.rawQuery(q, null);
            int articleIndex = c.getColumnIndex("articleID");
            int titleIndex = c.getColumnIndex("title");
            int urlIndex = c.getColumnIndex("url");

            titlesForList.clear();
            urlList.clear();
            // Clearing the list so date no redundacy is created on next onCreate call
            while (c.moveToNext()) {
                int id = c.getInt(articleIndex);
                String title = c.getString(titleIndex);
                String urL = c.getString(urlIndex);
                titlesForList.add(title);
                urlList.add(urL);
                ad.notifyDataSetChanged();              // Imp: it notifies the change to the adpter and list is populated automatically one by one

                Log.i("ID : ", String.valueOf(id));
                Log.i("\tTITLE : ", title);
                Log.i("\tURL : ", urL);

            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}
