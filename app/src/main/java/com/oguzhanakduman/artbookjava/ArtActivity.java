package com.oguzhanakduman.artbookjava;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.oguzhanakduman.artbookjava.databinding.ActivityArtBinding;

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {
    private ActivityArtBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    Bitmap selectedImage;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

        registerLauncher();

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");
        if(info.equals("new")){
            // Yeni eklenecek olana gösterilecek sayfa
            binding.artNameEditText.setText("");
            binding.artistNameEditText.setText("");
            binding.yearEditText.setText("");
            binding.saveButton.setVisibility(View.VISIBLE);
            binding.selectImageView.setImageResource(R.drawable.select_image);
        } else {
            // Kayıtlı olan öğelerden gösterilecek olan sayfa
            int artId = intent.getIntExtra("artId",1);
            binding.saveButton.setVisibility(View.INVISIBLE);

            try {
                // SQLiteStatement ile aynı işi tek satırda yapmamızı sağladı.
                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?",new String[] {String.valueOf(artId)});
                int artNameIx = cursor.getColumnIndex("artname");
                int artistNameIx = cursor.getColumnIndex("artistname");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){
                    binding.artNameEditText.setText(cursor.getString(artNameIx));
                    binding.artistNameEditText.setText(cursor.getString(artistNameIx));
                    binding.yearEditText.setText(cursor.getString(yearIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    binding.selectImageView.setImageBitmap(bitmap);

                }

                cursor.close();

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void saveButton(View view){
        String artName = binding.artNameEditText.getText().toString();
        String artistName = binding.artistNameEditText.getText().toString();
        String year = binding.yearEditText.getText().toString();

        Bitmap smallImage = makeSmallerImage(selectedImage,300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {

            database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)");

            String sqlString = "INSERT INTO arts (artname, artistname, year, image) VALUES (?, ?, ?, ?)";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,artName);
            sqLiteStatement.bindString(2,artistName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();


        } catch (Exception e) {

        }

        Intent intent = new Intent(ArtActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        //finish();
    }

    public Bitmap makeSmallerImage(Bitmap image, int maximumSize){
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if(bitmapRatio > 1){
            // Yatay (landscpae) image
            width = maximumSize;
            height = (int) (width / bitmapRatio);
        } else{
            // Dikey (portrait) image
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }
        return image.createScaledBitmap(image,width,height,true);
    }

    public void selectImage(View view){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            // Android 33+
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_MEDIA_IMAGES)) {
                    Snackbar.make(view,"Galeri Erişimi İçin İzin Vermelisiniz.",Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // İzin İstenecek
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                        }
                    }).show();
                }else {
                    // İzin İstenecek
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                }
            } else {
                // Galeriye Gidilmiş
                Intent intentToGallery = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(intentToGallery);
            }
        }else{
            // Android 32 ve altı
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Snackbar.make(view,"Galeri Erişimi İçin İzin Vermelisiniz.",Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // İzin İstenecek
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                        }
                    }).show();
                }else {
                    // İzin İstenecek
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            } else {
                // Galeriye Gidilmiş
                Intent intentToGallery = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(intentToGallery);
            }
        }


    }

    public void registerLauncher(){
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode() == Activity.RESULT_OK){
                    Intent intentFromResult = result.getData();
                    if(intentFromResult != null){
                        Uri imageData = intentFromResult.getData();
                        try {
                            ImageDecoder.Source source = ImageDecoder.createSource(ArtActivity.this.getContentResolver(),imageData);
                            selectedImage = ImageDecoder.decodeBitmap(source);
                            binding.selectImageView.setImageBitmap(selectedImage);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    // İzin Verildi
                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);
                }else {
                    // İzin Verilmedi
                    Toast.makeText(ArtActivity.this,"İzin Gerekli",Toast.LENGTH_LONG);
                }
            }
        });
    }
}