package com.example.mycompose.calorietrackerscreen.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mycompose.calorietrackerscreen.R
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    
    // Premium Colors
    val bgColor = Color(0xFF121212)
    val cardColor = Color(0xFF1E1E1E)
    val primaryColor = Color(0xFF00E5FF) // Neon Cyan
    val secondaryColor = Color(0xFF6200EA) // Purple

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Εδώ ξηλώσαμε το πυραυλάκι και μπήκε το λογότυπο σου!
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Καλώς ήρθες!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = primaryColor)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Κάνε είσοδο ή εγγραφή για να ξεκινήσεις.", fontSize = 16.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = Color.Gray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = cardColor,
                focusedContainerColor = cardColor,
                unfocusedBorderColor = Color.DarkGray,
                focusedBorderColor = primaryColor,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Κωδικός", color = Color.Gray) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "🙈" else "👁️", fontSize = 18.sp)
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = cardColor,
                focusedContainerColor = cardColor,
                unfocusedBorderColor = Color.DarkGray,
                focusedBorderColor = primaryColor,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                if (email.isNotEmpty()) {
                    auth.sendPasswordResetEmail(email.trim())
                        .addOnSuccessListener {
                            Toast.makeText(context, "Το email ανάκτησης στάλθηκε!", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Σφάλμα: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Βάλε το email σου στο πεδίο και πάτα το ξανά!", Toast.LENGTH_LONG).show()
                }
            }) {
                Text("Ξέχασα τον κωδικό μου", color = primaryColor, fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(color = primaryColor)
        } else {
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        auth.signInWithEmailAndPassword(email.trim(), password.trim())
                            .addOnSuccessListener {
                                isLoading = false
                                onAuthSuccess() 
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Λάθος στοιχεία μάγκα μου!", Toast.LENGTH_SHORT).show()
                                isLoading = false
                            }
                    } else {
                        Toast.makeText(context, "Συμπλήρωσε τα πεδία!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = secondaryColor)
            ) {
                Text("Είσοδος", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "Η εγγραφή πέτυχε!", Toast.LENGTH_SHORT).show()
                                onAuthSuccess()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Σφάλμα: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                isLoading = false
                            }
                    } else {
                        Toast.makeText(context, "Συμπλήρωσε τα πεδία για εγγραφή!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
            ) {
                Text("Νέα Εγγραφή", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
