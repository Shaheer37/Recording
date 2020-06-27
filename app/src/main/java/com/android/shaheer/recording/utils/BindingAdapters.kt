package com.android.shaheer.recording.utils

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.cardview.widget.CardView
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter
import com.android.shaheer.recording.R

@BindingAdapter("app:isSelected")
fun setCardElevation(cardView: CardView, isSelected: Boolean) {
    if(isSelected) cardView.cardElevation = cardView.resources.getDimensionPixelSize(R.dimen.record_cv_selected_elevation).toFloat()
    else cardView.cardElevation = cardView.resources.getDimensionPixelSize(R.dimen.record_cv_elevation).toFloat()
}

@BindingAdapter("app:isSelected")
fun setRowElevation(viewGroup: ViewGroup, isSelected: Boolean) {
    if(isSelected) viewGroup.background = viewGroup.resources.getDrawable(R.drawable.bg_row_record_selected, null)
    else viewGroup.background = viewGroup.resources.getDrawable(R.drawable.bg_row_record, null)
}

@BindingAdapter("app:isVisible")
fun toggleVisibility(view: View, isVisible:Boolean?){
    view.visibility = if(isVisible == true) View.VISIBLE
        else View.INVISIBLE
}