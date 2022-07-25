package com.example.jsontest04;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "getimagejson";
    private String REQUEST_URL = "http://192.168.121.3:8080/lifesupporter/api/did/test";
    private String ENDPOINT = "http://192.168.121.3:8080/lifesupporter/";
    public static final int LOAD_SUCCESS = 101;

    private ProgressDialog progressDialog;
    private TextView textviewJSONText;
    private Button buttonRequestJSON;
    private ImageView imageView;

    public static ArrayList imageList = new ArrayList();
    public static ArrayList ttsText = new ArrayList();

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tts = new TextToSpeech(this, this);

        buttonRequestJSON = findViewById(R.id.button_main_requestjson);
        textviewJSONText = findViewById(R.id.textview_main_jsontext);
        textviewJSONText.setMovementMethod(new ScrollingMovementMethod());
        imageView = findViewById(R.id.iv);

        buttonRequestJSON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                progressDialog = new ProgressDialog( MainActivity.this );
                progressDialog.setMessage("Please wait.....");
                progressDialog.show();

                GetJson getJson = new GetJson();
                PlayImage playImage = new PlayImage();
                getJson.start();
                try {
                    getJson.join();

                    playImage.start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }



            }
        });

    }

    private final MyHandler mHandler = new MyHandler(this);

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> weakReference;

        public MyHandler(MainActivity mainactivity) {
            weakReference = new WeakReference<MainActivity>(mainactivity);
        }

        @Override
        public void handleMessage(Message msg) {

            MainActivity mainactivity = weakReference.get();


            if (mainactivity != null) {
                switch (msg.what) {

                    case LOAD_SUCCESS:
                        mainactivity.progressDialog.dismiss();

                        String jsonString = (String)msg.obj;

                        mainactivity.textviewJSONText.setText(jsonString);

                        break;
                }
            }
        }
    }

    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS){
            int result = tts.setLanguage(Locale.KOREA); // TTS언어 한국어로 설정

            if(result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA){
                Log.e("TTS", "This Language is not supported");
            }
        }else{
            Log.e("TTS", "Initialization Failed!");
        }
    }

    // 텍스트를 음성으로 읽어주는 메소드 정의
    private void speakOut(int i){
        CharSequence text = (CharSequence) ttsText.get(i);
        tts.setPitch(1.0f); // 음성 톤 높이 지정
        tts.setSpeechRate(1.0f); // 음성 속도 지정

        // 첫 번째 매개변수: 음성 출력을 할 텍스트
        // 두 번째 매개변수: 1. TextToSpeech.QUEUE_FLUSH - 진행중인 음성 출력을 끊고 이번 TTS의 음성 출력
        //                 2. TextToSpeech.QUEUE_ADD - 진행중인 음성 출력이 끝난 후에 이번 TTS의 음성 출력
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "id1");
    }



    // imageList 뿌려주는 스레드드
    class PlayImage extends Thread {
        @Override
        public void run() {
            for (int i = 0; i < imageList.size(); i++) {
                int j = i;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(MainActivity.this)
                                .load(imageList.get(j))
                                .into(imageView);
                        speakOut(j);
                    }
                });
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


        }
    }

    // JSON정보 받아오는 스레드
    public class GetJson extends Thread {
        @Override
        public void run() {
            String result;

            try {
                Log.d(TAG, REQUEST_URL);
                URL url = new URL(REQUEST_URL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(3000);
                httpURLConnection.setConnectTimeout(3000);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setUseCaches(false);
                httpURLConnection.connect();

                int responseStatusCode = httpURLConnection.getResponseCode();

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {

                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();

                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }

                bufferedReader.close();
                httpURLConnection.disconnect();



                result = sb.toString().trim();

                jsonParser(result);


            } catch (Exception e){
                result = e.toString();
            }

            Message message = mHandler.obtainMessage(LOAD_SUCCESS, result);
            mHandler.sendMessage(message);

        }
    }

//    // JSON파싱하는 메소드
//    public void jsonParser(String jsonString) {
//
//        try {
//            // JSON 파싱
//            JSONObject jsonObject = new JSONObject(jsonString);
//            String images = jsonObject.getString("images");
//            JSONArray jsonArray = new JSONArray(images);
//
//            imageList.clear();
//
//            for (int i = 0; i < jsonArray.length(); i++){
//                try {
//                    Log.d(TAG, "jsonArray(" + i + ") = " + jsonArray.getString(i));
//                    imageList.add(ENDPOINT + jsonArray.getString(i));
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//            Log.d(TAG, "" + imageList);
//        } catch (JSONException e){
//            e.printStackTrace();
//        }
//
//    }

    // JSON파싱하는 메소드
    public void jsonParser(String jsonString) {

        try {
            // JSON 파싱
            JSONObject jsonObject = new JSONObject(jsonString);
            String images = jsonObject.getString("images");
            JSONArray jsonArray = new JSONArray(images);

            imageList.clear();
            ttsText.clear();

            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObject = new JSONObject(jsonArray.get(i).toString());
                imageList.add(ENDPOINT + jsonObject.getString("image"));
                ttsText.add(jsonObject.getString("text"));
            }

            Log.d(TAG, "imageList : " + imageList);
            Log.d(TAG, "ttsText : " + ttsText);


        } catch (JSONException e){
            e.printStackTrace();
        }

    }
}