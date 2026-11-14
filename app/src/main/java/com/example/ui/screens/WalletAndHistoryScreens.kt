package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.DepositRequest
import com.example.data.model.PaymentMethod
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.StatusPill
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.components.TransactionRowCard
import com.example.ui.components.formatPkr
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.MintProfit
import com.example.ui.theme.MonoFontFamily

@Composable
fun WalletScreen(
    initialMode: Int,
    profile: UserProfile?,
    settings: AppSettings,
    deposits: List<DepositRequest>,
    withdrawals: List<WithdrawalRequest>,
    isBusy: Boolean,
    onSubmitDeposit: (Double, PaymentMethod, String, String) -> Unit,
    onSubmitWithdrawal: (Double, PaymentMethod, String, String) -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    var walletMode by remember(initialMode) { mutableIntStateOf(initialMode) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.EASYPAISA) }

    var depositAmountStr by remember { mutableStateOf("550") }
    var senderPhone by remember { mutableStateOf(profile?.phone ?: "") }
    var transactionRef by remember { mutableStateOf("") }

    var withdrawAmountStr by remember { mutableStateOf("") }
    var accountTitle by remember { mutableStateOf(profile?.fullName ?: "") }
    var accountNumber by remember { mutableStateOf(profile?.phone ?: "") }

    val clipboardManager = LocalClipboardManager.current
    val depositAmount = depositAmountStr.toDoubleOrNull() ?: 0.0
    val feeAmount = ((depositAmount * settings.depositFeePercent) / 100.0).coerceAtLeast(0.0)
    val netCredit = (depositAmount - feeAmount).coerceAtLeast(0.0)

    val merchantNumber = if (selectedMethod == PaymentMethod.EASYPAISA) {
        settings.easypaisaNumber
    } else {
        settings.jazzcashNumber
    }
    val merchantTitle = if (selectedMethod == PaymentMethod.EASYPAISA) {
        settings.easypaisaTitle
    } else {
        settings.jazzcashTitle
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = walletMode == 0,
                        onClick = { walletMode = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("wallet_deposit_tab")
                    ) {
                        Text("ڈپازٹ کریں")
                    }
                    SegmentedButton(
                        selected = walletMode == 1,
                        onClick = { walletMode = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("wallet_withdraw_tab")
                    ) {
                        Text("رقم نکالیں")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PaymentChannelCard(
                        title = "ایزی پیسہ",
                        subtitle = "موبائل والٹ",
                        selected = selectedMethod == PaymentMethod.EASYPAISA,
                        onClick = { selectedMethod = PaymentMethod.EASYPAISA },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("method_easypaisa")
                    )
                    PaymentChannelCard(
                        title = "جاز کیش",
                        subtitle = "موبائل اکاؤنٹ",
                        selected = selectedMethod == PaymentMethod.JAZZCASH,
                        onClick = { selectedMethod = PaymentMethod.JAZZCASH },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("method_jazzcash")
                    )
                }
            }

            if (walletMode == 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "سرکاری ${selectedMethod.label} اکاؤنٹ پر رقم بھیجیں",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = merchantTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = merchantNumber,
                                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(merchantNumber))
                                            onCopiedNotice("${selectedMethod.label} نمبر کاپی ہو گیا!")
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "اکاؤنٹ نمبر کاپی کریں",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("550", "1350", "2750", "5500", "6500", "13200").forEach { preset ->
                                    FilterChip(
                                        selected = depositAmountStr == preset,
                                        onClick = { depositAmountStr = preset },
                                        label = { Text("Rs. $preset") }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = depositAmountStr,
                                onValueChange = { depositAmountStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("ڈپازٹ رقم (PKR)") },
                                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = "رقم") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("deposit_amount_input")
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FeeRow("ڈپازٹ رقم", formatPkr(depositAmount))
                                    FeeRow(
                                        "ڈپازٹ فیس (${settings.depositFeePercent}%)",
                                        formatPkr(feeAmount)
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    FeeRow("خالص والٹ کریڈٹ", formatPkr(netCredit), highlight = true)
                                }
                            }

                            OutlinedTextField(
                                value = senderPhone,
                                onValueChange = { senderPhone = it },
                                label = { Text("بھیجنے والے کا ${selectedMethod.label} نمبر") },
                                leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = "فون نمبر") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("deposit_phone_input")
                            )

                            OutlinedTextField(
                                value = transactionRef,
                                onValueChange = { transactionRef = it },
                                label = { Text("ٹرانزیکشن آئی ڈی (TID)") },
                                leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = "TID") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("deposit_tid_input")
                            )

                            Button(
                                onClick = {
                                    onSubmitDeposit(depositAmount, selectedMethod, senderPhone, transactionRef)
                                    transactionRef = ""
                                },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("submit_deposit_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("ڈپازٹ درخواست جمع کریں", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item { SectionHeaderRow(title = "حالیہ ڈپازٹ درخواستیں") }
                if (deposits.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "کوئی ڈپازٹ درخواست نہیں",
                            subtitle = "ایڈمن کی فوری منظوری کے لیے اوپر ایزی پیسہ یا جاز کیش ڈپازٹ جمع کریں۔"
                        )
                    }
                } else {
                    items(deposits, key = { it.id }) { dep ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${PaymentMethod.fromString(dep.method).label} • TID ${dep.transactionRef}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "خالص کریڈٹ: ${formatPkr(dep.netAmount)} (فیس ${formatPkr(dep.feeAmount)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!dep.adminNote.isNullOrBlank()) {
                                        Text(
                                            text = dep.adminNote,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = formatTimestamp(dep.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusPill(text = dep.status)
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("نکالنے کے قابل بیلنس", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = formatPkr(profile?.balance ?: 0.0),
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                                    color = MintProfit
                                )
                            }

                            OutlinedTextField(
                                value = withdrawAmountStr,
                                onValueChange = { withdrawAmountStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("نکالنے کی رقم (PKR)") },
                                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = "نکالنے کی رقم") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("withdraw_amount_input")
                            )

                            OutlinedTextField(
                                value = accountTitle,
                                onValueChange = { accountTitle = it },
                                label = { Text("${selectedMethod.label} اکاؤنٹ کا نام") },
                                leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = "اکاؤنٹ کا نام") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("withdraw_title_input")
                            )

                            OutlinedTextField(
                                value = accountNumber,
                                onValueChange = { accountNumber = it },
                                label = { Text("${selectedMethod.label} اکاؤنٹ نمبر") },
                                leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = "اکاؤنٹ نمبر") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("withdraw_number_input")
                            )

                            Button(
                                onClick = {
                                    onSubmitWithdrawal(
                                        withdrawAmountStr.toDoubleOrNull() ?: 0.0,
                                        selectedMethod,
                                        accountTitle,
                                        accountNumber
                                    )
                                    withdrawAmountStr = ""
                                },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("submit_withdrawal_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    text = if (profile?.twoFactorEnabled == true) {
                                        "2FA تصدیق کریں اور رقم نکالیں"
                                    } else {
                                        "${selectedMethod.label} سے رقم نکالنے کی درخواست دیں"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item { SectionHeaderRow(title = "حالیہ رقم نکالنے کی درخواستیں") }
                if (withdrawals.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "ابھی کوئی درخواست نہیں",
                            subtitle = "جب آپ کا منافع تیار ہو تو ایزی پیسہ یا جاز کیش کے ذریعے رقم نکالنے کی درخواست دیں۔"
                        )
                    }
                } else {
                    items(withdrawals, key = { it.id }) { wd ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${PaymentMethod.fromString(wd.method).label} • ${formatPkr(wd.amount)}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${wd.accountTitle} (${wd.accountNumber})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!wd.adminNote.isNullOrBlank()) {
                                        Text(
                                            text = wd.adminNote,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = formatTimestamp(wd.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusPill(text = wd.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentChannelCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Card(
        modifier = modifier
            .height(74.dp)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = title,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FeeRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
            fontWeight = FontWeight.Bold,
            color = if (highlight) MintProfit else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun HistoryScreen(
    transactions: List<WalletTransaction>
) {
    val filters = listOf("ALL", "DEPOSIT", "WITHDRAWAL", "INVESTMENT", "DAILY_PROFIT")
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedTx by remember { mutableStateOf<WalletTransaction?>(null) }

    val filtered = remember(transactions, selectedFilter) {
        if (selectedFilter == "ALL") transactions
        else transactions.filter { it.type == selectedFilter }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "لین دین کا ریکارڈ",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "مکمل رسید دیکھنے کے لیے کسی بھی اندراج پر کلک کریں",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    when (filter) {
                                        "ALL" -> "سب"
                                        "DEPOSIT" -> "ڈپازٹ"
                                        "WITHDRAWAL" -> "نکالنا"
                                        "INVESTMENT" -> "سرمایہ کاری"
                                        "DAILY_PROFIT" -> "منافع"
                                        else -> filter
                                    }
                                )
                            }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "کوئی ریکارڈ نہیں ملا",
                        subtitle = "اس قسم کا لین دین خودکار طور پر یہاں ظاہر ہوگا۔"
                    )
                }
            } else {
                items(filtered, key = { it.id }) { tx ->
                    TransactionRowCard(
                        transaction = tx,
                        onClick = { selectedTx = tx }
                    )
                }
            }
        }
    }

    if (selectedTx != null) {
        TransactionDetailDialog(
            transaction = selectedTx!!,
            onDismiss = { selectedTx = null }
        )
    }
}
