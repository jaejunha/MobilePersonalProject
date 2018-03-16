package dreamline91.naver.com.checker.functionality.lock;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

import dreamline91.naver.com.checker.R;
import dreamline91.naver.com.checker.functionality.lock.dialog.RandomDialog;

/**
 * Created by dream on 2018-02-27.
 */

public class LockActivity extends Activity {

    private final int INTENT_ALBUM = 0;
    private final int INTENT_CROP = 1;

    private int int_startCursor;
    private int int_currentCursor;
    private int int_endCursor;

    private int int_screenWidth;
    private int int_screenHeight;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        getDeviceSize();
        setBackground();
        loadBackground();
        setTextCalendar();
        setButtonRandom();
        setButtonBackground();
        slideEvent();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void setBackground() {
        LinearLayout layout_lock = (LinearLayout) findViewById(R.id.layout_lock);
        String string_backgroundPath = loadBackground();
        if (string_backgroundPath.equals("") == false)
            layout_lock.setBackground(makeBitmap(string_backgroundPath));
    }

    private String loadBackground() {
        SharedPreferences preference = getSharedPreferences("checker", Activity.MODE_PRIVATE);
        return preference.getString("string_backgroundPath", "");
    }

    private void setTextCalendar() {
        TextView text_calendar = (TextView) findViewById(R.id.text_calendar);
        Calendar cal = Calendar.getInstance();
        String string_calendar;
        String[] string_week = {"토요일", "일요일", "월요일", "화요일", "수요일", "목요일", "금요일"};
        string_calendar = String.format("%04d년 %2d월 %2d일 ", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DATE));
        string_calendar += string_week[cal.get(Calendar.DAY_OF_WEEK)];
        text_calendar.setText(string_calendar);
    }

    private void setButtonRandom() {
        ImageButton button_random = (ImageButton) findViewById(R.id.button_random);
        button_random.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new RandomDialog(LockActivity.this).show();
            }
        });
    }

    private void setButtonBackground() {
        ImageButton button_background = (ImageButton) findViewById(R.id.button_background);
        button_background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
                intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, INTENT_ALBUM);
            }
        });
    }

    private void slideEvent() {
        final LinearLayout linearLayout = findViewById(R.id.layout_lock);
        linearLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        int_startCursor = (int) motionEvent.getRawX();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int_currentCursor = (int) motionEvent.getRawX();
                        if (int_currentCursor - int_startCursor >= 0)
                            linearLayout.setX(int_currentCursor - int_startCursor);
                        return true;
                    case MotionEvent.ACTION_UP:
                        int_endCursor = (int) motionEvent.getRawX();
                        if ((int_endCursor - int_startCursor) > (int_screenWidth / 3.5))
                            finish();
                        else {
                            linearLayout.setX(0);
                            int_startCursor = 0;
                            int_currentCursor = 0;
                        }
                        return false;
                }
                return false;
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case INTENT_ALBUM:
                    Intent intent = new Intent("com.android.camera.action.CROP");
                    intent.setDataAndType(data.getData(), "image/*");
                    intent.putExtra("crop", "true");
                    intent.putExtra("scale", "true");
                    startActivityForResult(intent, INTENT_CROP);
                    break;
                case INTENT_CROP:
                    try {
                        LinearLayout layout_lock = (LinearLayout) findViewById(R.id.layout_lock);
                        String string_path = getPath(data.getData());
                        layout_lock.setBackground(makeBitmap(string_path));
                        saveBackground(string_path);

                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "배경화면 바꾸기에 실패했습니다", Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }
    }

    private void saveBackground(String string_path) {
        SharedPreferences preference = getSharedPreferences("checker", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preference.edit();
        editor.putString("string_backgroundPath", string_path);
        editor.commit();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private BitmapDrawable makeBitmap(String path) {

        Bitmap bitmap_background = BitmapFactory.decodeFile(path);
        if(bitmap_background == null)   //called when background image was deleted
            return null;
        Bitmap bitmap_resized;
        if (bitmap_background.getWidth() > bitmap_background.getHeight()) {
            bitmap_resized = Bitmap.createScaledBitmap(bitmap_background, int_screenHeight, int_screenWidth, true);
            bitmap_background.recycle();
        } else {
            bitmap_resized = Bitmap.createScaledBitmap(bitmap_background, int_screenWidth, int_screenHeight, true);
            bitmap_background.recycle();
        }
        try {
            ExifInterface exif = new ExifInterface(path);
            return new BitmapDrawable(rotateBitmap(bitmap_resized, exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)));
        } catch (Exception e) {
            return new BitmapDrawable(bitmap_resized);
        }
    }

    /* reference : https://stackoverflow.com/questions/20478765/how-to-get-the-correct-orientation-of-the-image-selected-from-the-default-image */
    private Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bitmap_rotate = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bitmap_rotate;
        } catch (OutOfMemoryError e) {
            return bitmap;
        }
    }
    /* reference : https://stackoverflow.com/questions/20478765/how-to-get-the-correct-orientation-of-the-image-selected-from-the-default-image */

    private String getPath(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            return uri.getPath();
        } else {
            cursor.moveToFirst();
            int int_index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(int_index);
        }
    }

    public void getDeviceSize() {
        DisplayMetrics display = getApplicationContext().getResources().getDisplayMetrics();
        int_screenWidth = display.widthPixels;
        int_screenHeight = display.heightPixels;
    }
}
