package com.daohoangson.droidbot

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.daohoangson.droidbot.bedrock.Client
import com.daohoangson.droidbot.ui.theme.DroidTakeOverTheme
import kotlinx.coroutines.async

class MainActivity : ComponentActivity() {
    private val prefs: SharedPreferencesLiveData by lazy { SharedPreferencesLiveData(this) }
    private val vm = DroidBotViewModel

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
                        AwsCredentialsInput(prefs)

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.accessibility_open_settings))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { vm.takeScreenshot() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Screenshot")
                        }
                        Button(
                            onClick = { vm.dispatchTap(540f, 198f) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.enter_aws_access_key_id))
                        }
                        Button(
                            onClick = { vm.dispatchTap(540f, 418f) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.enter_aws_secret_access_key))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                lifecycleScope.async {
                                    val bedrock = Client(
                                        accessKeyId = prefs.value?.awsAccessKeyId ?: "",
                                        secretAccessKey = prefs.value?.awsSecretAccessKey ?: ""
                                    )
                                    bedrock.invokeModel().collect {
                                        Log.d("Bedrock", "event: $it")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Bedrock")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AwsCredentialsInput(prefs: SharedPreferencesLiveData, modifier: Modifier = Modifier) {
    val prefValues = prefs.asFlow().collectAsStateWithLifecycle(null).value
    var reveal by remember { mutableStateOf(false) }

    var formValues by remember { mutableStateOf(SharedPreferencesLiveData.Values()) }
    val awsAccessKeyId = formValues.awsAccessKeyId ?: prefValues?.awsAccessKeyId ?: ""
    val awsSecretAccessKey = formValues.awsSecretAccessKey ?: prefValues?.awsSecretAccessKey ?: ""

    Column(modifier = modifier) {
        OutlinedTextField(
            value = awsAccessKeyId,
            label = { Text(stringResource(R.string.enter_aws_access_key_id)) },
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { formValues = formValues.copy(awsAccessKeyId = it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = awsSecretAccessKey,
            label = { Text(stringResource(R.string.enter_aws_secret_access_key)) },
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { formValues = formValues.copy(awsSecretAccessKey = it) },
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
            enabled = awsAccessKeyId.isNotEmpty()
                    && awsSecretAccessKey.isNotEmpty()
                    && (awsAccessKeyId != prefValues?.awsAccessKeyId || awsSecretAccessKey != prefValues.awsSecretAccessKey),
            onClick = {
                prefs.apply(
                    awsAccessKeyId = awsAccessKeyId,
                    awsSecretAccessKey = awsSecretAccessKey
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save))
        }
    }
}