package com.example.mycompose.calorietrackerscreen

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mycompose.calorietrackerscreen.ui.AuthScreen
import com.example.mycompose.calorietrackerscreen.ui.HomeScreen
import com.example.mycompose.calorietrackerscreen.ui.HistoryScreen
import com.example.mycompose.calorietrackerscreen.ui.ProfileScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        
        setContent {
            var isDarkTheme by remember { mutableStateOf(sharedPref.getBoolean("is_dark_theme", true)) }
            val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            
            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isDarkTheme) Color(0xFF121212) else MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { 
                            isDarkTheme = !isDarkTheme 
                            sharedPref.edit().putBoolean("is_dark_theme", isDarkTheme).apply()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    
    // Ακούει ζωντανά τη βάση δεδομένων για το αν είσαι Premium!
    var isPremiumUser by remember { mutableStateOf(false) }
    
    LaunchedEffect(auth.currentUser?.uid) {
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
                if (snapshot != null && snapshot.exists()) {
                    isPremiumUser = snapshot.getBoolean("isPremium") ?: false
                }
            }
        }
    }
    
    val startDestination = if (auth.currentUser != null) "home" else "auth"
    
    NavHost(navController = navController, startDestination = startDestination) {
        composable("auth") {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate("home") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToHistory = { navController.navigate("history") }
            )
        }
        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("auth") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        composable("history") {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                isPremium = isPremiumUser
            )
        }
    }
}
