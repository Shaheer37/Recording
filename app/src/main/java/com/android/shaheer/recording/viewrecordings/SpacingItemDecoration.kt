package com.android.shaheer.recording.viewrecordings

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpacingItemDecoration(private val padding: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = padding*2
            outRect.bottom = padding
        } else if (parent.getChildAdapterPosition(view) == parent.adapter!!.itemCount - 1){
            outRect.top = padding
            outRect.bottom = padding*2
        }else{
            outRect.top = padding
            outRect.bottom = padding
        }
    }
}