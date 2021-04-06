package com.example.kotlinchat

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlinchat.databinding.ItemMessageBinding
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

private const val TAG = "MessageViewHolder"

class MessageViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

    var messageTextView: TextView
    var messageImageView: ImageView
    var messegerTextView: TextView
    var messengerImageView: CircleImageView

    init {
        messageTextView = itemView.findViewById(R.id.messageTextView) as TextView
        messageImageView = itemView.findViewById(R.id.messageImageView) as ImageView
        messengerImageView = itemView.findViewById(R.id.messengerImageView) as CircleImageView
        messegerTextView = itemView.findViewById(R.id.messengerTextView) as TextView

    }

    fun bindMessage(friendlyMessage: FriendlyMessage) {

        if (friendlyMessage.text != null) {

            messageTextView.text = friendlyMessage.text
            messageTextView.visibility = TextView.VISIBLE
            messageImageView.visibility = ImageView.GONE

        } else if (friendlyMessage.imageUrl != null) {

            val imageUrl: String = friendlyMessage.imageUrl.toString()

            if (imageUrl.startsWith("gs://")) {

                val storageReference = FirebaseStorage.getInstance()
                    .getReference(imageUrl)

                storageReference.downloadUrl
                    .addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()

                        Glide.with(messageImageView.context)
                            .load(downloadUrl)
                            .into(messageImageView)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Getting download url was not successful.", e)
                    }
            } else {
                Glide.with(messageImageView.context)
                    .load(friendlyMessage.imageUrl)
                    .into(messageImageView)
            }
            messageImageView.visibility = ImageView.VISIBLE
            messageTextView.visibility = TextView.GONE
        }
    }
}