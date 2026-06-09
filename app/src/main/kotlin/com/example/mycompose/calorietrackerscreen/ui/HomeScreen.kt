package com.example.mycompose.calorietrackerscreen.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.mycompose.calorietrackerscreen.ApiKey
import com.example.mycompose.calorietrackerscreen.R
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: "unknown_user"
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Premium Παλέτα
    val bgColor = if (isDarkTheme) Color(0xFF15151E) else Color(0xFFF3F4F6)
    val cardColor = if (isDarkTheme) Color(0xFF222232) else Color.White
    val primaryAccent = Color(0xFFA78BFA) 
    val secondaryAccent = Color(0xFF4ADE80) 
    val warningAccent = Color(0xFFFB923C) 
    val dangerAccent = Color(0xFFF87171) 
    val premiumGold = Color(0xFFFBBF24) 
    val textPrimary = if (isDarkTheme) Color(0xFFF3F4F6) else Color(0xFF1F2937)
    val textSecondary = Color(0xFF9CA3AF)
    val dividerColor = textSecondary.copy(alpha = 0.15f)

    var mealDescription by remember { mutableStateOf("") }
    // Tags με εικονίδια
    val tags = listOf("🛵 Delivery", "🏠 Σπιτικό", "🍔 Cheat Meal", "🍖 Ψητό", "🥗 Σαλάτα")
    var selectedTag by remember { mutableStateOf("") }

    var capturedImages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var aiResult by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }

    var currentCalories by remember { mutableStateOf(0) }
    var currentProtein by remember { mutableStateOf(0) }
    var currentFat by remember { mutableStateOf(0) }
    var currentCarbs by remember { mutableStateOf(0) }
    var currentMeals by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var waterGlasses by remember { mutableStateOf(0) }
    var aiScansToday by remember { mutableStateOf(0) }
    var currentVitamins by remember { mutableStateOf<List<String>>(emptyList()) }

    var pendingCalories by remember { mutableStateOf(0) }
    var pendingProtein by remember { mutableStateOf(0) }
    var pendingFat by remember { mutableStateOf(0) }
    var pendingCarbs by remember { mutableStateOf(0) }
    var pendingVitamins by remember { mutableStateOf<List<String>>(emptyList()) }
    var showConfirmButtons by remember { mutableStateOf(false) }
    var mealToEdit by remember { mutableStateOf<Map<String, Any>?>(null) }

    var userName by remember { mutableStateOf("") }
    var isPremium by remember { mutableStateOf(false) }
    var targetCals by remember { mutableStateOf(1800) }
    var targetProt by remember { mutableStateOf(150) }
    var targetFat by remember { mutableStateOf(120) }
    var targetCarbs by remember { mutableStateOf(30) }
    var dietType by remember { mutableStateOf("Κετογονική") }
    var userGoal by remember { mutableStateOf("Απώλεια Λίπους") }

    val saveDailyData = {
        val dailyData = hashMapOf<String, Any>(
            "calories" to currentCalories,
            "protein" to currentProtein,
            "fat" to currentFat,
            "carbs" to currentCarbs,
            "water" to waterGlasses,
            "meals" to currentMeals,
            "aiScans" to aiScansToday,
            "vitamins" to currentVitamins,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("users").document(uid).collection("daily_logs").document(todayDate).set(dailyData)
    }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).collection("daily_logs").document(todayDate).get().addOnSuccessListener { doc ->
            if (doc != null && doc.exists()) {
                currentCalories = doc.getLong("calories")?.toInt() ?: 0
                currentProtein = doc.getLong("protein")?.toInt() ?: 0
                currentFat = doc.getLong("fat")?.toInt() ?: 0
                currentCarbs = doc.getLong("carbs")?.toInt() ?: 0
                waterGlasses = doc.getLong("water")?.toInt() ?: 0
                aiScansToday = doc.getLong("aiScans")?.toInt() ?: 0
                currentMeals = doc.get("meals") as? List<Map<String, Any>> ?: emptyList()
                currentVitamins = doc.get("vitamins") as? List<String> ?: emptyList()
            }
        }

        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc != null && doc.exists()) {
                userName = doc.getString("name") ?: ""
                isPremium = doc.getBoolean("isPremium") ?: false
                targetCals = doc.getLong("targetCals")?.toInt() ?: 1800
                targetProt = doc.getLong("targetProt")?.toInt() ?: 150
                targetFat = doc.getLong("targetFat")?.toInt() ?: 120
                targetCarbs = doc.getLong("targetCarbs")?.toInt() ?: 30
                dietType = doc.getString("dietType") ?: "Κετογονική"
                userGoal = doc.getString("userGoal") ?: "Απώλεια Λίπους"
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            if (capturedImages.size < 4) capturedImages = capturedImages + bitmap
            else Toast.makeText(context, "Έχεις ήδη βάλει 4 φωτογραφίες!", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) cameraLauncher.launch(null) else Toast.makeText(context, "Πρέπει να δώσεις άδεια!", Toast.LENGTH_SHORT).show()
    }

    val launchCamera = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Το κομμάτι του Input (Αριστερή πλευρά σε Fold)
    val InputContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Photos Area
            Column(modifier = Modifier.fillMaxWidth().height(340.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ImageSlot(index = 1, bitmap = capturedImages.getOrNull(0), onClick = launchCamera, onClear = { capturedImages = capturedImages.filterIndexed { i, _ -> i != 0 } }, modifier = Modifier.weight(1f), cardColor, primaryAccent)
                    ImageSlot(index = 2, bitmap = capturedImages.getOrNull(1), onClick = launchCamera, onClear = { capturedImages = capturedImages.filterIndexed { i, _ -> i != 1 } }, modifier = Modifier.weight(1f), cardColor, primaryAccent)
                }
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ImageSlot(index = 3, bitmap = capturedImages.getOrNull(2), onClick = launchCamera, onClear = { capturedImages = capturedImages.filterIndexed { i, _ -> i != 2 } }, modifier = Modifier.weight(1f), cardColor, primaryAccent)
                    ImageSlot(index = 4, bitmap = capturedImages.getOrNull(3), onClick = launchCamera, onClear = { capturedImages = capturedImages.filterIndexed { i, _ -> i != 3 } }, modifier = Modifier.weight(1f), cardColor, primaryAccent)
                }
            }

            Text("Βγάλε το πιάτο σου ή ετικέτες συστατικών. Όσο πιο καθαρά, τόσο πιο καλά.", fontSize = 13.sp, color = textSecondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

            OutlinedTextField(
                value = mealDescription, onValueChange = { mealDescription = it },
                placeholder = { Text("Πρόσθεσε φαγητό...", color = textSecondary) },
                leadingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                    )
                },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = cardColor, focusedContainerColor = cardColor,
                    unfocusedBorderColor = Color.Transparent, focusedBorderColor = primaryAccent,
                    unfocusedTextColor = textPrimary, focusedTextColor = textPrimary
                )
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(tags) { tag ->
                    FilterChip(
                        selected = selectedTag == tag, onClick = { selectedTag = if (selectedTag == tag) "" else tag },
                        label = { Text(tag, color = if (selectedTag == tag) cardColor else textPrimary, modifier = Modifier.padding(vertical = 4.dp)) },
                        shape = RoundedCornerShape(20.dp), border = null,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primaryAccent, containerColor = cardColor)
                    )
                }
            }

            Button(
                onClick = {
                    if (!isPremium && aiScansToday >= 4) { Toast.makeText(context, "Έφτασες το όριο! Γύρνα στο Premium!", Toast.LENGTH_LONG).show(); return@Button }
                    if (capturedImages.isEmpty() && mealDescription.isEmpty()) { Toast.makeText(context, "Βάλε φώτο ή γράψε κάτι!", Toast.LENGTH_SHORT).show(); return@Button }
                    isAiLoading = true; showConfirmButtons = false

                    coroutineScope.launch {
                        try {
                            val apiKey = ApiKey.GEMINI
                            if (apiKey.isEmpty() || apiKey == "ΤΟ_ΚΛΕΙΔΙ_ΣΟΥ_ΕΔΩ") { aiResult = "Ξέχασες το κλειδί στο ApiKey.kt!"; isAiLoading = false; return@launch }

                            val generativeModel = GenerativeModel(modelName = "gemini-2.5-flash", apiKey = apiKey)
                            
                            // Η ΜΑΓΕΙΑ ΕΔΩ: Προσθέτουμε το όνομα του χρήστη στο prompt!
                            val displayName = if (userName.isNotBlank()) userName else "φίλε"
                            val prompt = """
                                Είσαι ένας σύγχρονος, φιλικός διατροφολόγος. Το όνομα του χρήστη είναι $displayName.
                                Χρήστης: $mealDescription. Tag: $dietType. Στόχος: $userGoal.
                                Απάντησε ΑΥΣΤΗΡΑ ΜΟΝΟ με JSON. Στο 'text_response' θέλω να του μιλάς προσωπικά, χρησιμοποιώντας το όνομά του ($displayName).
                                { "portion_used": "Π.χ. 200g", "text_response": "Γράψε την ανάλυση", "calories": 0, "protein": 0, "fat": 0, "carbs": 0, "vitamins": ["Βιταμίνη C", "Ωμέγα 3"] }
                            """.trimIndent()

                            val response = if (capturedImages.isNotEmpty()) generativeModel.generateContent(content { capturedImages.forEach { image(it) }; text(prompt) })
                            else generativeModel.generateContent(prompt)

                            val cleanJson = (response.text ?: "").trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

                            try {
                                val jsonObject = JSONObject(cleanJson)
                                val aiText = jsonObject.optString("text_response", "Δεν προστέθηκε σχόλιο από το AI.")
                                val portionUsed = jsonObject.optString("portion_used", "Άγνωστη Μερίδα")
                                pendingCalories = jsonObject.optInt("calories", 0)
                                pendingProtein = jsonObject.optInt("protein", 0)
                                pendingFat = jsonObject.optInt("fat", 0)
                                pendingCarbs = jsonObject.optInt("carbs", 0)

                                val vitArray = jsonObject.optJSONArray("vitamins")
                                val extractedVitamins = mutableListOf<String>()
                                if (vitArray != null) {
                                    for (i in 0 until vitArray.length()) extractedVitamins.add(vitArray.getString(i))
                                }
                                pendingVitamins = extractedVitamins

                                val vitsDisplay = if (extractedVitamins.isNotEmpty()) "\nΒιταμίνες: ${extractedVitamins.joinToString(", ")}" else "" 
                                aiResult = """ Ανάλυση για: $portionUsed
                                🔥 $pendingCalories kcal | 🥩 ${pendingProtein}g Πρωτ. | 🥑 ${pendingFat}g Λίπος | 🥖 ${pendingCarbs}g Υδατ.$vitsDisplay

                                $aiText
                                """.trimIndent()
                                showConfirmButtons = true
                            } catch (e: Exception) { aiResult = "Σφάλμα JSON: Το AI δεν απάντησε σωστά. Δοκίμασε ξανά." }
                        } catch (e: Exception) { aiResult = "Σφάλμα API: Ελέγξτε τη σύνδεσή σας." } finally { isAiLoading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(20.dp), enabled = !isAiLoading,
                colors = ButtonDefaults.buttonColors(containerColor = primaryAccent)
            ) {
                if (isAiLoading) CircularProgressIndicator(color = bgColor, modifier = Modifier.size(26.dp))
                else Text("ΑΝΑΛΥΣΗ ΓΕΥΜΑΤΟΣ", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = bgColor, letterSpacing = 1.sp)
            }
        }
    }

    // Το κομμάτι του Dashboard (Δεξιά πλευρά σε Fold) - FLAT DESIGN
    val DashboardContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
            
            if (aiResult.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(primaryAccent.copy(alpha = 0.08f))) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🤖", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                            Text("Ανάλυση AI", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = primaryAccent)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(aiResult, fontSize = 14.sp, color = textPrimary, lineHeight = 22.sp)

                        if (showConfirmButtons) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { showConfirmButtons = false; pendingCalories = 0; pendingProtein = 0; pendingFat = 0; pendingCarbs = 0; pendingVitamins = emptyList(); aiResult = "Ακυρώθηκε." },
                                    colors = ButtonDefaults.buttonColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(50.dp)
                                ) { Text("Άκυρο", color = textPrimary, fontWeight = FontWeight.Medium) }

                                Button(
                                    onClick = {
                                        // Αποθήκευση με το tag (συμπεριλαμβανομένου του εικονιδίου)
                                        val newMealName = if (mealDescription.isNotEmpty()) mealDescription else selectedTag.ifEmpty { "Γεύμα με AI" }
                                        val newMeal = hashMapOf<String, Any>(
                                            "id" to System.currentTimeMillis().toString(), "name" to newMealName, "calories" to pendingCalories,
                                            "protein" to pendingProtein, "fat" to pendingFat, "carbs" to pendingCarbs, "time" to SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                        )
                                        val updatedMeals = currentMeals.toMutableList(); updatedMeals.add(newMeal); currentMeals = updatedMeals
                                        currentCalories = updatedMeals.sumOf { (it["calories"] as? Number)?.toInt() ?: 0 }
                                        currentProtein = updatedMeals.sumOf { (it["protein"] as? Number)?.toInt() ?: 0 }
                                        currentFat = updatedMeals.sumOf { (it["fat"] as? Number)?.toInt() ?: 0 }
                                        currentCarbs = updatedMeals.sumOf { (it["carbs"] as? Number)?.toInt() ?: 0 }
                                        currentVitamins = (currentVitamins + pendingVitamins).distinct()
                                        aiScansToday++; saveDailyData()
                                        showConfirmButtons = false; aiResult = "Αποθηκεύτηκε επιτυχώς!"; capturedImages = emptyList(); mealDescription = ""; selectedTag = ""; pendingVitamins = emptyList()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryAccent), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).height(50.dp)
                                ) { Text("Αποθήκευση", color = bgColor, fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
            }

            // Στόχοι Ημέρας
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Στόχοι Ημέρας", "🎯", textPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                MacroDashboard(currentCalories, currentProtein, currentFat, currentCarbs, targetCals, targetProt, targetFat, targetCarbs, textPrimary, textSecondary, primaryAccent, secondaryAccent, warningAccent, premiumGold)
            }

            HorizontalDivider(color = dividerColor, thickness = 1.dp)

            // Χρήσεις AI
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("Χρήσεις AI", "✨", textPrimary)
                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if(isPremium) premiumGold.copy(alpha=0.15f) else cardColor)) {
                        Text(if (isPremium) "PRO" else "FREE", color = if(isPremium) premiumGold else textSecondary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isPremium) {
                            Box(modifier = Modifier.height(48.dp).clip(RoundedCornerShape(16.dp)).background(premiumGold.copy(alpha = 0.1f)).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.app_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp).clip(RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Απεριόριστα Slots", color = premiumGold, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                            }
                        } else {
                            repeat(4) { index ->
                                val isFilled = index < aiScansToday
                                Box(
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(if (isFilled) primaryAccent.copy(alpha = 0.15f) else cardColor),
                                    contentAlignment = Alignment.Center
                                ) { Text(text = (index + 1).toString(), color = if (isFilled) primaryAccent else textSecondary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if(isPremium) aiScansToday.toString() else "$aiScansToday / 4", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(if(isPremium) "Σαρώσεις" else "Slots", color = textSecondary, fontSize = 12.sp)
                    }
                }
                if (!isPremium) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Ξεκλείδωσε απεριόριστες αναλύσεις στο PRO.", color = premiumGold, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }

            HorizontalDivider(color = dividerColor, thickness = 1.dp)

            // Ενυδάτωση
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    SectionHeader("Ενυδάτωση", "💧", textPrimary)
                    Text("$waterGlasses / 8 Ποτήρια", fontSize = 14.sp, color = secondaryAccent)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { if (waterGlasses > 0) { waterGlasses--; saveDailyData() } }, modifier = Modifier.background(cardColor, RoundedCornerShape(16.dp))) { Text("-", color = textPrimary, fontSize = 18.sp) }
                    IconButton(onClick = { waterGlasses++; saveDailyData() }, modifier = Modifier.background(cardColor, RoundedCornerShape(16.dp))) { Text("+", color = textPrimary, fontSize = 18.sp) }
                }
            }

            HorizontalDivider(color = dividerColor, thickness = 1.dp)

            // Γεύματα
            if (currentMeals.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader("Γεύματα Ημέρας", "🍽️", textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    currentMeals.forEach { meal ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(meal["name"].toString(), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = textPrimary)
                                Text("${meal["calories"]} kcal", fontSize = 13.sp, color = textSecondary)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { mealToEdit = meal }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = primaryAccent) }
                                IconButton(onClick = {
                                    val updatedMeals = currentMeals.toMutableList(); updatedMeals.remove(meal); currentMeals = updatedMeals
                                    currentCalories = updatedMeals.sumOf { (it["calories"] as? Number)?.toInt() ?: 0 }
                                    currentProtein = updatedMeals.sumOf { (it["protein"] as? Number)?.toInt() ?: 0 }
                                    currentFat = updatedMeals.sumOf { (it["fat"] as? Number)?.toInt() ?: 0 }
                                    currentCarbs = updatedMeals.sumOf { (it["carbs"] as? Number)?.toInt() ?: 0 }
                                    saveDailyData(); Toast.makeText(context, "Το γεύμα διαγράφηκε!", Toast.LENGTH_SHORT).show()
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = dangerAccent) }
                            }
                        }
                        if (meal != currentMeals.last()) {
                            HorizontalDivider(color = dividerColor.copy(alpha = 0.05f), thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 0.dp, color = bgColor) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Text("SMART CALORIE", color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                        Text("AI Nutrition Tracker", color = primaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    IconButton(onClick = onNavigateToHistory) { Icon(Icons.Default.DateRange, contentDescription = "Ιστορικό", tint = textPrimary) }
                    IconButton(onClick = onNavigateToProfile) { Icon(Icons.Default.AccountCircle, contentDescription = "Προφίλ", tint = textPrimary) }
                }
            }
        },
        containerColor = bgColor
    ) { paddingValues ->
        // Responsive Design!
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val isWideScreen = maxWidth > 600.dp
            
            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        InputContent()
                    }
                    Column(modifier = Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        DashboardContent()
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    InputContent()
                    DashboardContent()
                }
            }
        }
    }

    mealToEdit?.let { meal ->
        EditMealDialog(
            meal = meal, cardColor = cardColor, primaryAccent = primaryAccent, textPrimary = textPrimary, bgColor = bgColor,
            onDismiss = { mealToEdit = null },
            onSave = { updatedName, updatedCals, updatedProt, updatedFat, updatedCarbs ->
                val updatedMeals = currentMeals.map {
                    if (it["id"] == meal["id"]) it.toMutableMap().apply { this["name"] = updatedName; this["calories"] = updatedCals; this["protein"] = updatedProt; this["fat"] = updatedFat; this["carbs"] = updatedCarbs } else it
                }
                currentMeals = updatedMeals
                currentCalories = updatedMeals.sumOf { (it["calories"] as? Number)?.toInt() ?: 0 }
                currentProtein = updatedMeals.sumOf { (it["protein"] as? Number)?.toInt() ?: 0 }
                currentFat = updatedMeals.sumOf { (it["fat"] as? Number)?.toInt() ?: 0 }
                currentCarbs = updatedMeals.sumOf { (it["carbs"] as? Number)?.toInt() ?: 0 }
                saveDailyData(); mealToEdit = null; Toast.makeText(context, "Ενημερώθηκε!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// Βοηθητικό Composable για τους Τίτλους των Ενοτήτων
@Composable
fun SectionHeader(title: String, emojiIcon: String, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emojiIcon, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
        Text(title, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ImageSlot(index: Int, bitmap: Bitmap?, onClick: () -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier, cardColor: Color, primaryAccent: Color) {
    Box(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(24.dp)).background(cardColor).clickable { if (bitmap == null) onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(32.dp).background(Color.Black.copy(alpha=0.5f), RoundedCornerShape(16.dp)).clickable { onClear() }, contentAlignment = Alignment.Center) {
                Text("✕", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Image(painter = painterResource(id = R.drawable.camera_placeholder), contentDescription = "Placeholder", modifier = Modifier.size(64.dp), alpha = 0.5f)
        }
        Box(modifier = Modifier.align(Alignment.TopStart).padding(12.dp).background(primaryAccent.copy(alpha = 0.15f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
            Text(index.toString(), color = primaryAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun EditMealDialog(meal: Map<String, Any>, cardColor: Color, primaryAccent: Color, textPrimary: Color, bgColor: Color, onDismiss: () -> Unit, onSave: (String, Int, Int, Int, Int) -> Unit) {
    var name by remember { mutableStateOf(meal["name"].toString()) }
    var calories by remember { mutableStateOf(meal["calories"].toString()) }
    var protein by remember { mutableStateOf(meal["protein"].toString()) }
    var carbs by remember { mutableStateOf(meal["carbs"].toString()) }
    var fat by remember { mutableStateOf(meal["fat"].toString()) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = cardColor,
        title = { Text("Τροποποίηση", fontWeight = FontWeight.SemiBold, color = textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Όνομα", color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary))
                OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Θερμίδες", color = Color.Gray) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Πρωτεΐνη", color = Color.Gray) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary))
                    OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Υδατάνθρακες", color = Color.Gray) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary))
                }
                OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Λίπος", color = Color.Gray) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary))
            }
        },
        confirmButton = { Button(onClick = { onSave(name, calories.toIntOrNull() ?: 0, protein.toIntOrNull() ?: 0, fat.toIntOrNull() ?: 0, carbs.toIntOrNull() ?: 0) }, colors = ButtonDefaults.buttonColors(containerColor = primaryAccent)) { Text("Αποθήκευση", color = bgColor) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Άκυρο", color = textPrimary) } }
    )
}

@Composable
fun MacroDashboard(cals: Int, prot: Int, fat: Int, carbs: Int, targetCals: Int, targetProt: Int, targetFat: Int, targetCarbs: Int, textPrimary: Color, textSecondary: Color, c1: Color, c2: Color, c3: Color, c4: Color) {
    val calsProgress = if (targetCals > 0) (cals.toFloat() / targetCals).coerceIn(0f, 1f) else 0f
    val protProgress = if (targetProt > 0) (prot.toFloat() / targetProt).coerceIn(0f, 1f) else 0f
    val fatProgress = if (targetFat > 0) (fat.toFloat() / targetFat).coerceIn(0f, 1f) else 0f
    val carbsProgress = if (targetCarbs > 0) (carbs.toFloat() / targetCarbs).coerceIn(0f, 1f) else 0f

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MacroRow("Θερμίδες", "$cals / $targetCals", calsProgress, c3, textPrimary, textSecondary)
        MacroRow("Πρωτεΐνη", "${prot}g / ${targetProt}g", protProgress, c1, textPrimary, textSecondary)
        MacroRow("Λίπη", "${fat}g / ${targetFat}g", fatProgress, c4, textPrimary, textSecondary)
        MacroRow("Υδατάνθρακες", "${carbs}g / ${targetCarbs}g", carbsProgress, c2, textPrimary, textSecondary)
    }
}

@Composable
fun MacroRow(label: String, value: String, progress: Float, color: Color, textPrimary: Color, textSecondary: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = textSecondary, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 12.dp), color = textPrimary, fontSize = 14.sp)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)), color = color, trackColor = color.copy(alpha = 0.15f))
    }
}
