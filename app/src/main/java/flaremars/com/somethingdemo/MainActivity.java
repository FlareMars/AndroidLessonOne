package flaremars.com.somethingdemo;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import flaremars.com.somethingdemo.adapters.CachedPicturesAdapter;
import flaremars.com.somethingdemo.bean.BitmapBean;
import flaremars.com.somethingdemo.utils.DisplayUtils;
import flaremars.com.somethingdemo.utils.FileUtils;
import flaremars.com.somethingdemo.utils.network.HttpUtils;
import flaremars.com.somethingdemo.utils.network.INetworkContext;
import flaremars.com.somethingdemo.utils.network.NetworkHandler;

public class MainActivity extends AppCompatActivity implements INetworkContext {

    private static final int TAKE_PHOTO_ACTION_CODE = 1;

    private static final int SUCCESS_CODE = 200;

    private static final SimpleDateFormat PHOTO_NAME_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);

    private static final String BASE_URL = "http://192.168.1.6:8080/";

    public static int screenWidth = 0;

    public static int screenHeight = 0;

    public static int requiredImageWidth = 0;

    public static int requiredImageHeight = 0;

    private MaterialDialog progressDialog;

    private NetworkHandler handler;

    private Executor singleExecutor;

    private String tempPhotoName;

    private String tempPhotoPath;

    private int totalProgress = 0;

    private int currentProgress = 0;

    //结果显示相关
    private GridView contentView;

    private List<BitmapBean> bitmapBeanList;

    private CachedPicturesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Point screenSize = DisplayUtils.INSTANCE.getScreenWidth(this);
        screenWidth = screenSize.x;
        screenHeight = screenSize.y;
        int fiveDpValue = DisplayUtils.INSTANCE.dp2px(this,5.0f);
        requiredImageWidth = (screenWidth - 3 * fiveDpValue) / 2;
        requiredImageHeight = (DisplayUtils.INSTANCE.getWindowContentHeight(this) - 3 * fiveDpValue) / 2;

        contentView = (GridView) findViewById(R.id.contentView);
        bitmapBeanList = new ArrayList<>();
        adapter = new CachedPicturesAdapter(this,bitmapBeanList,contentView);
        contentView.setAdapter(adapter);
        contentView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BitmapBean item = bitmapBeanList.get(position);
                Toast.makeText(MainActivity.this, item.getInfo(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflate = getMenuInflater();
        menuInflate.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.upload) {
            takePhotoAction();
            return true;
        } else if (itemId == R.id.picture_show) {
            showPictures();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == TAKE_PHOTO_ACTION_CODE) {

                final File targetFile = new File(tempPhotoPath);
                totalProgress = (int)targetFile.length();
                singleExecutor = Executors.newSingleThreadExecutor();
                handler = new NetworkHandler(this);
                handler.setMSG_WHAT(1);
                new MaterialDialog.Builder(this)
                        .title("图片上传中...")
                        .contentGravity(GravityEnum.CENTER)
                        .progress(false, totalProgress, true)
                        .showListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(DialogInterface dialogInterface) {
                                final MaterialDialog dialog = (MaterialDialog) dialogInterface;
                                MainActivity.this.progressDialog = dialog;
                                singleExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        String targetUrl = BASE_URL + "WebDemo/upload";
                                        Map<String, String> params = new HashMap<>();
                                        params.put("extraData", "something information");

                                        final String response = HttpUtils.INSTANCE.uploadFile(targetUrl, params, targetFile, handler);
                                        Log.i("tag", response);
                                        MainActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                parsePhotosResult(response);

                                                if (loadingDialog != null) {
                                                    loadingDialog.dismiss();
                                                    loadingDialog = null;
                                                }

                                            }
                                        });
                                    }
                                });
                            }
                        }).show();
            }
        }
    }

    private static final String DATA_DEMO = "{\n" +
            "    \"statusCode\": 200,\n" +
            "    \"message\": \"success\",\n" +
            "    \"data\": [\n" +
            "        {\n" +
            "            \"url\": \"http://img2.3lian.com/2014/f2/8/d/95.jpg\",\n" +
            "            \"info\": \"testPicture1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://down.laifudao.com/tupian/a44999551.jpg\",\n" +
            "            \"info\": \"testPicture2\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://www.bz55.com/uploads/allimg/130608/1-13060PZ040.jpg\",\n" +
            "            \"info\": \"testPicture3\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://imga1.pic21.com/bizhi/140122/06901/s08.jpg\",\n" +
            "            \"info\": \"testPicture4\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://img.pconline.com.cn/images/upload/upc/tx/wallpaper/1208/09/c1/12788657_1344485553784_800x600.jpg\",\n" +
            "            \"info\": \"testPicture5\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://www.bz55.com/uploads/allimg/140626/1-140626093018.jpg\",\n" +
            "            \"info\": \"testPicture6\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://i4.download.fd.pchome.net/t_960x600/g1/M00/07/0B/oYYBAFMydeyIDh3uAAfStz8hB0UAABbpgH7AkcAB9LP003.jpg\",\n" +
            "            \"info\": \"testPicture7\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://img.taopic.com/uploads/allimg/120426/1942-1204260PU592.jpg\",\n" +
            "            \"info\": \"testPicture8\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://imga1.pic21.com/bizhi/131113/03765/s04.jpg\",\n" +
            "            \"info\": \"testPicture9\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://img2.3lian.com/2014/f2/8/d/100.jpg\",\n" +
            "            \"info\": \"testPicture10\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://pic.yesky.com/imagelist/06/48/1003187_7707.jpg\",\n" +
            "            \"info\": \"testPicture11\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://imga1.pic21.com/bizhi/140122/06901/s09.jpg\",\n" +
            "            \"info\": \"testPicture12\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://imga1.pic21.com/bizhi/140122/06901/s05.jpg\",\n" +
            "            \"info\": \"testPicture13\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://img5.imgtn.bdimg.com/it/u=2917642346,873387081&fm=21&gp=0.jpg\",\n" +
            "            \"info\": \"testPicture14\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://img2.3lian.com/2014/f2/8/d/101.jpg\",\n" +
            "            \"info\": \"testPicture15\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://imga1.pic21.com/bizhi/140122/06901/s08.jpg\",\n" +
            "            \"info\": \"testPicture16\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://www.bz55.com/uploads/allimg/130608/1-13060PZ040.jpg\",\n" +
            "            \"info\": \"testPicture17\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://img2.3lian.com/2014/f2/8/d/95.jpg\",\n" +
            "            \"info\": \"testPicture18\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"url\": \"http://down.laifudao.com/tupian/a44999551.jpg\",\n" +
            "            \"info\": \"testPicture19\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    private void showPictures() {
        parsePhotosResult(DATA_DEMO);
    }

    private void parsePhotosResult(String jsonStr) {
        try {
            JSONObject responseObject = new JSONObject(jsonStr);
            int statusCode = responseObject.getInt("statusCode");
            String msg = responseObject.getString("message");

            if (statusCode == SUCCESS_CODE) {
                bitmapBeanList.clear();
                JSONArray dataArray = responseObject.getJSONArray("data");
                int size = dataArray.length();
                BitmapBean tempBean;
                JSONObject tempObject;
                for (int i = 0; i < size; i++) {
                    tempObject = dataArray.getJSONObject(i);
                    String info = "";
                    if (tempObject.has("info")) {
                        info = tempObject.getString("info");
                    } else {
                        try {
                            info = URLDecoder.decode(tempObject.getString("name"),"utf-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    tempBean = new BitmapBean(tempObject.getString("url"), info);
                    bitmapBeanList.add(tempBean);
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(MainActivity.this, "错误：" + msg, Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void takePhotoAction() {
        final File directory = FileUtils.INSTANCE.getDirectory(this, "photos", false);
        tempPhotoName = PHOTO_NAME_FORMAT.format(new Date()) + ".jpg";
        assert directory != null;
        final String string = directory.getPath() + File.separator + tempPhotoName;
        tempPhotoPath = string;
        final File file = new File(string);
        final Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra("output", Uri.fromFile(file));
        intent.putExtra("return-data", true);
        startActivityForResult(intent, TAKE_PHOTO_ACTION_CODE);
    }

    private MaterialDialog loadingDialog;

    @Override
    public void invalidate(int progress) {
        Log.i("tag",progress + " " + currentProgress + " " + totalProgress);
        if (progressDialog != null) {
            progressDialog.incrementProgress(progress - currentProgress);
            currentProgress = progress;

            if (progress >= totalProgress) {
                progressDialog.dismiss();
                progressDialog = null;
                currentProgress = 0;
                loadingDialog = new MaterialDialog.Builder(this)
                        .title("等待数据回复")
                        .content("惊奇总是出现在等待之后")
                        .progress(true, 0)
                        .progressIndeterminateStyle(false)
                        .show();
            }
        }
    }
}
