package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.FaceVerificationAiService
import com.example.data.model.AdminNotification
import com.example.data.model.AppSettings
import com.example.data.model.DepositRequest
import com.example.data.model.KycStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PromoCode
import com.example.data.model.SupportMessage
import com.example.data.model.SupportThread
import com.example.data.model.UserProfile
import com.example.data.model.VipInvestment
import com.example.data.model.VipPlan
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.InvestmentRepository
import com.example.data.repository.extractFriendlyFirestoreMessage
import com.example.data.security.SecurityUtils
import com.example.ui.components.ImageEncodingUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class FeedbackBanner(
    val message: String,
    val isError: Boolean = false
)

sealed interface TwoFactorChallenge {
    data object LoginUnlock : TwoFactorChallenge
    data class WithdrawalConfirm(
        val amount: Double,
        val method: PaymentMethod,
        val accountTitle: String,
        val accountNumber: String
    ) : TwoFactorChallenge
}

sealed interface FaceAiState {
    data object Idle : FaceAiState
    data object Analyzing : FaceAiState
    data class Verified(val note: String) : FaceAiState
    data class Rejected(val note: String) : FaceAiState
    data class Error(val message: String) : FaceAiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val repository: InvestmentRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _isAppAuthenticated = MutableStateFlow(false)
    val isAppAuthenticated: StateFlow<Boolean> = _isAppAuthenticated.asStateFlow()

    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _faceAiState = MutableStateFlow<FaceAiState>(FaceAiState.Idle)
    val faceAiState: StateFlow<FaceAiState> = _faceAiState.asStateFlow()

    private val _feedback = MutableStateFlow<FeedbackBanner?>(null)
    val feedback: StateFlow<FeedbackBanner?> = _feedback.asStateFlow()

    private val _twoFactorChallenge = MutableStateFlow<TwoFactorChallenge?>(null)
    val twoFactorChallenge: StateFlow<TwoFactorChallenge?> = _twoFactorChallenge.asStateFlow()

    val appSettings: StateFlow<AppSettings> = repository.observeAppSettings()
        .catch { error ->
            if (isRealCancellation(error)) throw error
            Log.w(TAG, "Failed to observe app settings", error)
            emit(AppSettings())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = AppSettings()
        )

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            if (isRealCancellation(error)) throw error
            val root = unwrapFirestore(error) ?: error
            Log.w(TAG, "Error observing user profile", root)
            emit(UiState.Error(root.extractFriendlyFirestoreMessage("پروفائل لوڈ کرنے میں مسئلہ پیش آیا")))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val userInvestments: StateFlow<List<VipInvestment>> = repository.observeUserInvestments(currentUserId)
        .catch { error ->
            if (isRealCancellation(error)) throw error
            Log.w(TAG, "Error observing investments", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val userDeposits: StateFlow<List<DepositRequest>> = repository.observeUserDeposits(currentUserId)
        .catch { error ->
            if (isRealCancellation(error)) throw error
            Log.w(TAG, "Error observing deposits", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val userWithdrawals: StateFlow<List<WithdrawalRequest>> = repository.observeUserWithdrawals(currentUserId)
        .catch { error ->
            if (isRealCancellation(error)) throw error
            Log.w(TAG, "Error observing withdrawals", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val userTransactions: StateFlow<List<WalletTransaction>> = repository.observeUserTransactions(currentUserId)
        .catch { error ->
            if (isRealCancellation(error)) throw error
            Log.w(TAG, "Error observing transactions", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val userSupportThread: StateFlow<SupportThread?> = repository.observeSupportThread(currentUserId)
        .catch { error ->
            if (isRealCancellation(error)) throw error
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val userSupportMessages: StateFlow<List<SupportMessage>> = repository.observeSupportMessages(currentUserId)
        .catch { error ->
            if (isRealCancellation(error)) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    // Admin streams (started dynamically only after Admin credentials are verified)
    private val _adminDeposits = MutableStateFlow<List<DepositRequest>>(emptyList())
    val adminDeposits: StateFlow<List<DepositRequest>> = _adminDeposits.asStateFlow()

    private val _adminWithdrawals = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    val adminWithdrawals: StateFlow<List<WithdrawalRequest>> = _adminWithdrawals.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val adminUsers: StateFlow<List<UserProfile>> = _adminUsers.asStateFlow()

    private val _adminNotifications = MutableStateFlow<List<AdminNotification>>(emptyList())
    val adminNotifications: StateFlow<List<AdminNotification>> = _adminNotifications.asStateFlow()

    private val _adminPromoCodes = MutableStateFlow<List<PromoCode>>(emptyList())
    val adminPromoCodes: StateFlow<List<PromoCode>> = _adminPromoCodes.asStateFlow()

    private val _adminSupportThreads = MutableStateFlow<List<SupportThread>>(emptyList())
    val adminSupportThreads: StateFlow<List<SupportThread>> = _adminSupportThreads.asStateFlow()

    private val _selectedChatThreadUserId = MutableStateFlow<String?>(null)
    val selectedChatThreadUserId: StateFlow<String?> = _selectedChatThreadUserId.asStateFlow()

    val adminSelectedChatMessages: StateFlow<List<SupportMessage>> = _selectedChatThreadUserId
        .flatMapLatest { targetUid ->
            if (targetUid.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeSupportMessages(targetUid)
                    .catch { if (isRealCancellation(it)) throw it; emit(emptyList()) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private var adminStreamJobs: List<Job> = emptyList()

    init {
        viewModelScope.launch {
            repository.ensureDefaultSettingsIfMissing()
            if (repository.hasActiveAdminSession()) {
                _isAdminMode.value = true
                _isAppAuthenticated.value = true
                startAdminStreams()
            }
        }
    }

    private fun startAdminStreams() {
        stopAdminStreams()
        adminStreamJobs = listOf(
            viewModelScope.launch {
                repository.observeAllDeposits()
                    .catch { if (isRealCancellation(it)) throw it }
                    .collect { _adminDeposits.value = it }
            },
            viewModelScope.launch {
                repository.observeAllWithdrawals()
                    .catch { if (isRealCancellation(it)) throw it }
                    .collect { _adminWithdrawals.value = it }
            },
            viewModelScope.launch {
                repository.observeAllUsers()
                    .catch { if (isRealCancellation(it)) throw it }
                    .collect { _adminUsers.value = it }
            },
            viewModelScope.launch {
                repository.observeAdminNotifications()
                    .catch { if (isRealCancellation(it)) throw it }
                    .collect { _adminNotifications.value = it }
            },
            viewModelScope.launch {
                repository.observeAllPromoCodes()
                    .catch { if (isRealCancellation(it)) throw it }
                    .collect { _adminPromoCodes.value = it }
            },
            viewModelScope.launch {
                repository.observeAllSupportThreads()
                    .catch { if (isRealCancellation(it)) throw it }
                    .collect { _adminSupportThreads.value = it }
            }
        )
    }

    private fun stopAdminStreams() {
        adminStreamJobs.forEach { it.cancel() }
        adminStreamJobs = emptyList()
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    private fun showSuccess(msg: String) {
        _feedback.value = FeedbackBanner(msg, isError = false)
    }

    private fun showError(msg: String) {
        _feedback.value = FeedbackBanner(msg, isError = true)
    }

    // --- IN-APP LOGIN & REGISTRATION ---

    fun loginWithCredentials(username: String, password: String) {
        val cleanUser = username.trim()
        if (cleanUser.isEmpty() || password.isEmpty()) {
            showError("براہ کرم اپنا یوزرنیم اور پاس ورڈ درج کریں۔")
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            if (SecurityUtils.isAdminCredentialMatch(cleanUser, password)) {
                val res = repository.grantAdminSession(cleanUser, password)
                _isBusy.value = false
                if (res.isSuccess) {
                    _isAdminMode.value = true
                    _isAppAuthenticated.value = true
                    startAdminStreams()
                    showSuccess("ایڈمن کنٹرول سینٹر کھل گیا ہے۔")
                } else {
                    showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("ایڈمن تصدیق ناکام رہی۔") ?: "ایڈمن خرابی")
                }
                return@launch
            }

            val profile = (userProfileState.value as? UiState.Success)?.data
                ?: repository.getUserProfile(currentUserId).getOrNull()

            _isBusy.value = false
            if (profile == null) {
                showError("کوئی سرمایہ کار اکاؤنٹ نہیں ملا۔ براہ کرم رجسٹر ٹیب پر جائیں۔")
                return@launch
            }

            val inputHash = SecurityUtils.sha256(password)
            val matchesUser = profile.username.equals(cleanUser, ignoreCase = true) ||
                profile.phone.equals(cleanUser, ignoreCase = true) ||
                profile.email.equals(cleanUser, ignoreCase = true)

            if (!matchesUser || profile.passwordHash != inputHash) {
                showError("غلط یوزرنیم یا پاس ورڈ۔")
                return@launch
            }

            if (profile.twoFactorEnabled && !profile.twoFactorPinHash.isNullOrBlank()) {
                _twoFactorChallenge.value = TwoFactorChallenge.LoginUnlock
            } else {
                _isAppAuthenticated.value = true
                showSuccess("خوش آمدید، ${profile.fullName}!")
            }
        }
    }

    fun registerAccount(
        fullName: String,
        username: String,
        phone: String,
        email: String,
        password: String,
        inviteCode: String = ""
    ) {
        if (fullName.trim().length < 2) {
            showError("براہ کرم اپنا مکمل نام درج کریں۔")
            return
        }
        if (username.trim().length < 3) {
            showError("یوزرنیم کم از کم 3 حروف کا ہونا چاہیے۔")
            return
        }
        if (phone.trim().length < 7) {
            showError("درست موبائل نمبر درج کریں۔")
            return
        }
        if (password.length < 4) {
            showError("پاس ورڈ کم از کم 4 حروف کا ہونا چاہیے۔")
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.registerUserProfile(fullName, username, phone, email, password, inviteCode)
            _isBusy.value = false
            if (res.isSuccess) {
                _isAdminMode.value = false
                _isAppAuthenticated.value = true
                val invitedNote = if (res.getOrNull()?.referredByUserId != null) {
                    " دعوتی کوڈ منسلک ہو گیا — آپ کے پہلے ڈپازٹ پر مدعو کرنے والے کو Rs. 250 بونس ملے گا!"
                } else ""
                showSuccess("سرمایہ کار اکاؤنٹ کامیابی سے بن گیا!$invitedNote")
            } else {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("رجسٹریشن ناکام ہو گئی۔") ?: "رجسٹریشن ناکام")
            }
        }
    }

    fun verifyTwoFactorPin(pin: String) {
        val challenge = _twoFactorChallenge.value ?: return
        val profile = (userProfileState.value as? UiState.Success)?.data
        if (profile == null || profile.twoFactorPinHash != SecurityUtils.sha256(pin.trim())) {
            showError("غلط 2FA سیکیورٹی پن۔")
            return
        }
        _twoFactorChallenge.value = null

        when (challenge) {
            is TwoFactorChallenge.LoginUnlock -> {
                _isAppAuthenticated.value = true
                showSuccess("2FA تصدیق مکمل۔ خوش آمدید، ${profile.fullName}!")
            }
            is TwoFactorChallenge.WithdrawalConfirm -> {
                executeWithdrawal(
                    userName = profile.fullName,
                    amount = challenge.amount,
                    method = challenge.method,
                    accountTitle = challenge.accountTitle,
                    accountNumber = challenge.accountNumber
                )
            }
        }
    }

    fun dismissTwoFactorChallenge() {
        _twoFactorChallenge.value = null
    }

    fun lockAppSession() {
        viewModelScope.launch {
            if (_isAdminMode.value) {
                repository.revokeAdminSession()
            }
            stopAdminStreams()
            _selectedChatThreadUserId.value = null
            _isAdminMode.value = false
            _isAppAuthenticated.value = false
        }
    }

    // --- INVESTOR ACTIONS ---

    fun submitDeposit(
        amount: Double,
        method: PaymentMethod,
        senderPhone: String,
        transactionRef: String
    ) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        val userName = profile?.fullName?.ifBlank { profile.username } ?: "سرمایہ کار"
        val feePercent = appSettings.value.depositFeePercent

        if (amount <= 0) {
            showError("درست ڈپازٹ رقم درج کریں۔")
            return
        }
        if (senderPhone.trim().length < 7) {
            showError("بھیجنے والے کا موبائل نمبر درج کریں۔")
            return
        }
        if (transactionRef.trim().length < 3) {
            showError("${method.label} کی ٹرانزیکشن آئی ڈی (TID) درج کریں۔")
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.submitDepositRequest(
                userName = userName,
                amount = amount,
                feePercent = feePercent,
                method = method,
                senderPhone = senderPhone,
                transactionRef = transactionRef
            )
            _isBusy.value = false
            if (res.isSuccess) {
                showSuccess("ڈپازٹ درخواست ایڈمن کی منظوری کے لیے جمع کر دی گئی ہے۔")
            } else {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("ڈپازٹ درخواست ناکام ہو گئی۔") ?: "ڈپازٹ ناکام")
            }
        }
    }

    fun requestWithdrawal(
        amount: Double,
        method: PaymentMethod,
        accountTitle: String,
        accountNumber: String
    ) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        if (profile == null) {
            showError("پروفائل لوڈ ہو رہی ہے، براہ کرم انتظار کریں۔")
            return
        }
        if (amount <= 0) {
            showError("نکالنے کی درست رقم درج کریں۔")
            return
        }
        if (amount > profile.balance) {
            showError("آپ کے والٹ میں بیلنس ناکافی ہے۔")
            return
        }
        if (accountTitle.trim().length < 2 || accountNumber.trim().length < 7) {
            showError("درست ${method.label} اکاؤنٹ ٹائٹل اور نمبر درج کریں۔")
            return
        }

        if (profile.twoFactorEnabled && !profile.twoFactorPinHash.isNullOrBlank()) {
            _twoFactorChallenge.value = TwoFactorChallenge.WithdrawalConfirm(
                amount = amount,
                method = method,
                accountTitle = accountTitle,
                accountNumber = accountNumber
            )
        } else {
            executeWithdrawal(profile.fullName, amount, method, accountTitle, accountNumber)
        }
    }

    private fun executeWithdrawal(
        userName: String,
        amount: Double,
        method: PaymentMethod,
        accountTitle: String,
        accountNumber: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.submitWithdrawalRequest(
                userName = userName.ifBlank { "سرمایہ کار" },
                amount = amount,
                method = method,
                accountTitle = accountTitle,
                accountNumber = accountNumber
            )
            _isBusy.value = false
            if (res.isSuccess) {
                showSuccess("${method.label} کے ذریعے رقم نکالنے کی درخواست جمع ہو گئی ہے۔")
            } else {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("رقم نکالنے کی درخواست ناکام رہی۔") ?: "ناکام")
            }
        }
    }

    fun investInPlan(plan: VipPlan) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        if (profile == null) {
            showError("پہلے رجسٹریشن مکمل کریں۔")
            return
        }
        if (profile.balance < plan.investmentAmount) {
            showError("بیلنس ناکافی ہے۔ ${plan.name} فعال کرنے کے لیے مزید Rs. ${(plan.investmentAmount - profile.balance).toInt()} ڈپازٹ کریں۔")
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.investInVipPlan(plan, profile.fullName.ifBlank { profile.username })
            _isBusy.value = false
            if (res.isSuccess) {
                showSuccess("${plan.name} فعال ہو گیا! روزانہ Rs. ${plan.dailyReturn.toInt()} اور پرومو کوڈز کھل گئے ہیں۔")
            } else {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("پلان فعال نہیں ہو سکا۔") ?: "ناکام")
            }
        }
    }

    fun claimDailyProfit(investment: VipInvestment) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.claimDailyProfit(investment)
            _isBusy.value = false
            if (res.isSuccess) {
                val earned = res.getOrDefault(0.0).toInt()
                showSuccess("Rs. $earned روزانہ منافع آپ کے والٹ میں جمع ہو گیا!")
            } else {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("منافع وصول نہیں ہو سکا۔") ?: "ناکام")
            }
        }
    }

    fun redeemPromoCode(code: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.redeemPromoCode(code)
            _isBusy.value = false
            if (res.isSuccess) {
                val bonus = res.getOrDefault(0.0).toInt()
                showSuccess("پرومو کوڈ استعمال ہو گیا! Rs. $bonus والٹ میں شامل کر دیے گئے۔")
            } else {
                showError(
                    res.exceptionOrNull()?.extractFriendlyFirestoreMessage("پرومو کوڈ استعمال نہیں ہو سکا۔")
                        ?: "پرومو کوڈ ناکام"
                )
            }
        }
    }

    fun updateProfile(fullName: String, phone: String) {
        if (fullName.trim().length < 2 || phone.trim().length < 7) {
            showError("درست نام اور موبائل نمبر درج کریں۔")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateProfileDetails(fullName, phone)
            _isBusy.value = false
            if (res.isSuccess) {
                showSuccess("پروفائل کی تفصیلات محفوظ ہو گئیں۔")
            } else {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("اپڈیٹ ناکام ہو گئی۔") ?: "ناکام")
            }
        }
    }

    fun updateAvatar(avatarBase64: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateUserAvatar(avatarBase64)
            _isBusy.value = false
            if (res.isSuccess) {
                showSuccess("پروفائل تصویر تبدیل ہو گئی!")
            } else {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("تصویر اپلوڈ نہیں ہو سکی۔") ?: "ناکام")
            }
        }
    }

    fun submitAiFaceVerification(faceBitmap: Bitmap) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        val userName = profile?.fullName?.ifBlank { profile.username } ?: "سرمایہ کار"

        viewModelScope.launch {
            _isBusy.value = true
            _faceAiState.value = FaceAiState.Analyzing

            val selfieBase64 = ImageEncodingUtils.bitmapToCompactBase64(faceBitmap, maxDimension = 180, quality = 68)
            val aiResult = FaceVerificationAiService.verifyFaceBitmap(faceBitmap)
            if (aiResult.isFailure) {
                _isBusy.value = false
                val errMsg = aiResult.exceptionOrNull()?.localizedMessage
                    ?: "AI فیس ویریفکیشن ناکام ہو گئی۔ براہ کرم دوبارہ کوشش کریں۔"
                _faceAiState.value = FaceAiState.Error(errMsg)
                showError(errMsg)
                return@launch
            }

            val verdict = aiResult.getOrThrow()
            val status = if (verdict.isLiveHumanFace) KycStatus.VERIFIED else KycStatus.REJECTED
            val formattedNote = if (verdict.isLiveHumanFace) {
                "AI تصدیق شدہ (${verdict.confidencePercent}%): ${verdict.reasonUrdu}"
            } else {
                "AI مسترد (${verdict.confidencePercent}%): ${verdict.reasonUrdu}"
            }

            val saveRes = repository.submitFaceVerification(
                userName = userName,
                selfieBase64 = selfieBase64,
                faceStatus = status,
                faceAiNote = formattedNote
            )
            _isBusy.value = false

            if (saveRes.isFailure) {
                val saveErr = saveRes.exceptionOrNull()?.extractFriendlyFirestoreMessage("فیس ویریفکیشن محفوظ نہیں ہو سکی۔")
                    ?: "محفوظ کرنے میں خرابی"
                _faceAiState.value = FaceAiState.Error(saveErr)
                showError(saveErr)
            } else if (verdict.isLiveHumanFace) {
                _faceAiState.value = FaceAiState.Verified(formattedNote)
                showSuccess("AI فیس ویریفکیشن کامیاب ہو گئی (${verdict.confidencePercent}%)!")
            } else {
                _faceAiState.value = FaceAiState.Rejected(formattedNote)
                showError(formattedNote)
            }
        }
    }

    fun updateTwoFactor(enabled: Boolean, pin: String?) {
        if (enabled && (pin == null || pin.trim().length !in 4..6 || !pin.trim().all { it.isDigit() })) {
            showError("2FA فعال کرنے کے لیے 4 سے 6 ہندسوں کا پن درج کریں۔")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.configureTwoFactorAuth(enabled, pin)
            _isBusy.value = false
            if (res.isSuccess) {
                showSuccess(if (enabled) "ٹو فیکٹر سیکیورٹی فعال ہو گئی۔" else "ٹو فیکٹر سیکیورٹی بند کر دی گئی۔")
            } else {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("2FA اپڈیٹ ناکام ہو گئی۔") ?: "ناکام")
            }
        }
    }

    // --- LIVE CHAT (USER & ADMIN) ---

    fun sendUserChatMessage(text: String) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        val userName = profile?.fullName?.ifBlank { profile.username } ?: "سرمایہ کار"
        viewModelScope.launch {
            val res = repository.sendUserChatMessage(userName, text)
            if (res.isFailure) {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("پیغام بھیجنے میں خرابی پیش آئی۔") ?: "پیغام ناکام")
            }
        }
    }

    fun markUserChatRead() {
        viewModelScope.launch {
            repository.markChatReadByUser(currentUserId)
        }
    }

    fun selectAdminChatThread(userId: String?) {
        _selectedChatThreadUserId.value = userId
        if (!userId.isNullOrBlank()) {
            viewModelScope.launch {
                repository.markChatReadByAdmin(userId)
            }
        }
    }

    fun adminSendChatMessage(targetUserId: String, targetUserName: String, text: String) {
        viewModelScope.launch {
            val res = repository.sendAdminChatMessage(targetUserId, targetUserName, text)
            if (res.isFailure) {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("جواب بھیجنے میں خرابی پیش آئی۔") ?: "جواب ناکام")
            }
        }
    }

    // --- ADMIN ACTIONS ---

    fun adminSaveSettings(settings: AppSettings) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateAppSettings(settings)
            _isBusy.value = false
            if (res.isSuccess) {
                showSuccess("پلیٹ فارم کی ترتیبات، فیس اور ریفرل بونس محفوظ ہو گئے۔")
            } else {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("ترتیبات محفوظ نہیں ہو سکیں۔") ?: "ناکام")
            }
        }
    }

    fun adminApproveDeposit(deposit: DepositRequest) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.approveDeposit(deposit)
            _isBusy.value = false
            if (res.isSuccess) {
                showSuccess("${deposit.userName} کا Rs. ${deposit.netAmount.toInt()} ڈپازٹ منظور ہو گیا۔")
            } else {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("منظوری ناکام ہو گئی۔") ?: "ناکام")
            }
        }
    }

    fun adminRejectDeposit(deposit: DepositRequest, reason: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.rejectDeposit(deposit, reason.ifBlank { "ایڈمن کی جانب سے مسترد" })
            _isBusy.value = false
            if (res.isSuccess) {
                showSuccess("ڈپازٹ مسترد کر دیا گیا۔")
            } else {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("مسترد کرنے میں خرابی۔") ?: "ناکام")
            }
        }
    }

    fun adminApproveWithdrawal(withdrawal: WithdrawalRequest) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.approveWithdrawal(withdrawal)
            _isBusy.value = false
            if (res.isSuccess) {
                showSuccess("Rs. ${withdrawal.amount.toInt()} کی ادائیگی منظور ہو گئی۔")
            } else {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("منظوری ناکام ہو گئی۔") ?: "ناکام")
            }
        }
    }

    fun adminRejectWithdrawal(withdrawal: WithdrawalRequest, reason: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.rejectWithdrawal(withdrawal, reason.ifBlank { "والٹ میں واپس منتقل" })
            _isBusy.value = false
            if (res.isSuccess) {
                showSuccess("درخواست مسترد اور Rs. ${withdrawal.amount.toInt()} صارف کے والٹ میں واپس کر دیے گئے۔")
            } else {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("مسترد کرنے میں خرابی۔") ?: "ناکام")
            }
        }
    }

    fun adminReviewFace(userId: String, approve: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.reviewFaceVerification(userId, approve)
            _isBusy.value = false
            if (res.isSuccess) {
                showSuccess(if (approve) "فیس ویریفکیشن منظور ہو گئی۔" else "فیس ویریفکیشن مسترد کر دی گئی۔")
            } else {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("فیس جائزہ ناکام رہا۔") ?: "ناکام")
            }
        }
    }

    fun adminCreatePromoCode(code: String, bonusAmount: Double, maxUses: Int, description: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createPromoCode(code, bonusAmount, maxUses, description)
            _isBusy.value = false
            if (res.isSuccess) {
                showSuccess("پرومو کوڈ ${code.trim().uppercase()} بنا دیا گیا!")
            } else {
                showError(res.exceptionOrNull()?.extractFriendlyFirestoreMessage("پرومو کوڈ نہیں بن سکا۔") ?: "ناکام")
            }
        }
    }

    fun adminTogglePromoCode(code: String, active: Boolean) {
        viewModelScope.launch {
            repository.togglePromoCodeActive(code, active)
        }
    }

    fun adminDeletePromoCode(code: String) {
        viewModelScope.launch {
            repository.deletePromoCode(code)
        }
    }

    fun adminMarkNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    private fun unwrapFirestore(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    private fun isRealCancellation(error: Throwable): Boolean {
        return unwrapFirestore(error) == null && error is CancellationException
    }

    private companion object {
        const val TAG = "AppViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
