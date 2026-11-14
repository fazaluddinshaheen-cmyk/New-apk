package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AdminNotification
import com.example.data.model.AppSettings
import com.example.data.model.DepositRequest
import com.example.data.model.KycStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PromoCode
import com.example.data.model.RequestStatus
import com.example.data.model.SupportMessage
import com.example.data.model.SupportThread
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatPkr
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AmberPending
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.MintProfit
import com.example.ui.theme.MonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    settings: AppSettings,
    deposits: List<DepositRequest>,
    withdrawals: List<WithdrawalRequest>,
    users: List<UserProfile>,
    promoCodes: List<PromoCode>,
    supportThreads: List<SupportThread>,
    selectedChatUserId: String?,
    selectedChatMessages: List<SupportMessage>,
    notifications: List<AdminNotification>,
    isBusy: Boolean,
    snackbarHostState: SnackbarHostState,
    onSaveSettings: (AppSettings) -> Unit,
    onApproveDeposit: (DepositRequest) -> Unit,
    onRejectDeposit: (DepositRequest, String) -> Unit,
    onApproveWithdrawal: (WithdrawalRequest) -> Unit,
    onRejectWithdrawal: (WithdrawalRequest, String) -> Unit,
    onReviewFace: (userId: String, approve: Boolean) -> Unit,
    onCreatePromoCode: (code: String, bonus: Double, maxUses: Int, description: String) -> Unit,
    onTogglePromoActive: (code: String, active: Boolean) -> Unit,
    onDeletePromoCode: (code: String) -> Unit,
    onSelectChatThread: (String?) -> Unit,
    onSendAdminReply: (targetUserId: String, targetUserName: String, text: String) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onExitAdmin: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val unreadNotifCount = remember(notifications) { notifications.count { !it.isRead } }
    val pendingDepositCount = remember(deposits) { deposits.count { it.status == RequestStatus.PENDING.name } }
    val pendingWithdrawalCount = remember(withdrawals) { withdrawals.count { it.status == RequestStatus.PENDING.name } }
    val pendingFaceCount = remember(users) { users.count { it.faceStatus == KycStatus.PENDING.name } }
    val unreadChatCount = remember(supportThreads) { supportThreads.sumOf { it.unreadByAdmin } }

    var rejectDepositTarget by remember { mutableStateOf<DepositRequest?>(null) }
    var rejectWithdrawalTarget by remember { mutableStateOf<WithdrawalRequest?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "ایڈمن کنٹرول سینٹر",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.admin_panel_title),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { selectedTab = 6 },
                        modifier = Modifier.testTag("admin_bell_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifCount > 0) {
                                    Badge { Text(unreadNotifCount.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "ایڈمن اطلاعات"
                            )
                        }
                    }
                    IconButton(
                        onClick = onExitAdmin,
                        modifier = Modifier.testTag("exit_admin_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "ایڈمن پینل بند کریں"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val tabTitles = listOf(
                "کنٹرولز",
                "ڈپازٹس ($pendingDepositCount)",
                "ادائیگیاں ($pendingWithdrawalCount)",
                "فیس اور صارفین ($pendingFaceCount)",
                "پرومو کوڈز",
                "لائیو چیٹ ($unreadChatCount)",
                "اطلاعات ($unreadNotifCount)"
            )
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, maxLines = 1) },
                        modifier = Modifier.testTag("admin_tab_$index")
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                when (selectedTab) {
                    0 -> AdminControlsTab(
                        settings = settings,
                        pendingDeposits = pendingDepositCount,
                        pendingWithdrawals = pendingWithdrawalCount,
                        pendingFaces = pendingFaceCount,
                        totalUsers = users.size,
                        isBusy = isBusy,
                        onSaveSettings = onSaveSettings
                    )
                    1 -> AdminDepositsTab(
                        deposits = deposits,
                        isBusy = isBusy,
                        onApprove = onApproveDeposit,
                        onRejectClick = {
                            rejectReason = ""
                            rejectDepositTarget = it
                        }
                    )
                    2 -> AdminWithdrawalsTab(
                        withdrawals = withdrawals,
                        isBusy = isBusy,
                        onApprove = onApproveWithdrawal,
                        onRejectClick = {
                            rejectReason = ""
                            rejectWithdrawalTarget = it
                        }
                    )
                    3 -> AdminFaceAndUsersTab(
                        users = users,
                        isBusy = isBusy,
                        onReviewFace = onReviewFace
                    )
                    4 -> AdminPromoCodesTab(
                        promoCodes = promoCodes,
                        isBusy = isBusy,
                        onCreatePromoCode = onCreatePromoCode,
                        onTogglePromoActive = onTogglePromoActive,
                        onDeletePromoCode = onDeletePromoCode
                    )
                    5 -> AdminLiveChatTab(
                        threads = supportThreads,
                        selectedUserId = selectedChatUserId,
                        messages = selectedChatMessages,
                        onSelectThread = onSelectChatThread,
                        onSendAdminReply = onSendAdminReply
                    )
                    6 -> AdminNotificationsTab(
                        notifications = notifications,
                        onMarkRead = onMarkNotificationRead
                    )
                }
            }
        }
    }

    if (rejectDepositTarget != null) {
        AlertDialog(
            onDismissRequest = { rejectDepositTarget = null },
            title = { Text("ڈپازٹ درخواست مسترد کریں") },
            text = {
                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    label = { Text("مسترد کرنے کی وجہ") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectDeposit(rejectDepositTarget!!, rejectReason)
                        rejectDepositTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAlert)
                ) {
                    Text("مسترد کریں")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectDepositTarget = null }) { Text("منسوخ") }
            }
        )
    }

    if (rejectWithdrawalTarget != null) {
        AlertDialog(
            onDismissRequest = { rejectWithdrawalTarget = null },
            title = { Text("درخواست مسترد کریں اور رقم واپس کریں") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "مسترد کرنے پر ${formatPkr(rejectWithdrawalTarget!!.amount)} خودکار طور پر ${rejectWithdrawalTarget!!.userName} کے والٹ میں واپس جمع ہو جائیں گے۔",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("مسترد کرنے کی وجہ") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectWithdrawal(rejectWithdrawalTarget!!, rejectReason)
                        rejectWithdrawalTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAlert)
                ) {
                    Text("مسترد اور واپس")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectWithdrawalTarget = null }) { Text("منسوخ") }
            }
        )
    }
}

@Composable
private fun AdminControlsTab(
    settings: AppSettings,
    pendingDeposits: Int,
    pendingWithdrawals: Int,
    pendingFaces: Int,
    totalUsers: Int,
    isBusy: Boolean,
    onSaveSettings: (AppSettings) -> Unit
) {
    var maintenanceMode by remember(settings.maintenanceMode) { mutableStateOf(settings.maintenanceMode) }
    var maintenanceMessage by remember(settings.maintenanceMessage) { mutableStateOf(settings.maintenanceMessage) }
    var depositFeeStr by remember(settings.depositFeePercent) { mutableStateOf(settings.depositFeePercent.toString()) }
    var referralBonusStr by remember(settings.referralBonusAmount) { mutableStateOf(settings.referralBonusAmount.toInt().toString()) }
    var easypaisaNumber by remember(settings.easypaisaNumber) { mutableStateOf(settings.easypaisaNumber) }
    var easypaisaTitle by remember(settings.easypaisaTitle) { mutableStateOf(settings.easypaisaTitle) }
    var jazzcashNumber by remember(settings.jazzcashNumber) { mutableStateOf(settings.jazzcashNumber) }
    var jazzcashTitle by remember(settings.jazzcashTitle) { mutableStateOf(settings.jazzcashTitle) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminKpiBox("زیر غور ڈپازٹس", pendingDeposits.toString(), AmberPending, Modifier.weight(1f))
                AdminKpiBox("زیر غور ادائیگیاں", pendingWithdrawals.toString(), AmberPending, Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminKpiBox("زیر غور فیس تصدیق", pendingFaces.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                AdminKpiBox("کل سرمایہ کار", totalUsers.toString(), MintProfit, Modifier.weight(1f))
            }
        }

        // Maintenance Option
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "مرمت موڈ", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("سسٹم مرمت موڈ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "اپڈیٹ کے دوران صارف پورٹل عارضی طور پر بند کریں",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = maintenanceMode,
                            onCheckedChange = { maintenanceMode = it },
                            modifier = Modifier.testTag("admin_maintenance_switch")
                        )
                    }

                    OutlinedTextField(
                        value = maintenanceMessage,
                        onValueChange = { maintenanceMessage = it },
                        label = { Text("مرمت بینر پیغام") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }
        }

        // Deposit Charge, Referral Bonus & Merchant Accounts
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
                        text = "فیس، ریفرل بونس اور اکاؤنٹس",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = depositFeeStr,
                            onValueChange = { depositFeeStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("ڈپازٹ فیس (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_deposit_fee_input")
                        )
                        OutlinedTextField(
                            value = referralBonusStr,
                            onValueChange = { referralBonusStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("دعوتی بونس (Rs.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_referral_bonus_input")
                        )
                    }

                    OutlinedTextField(
                        value = easypaisaTitle,
                        onValueChange = { easypaisaTitle = it },
                        label = { Text("ایزی پیسہ اکاؤنٹ کا نام") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = easypaisaNumber,
                        onValueChange = { easypaisaNumber = it },
                        label = { Text("ایزی پیسہ اکاؤنٹ نمبر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = jazzcashTitle,
                        onValueChange = { jazzcashTitle = it },
                        label = { Text("جاز کیش اکاؤنٹ کا نام") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = jazzcashNumber,
                        onValueChange = { jazzcashNumber = it },
                        label = { Text("جاز کیش اکاؤنٹ نمبر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSaveSettings(
                                settings.copy(
                                    maintenanceMode = maintenanceMode,
                                    maintenanceMessage = maintenanceMessage,
                                    depositFeePercent = depositFeeStr.toDoubleOrNull()?.coerceIn(0.0, 50.0) ?: 0.0,
                                    referralBonusAmount = referralBonusStr.toDoubleOrNull()?.coerceIn(0.0, 50000.0) ?: 250.0,
                                    easypaisaTitle = easypaisaTitle,
                                    easypaisaNumber = easypaisaNumber,
                                    jazzcashTitle = jazzcashTitle,
                                    jazzcashNumber = jazzcashNumber
                                )
                            )
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_save_settings_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("ترتیبات محفوظ کریں", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiBox(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MonoFontFamily),
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AdminDepositsTab(
    deposits: List<DepositRequest>,
    isBusy: Boolean,
    onApprove: (DepositRequest) -> Unit,
    onRejectClick: (DepositRequest) -> Unit
) {
    var pendingOnly by remember { mutableStateOf(true) }
    val filtered = remember(deposits, pendingOnly) {
        if (pendingOnly) deposits.filter { it.status == RequestStatus.PENDING.name } else deposits
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = pendingOnly, onClick = { pendingOnly = true }, label = { Text("زیر غور") })
                FilterChip(selected = !pendingOnly, onClick = { pendingOnly = false }, label = { Text("تمام ڈپازٹس") })
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "کوئی ڈپازٹ درخواست نہیں",
                    subtitle = "ایزی پیسہ اور جاز کیش کی نئی ڈپازٹ درخواستیں یہاں فوری ظاہر ہوں گی۔"
                )
            }
        } else {
            items(filtered, key = { it.id }) { dep ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(dep.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${PaymentMethod.fromString(dep.method).label} • از ${dep.senderPhone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(text = dep.status)
                        }
                        Text(
                            text = "کل: ${formatPkr(dep.amount)} | فیس: ${formatPkr(dep.feeAmount)} | کریڈٹ: ${formatPkr(dep.netAmount)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
                            color = MintProfit
                        )
                        Text(
                            text = "TID: ${dep.transactionRef} • ${formatTimestamp(dep.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (dep.status == RequestStatus.PENDING.name) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onApprove(dep) },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("approve_deposit_${dep.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MintProfit, contentColor = MaterialTheme.colorScheme.onPrimary)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "منظور", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("منظور کریں", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onRejectClick(dep) },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("reject_deposit_${dep.id}")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "مسترد", tint = CoralAlert, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مسترد کریں", color = CoralAlert)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminWithdrawalsTab(
    withdrawals: List<WithdrawalRequest>,
    isBusy: Boolean,
    onApprove: (WithdrawalRequest) -> Unit,
    onRejectClick: (WithdrawalRequest) -> Unit
) {
    var pendingOnly by remember { mutableStateOf(true) }
    val filtered = remember(withdrawals, pendingOnly) {
        if (pendingOnly) withdrawals.filter { it.status == RequestStatus.PENDING.name } else withdrawals
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = pendingOnly, onClick = { pendingOnly = true }, label = { Text("زیر غور") })
                FilterChip(selected = !pendingOnly, onClick = { pendingOnly = false }, label = { Text("تمام ادائیگیاں") })
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "کوئی ادائیگی کی درخواست نہیں",
                    subtitle = "سرمایہ کاروں کی رقم نکالنے کی درخواستیں یہاں فوری نظر آئیں گی۔"
                )
            }
        } else {
            items(filtered, key = { it.id }) { wd ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(wd.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${PaymentMethod.fromString(wd.method).label} • ${wd.accountTitle} (${wd.accountNumber})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(text = wd.status)
                        }
                        Text(
                            text = "ادائیگی رقم: ${formatPkr(wd.amount)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatTimestamp(wd.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (wd.status == RequestStatus.PENDING.name) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onApprove(wd) },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("approve_withdrawal_${wd.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MintProfit, contentColor = MaterialTheme.colorScheme.onPrimary)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "منظور", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("منظور کریں", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onRejectClick(wd) },
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("reject_withdrawal_${wd.id}")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "مسترد اور واپس", tint = CoralAlert, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مسترد کریں", color = CoralAlert)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminFaceAndUsersTab(
    users: List<UserProfile>,
    isBusy: Boolean,
    onReviewFace: (userId: String, approve: Boolean) -> Unit
) {
    val pendingOrAiFaceUsers = remember(users) {
        users.filter { !it.faceSelfieBase64.isNullOrBlank() && it.faceStatus != KycStatus.UNVERIFIED.name }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Face, contentDescription = "AI فیس ویریفکیشن ریکارڈ", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI فیس ویریفکیشن (${pendingOrAiFaceUsers.size})", style = MaterialTheme.typography.titleLarge)
            }
        }

        if (pendingOrAiFaceUsers.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "کوئی فیس ویریفکیشن سیلفی نہیں",
                    subtitle = "صارفین کی AI فیس ویریفکیشن رپورٹس یہاں نظر آئیں گی۔"
                )
            }
        } else {
            items(pendingOrAiFaceUsers, key = { "face_${it.userId}" }) { user ->
                AdminFaceReviewCard(
                    user = user,
                    isBusy = isBusy,
                    onReviewFace = onReviewFace
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.People, contentDescription = "سرمایہ کار", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("رجسٹرڈ سرمایہ کار (${users.size})", style = MaterialTheme.typography.titleLarge)
            }
        }

        items(users, key = { "usr_${it.userId}" }) { user ->
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
                        Text(user.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "@${user.username} • ${user.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "بیلنس: ${formatPkr(user.balance)} • دعوتیں: ${user.referralCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MintProfit
                        )
                    }
                    StatusPill(text = "FACE: ${user.faceStatus}")
                }
            }
        }
    }
}

@Composable
private fun AdminNotificationsTab(
    notifications: List<AdminNotification>,
    onMarkRead: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "لائیو ایڈمن اطلاعات پینل",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (notifications.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "ابھی کوئی اطلاع نہیں",
                    subtitle = "ڈپازٹ، ادائیگی، AI فیس ویریفکیشن، لائیو چیٹ اور VIP پلان کی اطلاعات یہاں ظاہر ہوں گی۔"
                )
            }
        } else {
            items(notifications, key = { it.id }) { notif ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notif.isRead) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (notif.isRead) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.primary
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(notif.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = notif.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatTimestamp(notif.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!notif.isRead) {
                            IconButton(
                                onClick = { onMarkRead(notif.id) },
                                modifier = Modifier.testTag("mark_read_${notif.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "پڑھا ہوا نشان لگائیں",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
