package com.example.kotlinchat

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MyScrollToBottomObserver(aRecycler: RecyclerView, aAdapter: RecyclerView.Adapter<*>, aManager: LinearLayoutManager) :RecyclerView.AdapterDataObserver(){

    private var recycler: RecyclerView = aRecycler
    private var adapter: RecyclerView.Adapter<*> = aAdapter
    private var manager: LinearLayoutManager = aManager


    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        super.onItemRangeInserted(positionStart, itemCount)

        val count = adapter.itemCount
        val lastVisiblePosition = manager.findLastCompletelyVisibleItemPosition()

        val loading = lastVisiblePosition == -1

        val atBottom = positionStart >= (count - 1) && lastVisiblePosition == (positionStart - 1)

        if (loading || atBottom)
            recycler.scrollToPosition(positionStart)
    }
}