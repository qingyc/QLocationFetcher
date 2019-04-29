# QLocationFetcher
通过gms api和Android api两种方式获取定位

![Location](https://github.com/qingyc/QLocationFetcher/blob/master/art/location.png)


# How To Use

Step 1. Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

Step 2. Add the dependency

	dependencies {
	         implementation 'com.github.qingyc:QLocationFetcher:0.1'
	}

Step 3. Use

 ```
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
 ```

