package com.example.blooddonationapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class RecipientRegistrationActivity extends AppCompatActivity {

    private TextView backButton;
    private CircleImageView profile_image;
    private TextInputEditText registerFullName;
    private TextInputEditText registerIdNumber;
    private TextInputEditText registerPhoneNumber;
    private TextInputEditText registerEmail;
    private TextInputEditText registerPassword;
    private Spinner bloodGroupSpinner;
    private Button registerButton;
    private Uri resultUri;
    private ProgressDialog loader;

    private FirebaseAuth mAuth;
    private DatabaseReference userDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipient_registration);

        //direct user to gallery to pick an image
        profile_image = findViewById(R.id.profile_image);
        profile_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //to open gallery
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });


        registerFullName = findViewById(R.id.registerFullName);
        registerIdNumber = findViewById(R.id.registerIdNumber);
        registerPhoneNumber = findViewById(R.id.registerPhoneNumber);
        registerEmail = findViewById(R.id.registerEmail);
        registerPassword = findViewById(R.id.registerPassword);
        bloodGroupSpinner = findViewById(R.id.bloodGroupSpinner);

        loader = new ProgressDialog(this);
        mAuth = FirebaseAuth.getInstance();
        registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //verify that user has input all info
                final String email = registerEmail.getText().toString().trim();//trim is used to remove white spaces
                final String password = registerPassword.getText().toString().trim();
                final String fullName = registerFullName.getText().toString().trim();
                final String idNumber = registerIdNumber.getText().toString().trim();
                final  String phoneNumber = registerPhoneNumber.getText().toString().trim();
                final String bloodGroup = bloodGroupSpinner.getSelectedItem().toString();


                //ensuring that values are not empty
                if (TextUtils.isEmpty(email)){
                    registerEmail.setError("Email is required");
                    return;
                }

                if (TextUtils.isEmpty(password)){
                    registerEmail.setError("Password is required");
                    return;
                }

                if (TextUtils.isEmpty(fullName)){
                    registerEmail.setError("Full Name is required");
                    return;
                }

                if (TextUtils.isEmpty(idNumber)){
                    registerEmail.setError("ID is required");
                    return;
                }

                if (TextUtils.isEmpty(phoneNumber)){
                    registerEmail.setError("Phone Number is required");
                    return;
                }

                if (bloodGroup.equals("Select your blood group")){
                    Toast.makeText(RecipientRegistrationActivity.this, "Please select BloodGroup", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    loader.setMessage("Registering you");
                    loader.setCanceledOnTouchOutside(false);
                    loader.show();

                    //performing authentication and adding data to firebase
                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()){
                                String error = task.getException().toString();
                                Toast.makeText(RecipientRegistrationActivity.this, "Error "+error, Toast.LENGTH_SHORT).show();
                            } else {
                                String currentUserId = mAuth.getCurrentUser().getUid();
                                userDatabaseRef = FirebaseDatabase.getInstance().getReference().child("users").child(currentUserId);

                                HashMap userInfo = new HashMap();
                                userInfo.put("id", currentUserId);
                                userInfo.put("name", fullName);
                                userInfo.put("email", email);
                                userInfo.put("idNumber", idNumber);
                                userInfo.put("phoneNo", phoneNumber);
                                userInfo.put("bloodGroup", bloodGroup);
                                userInfo.put("type", "recipient");
                                userInfo.put("search", "recipient"+bloodGroup);

                                userDatabaseRef.updateChildren(userInfo).addOnCompleteListener(new OnCompleteListener() {
                                    @Override
                                    public void onComplete(@NonNull Task task) {
                                        if (task.isSuccessful()){
                                            Toast.makeText(RecipientRegistrationActivity.this, "Data set successful", Toast.LENGTH_SHORT).show();
                                        }else {
                                            Toast.makeText(RecipientRegistrationActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                                        }
                                        finish();
                                    }
                                });

                                //working on image -> getting as a link and storing in firebase db
                                if (resultUri != null){
                                    final StorageReference filepath = FirebaseStorage.getInstance().getReference().child("profile images").child(currentUserId);
                                    Bitmap bitmap = null;

                                    try {
                                        bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
                                    }catch (IOException e){
                                        e.printStackTrace();
                                    }

                                    //takes image picked -> compresses it -> tries to upload it to db
                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 20, byteArrayOutputStream);
                                    byte[] data = byteArrayOutputStream.toByteArray();
                                    UploadTask uploadTask = filepath.putBytes(data);

                                    //incase it fails to upload the image
                                    uploadTask.addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(RecipientRegistrationActivity.this, "Image Upload Failed", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    //if it succeeds
                                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                            //getting the Image Url from cloud that is stord in firebase db
                                            if (taskSnapshot.getMetadata() != null && taskSnapshot.getMetadata().getReference() != null){
                                                Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                                                result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                    @Override
                                                    public void onSuccess(Uri uri) {
                                                        String imageUrl = uri.toString();
                                                        Map newImageMap = new HashMap();
                                                        newImageMap.put("profilepictureurl", imageUrl);

                                                        userDatabaseRef.updateChildren(newImageMap).addOnCompleteListener(new OnCompleteListener() {
                                                            @Override
                                                            public void onComplete(@NonNull Task task) {
                                                                if (task.isSuccessful()){
                                                                    Toast.makeText(RecipientRegistrationActivity.this, "Image url added to db successfully", Toast.LENGTH_SHORT).show();
                                                                } else {
                                                                    Toast.makeText(RecipientRegistrationActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });
                                                        finish();
                                                    }
                                                });
                                            }
                                        }
                                    });

                                    Intent intent = new Intent(RecipientRegistrationActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                    loader.dismiss();
                                }

                            }
                        }
                    });

                }


            }
        });


        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecipientRegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null){
            //display image picked from gallery in the app
            resultUri = data.getData();
            profile_image.setImageURI(resultUri);
        }
    }

}