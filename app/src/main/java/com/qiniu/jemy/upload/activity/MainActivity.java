package com.qiniu.jemy.upload.activity;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.qiniu.jemy.upload.utils.Config;
import com.qiniu.jemy.upload.utils.FileUtils;
import com.qiniu.jemy.upload.utils.Tools;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.AsyncRun;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import okhttp3.OkHttpClient;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mStartBtn;
    private Button mOpenFile;

    private static final int REQUEST_CODE = 8090;
    private TextView mLog;
    private EditText keyNameEdit, tokenEdit;
    String[] allpermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private String token;
    private UploadManager uploadManager;
    private String keyname;
    private long uploadFileLength;
    private String uploadFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.qiniu.jemy.upload.R.layout.activity_main);
        applypermission();
        initView();
    }

    private OkHttpClient okHttpClient;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case com.qiniu.jemy.upload.R.id.openFile:
                Intent target = FileUtils.createGetContentIntent();
                Intent intent = Intent.createChooser(target,
                        this.getString(com.qiniu.jemy.upload.R.string.choose_file));
                try {
                    this.startActivityForResult(intent, REQUEST_CODE);
                } catch (ActivityNotFoundException ex) {
                }
                break;

            case com.qiniu.jemy.upload.R.id.start:
                //get token from you server
                token = Config.UPTOKEN_Z0;
                //token = tokenEdit.getText().toString();
                upload(token);
                break;
        }
    }


    private void initView() {
        mOpenFile = findViewById(com.qiniu.jemy.upload.R.id.openFile);
        mStartBtn = findViewById(com.qiniu.jemy.upload.R.id.start);
        keyNameEdit = findViewById(com.qiniu.jemy.upload.R.id.keyname);
        tokenEdit = findViewById(com.qiniu.jemy.upload.R.id.uptoken);
        mLog = findViewById(com.qiniu.jemy.upload.R.id.log);
        mLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        mOpenFile.setOnClickListener(this);
        mStartBtn.setOnClickListener(this);
    }


    private void upload(final String uploadToken) {
        final long startTime = System.currentTimeMillis();
        //???????????????zone
        //Zone zone = new FixedZone(new String[]{"domain1","domain2"});

        //????????????????????????
        //Zone zone = FixedZone.zone0;//??????

        //??????????????????
        /**
         FileRecorder fileRecorder = null;
         try {
         fileRecorder = new FileRecorder("directory");
         } catch (IOException e) {
         e.printStackTrace();
         }
         */

        //config??????????????????
        Configuration configuration = new Configuration.Builder()
                .connectTimeout(10)
                //.zone(zone)
                //.dns(buildDefaultDns())//??????dns?????????
                .responseTimeout(60).build();

        if (this.uploadManager == null) {
            //this.uploadManager = new UploadManager(fileRecorder);
            this.uploadManager = new UploadManager(configuration);//, 3
        }

        final UploadOptions opt = new UploadOptions(null, null, true, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                Log.i("qiniutest", "percent:" + percent);
            }
        }, null);

        keyname = keyNameEdit.getText().toString();
        final File uploadFile = new File(this.uploadFilePath);
        uploadFileLength = uploadFile.length();
        long time = new Date().getTime();
        if (keyname.equals(""))
            keyname = "test_" + time;
        writeLog(this.getString(com.qiniu.jemy.upload.R.string.qiniu_upload_file) + "...");

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                ResponseInfo respInfo=uploadManager.syncPut(uploadFile,keyname,uploadToken,opt);
                long endTime = System.currentTimeMillis();
                if (respInfo.isOK()) {
                    try {
                        Log.e("zw", respInfo.response.toString() + respInfo.toString());
                        writeLog("--------------------------------UPTime/ms: " + (endTime - startTime));
                        String fileKey = respInfo.response.getString("key");
                        String fileHash = respInfo.response.getString("hash");
                        writeLog("File Size: " + Tools.formatSize(uploadFileLength));
                        writeLog("File Key: " + fileKey);
                        writeLog("File Hash: " + fileHash);
                        writeLog("X-Reqid: " + respInfo.reqId);
                        writeLog("X-Via: " + respInfo.xvia);
                        writeLog("--------------------------------" + "\n????????????");
                    } catch (JSONException e) {
                        writeLog(MainActivity.this
                                .getString(com.qiniu.jemy.upload.R.string.qiniu_upload_file_response_parse_error));
                        if (respInfo.response != null) {
                            writeLog(respInfo.response.toString());
                        }
                        writeLog("--------------------------------" + "\n????????????");
                    }
                } else {
                    writeLog(respInfo.toString());
                    if (respInfo.response != null) {
                        writeLog(respInfo.response.toString());
                    }
                    writeLog("--------------------------------" + "\n????????????");
                }
            }
        });

        /*this.uploadManager.put(uploadFile, keyname, uploadToken,
                new UpCompletionHandler() {
                    @Override
                    public void complete(String key, ResponseInfo respInfo,
                                         JSONObject jsonData) {
                        long endTime = System.currentTimeMillis();
                        if (respInfo.isOK()) {
                            try {
                                Log.e("zw", jsonData.toString() + respInfo.toString());
                                writeLog("--------------------------------UPTime/ms: " + (endTime - startTime));
                                String fileKey = jsonData.getString("key");
                                String fileHash = jsonData.getString("hash");
                                writeLog("File Size: " + Tools.formatSize(uploadFileLength));
                                writeLog("File Key: " + fileKey);
                                writeLog("File Hash: " + fileHash);
                                writeLog("X-Reqid: " + respInfo.reqId);
                                writeLog("X-Via: " + respInfo.xvia);
                                writeLog("--------------------------------" + "\n????????????");
                            } catch (JSONException e) {
                                writeLog(MainActivity.this
                                        .getString(com.qiniu.jemy.upload.R.string.qiniu_upload_file_response_parse_error));
                                if (jsonData != null) {
                                    writeLog(jsonData.toString());
                                }
                                writeLog("--------------------------------" + "\n????????????");
                            }
                        } else {
                            writeLog(respInfo.toString());
                            if (jsonData != null) {
                                writeLog(jsonData.toString());
                            }
                            writeLog("--------------------------------" + "\n????????????");
                        }
                    }

                }, opt);*/
    }

    public void applypermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            boolean needapply = false;
            for (int i = 0; i < allpermissions.length; i++) {
                int chechpermission = ContextCompat.checkSelfPermission(getApplicationContext(),
                        allpermissions[i]);
                if (chechpermission != PackageManager.PERMISSION_GRANTED) {
                    needapply = true;
                }
            }
            if (needapply) {
                ActivityCompat.requestPermissions(MainActivity.this, allpermissions, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, permissions[i] + "?????????", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, permissions[i] + "????????????", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE:
                // If the file selection was successful
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        try {
                            // Get the file path from the URI
                            final String path = FileUtils.getPath(this, uri);
                            this.uploadFilePath = path;
                            this.clearLog();
                            this.writeLog(this
                                    .getString(com.qiniu.jemy.upload.R.string.qiniu_select_upload_file)
                                    + path);
                        } catch (Exception e) {
                            Toast.makeText(
                                    this,
                                    this.getString(com.qiniu.jemy.upload.R.string.qiniu_get_upload_file_failed),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void clearLog() {
        this.mLog.setText("");
    }

    private void writeLog(final String msg) {
        AsyncRun.runInMain(new Runnable() {
            @Override
            public void run() {
                mLog.append(msg);
                mLog.append("\r\n");
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * ???????????????DNS???????????????
     *
     * @return
     */
    /*public Dns buildDefaultDns() {
        // ?????????????????? IResolver ???????????????
        ArrayList<IResolver> rs = new ArrayList<IResolver>(3);
        try {
            IResolver r1 = new Resolver(InetAddress.getByName("119.29.29.29"));
            rs.add(r1);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
//        try {
//            rs.add(new Resolver(InetAddress.getByName("114.114.114.114")));
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        try {
//            // ????????????????????????
//            // android 27 ????????? ?????????
//            IResolver r2 = AndroidDnsServer.defaultResolver();
//            rs.add(r2);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
        if (rs.size() == 0) {
            return null;
        }
        final DnsManager happlyDns = new DnsManager(NetworkInfo.normal, rs.toArray(new IResolver[rs.size()]));
        Dns dns = new Dns() {
            // ??????????????? Exception ???????????? okhttp ???????????? dns ????????????
            @Override
            public List<InetAddress> lookup(String hostname) throws UnknownHostException {
                InetAddress[] ips;
                try {
                    ips = happlyDns.queryInetAdress(new Domain(hostname));
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new UnknownHostException(e.getMessage());
                }
                if (ips == null || ips.length == 0) {
                    throw new UnknownHostException(hostname + " resolve failed.");
                }
                List<InetAddress> l = new ArrayList<>();
                Collections.addAll(l, ips);
                return l;
            }
        };
        return dns;
    }*/

}
