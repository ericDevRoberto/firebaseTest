package com.example.kotlinchat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlinchat.databinding.ActivityMainBinding
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

private const val TAG = "MainActivity"
private const val MESSAGES_CHILD = "messages";
private const val ANONYMOUS = "anonymous"
private const val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"
private const val REQUEST_IMAGE = 2

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var signInClient: GoogleSignInClient
    private lateinit var dataBase: FirebaseDatabase
    private lateinit var firebaseAdapter: FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Binding
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        //Initiation Firebase
        firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth.currentUser == null) {
            startActivity(Intent(this, SignActivity::class.java))
            finish()
        }

        //take login
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        signInClient = GoogleSignIn.getClient(this, gso)

        //Initialize Realtime Database
        dataBase = FirebaseDatabase.getInstance()

        val messagesRef = dataBase.reference.child(MESSAGES_CHILD)

        // The FirebaseRecyclerAdapter class comes from the FirebaseUI library

        val options = FirebaseRecyclerOptions.Builder<FriendlyMessage>()
            .setQuery(messagesRef, FriendlyMessage::class.java)
            .build()


        firebaseAdapter =
            object : FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>(options) {

                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
                ): MessageViewHolder {
                    val inflater = LayoutInflater.from(parent.context)

                    return MessageViewHolder(inflater.inflate(R.layout.item_message, parent, false))
                }

                override fun onBindViewHolder(
                    holder: MessageViewHolder,
                    position: Int,
                    model: FriendlyMessage
                ) {
                    binding.progressBar.visibility = ProgressBar.INVISIBLE
                    holder.bindMessage(model)
                }
            }

        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        binding.messagesRecycleView.layoutManager = linearLayoutManager
        binding.messagesRecycleView.adapter = firebaseAdapter

        // Scroll down when a new message arrives
        // See MyScrollToBottomObserver.java for details
        firebaseAdapter.registerAdapterDataObserver(
            MyScrollToBottomObserver(
                aRecycler = binding.messagesRecycleView,
                aAdapter = firebaseAdapter,
                aManager = linearLayoutManager
            )
        )

        binding.messageEditText.addTextChangedListener(MyButtonObserver(binding.sendButton))

        binding.sendButton.setOnClickListener() {
            val friendlyMessage = FriendlyMessage(
                text = binding.messageEditText.text.toString(),
                name = getUserName(),
                photoUrl = getUserPhotUrl(),
                imageUrl = null
            )

            dataBase.reference.child(MESSAGES_CHILD).push().setValue(friendlyMessage)
            binding.messageEditText.setText("")
        }

        // When the image button is clicked, launch the image picker
        binding.addMessageImageView.setOnClickListener() {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.setType("image/*")
            startActivityForResult(intent, REQUEST_IMAGE)
        }
    }

    override fun onPause() {
        firebaseAdapter.stopListening()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        firebaseAdapter.startListening()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_sign_out -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(
            TAG,
            "onActivityResult: requestCode=$requestCode, resultCode=$resultCode"
        )

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val uri = data.data
                Log.d(TAG, "Uri: " + uri.toString())

                val user = firebaseAuth.currentUser
                val tempMessage =
                    FriendlyMessage(null, getUserName(), getUserPhotUrl(), LOADING_IMAGE_URL)

                dataBase.reference.child(MESSAGES_CHILD).push()
                    .setValue(tempMessage) { databaseError: DatabaseError?, databaseReference: DatabaseReference ->

                        if (databaseError != null)
                            Log.w(
                                TAG, "Unable to write message to database.",
                                databaseError.toException()
                            )
                        var keyNotNull = String()
                        val key = databaseReference.key
                        key?.let { keyNotNull = it }

                        var lastPathSegment = String()
                        val uriLastPastSegment = uri?.lastPathSegment
                        uriLastPastSegment?.let { lastPathSegment = it }

                        val storageReference = FirebaseStorage.getInstance()
                            .getReference(user.uid)
                            .child(keyNotNull)
                            .child(lastPathSegment)

                        putImageInStorage(storageReference,uri!!, keyNotNull)
                    }
            }
        }
    }

    private fun putImageInStorage(storageReference: StorageReference, uri: Uri, key: String) {

        storageReference.putFile(uri)
            .addOnSuccessListener(this) { taskSnapshot ->

                taskSnapshot.metadata!!.reference?.downloadUrl
                    ?.addOnSuccessListener {
                        val friendlyMessage =
                            FriendlyMessage(
                                text = null,
                                name = getUserName(),
                                photoUrl = getUserPhotUrl(),
                                imageUrl = uri.toString()
                            )
                        dataBase.reference.child(MESSAGES_CHILD).child(key).setValue(friendlyMessage)
                    }
            }
            .addOnFailureListener(this) {
                Log.w(TAG, "Image upload task was not successful.", it)
            }
    }

    private fun signOut() {
        firebaseAuth.signOut()
        signInClient.signOut()
        startActivity(Intent(this, SignActivity::class.java))
        finish()
    }

    private fun getUserName(): String {
        val user = firebaseAuth.currentUser

        return if (user != null)
            user.displayName
        else
            ANONYMOUS
    }

    private fun getUserPhotUrl(): String {
        val user = firebaseAuth.currentUser

        return if (user != null && user.photoUrl != null)
            user.photoUrl.toString()
        else
            String()
    }
}