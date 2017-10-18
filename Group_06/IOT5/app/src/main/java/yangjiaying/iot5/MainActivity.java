// reference: http://stackandroid.com/tutorial/android-speech-to-text-tutorial/


package yangjiaying.iot5;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.net.ssl.HttpsURLConnection;

import java.text.DateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {


    protected static final int RESULT_OUTPUT = 1;

    // parameters created in the layout, button and txtboxes; and responses sent back
    private ImageButton ButtonforSpeak;
    private TextView output;
    private Button buttonSend;
    String STT_output = new String();
    String message_response = new String();
    int response_Code;    
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // link the parameters to the layout
        output = (TextView) findViewById(R.id.output);
        ButtonforSpeak = (ImageButton) findViewById(R.id.ButtonforSpeak);
        buttonSend = (Button) findViewById(R.id.buttonSend);
        ButtonforSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent intent = new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

                try {
                    startActivityForResult(intent, RESULT_OUTPUT);
                    output.setText("");
                } catch (ActivityNotFoundException a) {
                    Toast t = Toast.makeText(getApplicationContext(),
                            "Oh! This is not a supported device, please try another",
                            Toast.LENGTH_SHORT);
                    t.show();
                }
            }
        });

        // when pressed the button, set the output box message
        View.OnClickListener oclbuttonSend = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Change text of TextView (output)
                new SendPostRequest().execute();
                output.setText("message sent");
            }
        };

        buttonSend.setOnClickListener(oclbuttonSend);
        
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    // send the request to control the oled
    public class SendPostRequest extends AsyncTask<String, Void, String> {

        protected void onPreExecute(){}

        protected String doInBackground(String... arg0) {

            try{
                URL url = new URL("http://129.236.208.177");  // set the url to the board's current url
                JSONObject postDataParams = new JSONObject();

                // 4 different conditions
                if(STT_output.compareTo("on display")==0)     //turn on the display
                {
                    postDataParams.put("OnDisplay","");
                }
                else if(STT_output.compareTo("off display")==0)     //turn off the display
                {
                    postDataParams.put("OffDisplay","");
                }
                else if(STT_output.compareTo("display time")==0)     //turn on the display and display the time
                {
                    String currentDateTimeString = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date());
                    //change the currentDateTime into string format
                    currentDateTimeString=currentDateTimeString.replace("/","");
                    currentDateTimeString=currentDateTimeString.replace(":","");
                    currentDateTimeString=currentDateTimeString.replace(" ","");
                    postDataParams.put("DisplayTime",currentDateTimeString);
                }
                else if(STT_output.indexOf("show message")!=-1)     //turn on the display and show the message
                {
                    String delimiter = "show message ";
                    String STT_output2;
                    STT_output2=STT_output.split(delimiter)[1];
                    postDataParams.put("message", STT_output2.toUpperCase());
                }

                Log.e("params",postDataParams.toString());

                //set the connection
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(5000 /* milliseconds */);
                conn.setConnectTimeout(5000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(postDataParams));

                writer.flush();
                writer.close();
                os.close();

                //get the response message
                message_response = conn.getResponseMessage();

                int responseCode=conn.getResponseCode();
                response_Code=responseCode;

                if (responseCode == HttpsURLConnection.HTTP_OK) {

                    // the input message from the board
                    BufferedReader in=new BufferedReader(new
                            InputStreamReader(
                            conn.getInputStream()));
                    // the error message
                    BufferedReader er=new BufferedReader(new
                            InputStreamReader(
                            conn.getErrorStream()));

                    StringBuffer sb = new StringBuffer("");
                    String line="";
                    StringBuffer sb2 = new StringBuffer("");


                    while((line = in.readLine()) != null) {
                        sb.append(line);
                        break;
                    }

                    while((line = er.readLine()) != null) {
                        sb2.append(line);
                        break;
                    }

                    in.close();
                    er.close();
                    return sb.toString();
                }
                else {
                    return new String("false : "+responseCode);
                }

            }
            catch(Exception e){
                return new String("Exception: " + e.getMessage());
            }

        }

        // set the response message and display it
        @Override
        protected void onPostExecute(String result) {
            output.setText(message_response);
        }


        public String getPostDataString(JSONObject params) throws Exception {

            StringBuilder result = new StringBuilder();
            boolean first = true;

            Iterator<String> itr = params.keys();

            while(itr.hasNext()){

                String key= itr.next();
                Object value = params.get(key);

                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(key, "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(value.toString(), "UTF-8"));

            }
            return result.toString();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //set the output string
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_OUTPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    STT_output=text.get(0);
                    output.setText(STT_output);
                }
                break;
            }

        }
    }
}
