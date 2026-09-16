package com.neatfiles.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OpenSourceLibrary(
    val name: String,
    val author: String,
    val version: String,
    val licenseType: String,
    val licenseText: String
)

private const val APACHE_2_0_LICENSE = """Copyright 2024 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License."""

private const val JETBRAINS_APACHE_LICENSE = """Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License."""

private const val EPL_LICENSE = """Eclipse Public License - v 1.0

THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT."""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLibrary by remember { mutableStateOf<OpenSourceLibrary?>(null) }

    val libraries = remember {
        listOf(
            OpenSourceLibrary(
                name = "Jetpack Compose (UI & Foundation)",
                author = "Google LLC / The Android Open Source Project",
                version = "2024.10.00 (BOM)",
                licenseType = "Apache License 2.0",
                licenseText = APACHE_2_0_LICENSE
            ),
            OpenSourceLibrary(
                name = "Material 3 (Material Design Components)",
                author = "Google LLC / The Android Open Source Project",
                version = "1.3.0",
                licenseType = "Apache License 2.0",
                licenseText = APACHE_2_0_LICENSE
            ),
            OpenSourceLibrary(
                name = "AndroidX Navigation Compose",
                author = "Google LLC / The Android Open Source Project",
                version = "2.8.3",
                licenseType = "Apache License 2.0",
                licenseText = APACHE_2_0_LICENSE
            ),
            OpenSourceLibrary(
                name = "AndroidX WorkManager (Work Runtime KTX)",
                author = "Google LLC / The Android Open Source Project",
                version = "2.9.1",
                licenseType = "Apache License 2.0",
                licenseText = APACHE_2_0_LICENSE
            ),
            OpenSourceLibrary(
                name = "AndroidX Core KTX & Activity Compose",
                author = "Google LLC / The Android Open Source Project",
                version = "1.15.0 / 1.9.3",
                licenseType = "Apache License 2.0",
                licenseText = APACHE_2_0_LICENSE
            ),
            OpenSourceLibrary(
                name = "AndroidX Lifecycle & ViewModel Compose",
                author = "Google LLC / The Android Open Source Project",
                version = "2.8.6",
                licenseType = "Apache License 2.0",
                licenseText = APACHE_2_0_LICENSE
            ),
            OpenSourceLibrary(
                name = "Kotlin Standard Library & Coroutines",
                author = "JetBrains s.r.o.",
                version = "2.0.21 / 1.9.0",
                licenseType = "Apache License 2.0",
                licenseText = JETBRAINS_APACHE_LICENSE
            ),
            OpenSourceLibrary(
                name = "JUnit 4 Testing Framework",
                author = "JUnit.org",
                version = "4.13.2",
                licenseType = "Eclipse Public License 1.0",
                licenseText = EPL_LICENSE
            )
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Third-Party Licenses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "NeatFiles is built using the following open source software components. Tap any entry to view complete license terms.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            items(libraries) { lib ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLibrary = lib },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lib.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${lib.author} • v${lib.version}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = lib.licenseType,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    selectedLibrary?.let { lib ->
        AlertDialog(
            onDismissRequest = { selectedLibrary = null },
            title = {
                Column {
                    Text(lib.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(lib.licenseType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    Text(
                        text = lib.licenseText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLibrary = null }) {
                    Text("Close")
                }
            }
        )
    }
}
