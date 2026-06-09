package com.example.mycompose.calorietrackerscreen.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mycompose.calorietrackerscreen.ApiKey
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.log10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, isPremium: Boolean) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()
    
    val bgColor = Color(0xFF15151E)
    val cardColor = Color(0xFF222232)
    val primaryAccent = Color(0xFFA78BFA)
    val premiumGold = Color(0xFFFBBF24)
    val textPrimary = Color(0xFFF3F4F6)
    val textSecondary = Color(0xFF9CA3AF)
    val dividerColor = textSecondary.copy(alpha = 0.15f)
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Ημερήσιο", "Εβδομάδα PRO")
    
    val calendar = java.util.Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)) }
    
    var pastDayData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var pastMeals by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    
    var isUserPremium by remember { mutableStateOf(isPremium) }
    var userName by remember { mutableStateOf("") }
    
    var userTargetCals by remember { mutableStateOf(1800) }
    var startFatPercentage by remember { mutableStateOf("0") }
    var startWeight by remember { mutableStateOf("0") }
    var targetFatPercentage by remember { mutableStateOf("15") }
    var userGoal by remember { mutableStateOf("Απώλεια Λίπους") }
    var gender by remember { mutableStateOf("Άνδρας") }
    var height by remember { mutableStateOf("0") }

    var weeklyCals by remember { mutableStateOf(List(7) { 0 }) }
    var weeklyLabels by remember { mutableStateOf(List(7) { "" }) }
    var weeklyVitamins by remember { mutableStateOf<List<String>>(emptyList()) }
    var isWeeklyLoading by remember { mutableStateOf(true) }

    val datePickerDialog = android.app.DatePickerDialog(
        context, { _, year, month, dayOfMonth -> selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth) },
        calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc != null && doc.exists()) {
                isUserPremium = doc.getBoolean("isPremium") ?: false
                userName = doc.getString("name") ?: ""
                userTargetCals = doc.getLong("targetCals")?.toInt() ?: 1800
                startFatPercentage = doc.getString("currentFatPercentage") ?: "0"
                startWeight = doc.getString("weight") ?: "0"
                targetFatPercentage = doc.getString("targetFatPercentage") ?: "15"
                userGoal = doc.getString("userGoal") ?: "Απώλεια Λίπους"
                gender = doc.getString("gender") ?: "Άνδρας"
                height = doc.getString("height") ?: "0"
            }
        }
    }

    LaunchedEffect(selectedDate) {
        db.collection("users").document(uid).collection("daily_logs").document(selectedDate).get().addOnSuccessListener { doc ->
            if (doc != null && doc.exists()) { pastDayData = doc.data; pastMeals = doc.get("meals") as? List<Map<String, Any>> ?: emptyList() } 
            else { pastDayData = null; pastMeals = emptyList() }
        }
    }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1) {
            isWeeklyLoading = true
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val tempCals = MutableList(7) { 0 }
            val tempLabels = MutableList(7) { "" }
            val tempVitamins = mutableListOf<String>()
            var fetchesCompleted = 0
            
            for (i in 0..6) {
                val c = java.util.Calendar.getInstance(); c.add(java.util.Calendar.DAY_OF_YEAR, -(6 - i))
                tempLabels[i] = when(c.get(java.util.Calendar.DAY_OF_WEEK)) { java.util.Calendar.MONDAY -> "Δ"; java.util.Calendar.TUESDAY -> "Τ"; java.util.Calendar.WEDNESDAY -> "Τ"; java.util.Calendar.THURSDAY -> "Π"; java.util.Calendar.FRIDAY -> "Π"; java.util.Calendar.SATURDAY -> "Σ"; else -> "Κ" }
                
                db.collection("users").document(uid).collection("daily_logs").document(sdf.format(c.time)).get().addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result?.exists() == true) {
                        tempCals[i] = task.result?.getLong("calories")?.toInt() ?: 0
                        tempVitamins.addAll(task.result?.get("vitamins") as? List<String> ?: emptyList())
                    }
                    fetchesCompleted++
                    if (fetchesCompleted == 7) { weeklyCals = tempCals.toList(); weeklyLabels = tempLabels.toList(); weeklyVitamins = tempVitamins.distinct().toList(); isWeeklyLoading = false }
                }
            }
        }
    }

    // Περιεχόμενο "Ημερήσιο"
    val DailyContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Button(
                onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cardColor), shape = RoundedCornerShape(20.dp)
            ) { Text("📅 $selectedDate", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textPrimary) }
            
            if (pastDayData == null) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text("Δεν υπάρχουν δεδομένα", color = textSecondary, fontSize = 15.sp) }
            } else {
                val cals = (pastDayData!!["calories"] as? Number)?.toInt() ?: 0
                val prot = (pastDayData!!["protein"] as? Number)?.toInt() ?: 0
                val fat = (pastDayData!!["fat"] as? Number)?.toInt() ?: 0
                val carbs = (pastDayData!!["carbs"] as? Number)?.toInt() ?: 0
                val water = (pastDayData!!["water"] as? Number)?.toInt() ?: 0
                val vits = pastDayData!!["vitamins"] as? List<String> ?: emptyList()
                
                // Σύνολα Ημέρας (Flat)
                Column(modifier = Modifier.fillMaxWidth()) {
                    HistorySectionHeader("Σύνολα Ημέρας", "📊", textPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("🔥 $cals kcal", color = primaryAccent, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🥩 ${prot}g Πρωτεΐνη • 🥑 ${fat}g Λίπη • 🥖 ${carbs}g Υδατ.", fontSize = 14.sp, color = textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("💧 $water Ποτήρια Νερό", color = textSecondary, fontSize = 14.sp)
                    if (vits.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("✨ Βιταμίνες: ${vits.joinToString(", ")}", fontSize = 13.sp, color = primaryAccent)
                    }
                }
            }
        }
    }

    val DailyMealsContent: @Composable () -> Unit = {
        if (pastMeals.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HistorySectionHeader("Τι έφαγες", "🍽️", textPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                pastMeals.forEach { meal ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meal["name"].toString(), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = textPrimary)
                            Text("🕒 ${meal["time"]}", fontSize = 13.sp, color = textSecondary)
                        }
                        Text("${meal["calories"]} kcal", fontWeight = FontWeight.Bold, color = primaryAccent, fontSize = 16.sp)
                    }
                    if (meal != pastMeals.last()) {
                        HorizontalDivider(color = dividerColor.copy(alpha = 0.05f), thickness = 1.dp)
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ιστορικό & Πρόγνωση", fontWeight = FontWeight.SemiBold, color = textPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textPrimary) } }
            )
        },
        containerColor = bgColor
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTabIndex, containerColor = bgColor, contentColor = textPrimary, divider = {},
                indicator = { tabPositions -> TabRowDefaults.Indicator(modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)), color = primaryAccent, height = 4.dp) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index, onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold, color = if (selectedTabIndex == index) primaryAccent else textSecondary) }
                    )
                }
            }
            
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth > 600.dp

                if (selectedTabIndex == 0) {
                    if (isWideScreen) {
                        Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { DailyContent() }
                            Column(modifier = Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState())) { DailyMealsContent() }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                            DailyContent()
                            if (pastMeals.isNotEmpty()) {
                                HorizontalDivider(color = dividerColor, thickness = 1.dp)
                                DailyMealsContent()
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val blurMod = if (!isUserPremium) Modifier.blur(20.dp) else Modifier
                        Box(modifier = blurMod) {
                            WeeklyProDashboard(
                                isUserPremium = isUserPremium, userName = userName, targetCals = userTargetCals, calsData = weeklyCals, weekDays = weeklyLabels, 
                                weekVitamins = weeklyVitamins, isLoading = isWeeklyLoading, userGoal = userGoal, startFatPercentage = startFatPercentage, 
                                startWeight = startWeight, targetFatPercentage = targetFatPercentage, gender = gender, height = height, 
                                coroutineScope = coroutineScope, bgColor = bgColor, cardColor = cardColor, primaryAccent = primaryAccent, premiumGold = premiumGold, 
                                textPrimary = textPrimary, textSecondary = textSecondary, dividerColor = dividerColor, isWideScreen = isWideScreen
                            )
                        }
                        if (!isUserPremium) {
                            Box(modifier = Modifier.fillMaxSize().background(bgColor.copy(alpha = 0.6f)).clickable(enabled = true, onClick = {}))
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Card(
                                    modifier = Modifier.padding(32.dp), colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(24.dp)
                                ) {
                                    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(modifier = Modifier.size(64.dp).background(premiumGold.copy(alpha=0.15f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) { Text("👑", fontSize = 32.sp) }
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Text("Εβδομάδα PRO", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textPrimary, textAlign = TextAlign.Center)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Ξεκλείδωσε αναλυτικά γραφήματα 7 ημερών και AI Forecasting με βάση τους στόχους και τις βιταμίνες σου.", color = textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                                        Spacer(modifier = Modifier.height(32.dp))
                                        Button(
                                            onClick = { Toast.makeText(context, "Σύντομα διαθέσιμο!", Toast.LENGTH_SHORT).show() },
                                            modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = premiumGold), shape = RoundedCornerShape(20.dp)
                                        ) { Text("Γίνε PRO", color = bgColor, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyProDashboard(
    isUserPremium: Boolean, userName: String, targetCals: Int, calsData: List<Int>, weekDays: List<String>, weekVitamins: List<String>, isLoading: Boolean,
    userGoal: String, startFatPercentage: String, startWeight: String, targetFatPercentage: String, gender: String, height: String, coroutineScope: kotlinx.coroutines.CoroutineScope,
    bgColor: Color, cardColor: Color, primaryAccent: Color, premiumGold: Color, textPrimary: Color, textSecondary: Color, dividerColor: Color, isWideScreen: Boolean
) {
    if (isLoading) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = primaryAccent) }; return }

    val validDays = calsData.filter { it > 0 }
    val avgCals = if (validDays.isNotEmpty()) validDays.average().toInt() else 0

    var currentWeight by remember { mutableStateOf("") }
    var currentNeck by remember { mutableStateOf("") }
    var currentWaist by remember { mutableStateOf("") }
    var currentHip by remember { mutableStateOf("") }
    var computedFat by remember { mutableStateOf("") }
    var aiAdvice by remember { mutableStateOf("Βάλε τις σημερινές σου μετρήσεις και ζήτα PRO ανάλυση για να δεις τη διαφορά.") }
    var isAiLoading by remember { mutableStateOf(false) }

    val ChartContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            // Μέσος Όρος (Flat)
            Column(modifier = Modifier.fillMaxWidth()) {
                HistorySectionHeader("Μέσος Όρος Εβδομάδας", "📈", textPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$avgCals", color = textPrimary, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    Text(" kcal", color = primaryAccent, fontSize = 16.sp, modifier = Modifier.padding(bottom = 6.dp, start = 8.dp), fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(color = dividerColor, thickness = 1.dp)

            // Γράφημα (Flat)
            Column(modifier = Modifier.fillMaxWidth()) {
                HistorySectionHeader("Θερμίδες vs Στόχος ($targetCals)", "📊", textPrimary)
                Spacer(modifier = Modifier.height(24.dp))
                
                Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    val maxCals = (calsData.maxOrNull() ?: 2500).coerceAtLeast(targetCals) + 200
                    val barWidth = size.width / 14f
                    val space = barWidth
                    val targetY = size.height - (targetCals.toFloat() / maxCals) * size.height
                    drawLine(color = premiumGold.copy(alpha = 0.5f), start = Offset(0f, targetY), end = Offset(size.width, targetY), strokeWidth = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f))

                    calsData.forEachIndexed { index, cals ->
                        val barHeight = (cals.toFloat() / maxCals) * size.height
                        val xOffset = index * (barWidth + space) + space / 2
                        val yOffset = size.height - barHeight
                        val barColor = if (cals <= targetCals) primaryAccent else Color(0xFFF87171)
                        drawRoundRect(color = barColor, topLeft = Offset(xOffset, yOffset), size = Size(barWidth, barHeight), cornerRadius = CornerRadius(16f, 16f))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    weekDays.forEach { day -> Text(day, color = textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                }
            }
        }
    }

    val AiContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            // Μετρήσεις (Flat)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    HistorySectionHeader("Σημερινές Μετρήσεις", "📏", textPrimary)
                    if(computedFat.isNotEmpty()) Text("Λίπος: $computedFat%", color = primaryAccent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = currentWeight, onValueChange = { currentWeight = it }, label = { Text("Βάρος (kg)", color = textSecondary) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))
                    OutlinedTextField(value = currentNeck, onValueChange = { currentNeck = it }, label = { Text("Λαιμός (cm)", color = textSecondary) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = currentWaist, onValueChange = { currentWaist = it }, label = { Text("Μέση (cm)", color = textSecondary) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))
                    if (gender == "Γυναίκα") OutlinedTextField(value = currentHip, onValueChange = { currentHip = it }, label = { Text("Γοφοί (cm)", color = textSecondary) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))
                }
            }

            HorizontalDivider(color = dividerColor, thickness = 1.dp)
            
            // AI Box
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(primaryAccent.copy(alpha=0.08f))) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤖", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                        Text("AI Πρόγνωση", color = primaryAccent, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(aiAdvice, color = textPrimary, fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            isAiLoading = true; val h = height.toDoubleOrNull() ?: 0.0; val n = currentNeck.toDoubleOrNull() ?: 0.0; val waistVal = currentWaist.toDoubleOrNull() ?: 0.0; val hipVal = currentHip.toDoubleOrNull() ?: 0.0
                            var newFatStr = startFatPercentage; var newWeightStr = startWeight; if (currentWeight.isNotEmpty()) newWeightStr = currentWeight
                            if (h > 0 && n > 0 && waistVal > 0) {
                                val fatPercentage = if (gender == "Άνδρας" && waistVal - n > 0) (495.0 / (1.0324 - 0.19077 * log10(waistVal - n) + 0.15456 * log10(h))) - 450.0 else if (gender == "Γυναίκα" && waistVal + hipVal - n > 0) (495.0 / (1.29579 - 0.35004 * log10(waistVal + hipVal - n) + 0.22100 * log10(h))) - 450.0 else 0.0
                                if (fatPercentage in 1.0..80.0) { newFatStr = String.format(Locale.US, "%.1f", fatPercentage); computedFat = newFatStr }
                            }
                            coroutineScope.launch {
                                try {
                                    val apiKey = ApiKey.GEMINI
                                    val generativeModel = GenerativeModel(modelName = "gemini-2.5-flash", apiKey = apiKey)
                                    val vitsStr = if (weekVitamins.isNotEmpty()) weekVitamins.joinToString(", ") else "Καμία"
                                    val prompt = """
                                        Είσαι σύγχρονος διατροφολόγος. Χρήστης: ${userName}.
                                        ΕΚΚΙΝΗΣΗ: Βάρος: ${startWeight}kg, Λίπος: ${startFatPercentage}%, Στόχος: ${targetFatPercentage}% (${userGoal}).
                                        ΣΗΜΕΡΑ: Βάρος: ${newWeightStr}kg, Λίπος: ${newFatStr}%. Μέσος όρος θερμίδων εβδομάδας: ${avgCals} kcal (Στόχος: ${targetCals}).
                                        Βιταμίνες/Ιχνοστοιχεία εβδομάδας: ${vitsStr}.
                                        Σύγκρινε αρχικό με σημερινό. Δικαιολογούν οι θερμίδες το αποτέλεσμα; 
                                        ΣΧΟΛΙΑΣΕ ΤΙΣ ΒΙΤΑΜΙΝΕΣ: Αν λείπει π.χ. Ω3, Σίδηρος, πες το στα ίσια και πρότεινε τροφές.
                                        Γράψε ΜΟΝΟ μια σύντομη ανάλυση 4 προτάσεων, φιλικά, μιλώντας στο χρήστη με το όνομά του.
                                    """.trimIndent()
                                    aiAdvice = generativeModel.generateContent(prompt).text ?: "Πρόβλημα AI."
                                } catch (e: Exception) { aiAdvice = "Σφάλμα: ${e.message}" } finally { isAiLoading = false }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryAccent), shape = RoundedCornerShape(20.dp), enabled = isUserPremium && !isAiLoading
                    ) {
                        if (isAiLoading) CircularProgressIndicator(color = bgColor, modifier = Modifier.size(26.dp))
                        else Text("ΖΗΤΑ PRO ΑΝΑΛΥΣΗ", color = bgColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }

    if (isWideScreen) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { ChartContent() }
            Column(modifier = Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState())) { AiContent() }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(32.dp)) {
            ChartContent()
            HorizontalDivider(color = dividerColor, thickness = 1.dp)
            AiContent()
        }
    }
}

// Βοηθητικό Composable για τους Τίτλους των Ενοτήτων (ίδιο με το HomeScreen)
@Composable
fun HistorySectionHeader(title: String, emojiIcon: String, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emojiIcon, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
        Text(title, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}
