package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

val categories = listOf("Makanan", "Transport", "Belanja", "Tagihan", "Hiburan", "Kesehatan", "Lainnya")
fun rp(v: Long): String = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(v)
fun Long.toDate(): LocalDate = LocalDate.ofEpochDay(this)

class MainActivity : ComponentActivity() {
    private val vm: ExpenseViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { App(vm) } }
    }
}

@Composable
fun App(vm: ExpenseViewModel) {
    val list by vm.expenses.collectAsState()
    var editing by remember { mutableStateOf<Expense?>(null) }
    var showForm by remember { mutableStateOf(false) }
    if (showForm) {
        BackHandler { showForm = false }
        FormScreen(editing, onBack = { showForm = false }) {
            if (it.id == 0L) vm.add(it) else vm.update(it)
            showForm = false
        }
    } else {
        HomeScreen(list, onAdd = { editing = null; showForm = true },
            onEdit = { editing = it; showForm = true }, onDelete = { vm.delete(it) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(list: List<Expense>, onAdd: () -> Unit, onEdit: (Expense) -> Unit, onDelete: (Expense) -> Unit) {
    val today = LocalDate.now()
    val todayTotal = list.filter { it.date == today.toEpochDay() }.sumOf { it.amount }
    val month = list.filter { val d = it.date.toDate(); d.month == today.month && d.year == today.year }
    val byCat = month.groupBy { it.category }.map { it.key to it.value.sumOf { e -> e.amount } }
        .sortedByDescending { it.second }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Pengeluaran Harian") }) },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, "Tambah") } }
    ) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Hari ini")
                        Text(rp(todayTotal), style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Bulan ini: ${rp(month.sumOf { it.amount })}", fontWeight = FontWeight.Bold)
                        byCat.forEach { (c, t) ->
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(c); Text(rp(t)) }
                        }
                    }
                }
            }
            if (list.isEmpty()) item { Text("Belum ada pengeluaran. Tekan + untuk menambah.") }
            items(list, key = { it.id }) { e ->
                Card(Modifier.fillMaxWidth().clickable { onEdit(e) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.title, fontWeight = FontWeight.Bold)
                            Text("${e.category} • ${e.date.toDate()}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(rp(e.amount))
                        IconButton(onClick = { onDelete(e) }) { Icon(Icons.Default.Delete, "Hapus") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormScreen(e: Expense?, onBack: () -> Unit, onSave: (Expense) -> Unit) {
    var title by remember { mutableStateOf(e?.title ?: "") }
    var amount by remember { mutableStateOf(e?.amount?.toString() ?: "") }
    var cat by remember { mutableStateOf(e?.category ?: categories[0]) }
    var note by remember { mutableStateOf(e?.note ?: "") }
    var date by remember { mutableStateOf(e?.date ?: LocalDate.now().toEpochDay()) }
    var picker by remember { mutableStateOf(false) }
    val valid = title.isNotBlank() && (amount.toLongOrNull() ?: 0L) > 0L

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (e == null) "Tambah Pengeluaran" else "Ubah Pengeluaran") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } }
        )
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Judul") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Jumlah (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth())
            Text("Kategori")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { c -> FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c) }) }
            }
            OutlinedButton(onClick = { picker = true }) { Text("Tanggal: ${date.toDate()}") }
            OutlinedTextField(note, { note = it }, label = { Text("Catatan (opsional)") }, modifier = Modifier.fillMaxWidth())
            Button(enabled = valid, modifier = Modifier.fillMaxWidth(),
                onClick = { onSave(Expense(e?.id ?: 0, title.trim(), amount.toLong(), cat, date, note.trim())) }) {
                Text("Simpan")
            }
        }
    }
    if (picker) {
        val st = rememberDatePickerState(
            initialSelectedDateMillis = date.toDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { picker = false }, confirmButton = {
            TextButton(onClick = {
                st.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                picker = false
            }) { Text("OK") }
        }) { DatePicker(st) }
    }
}
