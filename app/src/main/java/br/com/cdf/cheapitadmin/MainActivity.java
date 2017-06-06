package br.com.cdf.cheapitadmin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    String coupon_code = "";
    EditText etCouponCode;
    Button btVerify, btClear;
    TextView tvPartnerName, tvOfferDescription, tvExpiresAt, tvStatus, tvUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCouponCode = (EditText)findViewById(R.id.etCouponCode);
        btVerify = (Button)findViewById(R.id.btVerify);
        btClear = (Button)findViewById(R.id.btClear);
        tvPartnerName = (TextView)findViewById(R.id.tvPartnerName);
        tvOfferDescription = (TextView)findViewById(R.id.tvOfferDescription);
        tvExpiresAt = (TextView)findViewById(R.id.tvExpiresAt);
        tvStatus = (TextView)findViewById(R.id.tvStatus);
        tvUserName = (TextView)findViewById(R.id.tvUserName);


        btVerify.setOnClickListener(this);
        btClear.setOnClickListener(this);


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btVerify:
                coupon_code = etCouponCode.getText().toString();
                new GetCoupon().execute("","",coupon_code);
                break;
            case R.id.btClear:
                etCouponCode.setText("");
                break;
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

                tvPartnerName.setText("Loja: "+jsonObject.getString("partner_name"));
                tvOfferDescription.setText("Descrição: "+jsonObject.getString("description"));
                tvExpiresAt.setText("Valido até: "+jsonObject.getString("expires_at"));
                // TODO: 6/5/17 RETURN STATUS AND USERNAME
                tvStatus.setText("Situação: "+jsonObject.getString("status"));
                tvUserName.setText("Usuário: "+jsonObject.getString("first_name")+" "+jsonObject.getString("last_name"));




            }catch(Exception e){
                Log.e("erro",e.getMessage());
            }
            Log.i("Offer Result",result);
        }
    }
}
