package com.wj.uikit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ap.ezviz.pub.YsApManager;
import com.ap.ezviz.pub.ap.APWiredConfigInfo;
import com.ap.ezviz.pub.ap.FIXED_IP;
import com.google.gson.Gson;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.impl.LoadingPopupView;
import com.lxj.xpopup.interfaces.OnConfirmListener;
import com.lxj.xpopup.interfaces.SimpleCallback;
import com.thanosfisherman.wifiutils.WifiUtils;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener;
import com.videogo.openapi.EZOpenSDK;
import com.videogo.openapi.bean.EZProbeDeviceInfoResult;
import com.wj.camera.callback.JsonCallback;
import com.wj.camera.net.DeviceApi;
import com.wj.camera.net.ISAPI;
import com.wj.camera.net.RxConsumer;
import com.wj.camera.net.SafeGuardInterceptor;
import com.wj.camera.response.BaseDeviceResponse;
import com.wj.camera.response.RtmpConfig;
import com.wj.camera.view.WJDeviceConfig;
import com.wj.uikit.adapter.OnItemClickListener;
import com.wj.uikit.db.DeviceInfo;
import com.wj.uikit.pop.SelectPop;
import com.wj.uikit.uitl.WJActivityControl;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * FileName: WJSettingWiredActivity
 * Author: xiongxiang
 * Date: 2021/3/23
 * Description: ????????????
 * History:
 * <author> <time> <version> <desc>
 * ???????????? ???????????? ????????? ??????
 */
public class WJSettingWiredActivity extends BaseUikitActivity {
    private LoadingPopupView mLoadingPopupView;
    private DeviceInfo mDeviceInfo;
    private View mFl_ip;
    private TextView mTv_ip;
    private View mFl_ip_config;
    private View mFl_dns_config;
    private EditText mEt_ip4;
    private EditText mEt_subnet_mask;
    private EditText mEt_default_gateway;
    private EditText mEt_dns_primary;
    private EditText mEt_dns_secondary;
    private APWiredConfigInfo.Builder mApWiredConfigInfo;
    private WifiManager mWifiMgr;
    private int mDeviceCode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wj_activity_setting_wired);
        mWifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        getData();
        registerPermission();
        initView();
        defaultIpMode(1);
    }

    private void initView() {
        mFl_ip = findViewById(R.id.fl_ip);
        mTv_ip = findViewById(R.id.tv_ip);
        mFl_ip_config = findViewById(R.id.fl_ip_config);
        mFl_dns_config = findViewById(R.id.fl_dns_config);
        mEt_ip4 = findViewById(R.id.et_ip4);
        mEt_subnet_mask = findViewById(R.id.et_subnet_mask);
        mEt_default_gateway = findViewById(R.id.et_default_gateway);
        mEt_dns_primary = findViewById(R.id.et_dns_primary);
        mEt_dns_secondary = findViewById(R.id.et_dns_secondary);
        mEt_ip4.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.i(TAG, "onFocusChange: " + hasFocus);
                if (!hasFocus) {
                    String ip = mEt_ip4.getText().toString().trim();
                    String trim = mEt_subnet_mask.getText().toString().trim();
                    if (TextUtils.isEmpty(trim)) {
                        boolean isIp = checkIp(ip);
                        if (isIp) {
                            String[] split = ip.split("\\.");
                            String s = split[0];
                            Integer integer = Integer.valueOf(s);
                            Log.i(TAG, "onFocusChange: " + integer);
                            String subnetMask = null;
                            if (integer >= 1 && integer <= 126) {
                                subnetMask = "255.0.0.0";
                            } else if (integer >= 128 && integer <= 191) {
                                subnetMask = "255.255.0.0";
                            } else if (integer >= 192 && integer <= 223) {
                                subnetMask = "255.255.255.0";
                            }
                            if (TextUtils.isEmpty(trim)) {
                                mEt_subnet_mask.setText(subnetMask);
                            }
                        }
                    }

                }

            }
        });

        findViewById(R.id.back_iv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        findViewById(R.id.tv_wired).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (registerPermission() == true) {
                    checkConfig();
                }
            }
        });

        mFl_ip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectPop selectPop = new SelectPop(WJSettingWiredActivity.this);
                selectPop.setListener(new OnItemClickListener<String>() {
                    @Override
                    public void onClick(String s, int position) {
                        if (position == 0) {
                            mFl_ip_config.setVisibility(View.GONE);
                            mFl_dns_config.setVisibility(View.GONE);
                        } else {
                            mFl_ip_config.setVisibility(View.VISIBLE);
                            mFl_dns_config.setVisibility(View.VISIBLE);
                        }
                        mTv_ip.setText(s);

                    }
                });
                new XPopup.Builder(WJSettingWiredActivity.this).asCustom(selectPop).show();
            }
        });
    }


    private void defaultIpMode(int mode) {
        if (mode == 0) {
            //????????????
            mFl_ip_config.setVisibility(View.GONE);
            mFl_dns_config.setVisibility(View.GONE);
            mTv_ip.setText("??????");
        } else if (mode == 1) {
            //????????????
            mFl_ip_config.setVisibility(View.VISIBLE);
            mFl_dns_config.setVisibility(View.VISIBLE);
            mTv_ip.setText("??????");
        }

    }

    private void checkConfig() {
        mApWiredConfigInfo = new APWiredConfigInfo.Builder();
        mApWiredConfigInfo.fixedIP(FIXED_IP.Companion.getWIRELESS_IPC_YS());
        if (!configIp4(mApWiredConfigInfo)) {
            return;
        }
        if (!configDNS(mApWiredConfigInfo)) {
            return;
        }
        boolean wifiEnabled = mWifiMgr.isWifiEnabled();
        if (wifiEnabled) {
            startwired(mApWiredConfigInfo);
        } else {
//            startActivityForResult(new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY), 200);
            startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200) {
            boolean wifiEnabled = mWifiMgr.isWifiEnabled();
            if (wifiEnabled) {
                startwired(mApWiredConfigInfo);
            }
        }
    }

    private boolean configIp4(APWiredConfigInfo.Builder apWiredConfigInfo) {
        if (mFl_ip_config.getVisibility() != View.VISIBLE) {
            //????????????
            apWiredConfigInfo.ipAddress(
                    "192.168.0.84",
                    "255.255.255.0",
                    "192.168.0.1"
            )
                    .dhcp(getString(R.string.type))
                    .fixedIP(FIXED_IP.Companion.getWIRELESS_IPC_YS());


        } else {
            String ip4 = mEt_ip4.getText().toString().trim();
            String subnetMask = mEt_subnet_mask.getText().toString().trim();
            String defaultGateway = mEt_default_gateway.getText().toString().trim();


            if (!checkIp(ip4)) {
                toast("ip????????????");
                return false;
            }
            if (!checkIp(subnetMask)) {
                toast("??????????????????");
                return false;
            }
            if (!checkIp(defaultGateway)) {
                toast("??????????????????");
                return false;
            }


            //????????????
            apWiredConfigInfo.ipAddress(
                    ip4,
                    subnetMask,
                    defaultGateway
            )
                    .dhcp(getString(R.string.type1))
                    .fixedIP(FIXED_IP.Companion.getWIRELESS_IPC_YS());
        }
        return true;
    }


    private boolean configDNS(APWiredConfigInfo.Builder apWiredConfigInfo) {
        //????????????
        if (mFl_dns_config.getVisibility() != View.VISIBLE) {
            apWiredConfigInfo.dns(
                    getString(R.string.dns_enabled),
                    "192.168.0.1",
                    "114.114.114.114"
            );
        } else {
            //????????????
            String dnsPrimary = mEt_dns_primary.getText().toString().trim();
            String dnsSecondary = mEt_dns_secondary.getText().toString().trim();

            if (!checkIp(dnsPrimary)) {
                toast("??????DNS??????");
                return false;
            }
            if (TextUtils.isEmpty(dnsSecondary)) {
                dnsSecondary="114.114.114.114";
            }else {
                if (!checkIp(dnsSecondary)) {
                    toast("??????DNS??????");
                    return false;
                }
            }

            apWiredConfigInfo.dns(getString(R.string.dns_enabled), dnsPrimary, dnsSecondary);


        }

        return true;

    }


    private boolean checkIp(String ip) {
        if (TextUtils.isEmpty(ip)) {
            return false;
        }
        String[] split = ip.split("\\.");
        if (split.length != 4) {
            return false;
        }


        for (int i = 0; i < split.length; i++) {
            Integer integer = Integer.valueOf(split[i]);
            if (integer == null) {
                return false;
            } else {

                if (i == 0 && integer == 127) {
                    return false;
                }

                if (i == 0 && integer <= 0) {
                    return false;
                }

                if (integer > 255) {
                    return false;
                }
            }
        }
        return true;
    }


    private boolean registerPermission() {
        //????????????????????????
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE},
                    100);
            return false;
        } else {

            return true;
        }
    }

    private void getData() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        mDeviceInfo = (DeviceInfo) extras.getSerializable(WJDeviceConfig.DEVICE_INFO);
        mDeviceCode = extras.getInt(WJDeviceConfig.DEVICE_CODE);
    }

    private long startWiredTime;

    public void startwired(APWiredConfigInfo.Builder apWiredConfigInfo) {
        String password = "AP" + mDeviceInfo.device_code;
        //"EZVIZ_"+???????????????
        String ssid = "HAP_" + mDeviceInfo.device_serial;
        //Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
        startWiredTime = System.currentTimeMillis();
        WifiUtils.withContext(getApplicationContext())
                .connectWith(ssid, password)
                .setTimeout(15000)
                .onConnectionResult(new ConnectionSuccessListener() {
                    @Override
                    public void success() {
                        mLoadingPopupView = new XPopup.Builder(WJSettingWiredActivity.this).dismissOnTouchOutside(false).setPopupCallback(new SimpleCallback() {
                            @Override
                            public void onDismiss(BasePopupView popupView) {
                                if (handler != null) {
                                    handler.removeMessages(SEND_CHECK_DEVICE_MSG);
                                }
                            }
                        }).asLoading();
                        mLoadingPopupView.show();


                        apWiredConfigInfo.deviceSN(mDeviceInfo.device_serial)  // ???????????????????????????
                                .activatePwd("Hik" + mDeviceInfo.device_code)  // ??????????????????????????????????????????????????? Hik+??????????????????????????????????????? Hik+?????????
                                .verifyCode(mDeviceInfo.device_code); // ????????????????????????
                        Log.i(TAG, "success: " + new Gson().toJson(apWiredConfigInfo.getApWiredConfigInfo()));
                        YsApManager.INSTANCE.activateWired(apWiredConfigInfo.build(), new YsApManager.ApActivateCallback() {
                            @Override
                            public void onStartSearch() {
                                logPrint("?????????????????????????????????????????????");
                            }

                            @Override
                            public void onStartActivate() {
                                logPrint("????????????????????????");
                            }

                            @Override
                            public void onStartConfigWifi() {
                                logPrint("????????????????????????wifi");

                            }

                            @Override
                            public void onSuccess() {
                                logPrint("wifi ??????????????????");
                                if (handler != null) {
                                    if (mDeviceCode == 120020) {
                                        //?????????????????????
                                        handler.sendEmptyMessageDelayed(SEND_CHECK_ISAPI, 2000L);
                                    } else {
                                        handler.sendEmptyMessageDelayed(SEND_CHECK_DEVICE_MSG, 2000L);
                                    }
                                }
                            }

                            @Override
                            public void onFailed(int code, @NotNull String msg, @org.jetbrains.annotations.Nullable Throwable exception) {
                                String format = String.format("?????? ?????????code=%s , msg=%s ", code + "", msg + "");
                                if (mLoadingPopupView!=null){
                                    mLoadingPopupView.dismiss();
                                }
                                showWiredHint();
                               // logPrint(format);
                            }
                        });

                    }

                    @Override
                    public void failed(@NonNull ConnectionErrorCode errorCode) {
                        Log.i(TAG, "failed: " + errorCode.name());
                        if (errorCode == ConnectionErrorCode.USER_CANCELLED) {

                        } else {
                            new XPopup.Builder(WJSettingWiredActivity.this)
                                    .asConfirm("????????????????????????????????????",
                                            "1.???????????????????????????\n2.??????????????????????????????????????????????????????",
                                            "??????",
                                            "??????",
                                            new OnConfirmListener() {
                                                @Override
                                                public void onConfirm() {
                                                    startwired(apWiredConfigInfo);
                                                }
                                            },
                                            null,
                                            false,
                                            0
                                    ).show();
                        }
                    }
                })
                .start();

    }

    private void clear() {
        if (handler != null) {
            handler.removeMessages(SEND_CHECK_DEVICE_MSG);
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clear();
    }

    //??????????????????
    private final int SEND_CHECK_DEVICE_MSG = 1001;
    private final int SEND_CHECK_ISAPI = 1002;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SEND_CHECK_DEVICE_MSG:
                    logPrint("???????????????????????????....");
                    checkDevice();
                    break;
                case SEND_CHECK_ISAPI:
                    logPrint("???????????????????????????....");
                    checkIsApi();
                    break;
            }
        }
    };

    @SuppressLint("CheckResult")
    private void checkIsApi() {
        Observable.just(mDeviceInfo).map(new Function<DeviceInfo, DeviceInfo>() {
            @Override
            public DeviceInfo apply(@io.reactivex.annotations.NonNull DeviceInfo deviceInfo) throws Exception {
                OkHttpClient mClient = new OkHttpClient.Builder()
                        .addNetworkInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                        .addInterceptor(new SafeGuardInterceptor())
                        .writeTimeout(2, TimeUnit.SECONDS)
                        .connectTimeout(2, TimeUnit.SECONDS)
                        .readTimeout(2, TimeUnit.SECONDS).build();

                RtmpConfig rtmp = ISAPI.getInstance().getRTMP(mDeviceInfo.device_serial, mClient);
                Log.i(TAG, "apply: " + System.currentTimeMillis() + "    rtmp= " + new Gson().toJson(rtmp));
                if (rtmp == null || rtmp.getRTMP() == null) {
                    return deviceInfo;
                }
                deviceInfo.rtmpConfig = rtmp;
                return deviceInfo;
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        mLoadingPopupView.dismiss();
                        finish();
                    }
                }).doOnSubscribe(new RxConsumer(this))
                .subscribe(new Consumer<DeviceInfo>() {
                    @Override
                    public void accept(DeviceInfo deviceInfo) throws Exception {
                        if (deviceInfo.rtmpConfig == null || deviceInfo.rtmpConfig.getRTMP() == null) {
                            if (startWiredTime + 1000 * 60 <= System.currentTimeMillis()) {
                                if (mLoadingPopupView != null) {
                                    mLoadingPopupView.dismiss();
                                }
                                showWiredHint();
                            } else {
                                handler.sendEmptyMessageDelayed(SEND_CHECK_ISAPI, 1000L);
                            }
                        } else {
                            clear();
                            EventBus.getDefault().post(mDeviceInfo);
                            Toast.makeText(WJSettingWiredActivity.this, "??????????????????", Toast.LENGTH_SHORT).show();
                            WJActivityControl.getInstance().finishActivity(WJSettingModeActivity.class);
                            finish();
                        }
                    }
                });
    }

    private static final String TAG = "WJSettingWiredActivity";

    public void logPrint(String log) {
        if (mLoadingPopupView != null) {
            mLoadingPopupView.setTitle(log);
        }
        Log.i(TAG, "logPrint: " + log);
    }

    @SuppressLint("CheckResult")
    protected void toast(String text) {
        Observable.just(text)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Toast.makeText(WJSettingWiredActivity.this, s, Toast.LENGTH_LONG).show();

                    }
                });

    }

    @SuppressLint("CheckResult")
    public void checkDevice() {
        Log.i(TAG, "checkDevice: ");
        if (mDeviceInfo != null) {
            Observable.just(mDeviceInfo)
                    .map(new Function<DeviceInfo, EZProbeDeviceInfoResult>() {
                        @Override
                        public EZProbeDeviceInfoResult apply(@io.reactivex.annotations.NonNull DeviceInfo deviceInfo) throws Exception {
                            EZProbeDeviceInfoResult result = EZOpenSDK.getInstance().probeDeviceInfo(deviceInfo.device_serial, deviceInfo.device_type);
                            return result;
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError(new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            mLoadingPopupView.dismiss();
                            finish();
                        }
                    }).doOnSubscribe(new RxConsumer(this))
                    .doOnError(new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            if (startWiredTime + 1000 * 60 >= System.currentTimeMillis()) {
                                showWiredHint();
                            } else {
                                if (handler != null) {
                                    handler.sendEmptyMessageDelayed(SEND_CHECK_DEVICE_MSG, 2000L);
                                }
                            }

                        }
                    })
                    .subscribe(new Consumer<EZProbeDeviceInfoResult>() {
                        @Override
                        public void accept(EZProbeDeviceInfoResult result) throws Exception {
                            if (result.getBaseException() == null) {
                                // toast("??????????????????");
                                DeviceApi.getInstance().addDevie(mDeviceInfo.device_serial, mDeviceInfo.device_code, new JsonCallback<BaseDeviceResponse>() {
                                    @Override
                                    public void onSuccess(BaseDeviceResponse data) {
                                        if (mLoadingPopupView != null) {
                                            mLoadingPopupView.dismiss();
                                        }
                                        clear();
                                        EventBus.getDefault().post(mDeviceInfo);
                                        Toast.makeText(WJSettingWiredActivity.this, "??????????????????", Toast.LENGTH_SHORT).show();

                                        WJActivityControl.getInstance().finishActivity(WJSettingModeActivity.class);

                                        finish();
                                    }

                                    @Override
                                    public void onError(int code, String msg) {
                                        super.onError(code, msg);
                                        if (mLoadingPopupView != null) {
                                            mLoadingPopupView.dismiss();
                                        }
                                        clear();
                                        Toast.makeText(WJSettingWiredActivity.this, "??????????????????", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                });
                            } else {

                                int errorCode = result.getBaseException().getErrorCode();
                                if (errorCode == 120020) {
                                    clear();
                                    //???????????????
                                    EventBus.getDefault().post(mDeviceInfo);
                                    Toast.makeText(WJSettingWiredActivity.this, "??????????????????", Toast.LENGTH_SHORT).show();
                                    WJActivityControl.getInstance().finishActivity(WJSettingModeActivity.class);
                                    finish();
                                } else {
                                    if (startWiredTime + 1000 * 60 <= System.currentTimeMillis()) {
                                        if (mLoadingPopupView != null) {
                                            mLoadingPopupView.dismiss();
                                        }
                                        showWiredHint();
                                    } else {
                                        if (handler != null) {
                                            handler.sendEmptyMessageDelayed(SEND_CHECK_DEVICE_MSG, 2000L);
                                        }
                                    }
                                    //toast(errorCode + "");
                                }
                            }
                        }
                    });
        }
    }

    public void showWiredHint() {
        new XPopup.Builder(this).asConfirm("????????????????????????", "1.?????????????????????????????? \n2.??????????????????IP??????????????????", new OnConfirmListener() {
            @Override
            public void onConfirm() {

            }
        }).show();
    }
}
