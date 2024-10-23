package com.daohoangson.droidbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.daohoangson.droidbot.ui.theme.DroidTakeOverTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val PREFS_NAME = "DroidBotPrefs"
        const val KEY_API_KEY = "api_key"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DroidTakeOverTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ApiKeyInput(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ApiKeyInput(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            MainActivity.PREFS_NAME, ComponentActivity.MODE_PRIVATE
        )
    }
    var apiKey by remember { mutableStateOf(prefs.getString(MainActivity.KEY_API_KEY, "") ?: "") }
    var reveal by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = apiKey,
            label = { Text(stringResource(R.string.enter_api_key)) },
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { apiKey = it },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            trailingIcon = {
                IconButton(onClick = { reveal = !reveal }) {
                    Icon(
                        imageVector = if (reveal) Icons.Outlined.Lock else Icons.Filled.Lock,
                        contentDescription = if (reveal) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                prefs.edit().putString(MainActivity.KEY_API_KEY, apiKey).apply()
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save))
        }
    }
}