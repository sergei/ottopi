package com.santacruzinstruments.ottopi.ui.race_setup

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment
import com.santacruzinstruments.ottopi.R

/**
 * Time Picker that shows a Dialog to select
 * hours, minutes and seconds. You can choose
 * to use only minutes and seconds by setting
 * to false the property "includeHours".
 */
class MyTimePicker : DialogFragment() {

    private lateinit var timePickerLayout: View
    private lateinit var hourPicker: NumberPicker
    private lateinit var minPicker: NumberPicker
    private lateinit var secPicker: NumberPicker

    private var onTimeSetOption:
            (hour: Int, minute: Int, second: Int) -> Unit = {_, _, _ -> }
    private var timeSetText: String = "Ok"

    private var onCancelOption: () -> Unit = {}
    private var cancelText: String = "Cancel"

    /**
     * Which value will appear a the start of
     * the Dialog for the Hour picker.
     * Default value is 0.
     */
    var initialHour: Int = 0
    /**
     * Which value will appear a the start of
     * the Dialog for the Minute picker.
     * Default value is 0.
     */
    var initialMinute: Int = 0
    /**
     * Which value will appear a the start of
     * the Dialog for the Second picker.
     * Default value is 0.
     */
    var initialSeconds: Int = 0

    /**
     * Max value for the Hour picker.
     * Default value is 23.
     */
    private var maxValueHour: Int = 23
    /**
     * Max value for the Minute picker.
     * Default value is 59.
     */
    private var maxValueMinute: Int = 59
    /**
     * Max value for the Second picker.
     * Default value is 59.
     */
    private var maxValueSeconds: Int = 59

    /**
     * Min value for the Hour picker.
     * Default value is 0.
     */
    private var minValueHour: Int = 0
    /**
     * Min value for the Minute picker.
     * Default value is 0.
     */
    private var minValueMinute: Int = 0
    /**
     * Min value for the Second picker.
     * Default value is 0.
     */
    private var minValueSecond: Int = 0

    /**
     * Default value is true.
     * If set to false the hour picker is not
     * visible in the Dialog
     */
    private var includeHours: Boolean = true

    private var title: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)

            timePickerLayout = requireActivity()
                    .layoutInflater.inflate(R.layout.my_time_picker_content, null)

            setupTimePickerLayout()

            builder.setView(timePickerLayout)

            title?.let { title ->
                builder.setTitle(title)
            }
            builder.setPositiveButton(timeSetText) { _, _ ->
                var hour = hourPicker.value
                if (!includeHours) hour = 0
                onTimeSetOption(hour, minPicker.value, secPicker.value)
            }
                    .setNegativeButton(cancelText) { _, _ ->
                        onCancelOption
                    }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    /**
     * Set the title displayed in the Dialog
     */
    fun setTitle(title: String) {
        this.title = title
    }

    /**
     * Set a listener to be invoked when the Set Time button of the dialog is pressed.
     * If have set includeHours to false, the hour parameter here will be always 0.
     * @param text text to display in the Set Time button.
     */
    fun setOnTimeSetOption (text: String, onTimeSet: (hour: Int, minute: Int, second: Int) -> Unit) {
        onTimeSetOption = onTimeSet
        timeSetText = text
    }

    /**
     * Set a listener to be invoked when the Cancel button of the dialog is pressed.
     * @param text text to display in the Cancel button.
     */
    fun setOnCancelOption (text: String, onCancelOption: () -> Unit) {
        this.onCancelOption = onCancelOption
        cancelText = text
    }

    private fun setupTimePickerLayout() {
        bindViews()

        setupMaxValues()
        setupMinValues()
        setupInitialValues()

        if (!includeHours) {
            timePickerLayout.findViewById<LinearLayout>(R.id.hours_container)
                    .visibility = View.GONE
        }
    }

    private fun bindViews() {
        hourPicker = timePickerLayout.findViewById(R.id.hours)
        minPicker = timePickerLayout.findViewById(R.id.minutes)
        secPicker = timePickerLayout.findViewById(R.id.seconds)
    }

    private fun setupMaxValues () {
        hourPicker.maxValue = maxValueHour
        minPicker.maxValue = maxValueMinute
        secPicker.maxValue = maxValueSeconds
    }

    private fun setupMinValues () {
        hourPicker.minValue = minValueHour
        minPicker.minValue = minValueMinute
        secPicker.minValue = minValueSecond
    }

    private fun setupInitialValues () {
        hourPicker.value = initialHour
        minPicker.value = initialMinute
        secPicker.value = initialSeconds
    }
}