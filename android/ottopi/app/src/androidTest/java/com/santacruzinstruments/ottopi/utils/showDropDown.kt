package com.santacruzinstruments.ottopi.utils

import android.view.View
import android.widget.AutoCompleteTextView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

// Somewhere else in your code
fun showDropDown(): ViewAction =
        object : ViewAction {
            override fun getDescription(): String = "Shows the dropdown menu of an AutoCompleteTextView"

            override fun getConstraints(): Matcher<View> = allOf(
                    isEnabled(), isAssignableFrom(AutoCompleteTextView::class.java)
            )

            override fun perform(uiController: UiController, view: View) {
                val autoCompleteTextView = view as AutoCompleteTextView
                autoCompleteTextView.showDropDown()
                uiController.loopMainThreadUntilIdle()
            }
        }
