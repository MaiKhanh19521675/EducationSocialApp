package com.example.kmapp.fragments;

import static android.app.Activity.RESULT_OK;

import static com.example.kmapp.MainActivity.IS_SEARCHED_USER;
import static com.example.kmapp.MainActivity.USER_ID;
import static com.example.kmapp.utils.Constants.PREF_DIRECTORY;
import static com.example.kmapp.utils.Constants.PREF_NAME;
import static com.example.kmapp.utils.Constants.PREF_STORED;
import static com.example.kmapp.utils.Constants.PREF_URL;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.kmapp.MainActivity;
import com.example.kmapp.R;
import com.example.kmapp.ReplaceActivity;
import com.example.kmapp.models.PostImage;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileFragment extends Fragment {


    private Toolbar toolbar;
    private TextView tvName, tvToolbarName, tvStatus, tvFollowingCount, tvFollowersCount, tvPostCount;
    private CircleImageView cimgProfileImage;
    private Button followBtn, btnStartChat;
    private RecyclerView rcvProfile;
    private LinearLayout countLayout;
    private ImageButton editProfileBtn;
    private List<String> followersList, followingList, followingList_2;
    private FirestoreRecyclerAdapter<PostImage, PostImageHolder> adapter;
    private FirebaseUser user;
    private DocumentReference userRef,myRef;
    private FirebaseAuth auth;
    private boolean isMyProfile = true;
    private int count;
    private  String userUID;
    private   boolean isFollowed;
//    private FirestoreRecyclerAdapter<PostImage, PostImageHolder> adapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
        myRef = FirebaseFirestore.getInstance().collection("Users")
                .document(user.getUid());


        if (IS_SEARCHED_USER){
            isMyProfile = false;
            userUID = USER_ID;

            loadData();
        } else {
            isMyProfile = true;
            userUID = user.getUid();
        }


        if (isMyProfile) {
            editProfileBtn.setVisibility(View.VISIBLE);
            followBtn.setVisibility(View.GONE);
            countLayout.setVisibility(View.VISIBLE);

            //Hide chat btn
            btnStartChat.setVisibility(View.GONE);

        } else {
            editProfileBtn.setVisibility(View.GONE);
            followBtn.setVisibility(View.VISIBLE);
//            countLayout.setVisibility(View.GONE);
        }
        userRef = FirebaseFirestore.getInstance().collection("Users").document(userUID);

        loadBasicData();
        rcvProfile.setHasFixedSize(true);
        rcvProfile.setLayoutManager(new GridLayoutManager(getContext(), 3));

        loadPostImages();

        rcvProfile.setAdapter(adapter);

        clickListener();
    }

    private void loadData() {

        myRef.addSnapshotListener((value, error) -> {

            if (error != null) {
                Log.e("Tag_b", error.getMessage());
                return;
            }

            if (value == null || !value.exists()) {
                return;
            }

            followingList_2 = (List<String>) value.get("following");


        });

    }


//duyệt
    private void loadPostImages(){
        String Uid = user.getUid();
        DocumentReference reference = FirebaseFirestore.getInstance().collection("Users").document(userUID);

        Query query = reference.collection("Post Images");

        FirestoreRecyclerOptions<PostImage> options = new FirestoreRecyclerOptions.Builder<PostImage>()
                .setQuery(query, PostImage.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<PostImage, PostImageHolder>(options){

            @NonNull
            @Override
            public PostImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.profile_image_items, parent, false);
                return new PostImageHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull PostImageHolder holder, int position, @NonNull PostImage model) {
                Glide.with(holder.itemView.getContext().getApplicationContext())
                        .load(model.getImageUrl())
                        .timeout(6500)
                        .into(holder.imageView);
                count = getItemCount();
                tvPostCount.setText("" + count);
            }

            @Override
            public int getItemCount() {
                return super.getItemCount();
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    private void init(View view) {

        toolbar = view.findViewById(R.id.toolbar);
//        assert getActivity() != null;
//        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        tvName = view.findViewById(R.id.nameTv);
        tvStatus = view.findViewById(R.id.statusTV);
        tvToolbarName = view.findViewById(R.id.toolbarNameTV);
        tvFollowersCount = view.findViewById(R.id.followersCountTv);
        tvFollowingCount = view.findViewById(R.id.followingCountTv);
        tvPostCount = view.findViewById(R.id.postCountTv);
        cimgProfileImage = view.findViewById(R.id.profileImage);
        followBtn = view.findViewById(R.id.followBtn);
        rcvProfile = view.findViewById(R.id.recyclerView);
        countLayout = view.findViewById(R.id.countLayout);
        editProfileBtn  = view.findViewById(R.id.edit_profileImage);
        btnStartChat = view.findViewById(R.id.startChatBtn);


        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();



    }

    private void clickListener() {


        followBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFollowed) {
                    followersList.remove(user.getUid()); //opposite user

                    followingList_2.remove(userUID); //us

                    final Map<String, Object> map_2 = new HashMap<>();
                    map_2.put("following", followingList_2);
                }else{

                    followersList.add(user.getUid());

                    followingList_2.add(userUID); //us

                    final Map<String, Object> map_2 = new HashMap<>();
                    map_2.put("following", followingList_2);
                }
            }
        });



//        followBtn.setOnClickListener(v -> {
//
//            if (isFollowed) {
//
//                followersList.remove(user.getUid()); //opposite user
//
//                followingList_2.remove(userUID); //us
//
//                final Map<String, Object> map_2 = new HashMap<>();
//                map_2.put("following", followingList_2);
//
//
//                Map<String, Object> map = new HashMap<>();
//                map.put("followers", followersList);
//
//
//                userRef.update(map).addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        followBtn.setText("Follow");
//
//                        myRef.update(map_2).addOnCompleteListener(task1 -> {
//                            if (task1.isSuccessful()) {
//                                Toast.makeText(getContext(), "UnFollowed", Toast.LENGTH_SHORT).show();
//                            } else {
//                                assert task1.getException() != null;
//                                Log.e("Tag_3", task1.getException().getMessage());
//                            }
//                        });
//
//                    } else {
//                        assert task.getException() != null;
//                        Log.e("Tag", "" + task.getException().getMessage());
//                    }
//                });
//
//
//            } else {
//
//                createNotification();
//
//                followersList.add(user.getUid()); //opposite user
//
//                followingList_2.add(userUID); //us
//
//                final Map<String, Object> map_2 = new HashMap<>();
//                map_2.put("following", followingList_2);
//
//
//                Map<String, Object> map = new HashMap<>();
//                map.put("followers", followersList);
//
//
//                userRef.update(map).addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        followBtn.setText("UnFollow");
//
//                        myRef.update(map_2).addOnCompleteListener(task12 -> {
//                            if (task12.isSuccessful()) {
//                                Toast.makeText(getContext(), "Followed", Toast.LENGTH_SHORT).show();
//                            } else {
//                                assert task12.getException() != null;
//                                Log.e("tag_3_1", task12.getException().getMessage());
//                            }
//                        });
//
//
//                    } else {
//                        assert task.getException() != null;
//                        Log.e("Tag", "" + task.getException().getMessage());
//                    }
//                });
//
//
//            }
//
//        });

        assert getContext() != null;

        editProfileBtn.setOnClickListener(v -> CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .start(getContext(), ProfileFragment.this));

        btnStartChat.setOnClickListener(v -> {
            queryChat();
        });

    }

    private void queryChat() {
    }

    private void createNotification() {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (result == null)
                return;

            Uri uri = result.getUri();

            uploadImage(uri);

        }
    }
// duyệt
    private void uploadImage(Uri uri) {

        final StorageReference reference = FirebaseStorage.getInstance().getReference().child("Profile Images");

        reference.putFile(uri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()){
                    reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String imageURL = uri.toString();

                            UserProfileChangeRequest.Builder request = new UserProfileChangeRequest.Builder();
                            request.setPhotoUri(uri);

                            user.updateProfile(request.build());

                            Map<String, Object> map = new HashMap<>();
                            map.put("profileImage", imageURL);

                            FirebaseFirestore.getInstance().collection("Users")
                                    .document(user.getUid())
                                    .update(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful())
                                                Toast.makeText(getContext(),
                                                        "Updated Successful", Toast.LENGTH_SHORT).show();
                                            else {
                                                assert task.getException() != null;
                                                Toast.makeText(getContext(),
                                                        "Error: " + task.getException().getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    });
                } else {
                    assert task.getException() != null;
                    Toast.makeText(getContext(), "Error: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    private void loadBasicData() {
        userRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {

                if (error != null) {
                    Log.e("Tag_0", error.getMessage());
                    return;
                }

                assert value != null;
                if (value.exists()){
                    String name = value.getString("name");
                    String status = value.getString("status");

//                    int followers = value.getLong("followers").intValue();
//                    int following = value.getLong("following").intValue();
                    followersList = (List<String>) value.getDate("followers");
                    followingList = (List<String>) value.getDate("following");

                    final String profileURL = value.getString("profileImage");

                    tvName.setText(name);
                    tvToolbarName.setText(name);
                    tvStatus.setText(status);
                    tvFollowersCount.setText(String.valueOf("" + followersList.size()));
                    tvFollowingCount.setText(String.valueOf("" + followingList.size()));
//                    tvFollowersCount.setText(String.valueOf(followers));
//                    tvFollowersCount.setText(String.valueOf(following));
                    try {
                        Glide.with(getContext().getApplicationContext())
                                .load(profileURL)

                                .placeholder(R.drawable.ic_person)
                                .circleCrop()
                                .listener(new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                        Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                                        storeProfileImage(bitmap, profileURL);
                                        return false;
                                    }
                                })
                                .timeout(6500)
                                .into(cimgProfileImage);

                    }catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (followersList.contains(user.getUid())) {
                        followBtn.setText("UnFollow");
                        isFollowed = true;
                        btnStartChat.setVisibility(View.VISIBLE);


                    } else {
                        isFollowed = false;
                        followBtn.setText("Follow");

                        btnStartChat.setVisibility(View.GONE);

                    }



                }
            }
        });




    }

    private void storeProfileImage(Bitmap bitmap, String url) {
        SharedPreferences preferences = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isStored = preferences.getBoolean(PREF_STORED, false);
        String urlString = preferences.getString(PREF_URL, "");

        SharedPreferences.Editor editor = preferences.edit();

        if (isStored && urlString.equals(url))
            return;

        if (IS_SEARCHED_USER)
            return;

        ContextWrapper contextWrapper = new ContextWrapper(getActivity().getApplicationContext());

        File directory = contextWrapper.getDir("image_data", Context.MODE_PRIVATE);

        if (!directory.exists()) {
            boolean isMade = directory.mkdirs();
            Log.d("Directory", String.valueOf(isMade));
        }


        File path = new File(directory, "profile.png");

        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(path);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {

            try {
                assert outputStream != null;
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        editor.putBoolean(PREF_STORED, true);
        editor.putString(PREF_URL, url);
        editor.putString(PREF_DIRECTORY, directory.getAbsolutePath());
        editor.apply();
    }

    private static class  PostImageHolder extends RecyclerView.ViewHolder{

        private ImageView imageView;


        public PostImageHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }

}


