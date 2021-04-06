package com.example.kotlinchat

import android.text.Editable
import android.text.TextWatcher
import android.widget.Button

class MyButtonObserver(aButton: Button) : TextWatcher {

    private var button = aButton

    override fun afterTextChanged(s: Editable?) {}

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        button.isEnabled = s.toString().trim().isNotEmpty()
    }
}