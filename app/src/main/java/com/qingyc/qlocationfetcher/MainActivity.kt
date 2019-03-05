package com.qingyc.qlocationfetcher

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.qingyc.qlocationlib.QLocationFetcher
import com.qingyc.qlogger.QLogger
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener, QLocationFetcher.LocationCallBack {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        QLogger.init(BuildConfig.DEBUG)
        bt_get_location.setOnClickListener(this)
    }


    @SuppressLint("CheckResult")
    override fun onClick(v: View?) {
        val rxPermissions = RxPermissions(this@MainActivity)
        rxPermissions.requestEachCombined(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
            .subscribe { permission ->
                when {
                    permission.granted -> {
                        QLogger.e("有权限")
                        QLocationFetcher.getInstance().getLocation(this@MainActivity, true, this@MainActivity)
                    }
                    permission.shouldShowRequestPermissionRationale -> {
                        QLogger.e("需要获取")
                    }
                    //如果用户永久拒绝了 给提示
                    else -> {
                        QLogger.e("没有定位权限")

                    }
                }
            }
    }

    @SuppressLint("SetTextI18n")
    override fun onLocationCallBack(location: Location?) {
        QLogger.e("get location :  ==== " + location?.toString())
        tv_location.text = "location === ${location?.toString()}"
    }

}
