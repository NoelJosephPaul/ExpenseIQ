package com.example.expenseiq

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import java.util.regex.Pattern
import java.text.SimpleDateFormat
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras
        if (bundle != null) {
            val messages = bundle.get("pdus") as Array<*>
            for (message in messages) {
                val smsMessage = SmsMessage.createFromPdu(message as ByteArray)
                val messageBody = smsMessage.messageBody
                val sender = smsMessage.displayOriginatingAddress

                // Check if the message contains debit information
                if (messageBody.contains("debited", ignoreCase = true)) {
                    // Decide extraction method based on sender containing "UPI"
                    val (amount, date) = if (sender.contains("upi", ignoreCase = true)) {
                        extractForUPIMessage(messageBody)
                    } else {
                        extractForBankMessage(messageBody)
                    }

                    // Insert into pending_payments table
                    val db = AppDatabase.getDatabase(context)
                    CoroutineScope(Dispatchers.IO).launch {
                        val pendingPayment = PendingPayment(amount = amount, date = date)
                        db.pendingPaymentDao().insertPendingPayment(pendingPayment)
                    }
                }
            }
        }
    }

    // Extraction for UPI format messages
    private fun extractForUPIMessage(message: String): Pair<Float, String> {
        val amountPattern = Pattern.compile("debited by (\\d+(?:\\.\\d{1,2})?)") // Matches "debited by" followed by amount
        val datePattern = Pattern.compile("\\b(\\d{1,2}[A-Za-z]{3}\\d{2})\\b") // Matches "DDMonYY" format

        val amountMatcher = amountPattern.matcher(message)
        val dateMatcher = datePattern.matcher(message)

        val amount = if (amountMatcher.find()) {
            amountMatcher.group(1).toFloat()
        } else {
            0f
        }

        val date = if (dateMatcher.find()) {
            val rawDate = dateMatcher.group(1)
            formatDateToDDMMYYYY(rawDate)
        } else {
            "Unknown"
        }

        return Pair(amount, date)
    }

    // Extraction for Bank format messages
    private fun extractForBankMessage(message: String): Pair<Float, String> {
        val amountPattern = Pattern.compile("INR\\s?([\\d,]+\\.\\d{2})") // Matches "INR" followed by amount
        val datePattern = Pattern.compile("\\b(\\d{1,2}/\\d{1,2}/\\d{2,4})\\b") // Matches "DD/MM/YY" or "DD/MM/YYYY"

        val amountMatcher = amountPattern.matcher(message)
        val dateMatcher = datePattern.matcher(message)

        val amount = if (amountMatcher.find()) {
            amountMatcher.group(1).replace(",", "").toFloat()
        } else {
            0f
        }

        val date = if (dateMatcher.find()) {
            val rawDate = dateMatcher.group(1)
            // Check if date is in DD/MM/YY format (2-digit year)
            if (rawDate.matches(Regex("\\d{1,2}/\\d{1,2}/\\d{2}"))) {
                formatDateToDDMMYYYYFromDDMMYY(rawDate)
            } else {
                rawDate // Already in DD/MM/YYYY format
            }
        } else {
            "Unknown"
        }

        return Pair(amount, date)
    }

    private fun formatDateToDDMMYYYY(rawDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("ddMMMyy", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            val date = inputFormat.parse(rawDate)
            outputFormat.format(date)
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Date parsing failed", e)
            "Unknown"
        }
    }

    private fun formatDateToDDMMYYYYFromDDMMYY(rawDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd/MM/yy", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            val date = inputFormat.parse(rawDate)
            outputFormat.format(date)
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Date parsing failed", e)
            "Unknown"
        }
    }
}
