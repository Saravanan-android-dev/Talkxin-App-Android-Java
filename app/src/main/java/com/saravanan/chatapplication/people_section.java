package com.saravanan.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class people_section extends AppCompatActivity {

    ListView listPeoples;
    EditText edtsearch;

    ArrayList<String> usersList;
    ArrayList<String> uidList;
    ArrayList<String> imageList;

    ArrayAdapter<String> adapter;

    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_people_section);
        FirebaseFirestore.getInstance().clearPersistence();


        listPeoples = findViewById(R.id.listPeoples);
        edtsearch = findViewById(R.id.edtSearch);

        usersList = new ArrayList<>();
        uidList = new ArrayList<>();
        imageList = new ArrayList<>();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new ArrayAdapter<String>(
                this,
                R.layout.singlerowuser,
                R.id.txtUsername,
                usersList
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                View view = super.getView(position, convertView, parent);

                ImageView imgUserDp = view.findViewById(R.id.imgProfile);

                String imageUrl = imageList.get(position);

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(people_section.this)
                            .load(imageUrl)
                            .placeholder(R.drawable.logo)
                            .into(imgUserDp);
                } else {
                    imgUserDp.setImageResource(R.drawable.logo);
                }

                return view;
            }
        };

        listPeoples.setAdapter(adapter);

        // Open chat
        listPeoples.setOnItemClickListener((parent, view, position, id) -> {

            String receiverUid = uidList.get(position);
            String receiverUsername = usersList.get(position);

            Intent intent = new Intent(people_section.this, Chat_activity.class);
            intent.putExtra("receiverUid", receiverUid);
            intent.putExtra("receiverUsername", receiverUsername);
            startActivity(intent);
        });

        // Search
        edtsearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadUsers();
    }

    // ✅ LOAD USERS (DELETED USER WILL NOT SHOW)
    private void loadUsers() {

        String myUid = auth.getCurrentUser().getUid();

        db.collection("Users")   // SAME collection as delete
                .get()
                .addOnSuccessListener(snapshot -> {

                    // 🔥 MOST IMPORTANT LINES
                    usersList.clear();
                    uidList.clear();
                    imageList.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        // skip if document does not exist
                        if (!doc.exists()) continue;

                        String uid = doc.getId();

                        // skip current user
                        if (uid.equals(myUid)) continue;

                        String username = doc.getString("username");
                        String profileImage = doc.getString("profileImage");

                        if (username != null) {
                            usersList.add(username);
                            uidList.add(uid);
                            imageList.add(profileImage);
                        }
                    }

                    adapter.notifyDataSetChanged(); // 🔥 MUST
                });
    }
}
