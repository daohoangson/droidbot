package com.daohoangson.droidbot

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
        const val KEY_AWS_ACCESS_KEY_ID = "awsAccessKeyId"
        const val KEY_AWS_SECRET_ACCESS_KEY = "awsSecretAccessKey"
    }

    private val takeOverLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("takeOverLauncher: result -> ", result.toString())
        if (result.resultCode != RESULT_OK) return@registerForActivityResult

        val serviceIntent = Intent(this, TakeOverService::class.java).apply {
            putExtra(TakeOverService.EXTRA_INT_MEDIA_PROJECTION_RESULT_CODE, result.resultCode)
            putExtra(TakeOverService.EXTRA_PARCELABLE_MEDIA_PROJECTION_RESULT_DATA, result.data)
        }
        startForegroundService(serviceIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DroidTakeOverTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        AwsCredentialsInput()

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val mpm = getSystemService(MediaProjectionManager::class.java)
                                takeOverLauncher.launch(mpm.createScreenCaptureIntent())
                            }, modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.take_over))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.accessibility_open_settings))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AwsCredentialsInput(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            MainActivity.PREFS_NAME, ComponentActivity.MODE_PRIVATE
        )
    }
    var awsAccessKeyId by remember {
        mutableStateOf(
            prefs.getString(
                MainActivity.KEY_AWS_ACCESS_KEY_ID, ""
            ) ?: ""
        )
    }
    var awsSecretAccessKey by remember {
        mutableStateOf(
            prefs.getString(
                MainActivity.KEY_AWS_SECRET_ACCESS_KEY, ""
            ) ?: ""
        )
    }
    var hasChanges by remember { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = awsAccessKeyId,
            label = { Text(stringResource(R.string.enter_aws_access_key_id)) },
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = {
                awsAccessKeyId = it
                hasChanges = true
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = awsSecretAccessKey,
            label = { Text(stringResource(R.string.enter_aws_secret_access_key)) },
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = {
                awsSecretAccessKey = it
                hasChanges = true
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            trailingIcon = {
                IconButton(onClick = { reveal = !reveal }) {
                    Icon(
                        imageVector = if (reveal) Icons.Outlined.Lock else Icons.Filled.Lock,
                        contentDescription = if (reveal) stringResource(R.string.enter_aws_secret_access_key_hide)
                        else stringResource(R.string.enter_aws_secret_access_key_reveal)
                    )
                }
            },
            visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            enabled = hasChanges,
            onClick = {
                prefs.edit()
                    .putString(MainActivity.KEY_AWS_ACCESS_KEY_ID, awsAccessKeyId)
                    .putString(MainActivity.KEY_AWS_SECRET_ACCESS_KEY, awsSecretAccessKey)
                    .apply()
                hasChanges = false
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save))
        }
    }
}