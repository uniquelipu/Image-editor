package abinashgiri.github.io.ananas.editimage;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.theartofdev.edmodo.cropper.CropImageView;

import org.jetbrains.annotations.NotNull;

import abinashgiri.github.io.ananas.BaseActivity;
import abinashgiri.github.io.ananas.R;
import abinashgiri.github.io.ananas.editimage.fragment.AddTextFragment;
import abinashgiri.github.io.ananas.editimage.fragment.BeautyFragment;
import abinashgiri.github.io.ananas.editimage.fragment.BrightnessFragment;
import abinashgiri.github.io.ananas.editimage.fragment.FilterListFragment;
import abinashgiri.github.io.ananas.editimage.fragment.MainMenuFragment;
import abinashgiri.github.io.ananas.editimage.fragment.RotateFragment;
import abinashgiri.github.io.ananas.editimage.fragment.SaturationFragment;
import abinashgiri.github.io.ananas.editimage.fragment.StickerFragment;
import abinashgiri.github.io.ananas.editimage.fragment.crop.CropFragment;
import abinashgiri.github.io.ananas.editimage.fragment.paint.PaintFragment;
import abinashgiri.github.io.ananas.editimage.interfaces.OnLoadingDialogListener;
import abinashgiri.github.io.ananas.editimage.interfaces.OnMainBitmapChangeListener;
import abinashgiri.github.io.ananas.editimage.utils.BitmapUtils;
import abinashgiri.github.io.ananas.editimage.utils.PermissionUtils;
import abinashgiri.github.io.ananas.editimage.view.BrightnessView;
import abinashgiri.github.io.ananas.editimage.view.CustomPaintView;
import abinashgiri.github.io.ananas.editimage.view.CustomViewPager;
import abinashgiri.github.io.ananas.editimage.view.RotateImageView;
import abinashgiri.github.io.ananas.editimage.view.SaturationView;
import abinashgiri.github.io.ananas.editimage.view.StickerView;
import abinashgiri.github.io.ananas.editimage.view.TextStickerView;
import abinashgiri.github.io.ananas.editimage.view.imagezoom.ImageViewTouch;
import abinashgiri.github.io.ananas.editimage.view.imagezoom.ImageViewTouchBase;
import abinashgiri.github.io.ananas.editimage.widget.RedoUndoController;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class EditImageActivity extends BaseActivity implements OnLoadingDialogListener {
    public static final String IS_IMAGE_EDITED = "is_image_edited";
    public static final int MODE_NONE = 0;
    public static final int MODE_STICKERS = 1;
    public static final int MODE_FILTER = 2;
    public static final int MODE_CROP = 3;
    public static final int MODE_ROTATE = 4;
    public static final int MODE_TEXT = 5;
    public static final int MODE_PAINT = 6;
    public static final int MODE_BEAUTY = 7;
    public static final int MODE_BRIGHTNESS = 8;
    public static final int MODE_SATURATION = 9;
    private static final int PERMISSIONS_REQUEST_CODE = 110;
    private final String[] requiredPermissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public String sourceFilePath;
    public String outputFilePath;
    public String editorTitle;
    public StickerView stickerView;
    public CropImageView cropPanel;
    public ImageViewTouch mainImage;
    public TextStickerView textStickerView;
    public int mode = MODE_NONE;
    protected boolean isBeenSaved = false;
    protected boolean isPortraitForced = false;
    protected boolean isSupportActionBarEnabled = false;
    public CustomPaintView paintView;
    public ViewFlipper bannerFlipper;
    public BrightnessView brightnessView;
    public SaturationView saturationView;
    public RotateImageView rotatePanel;
    public CustomViewPager bottomGallery;
    public StickerFragment stickerFragment;
    public FilterListFragment filterListFragment;
    public CropFragment cropFragment;
    public RotateFragment rotateFragment;
    public AddTextFragment addTextFragment;
    public PaintFragment paintFragment;
    public BeautyFragment beautyFragment;
    public BrightnessFragment brightnessFragment;
    public SaturationFragment saturationFragment;
    protected int numberOfOperations = 0;
    private int imageWidth, imageHeight;
    private Bitmap mainBitmap;
    private Dialog loadingDialog;
    private MainMenuFragment mainMenuFragment;
    private RedoUndoController redoUndoController;
    private OnMainBitmapChangeListener onMainBitmapChangeListener;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public static void start(ActivityResultLauncher<Intent> launcher, Intent intent, Context context) {
        if (TextUtils.isEmpty(intent.getStringExtra(ImageEditorIntentBuilder.SOURCE_PATH))) {
            Toast.makeText(context, R.string.iamutkarshtiwari_github_io_ananas_not_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        launcher.launch(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit);
        getData();
        initView();
    }

    @Override
    public void showLoadingDialog() {
        loadingDialog.show();
    }

    @Override
    public void dismissLoadingDialog() {
        loadingDialog.dismiss();
    }

    private void getData() {
        isPortraitForced = getIntent().getBooleanExtra(ImageEditorIntentBuilder.FORCE_PORTRAIT, false);
        isSupportActionBarEnabled  = getIntent().getBooleanExtra(ImageEditorIntentBuilder.SUPPORT_ACTION_BAR_VISIBILITY, false);

        sourceFilePath = getIntent().getStringExtra(ImageEditorIntentBuilder.SOURCE_PATH);
        outputFilePath = getIntent().getStringExtra(ImageEditorIntentBuilder.OUTPUT_PATH);
        editorTitle = getIntent().getStringExtra(ImageEditorIntentBuilder.EDITOR_TITLE);
    }

    private void initView() {
        TextView titleView = findViewById(R.id.title);
        if (editorTitle != null) {
            titleView.setText(editorTitle);
        }
        loadingDialog = BaseActivity.getLoadingDialog(this, R.string.iamutkarshtiwari_github_io_ananas_loading,
                false);

        if (getSupportActionBar() != null) {
            if (isSupportActionBarEnabled) {
                getSupportActionBar().show();
            } else {
                getSupportActionBar().hide();
            }
        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        imageWidth = metrics.widthPixels / 2;
        imageHeight = metrics.heightPixels / 2;

        bannerFlipper = findViewById(R.id.banner_flipper);
        bannerFlipper.setInAnimation(this, R.anim.in_bottom_to_top);
        bannerFlipper.setOutAnimation(this, R.anim.out_bottom_to_top);
        View applyBtn = findViewById(R.id.apply);
        applyBtn.setOnClickListener(new ApplyBtnClick());
        View saveBtn = findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(new SaveBtnClick());

        mainImage = findViewById(R.id.main_image);

        View backBtn = findViewById(R.id.back_btn);
        backBtn.setOnClickListener(v -> onBackPressed());

        stickerView = findViewById(R.id.sticker_panel);
        cropPanel = findViewById(R.id.crop_panel);
        rotatePanel = findViewById(R.id.rotate_panel);
        textStickerView = findViewById(R.id.text_sticker_panel);
        paintView = findViewById(R.id.custom_paint_view);
        brightnessView = findViewById(R.id.brightness_panel);
        saturationView = findViewById(R.id.contrast_panel);
        bottomGallery = findViewById(R.id.bottom_gallery);

        mainMenuFragment = MainMenuFragment.newInstance();
        mainMenuFragment.setArguments(getIntent().getExtras());

        BottomGalleryAdapter bottomGalleryAdapter = new BottomGalleryAdapter(
                this.getSupportFragmentManager());
        stickerFragment = StickerFragment.newInstance();
        filterListFragment = FilterListFragment.newInstance();
        cropFragment = CropFragment.newInstance();
        rotateFragment = RotateFragment.newInstance();
        paintFragment = PaintFragment.newInstance();
        beautyFragment = BeautyFragment.newInstance();
        brightnessFragment = BrightnessFragment.newInstance();
        saturationFragment = SaturationFragment.newInstance();
        addTextFragment = AddTextFragment.newInstance();
        setOnMainBitmapChangeListener(addTextFragment);

        bottomGallery.setAdapter(bottomGalleryAdapter);

        mainImage.setFlingListener((e1, e2, velocityX, velocityY) -> {
            if (velocityY > 1) {
                closeInputMethod();
            }
        });

        redoUndoController = new RedoUndoController(this, findViewById(R.id.redo_undo_panel));

        if (!PermissionUtils.hasPermissions(this, requiredPermissions)) {
            ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSIONS_REQUEST_CODE);
        }

        loadImageFromFile(sourceFilePath);
    }

    private void setOnMainBitmapChangeListener(OnMainBitmapChangeListener listener) {
        onMainBitmapChangeListener = listener;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NotNull String permissions[], @NotNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (!(grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Lock orientation for this activity
        if (isPortraitForced) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setLockScreenOrientation(true);
        }
    }

    private void closeInputMethod() {
        if (addTextFragment.isAdded()) {
            addTextFragment.hideInput();
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) { }

    @Override
    public void onBackPressed() {
        switch (mode) {
            case MODE_STICKERS:
                stickerFragment.backToMain();
                break;
            case MODE_FILTER:
                filterListFragment.backToMain();
                break;
            case MODE_CROP:
                cropFragment.backToMain();
                break;
            case MODE_ROTATE:
                rotateFragment.backToMain();
                break;
            case MODE_TEXT:
                addTextFragment.backToMain();
                break;
            case MODE_PAINT:
                paintFragment.backToMain();
                break;
            case MODE_BEAUTY:
                beautyFragment.backToMain();
                break;
            case MODE_BRIGHTNESS:
                brightnessFragment.backToMain();
                break;
            case MODE_SATURATION:
                saturationFragment.backToMain();
                break;
            default:
                if (canAutoExit()) {
                    onSaveTaskDone();
                } else {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setMessage(R.string.iamutkarshtiwari_github_io_ananas_exit_without_save)
                            .setCancelable(false).setPositiveButton(R.string.iamutkarshtiwari_github_io_ananas_confirm, (dialog, id) -> finish()).setNegativeButton(R.string.iamutkarshtiwari_github_io_ananas_cancel, (dialog, id) -> dialog.cancel());

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
                break;
        }
    }

    public void changeMainBitmap(Bitmap newBit, boolean needPushUndoStack) {
        if (newBit == null)
            return;

        if (mainBitmap == null || mainBitmap != newBit) {
            if (needPushUndoStack) {
                redoUndoController.switchMainBit(mainBitmap, newBit);
                increaseOpTimes();
            }
            mainBitmap = newBit;
            mainImage.setImageBitmap(mainBitmap);
            mainImage.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);

            if (mode == MODE_TEXT) {
                onMainBitmapChangeListener.onMainBitmapChange();
            }
        }
    }

    protected void onSaveTaskDone() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(ImageEditorIntentBuilder.SOURCE_PATH, sourceFilePath);
        returnIntent.putExtra(ImageEditorIntentBuilder.OUTPUT_PATH, outputFilePath);
        returnIntent.putExtra(IS_IMAGE_EDITED, numberOfOperations > 0);

        setResult(RESULT_OK, returnIntent);
        finish();
    }

    protected void doSaveImage() {
        if (numberOfOperations <= 0)
            return;

        Disposable saveImageDisposable = saveImage(mainBitmap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscriber -> loadingDialog.show())
                .doFinally(() -> loadingDialog.dismiss())
                .subscribe(result -> {
                    if (result) {
                        resetOpTimes();
                        onSaveTaskDone();
                    } else {
                        showToast(R.string.iamutkarshtiwari_github_io_ananas_save_error);
                    }
                }, e -> showToast(R.string.iamutkarshtiwari_github_io_ananas_save_error));

        compositeDisposable.add(saveImageDisposable);
    }

    private Single<Boolean> saveImage(Bitmap finalBitmap) {
        return Single.fromCallable(() -> {
            if (TextUtils.isEmpty(outputFilePath))
                return false;

            return BitmapUtils.saveBitmap(finalBitmap, outputFilePath);
        });
    }

    private void loadImageFromFile(String filePath) {
        Disposable loadImageDisposable = loadImage(filePath)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscriber -> {
                    loadingDialog.show();
                    mainMenuFragment.setMenuOptionsClickable(false);
                })
                .doOnSuccess(bitmap -> {
                    mainMenuFragment.setMenuOptionsClickable(true);
                })
                .doFinally(() -> loadingDialog.dismiss())
                .subscribe(processedBitmap -> changeMainBitmap(processedBitmap, false), e -> {
                    showToast(R.string.iamutkarshtiwari_github_io_ananas_load_error);
                    Log.wtf("Error", e.getMessage());
                });

        compositeDisposable.add(loadImageDisposable);
    }

    private Single<Bitmap> loadImage(String filePath) {
        return Single.fromCallable(() -> BitmapUtils.getSampledBitmap(filePath, imageWidth,
                imageHeight));
    }

    private void showToast(@StringRes int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.dispose();

        if (redoUndoController != null) {
            redoUndoController.onDestroy();
        }

        if (!isPortraitForced) {
            setLockScreenOrientation(false);
        }
    }

    protected void setLockScreenOrientation(boolean lock) {
        if (Build.VERSION.SDK_INT >= 18) {
            setRequestedOrientation(lock ? ActivityInfo.SCREEN_ORIENTATION_LOCKED : ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            return;
        }

        if (lock) {
            switch (getWindowManager().getDefaultDisplay().getRotation()) {
                case Surface
                        .ROTATION_0:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    break;
                case Surface
                        .ROTATION_90:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    break;
                case Surface
                        .ROTATION_180:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    break;
                case Surface
                        .ROTATION_270:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    break;
            }
        } else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    public void increaseOpTimes() {
        numberOfOperations++;
        isBeenSaved = false;
    }

    public boolean canAutoExit() {
        return isBeenSaved || numberOfOperations == 0;
    }

    public void resetOpTimes() {
        isBeenSaved = true;
    }

    public Bitmap getMainBit() {
        return mainBitmap;
    }

    private final class BottomGalleryAdapter extends FragmentPagerAdapter {
        BottomGalleryAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int index) {
            switch (index) {
                case MainMenuFragment.INDEX:
                    return mainMenuFragment;
                case StickerFragment.INDEX:
                    return stickerFragment;
                case FilterListFragment.INDEX:
                    return filterListFragment;
                case CropFragment.INDEX:
                    return cropFragment;
                case RotateFragment.INDEX:
                    return rotateFragment;
                case AddTextFragment.INDEX:
                    return addTextFragment;
                case PaintFragment.INDEX:
                    return paintFragment;
                case BeautyFragment.INDEX:
                    return beautyFragment;
                case BrightnessFragment.INDEX:
                    return brightnessFragment;
                case SaturationFragment.INDEX:
                    return saturationFragment;
            }
            return MainMenuFragment.newInstance();
        }

        @Override
        public int getCount() {
            return 10;
        }
    }

    private final class SaveBtnClick implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (numberOfOperations == 0) {
                onSaveTaskDone();
            } else {
                doSaveImage();
            }
        }
    }

    private final class ApplyBtnClick implements OnClickListener {
        @Override
        public void onClick(View v) {
            switch (mode) {
                case MODE_STICKERS:
                    stickerFragment.applyStickers();
                    break;
                case MODE_FILTER:
                    filterListFragment.applyFilterImage();
                    break;
                case MODE_CROP:
                    cropFragment.applyCropImage();
                    break;
                case MODE_ROTATE:
                    rotateFragment.applyRotateImage();
                    break;
                case MODE_TEXT:
                    addTextFragment.applyTextImage();
                    break;
                case MODE_PAINT:
                    paintFragment.savePaintImage();
                    break;
                case MODE_BEAUTY:
                    beautyFragment.applyBeauty();
                    break;
                case MODE_BRIGHTNESS:
                    brightnessFragment.applyBrightness();
                    break;
                case MODE_SATURATION:
                    saturationFragment.applySaturation();
                    break;
                default:
                    break;
            }
        }
    }
}
