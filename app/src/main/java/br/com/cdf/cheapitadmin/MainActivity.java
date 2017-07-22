package br.com.cdf.cheapitadmin;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    String coupon_code = "", coupon_id = "";
    EditText etCouponCode;
    Button btQR, btVerify, btClear, btValidate;
    TextView tvPartnerName, tvOfferDescription, tvExpiresAt, tvStatus, tvUserName;

    //qr code scanner object
    private IntentIntegrator qrScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCouponCode = (EditText)findViewById(R.id.etCouponCode);
        btQR = (Button)findViewById(R.id.btQR);
        btVerify = (Button)findViewById(R.id.btVerify);
        btClear = (Button)findViewById(R.id.btClear);
        btValidate = (Button)findViewById(R.id.btValidate);
        tvPartnerName = (TextView)findViewById(R.id.tvPartnerName);
        tvOfferDescription = (TextView)findViewById(R.id.tvOfferDescription);
        tvExpiresAt = (TextView)findViewById(R.id.tvExpiresAt);
        tvStatus = (TextView)findViewById(R.id.tvStatus);
        tvUserName = (TextView)findViewById(R.id.tvUserName);

        btQR.setOnClickListener(this);
        btVerify.setOnClickListener(this);
        btClear.setOnClickListener(this);
        btValidate.setOnClickListener(this);

        etCouponCode.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    btVerify.performClick();
                    return true;
                }
                return false;
            }
        });


        //intializing scan object
        qrScan = new IntentIntegrator(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btQR:
                qrScan.initiateScan();
                break;
            case R.id.btVerify:
                coupon_code = etCouponCode.getText().toString();
                if(!coupon_code.isEmpty())  new GetCoupon().execute("","",coupon_code);
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);

                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
                break;
            case R.id.btClear:
                etCouponCode.setText("");
                resetValues();
                break;
            // TODO: 10/06/17 MAKE DATE VALIDATION, MAKE PARTNER VALIDATION
            case R.id.btValidate:
                if(tvStatus.getText().equals("ORDERED")){
                    new ValidateCoupon().execute(coupon_id,"USED");
                }
                else {
                    Toast.makeText(this,"Esse cupom já foi utilizado ou está expirado!",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    //Getting the scan results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            //if qrcode has nothing in it
            if (result.getContents() == null) {
                Toast.makeText(this, "Código não encontrado", Toast.LENGTH_LONG).show();
            } else {
                //if qr contains data
                try {
                    //converting the data to json
                    JSONObject obj = new JSONObject(result.getContents());
                    //setting values to textviews
                    etCouponCode.setText(obj.getString("name"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    //case it is not JSON FILE
                    Toast.makeText(this, result.getContents(), Toast.LENGTH_LONG).show();
                    etCouponCode.setText(result.getContents());
                }
                btVerify.performClick();

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class GetCoupon extends AsyncTask<String,String,String> {

        public static final int CONNECTION_TIMEOUT=10000;
        public static final int READ_TIMEOUT=15000;

        ProgressDialog pdLoading = new ProgressDialog(MainActivity.this);
        HttpURLConnection conn;
        URL url = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread
            pdLoading.setMessage("\tCarregando...");
            pdLoading.setCancelable(false);
            pdLoading.show();

        }

        @Override
        protected String doInBackground(String... params) {
            try {
                // Enter URL address where your php file resides
                url = new URL(DBController.coupons_url);

            } catch (MalformedURLException e) {
                e.printStackTrace();
                return "exception";
            }
            try {
                // Setup HttpURLConnection class to send and receive data from php and mysql
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setConnectTimeout(CONNECTION_TIMEOUT);
                conn.setRequestMethod("POST");

                // setDoInput and setDoOutput method depict handling of both send and receive
                conn.setDoInput(true);
                conn.setDoOutput(true);

                // Append parameters to URL
                // Aqui se define os campos a serem consultados
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("status", params[0])
                        .appendQueryParameter("user_id", params[1])
                        .appendQueryParameter("coupon_code", params[2]);
                String query = builder.build().getEncodedQuery();
                Log.i("Coupon Query",query);


                // Open connection for sending data
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();
                conn.connect();

            } catch (IOException e1) {
                e1.printStackTrace();
                return "exception";
            }

            try {

                int response_code = conn.getResponseCode();

                // Check if successful connection made
                if (response_code == HttpURLConnection.HTTP_OK) {

                    // Read data sent from server
                    InputStream input = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    StringBuilder result = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    // Pass data to onPostExecute method
                    return (result.toString());

                } else {
                    Toast.makeText(getApplicationContext(),"connection_bad",Toast.LENGTH_SHORT).show();
                    return ("unsuccessful");
                }

            } catch (IOException e) {
                e.printStackTrace();
                return "exception";
            } finally {
                conn.disconnect();
            }


        }

        @Override
        protected void onPostExecute(String result) {

            //this method will be running on UI thread
            pdLoading.dismiss();

            Log.i("Coupon Result",result);

            String json = result;

            try {
                JSONObject jsonObj = new JSONObject(json);
                // Getting JSON Array node
                JSONArray coupon_array = jsonObj.getJSONArray("coupons_array");
                Log.i("Tamanho coupon_array", String.valueOf(coupon_array.length()));

                JSONObject jsonObject = coupon_array.getJSONObject(0);


                tvPartnerName.setText(jsonObject.getString("partner_name"));
                tvOfferDescription.setText(jsonObject.getString("description"));
                tvExpiresAt.setText(jsonObject.getString("expires_at"));
                tvStatus.setText(jsonObject.getString("status"));
                tvUserName.setText(jsonObject.getString("first_name")+" "+jsonObject.getString("last_name"));

                coupon_id = jsonObject.getString("coupon_id");


            }catch(Exception e){
                Log.e("erro",e.getMessage());
                Toast.makeText(getApplicationContext(),"Erro: Cupom não encontrado",Toast.LENGTH_SHORT).show();
                resetValues();
            }
            Log.i("Offer Result",result);
        }
    }

    private class ValidateCoupon extends AsyncTask<String,String,String> {

        public static final int CONNECTION_TIMEOUT=10000;
        public static final int READ_TIMEOUT=15000;

        ProgressDialog pdLoading = new ProgressDialog(MainActivity.this);
        HttpURLConnection conn;
        URL url = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //this method will be running on UI thread
            pdLoading.setMessage("\tCarregando...");
            pdLoading.setCancelable(false);
            pdLoading.show();

        }

        @Override
        protected String doInBackground(String... params) {
            try {
                // Enter URL address where your php file resides
                url = new URL(DBController.update_coupon_url);

            } catch (MalformedURLException e) {
                e.printStackTrace();
                return "exception";
            }
            try {
                // Setup HttpURLConnection class to send and receive data from php and mysql
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(READ_TIMEOUT);
                conn.setConnectTimeout(CONNECTION_TIMEOUT);
                conn.setRequestMethod("POST");

                // setDoInput and setDoOutput method depict handling of both send and receive
                conn.setDoInput(true);
                conn.setDoOutput(true);

                // Append parameters to URL
                // Aqui se define os campos a serem consultados
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("coupon_id", params[0])
                        .appendQueryParameter("status", params[1]);
                String query = builder.build().getEncodedQuery();
                Log.i("Validation Query",query);


                // Open connection for sending data
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();
                conn.connect();

            } catch (IOException e1) {
                e1.printStackTrace();
                return "exception";
            }

            try {

                int response_code = conn.getResponseCode();

                // Check if successful connection made
                if (response_code == HttpURLConnection.HTTP_OK) {

                    // Read data sent from server
                    InputStream input = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    StringBuilder result = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    // Pass data to onPostExecute method
                    return (result.toString());

                } else {
                    Toast.makeText(getApplicationContext(),"connection_bad",Toast.LENGTH_SHORT).show();
                    return ("unsuccessful");
                }

            } catch (IOException e) {
                e.printStackTrace();
                return "exception";
            } finally {
                conn.disconnect();
            }


        }

        @Override
        protected void onPostExecute(String result) {

            //this method will be running on UI thread
            pdLoading.dismiss();

            Log.i("Validation Result",result);

            Toast.makeText(getApplicationContext(),"O cupom foi validado com sucesso!",Toast.LENGTH_SHORT).show();

        }
    }

    public void resetValues() {
        tvPartnerName.setText("");
        tvOfferDescription.setText("");
        tvExpiresAt.setText("");
        tvStatus.setText("");
        tvUserName.setText("");

        coupon_id = "";

    }
}
