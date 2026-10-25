package com.example.packinglist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.InputStream

private enum class AppTab(val title: String) {
    LIST("Liste"),
    SETTINGS("Settings")
}

private data class PackingItem(
    val id: Int,
    var name: String,
    var type: String,
    var checked: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PackingListApp()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PackingListApp() {
    val context = LocalContext.current
    val listTypes = remember {
        mutableStateListOf("Reise", "Wochenende", "Camping", "Schule", "Arbeit")
    }
    val items = remember {
        mutableStateListOf(
            PackingItem(1, "Unterwäsche", "Reise"),
            PackingItem(2, "Zahnbürste", "Reise"),
            PackingItem(3, "Zelt", "Camping"),
            PackingItem(4, "Schlafsack", "Camping")
        )
    }

    var selectedTab by remember { mutableStateOf(AppTab.LIST) }
    var newItemName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(listTypes.first()) }
    var newTypeName by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.bufferedWriter().use { it.write(itemsToCsv(items)) }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val importedItems = readItemsFromCsv(context.contentResolver.openInputStream(uri))
            if (importedItems.isNotEmpty()) {
                items.clear()
                items.addAll(importedItems)
                importedItems.forEach { importedItem ->
                    if (!listTypes.any { it.equals(importedItem.type, ignoreCase = true) }) {
                        listTypes.add(importedItem.type)
                    }
                }
                if (selectedType !in listTypes) {
                    selectedType = listTypes.first()
                }
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Packing List") }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TabRow(
                        selectedTabIndex = AppTab.entries.indexOf(selectedTab)
                    ) {
                        AppTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.title) }
                            )
                        }
                    }

                    when (selectedTab) {
                        AppTab.LIST -> {
                            TypeSelector(
                                listTypes = listTypes,
                                selectedType = selectedType,
                                onTypeSelected = { selectedType = it }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newItemName,
                                    onValueChange = { newItemName = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Neues Item") }
                                )

                                Button(
                                    onClick = {
                                        if (newItemName.isNotBlank()) {
                                            val normalizedName = newItemName.trim()
                                            items.add(
                                                PackingItem(
                                                    id = System.currentTimeMillis().toInt(),
                                                    name = normalizedName,
                                                    type = selectedType
                                                )
                                            )
                                            newItemName = ""
                                        }
                                    }
                                ) {
                                    Text("Hinzufügen")
                                }
                            }

                            val visibleItems = items.filter { it.type == selectedType }
                            if (visibleItems.isEmpty()) {
                                Card {
                                    Text(
                                        text = "Keine Einträge für diesen Typ vorhanden.",
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(visibleItems, key = { it.id }) { item ->
                                        PackingItemRow(
                                            item = item,
                                            onCheckedChange = { item.checked = it },
                                            onDelete = { items.remove(item) }
                                        )
                                    }
                                }
                            }
                        }

                        AppTab.SETTINGS -> {
                            SettingsTab(
                                items = items,
                                itemTypes = listTypes,
                                newTypeName = newTypeName,
                                onNewTypeNameChange = { newTypeName = it },
                                onAddType = {
                                    val newType = newTypeName.trim()
                                    if (newType.isNotBlank() && listTypes.none { it.equals(newType, ignoreCase = true) }) {
                                        listTypes.add(newType)
                                        if (selectedType.isBlank()) {
                                            selectedType = newType
                                        }
                                        newTypeName = ""
                                    }
                                },
                                onAddItem = { itemName, itemType ->
                                    val name = itemName.trim()
                                    if (name.isNotBlank()) {
                                        val safeType = itemType.trim()
                                        if (!listTypes.any { it.equals(safeType, ignoreCase = true) }) {
                                            listTypes.add(safeType)
                                        }
                                        items.add(
                                            PackingItem(
                                                id = System.currentTimeMillis().toInt(),
                                                name = name,
                                                type = safeType
                                            )
                                        )
                                    }
                                },
                                onDeleteItem = { item -> items.remove(item) },
                                onUpdateItemName = { item, value -> item.name = value },
                                onUpdateItemType = { item, value ->
                                    item.type = value
                                    if (!listTypes.any { it.equals(value, ignoreCase = true) }) {
                                        listTypes.add(value)
                                    }
                                },
                                onExportCsv = { exportLauncher.launch("packing-list.csv") },
                                onImportCsv = { importLauncher.launch(arrayOf("text/csv", "application/csv", "text/plain")) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeSelector(
    listTypes: List<String>,
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedType,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsTab(
    items: List<PackingItem>,
    itemTypes: List<String>,
    newTypeName: String,
    onNewTypeNameChange: (String) -> Unit,
    onAddType: () -> Unit,
    onAddItem: (String, String) -> Unit,
    onDeleteItem: (PackingItem) -> Unit,
    onUpdateItemName: (PackingItem, String) -> Unit,
    onUpdateItemType: (PackingItem, String) -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit
) {
    var newItemName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(itemTypes.firstOrNull() ?: "Reise") }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("CSV Export / Import")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onExportCsv) {
                        Text("Exportieren")
                    }
                    Button(onClick = onImportCsv) {
                        Text("Importieren")
                    }
                }
            }
        }

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Typen verwalten")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTypeName,
                        onValueChange = onNewTypeNameChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Neuen Typ") }
                    )
                    Button(onClick = onAddType) {
                        Text("Hinzufügen")
                    }
                }

                if (itemTypes.isNotEmpty()) {
                    itemTypes.forEach { type ->
                        Text(type)
                    }
                }
            }
        }

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Items verwalten")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Neues Item") }
                    )
                    Button(
                        onClick = {
                            onAddItem(newItemName, selectedType)
                            newItemName = ""
                        }
                    ) {
                        Text("Hinzufügen")
                    }
                }

                TypeSelector(
                    listTypes = itemTypes,
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )

                if (items.isEmpty()) {
                    Text("Keine Items vorhanden.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            SettingsItemRow(
                                item = item,
                                itemTypes = itemTypes,
                                onDelete = { onDeleteItem(item) },
                                onNameChange = { onUpdateItemName(item, it) },
                                onTypeChange = { onUpdateItemType(item, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsItemRow(
    item: PackingItem,
    itemTypes: List<String>,
    onDelete: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = item.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Item") }
            )

            TypeSelector(
                listTypes = itemTypes,
                selectedType = item.type,
                onTypeSelected = onTypeChange
            )

            Button(onClick = onDelete) {
                Text("Löschen")
            }
        }
    }
}

@Composable
private fun PackingItemRow(
    item: PackingItem,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = item.checked,
                onCheckedChange = onCheckedChange
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = item.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(onClick = onDelete) {
                Text("Löschen")
            }
        }
    }
}

private fun itemsToCsv(items: List<PackingItem>): String {
    val rows = buildString {
        appendLine("name,type,checked")
        items.forEach { item ->
            appendLine(
                listOf(
                    escapeCsvField(item.name),
                    escapeCsvField(item.type),
                    item.checked.toString()
                ).joinToString(",")
            )
        }
    }
    return rows
}

private fun readItemsFromCsv(inputStream: InputStream?): List<PackingItem> {
    if (inputStream == null) return emptyList()

    return inputStream.bufferedReader().use { reader ->
        val lines = reader.readLines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            emptyList()
        } else {
            val dataLines = if (lines.first().equals("name,type,checked", ignoreCase = true)) {
                lines.drop(1)
            } else {
                lines
            }

            dataLines.mapIndexedNotNull { index, line ->
                val fields = splitCsvLine(line)
                if (fields.size >= 3) {
                    val name = fields[0].trim()
                    val type = fields[1].trim()
                    val checked = fields[2].trim().equals("true", ignoreCase = true)
                    if (name.isNotBlank() && type.isNotBlank()) {
                        PackingItem(
                            id = System.currentTimeMillis().toInt() + index,
                            name = name,
                            type = type,
                            checked = checked
                        )
                    } else null
                } else null
            }
        }
    }
}

private fun escapeCsvField(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return "\"$escaped\""
}

private fun splitCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var index = 0

    while (index < line.length) {
        val char = line[index]
        when {
            char == '"' -> {
                if (inQuotes && index + 1 < line.length && line[index + 1] == '"') {
                    current.append('"')
                    index += 1
                } else {
                    inQuotes = !inQuotes
                }
            }
            char == ',' && !inQuotes -> {
                result.add(current.toString())
                current.setLength(0)
            }
            else -> current.append(char)
        }
        index += 1
    }

    result.add(current.toString())
    return result.map { it.trim() }
}
