package com.example.mycompose.calorietrackerscreen.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mycompose.calorietrackerscreen.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.log10
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: return

    val bgColor = Color(0xFF15151E)
    val cardColor = Color(0xFF222232)
    val primaryAccent = Color(0xFFA78BFA)
    val premiumGold = Color(0xFFFBBF24)
    val textPrimary = Color(0xFFF3F4F6)
    val textSecondary = Color(0xFF9CA3AF)
    val dangerAccent = Color(0xFFF87171)

    var userName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Άνδρας") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var neck by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var hip by remember { mutableStateOf("") }
    var currentFatPercentage by remember { mutableStateOf("0") }
    var targetFatPercentage by remember { mutableStateOf("15") }
    var dietType by remember { mutableStateOf("Κετογονική") }
    var userGoal by remember { mutableStateOf("Απώλεια Λίπους") }
    var targetCals by remember { mutableStateOf("1800") }
    var targetProt by remember { mutableStateOf("150") }
    var targetFat by remember { mutableStateOf("120") }
    var targetCarbs by remember { mutableStateOf("30") }
    var expandedGender by remember { mutableStateOf(false) }
    var expandedDiet by remember { mutableStateOf(false) }
    var expandedGoal by remember { mutableStateOf(false) }
    val genderOptions = listOf("Άνδρας", "Γυναίκα")
    val dietOptions = listOf("Κετογονική", "Low Carb", "Ισορροπημένη", "High Protein", "Vegan", "Vegetarian", "Pescatarian", "Paleo", "Μεσογειακή")
    val goalOptions = listOf("Απώλεια Λίπους", "Συντήρηση", "Αύξηση Μυϊκής Μάζας")
    var isPremium by remember { mutableStateOf(false) }
    var promoCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc != null && doc.exists()) {
                userName = doc.getString("name") ?: ""
                gender = doc.getString("gender") ?: "Άνδρας"
                age = doc.getString("age") ?: ""
                weight = doc.getString("weight") ?: ""
                height = doc.getString("height") ?: ""
                neck = doc.getString("neck") ?: ""
                waist = doc.getString("waist") ?: ""
                hip = doc.getString("hip") ?: ""
                currentFatPercentage = doc.getString("currentFatPercentage") ?: "0"
                targetFatPercentage = doc.getString("targetFatPercentage") ?: "15"
                dietType = doc.getString("dietType") ?: "Κετογονική"
                userGoal = doc.getString("userGoal") ?: "Απώλεια Λίπους"
                targetCals = doc.getLong("targetCals")?.toString() ?: "1800"
                targetProt = doc.getLong("targetProt")?.toString() ?: "150"
                targetFat = doc.getLong("targetFat")?.toString() ?: "120"
                targetCarbs = doc.getLong("targetCarbs")?.toString() ?: "30"
                isPremium = doc.getBoolean("isPremium") ?: false
            }
            isLoading = false
        }
    }

    val saveProfile = {
        val data = hashMapOf(
            "name" to userName, "gender" to gender, "age" to age, "weight" to weight, "height" to height,
            "neck" to neck, "waist" to waist, "hip" to hip,
            "currentFatPercentage" to currentFatPercentage, "targetFatPercentage" to targetFatPercentage,
            "dietType" to dietType, "userGoal" to userGoal, "targetCals" to (targetCals.toIntOrNull() ?: 1800),
            "targetProt" to (targetProt.toIntOrNull() ?: 150), "targetFat" to (targetFat.toIntOrNull() ?: 120),
            "targetCarbs" to (targetCarbs.toIntOrNull() ?: 30), "isPremium" to isPremium
        )
        db.collection("users").document(uid).set(data).addOnSuccessListener { Toast.makeText(context, "Το προφίλ ενημερώθηκε!", Toast.LENGTH_SHORT).show() }
    }

    val recalculateMacrosFromCals = { calsStr: String, currentDiet: String ->
        val cals = calsStr.toDoubleOrNull() ?: 0.0
        if (cals > 0) {
            val (pCarb, pProt, pFat) = when (currentDiet) {
                "Κετογονική" -> Triple(0.05, 0.25, 0.70)
                "Low Carb" -> Triple(0.20, 0.35, 0.45)
                "High Protein" -> Triple(0.30, 0.40, 0.30)
                "Vegan", "Vegetarian", "Pescatarian" -> Triple(0.50, 0.20, 0.30)
                else -> Triple(0.40, 0.30, 0.30)
            }
            targetCarbs = ((cals * pCarb) / 4).toInt().toString(); targetProt = ((cals * pProt) / 4).toInt().toString(); targetFat = ((cals * pFat) / 9).toInt().toString()
        }
    }

    val calculateMacros = {
        val w = weight.toDoubleOrNull() ?: 0.0; val h = height.toDoubleOrNull() ?: 0.0; val a = age.toIntOrNull() ?: 0
        val n = neck.toDoubleOrNull() ?: 0.0; val waistVal = waist.toDoubleOrNull() ?: 0.0; val hipVal = hip.toDoubleOrNull() ?: 0.0
        if (w > 0 && h > 0 && a > 0) {
            if (n > 0 && waistVal > 0) {
                val fatPercentage = if (gender == "Άνδρας" && waistVal - n > 0) (495.0 / (1.0324 - 0.19077 * log10(waistVal - n) + 0.15456 * log10(h))) - 450.0 else if (gender == "Γυναίκα" && waistVal + hipVal - n > 0) (495.0 / (1.29579 - 0.35004 * log10(waistVal + hipVal - n) + 0.22100 * log10(h))) - 450.0 else 0.0
                if (fatPercentage in 1.0..80.0) currentFatPercentage = String.format(Locale.US, "%.1f", fatPercentage)
            }
            var bmr = (10.0 * w) + (6.25 * h) - (5.0 * a); bmr += if (gender == "Άνδρας") 5.0 else -161.0
            val tdee = bmr * 1.375
            val finalCals = when (userGoal) { "Απώλεια Λίπους" -> tdee - 500; "Αύξηση Μυϊκής Μάζας" -> tdee + 300; else -> tdee }
            targetCals = finalCals.toInt().toString(); recalculateMacrosFromCals(targetCals, dietType)
            Toast.makeText(context, "Υπολογίστηκε!", Toast.LENGTH_SHORT).show()
        } else Toast.makeText(context, "Συμπλήρωσε Βάρος, Ύψος & Ηλικία!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Προφίλ & Αφετηρία", color = textPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { padding ->
        if (isLoading) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = primaryAccent) }
        else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {

                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = premiumGold.copy(alpha=0.1f)), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👑 Premium Πρόσβαση", color = premiumGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (isPremium) {
                            Box(modifier = Modifier.background(premiumGold.copy(alpha=0.2f), RoundedCornerShape(16.dp)).padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically, 
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.app_logo),
                                        contentDescription = "App Logo",
                                        modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Είσαι PRO χρήστης! Έχεις ξεκλειδώσει το εβδομαδιαίο AI Tracking.", color = textPrimary, textAlign = TextAlign.Center, fontSize = 14.sp)
                                }
                            }
                        } else {
                            Text("Έχεις VIP κωδικό; Βάλτον εδώ.", color = textSecondary, textAlign = TextAlign.Center, fontSize = 13.sp)
                            OutlinedTextField(value = promoCode, onValueChange = { promoCode = it.uppercase() }, placeholder = { Text("Promo Code", color = textSecondary) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = premiumGold, unfocusedBorderColor = Color.Transparent, unfocusedContainerColor = cardColor, focusedContainerColor = cardColor, focusedTextColor = textPrimary, unfocusedTextColor = textPrimary))
                            Button(
                                onClick = { if (promoCode == "B2BIT_PRO" || promoCode == "KALLISTI_VIP") { isPremium = true; db.collection("users").document(uid).update("isPremium", true).addOnSuccessListener { Toast.makeText(context, "Καλώς ήρθες στο Premium!", Toast.LENGTH_LONG).show() } } else Toast.makeText(context, "Άκυρος κωδικός!", Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = premiumGold), shape = RoundedCornerShape(20.dp)
                            ) { Text("Ξεκλείδωμα", color = bgColor, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Αφετηρία (Σταθερά)", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            if(currentFatPercentage.toDoubleOrNull() ?: 0.0 > 0) Text("Λίπος: $currentFatPercentage%", color = primaryAccent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        OutlinedTextField(value = userName, onValueChange = { userName = it }, label = { Text("Όνομα", color = textSecondary) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExposedDropdownMenuBox(expanded = expandedGender, onExpandedChange = { expandedGender = !expandedGender }, modifier = Modifier.weight(1f)) {
                                OutlinedTextField(value = gender, onValueChange = {}, readOnly = true, label = { Text("Φύλο", color = textSecondary) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGender) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))
                                ExposedDropdownMenu(expanded = expandedGender, onDismissRequest = { expandedGender = false }) { genderOptions.forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption) }, onClick = { gender = selectionOption; expandedGender = false }) } }
                            }
                            OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Ηλικία", color = textSecondary) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Βάρος (kg)", color = textSecondary) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))
                            OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Ύψος (cm)", color = textSecondary) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))
                        }

                        HorizontalDivider(color = bgColor)
                        Text("Μεζούρα (Αφετηρία)", color = textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = neck, onValueChange = { neck = it }, label = { Text("Λαιμός (cm)", color = textSecondary) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))
                            OutlinedTextField(value = waist, onValueChange = { waist = it }, label = { Text("Μέση (cm)", color = textSecondary) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))
                        }
                        if (gender == "Γυναίκα") OutlinedTextField(value = hip, onValueChange = { hip = it }, label = { Text("Γοφοί (cm)", color = textSecondary) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))
                    }
                }

                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Στόχοι & Macros", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = targetFatPercentage, onValueChange = { targetFatPercentage = it }, label = { Text("Στόχος Λίπους (%)", color = premiumGold, fontSize = 12.sp) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = premiumGold, unfocusedBorderColor = bgColor))
                            ExposedDropdownMenuBox(expanded = expandedGoal, onExpandedChange = { expandedGoal = !expandedGoal }, modifier = Modifier.weight(1.5f)) {
                                OutlinedTextField(value = userGoal, onValueChange = {}, readOnly = true, label = { Text("Στόχος", color = textSecondary) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGoal) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))
                                ExposedDropdownMenu(expanded = expandedGoal, onDismissRequest = { expandedGoal = false }) { goalOptions.forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption) }, onClick = { userGoal = selectionOption; expandedGoal = false }) } }
                            }
                        }

                        ExposedDropdownMenuBox(expanded = expandedDiet, onExpandedChange = { expandedDiet = !expandedDiet }) {
                            OutlinedTextField(value = dietType, onValueChange = {}, readOnly = true, label = { Text("Διατροφή", color = textSecondary) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDiet) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))
                            ExposedDropdownMenu(expanded = expandedDiet, onDismissRequest = { expandedDiet = false }) { dietOptions.forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption) }, onClick = { dietType = selectionOption; expandedDiet = false; recalculateMacrosFromCals(targetCals, selectionOption) }) } }
                        }

                        Button(onClick = { calculateMacros() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = cardColor, contentColor = primaryAccent), border = androidx.compose.foundation.BorderStroke(1.dp, primaryAccent), shape = RoundedCornerShape(20.dp)) { Text("Αυτόματος Υπολογισμός", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }

                        HorizontalDivider(color = bgColor)

                        OutlinedTextField(value = targetCals, onValueChange = { targetCals = it; recalculateMacrosFromCals(it, dietType) }, label = { Text("Ημερήσιες Θερμίδες", color = textSecondary) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = targetProt, onValueChange = { targetProt = it }, label = { Text("Πρωτεΐνη (g)", color = textSecondary) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))
                            OutlinedTextField(value = targetFat, onValueChange = { targetFat = it }, label = { Text("Λίπος (g)", color = textSecondary) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))
                        }
                        OutlinedTextField(value = targetCarbs, onValueChange = { targetCarbs = it }, label = { Text("Υδατάνθρακες (g)", color = textSecondary) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryAccent, unfocusedBorderColor = bgColor))

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { saveProfile() }, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryAccent), shape = RoundedCornerShape(20.dp)) { Text("Αποθήκευση", color = bgColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp) }
                    }
                }

                Button(onClick = { auth.signOut(); onLogout() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = dangerAccent.copy(alpha=0.1f)), shape = RoundedCornerShape(20.dp)) { Text("Αποσύνδεση", color = dangerAccent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
            }
        }
    }
}
