package com.example.data.repository

import android.content.Context
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
import com.example.data.model.VipInvestment
import com.example.data.model.VipPlan
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.data.security.SecurityUtils
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class InvestmentRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.withFirestoreErrorHandling(
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser != null) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    // --- APP SETTINGS (MAINTENANCE MODE, DEPOSIT CHARGE, REFERRAL BONUS & GATEWAYS) ---

    suspend fun ensureDefaultSettingsIfMissing() {
        val path = "config/app_settings"
        val docRef = db.collection("config").document("app_settings")
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "maintenanceMode" to false,
                    "maintenanceMessage" to "Scheduled platform upgrade in progress. Deposits and withdrawals will resume shortly.",
                    "depositFeePercent" to 0.0,
                    "referralBonusAmount" to 250.0,
                    "easypaisaNumber" to "0345-8812490",
                    "easypaisaTitle" to "Crescent Wealth Merchant",
                    "jazzcashNumber" to "0300-8812490",
                    "jazzcashTitle" to "Crescent Wealth Merchant",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
        }
    }

    fun observeAppSettings(): Flow<AppSettings> = flow {
        val path = "config/app_settings"
        emitAll(
            db.collection("config").document("app_settings")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(AppSettings::class.java, ServerTimestampBehavior.ESTIMATE)
                        ?: AppSettings()
                }
                .withFirestoreErrorHandling(OperationType.GET, path)
        )
    }

    suspend fun updateAppSettings(settings: AppSettings): Result<Unit> {
        val path = "config/app_settings"
        return try {
            requireUserId()
            val payload = mapOf(
                "maintenanceMode" to settings.maintenanceMode,
                "maintenanceMessage" to settings.maintenanceMessage.trim().take(300),
                "depositFeePercent" to settings.depositFeePercent.coerceIn(0.0, 50.0),
                "referralBonusAmount" to settings.referralBonusAmount.coerceIn(0.0, 50000.0),
                "easypaisaNumber" to settings.easypaisaNumber.trim(),
                "easypaisaTitle" to settings.easypaisaTitle.trim(),
                "jazzcashNumber" to settings.jazzcashNumber.trim(),
                "jazzcashTitle" to settings.jazzcashTitle.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.document(path).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN SESSION ---

    suspend fun grantAdminSession(username: String, password: String): Result<Unit> {
        val uid = requireUserId()
        val path = "admins/$uid"
        return try {
            val hash = SecurityUtils.adminAccessHashForCredentials(username, password)
            val payload = mapOf(
                "userId" to uid,
                "adminAccessHash" to hash,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("admins").document(uid).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun revokeAdminSession(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val path = "admins/$uid"
        return try {
            db.collection("admins").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun hasActiveAdminSession(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val path = "admins/$uid"
        return try {
            db.collection("admins").document(uid).get().await().exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            false
        }
    }

    // --- USER PROFILE, REGISTRATION, REFERRALS, AVATAR, FACE & KYC ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .withFirestoreErrorHandling(OperationType.GET, path)
        )
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile?> {
        val path = "users/$userId"
        return try {
            val snap = db.collection("users").document(userId).get().await()
            val profile = if (snap.exists()) {
                snap.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun registerUserProfile(
        fullName: String,
        username: String,
        phone: String,
        email: String,
        password: String,
        inviteCode: String = ""
    ): Result<UserProfile> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val existing = db.collection("users").document(uid).get().await()
            val cleanFullName = fullName.trim()
            val cleanUsername = username.trim()
            val cleanPhone = phone.trim()
            val cleanEmail = email.trim()
            val passwordHash = SecurityUtils.sha256(password)

            if (!existing.exists()) {
                val ownCode = "CW" + uid.replace(Regex("[^A-Za-z0-9]"), "").take(6).uppercase()
                    .padEnd(6, 'X')

                val cleanInvite = inviteCode.trim().uppercase()
                var inviterUid: String? = null
                var appliedCode: String? = null
                if (cleanInvite.length >= 4 && cleanInvite != ownCode) {
                    val codeSnap = db.collection("referral_codes").document(cleanInvite).get().await()
                    val candidateOwner = codeSnap.getString("ownerUserId")
                    if (codeSnap.exists() && !candidateOwner.isNullOrBlank() && candidateOwner != uid) {
                        inviterUid = candidateOwner
                        appliedCode = cleanInvite
                    }
                }

                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "fullName" to cleanFullName,
                    "username" to cleanUsername,
                    "phone" to cleanPhone,
                    "email" to cleanEmail,
                    "passwordHash" to passwordHash,
                    "balance" to 0.0,
                    "totalInvested" to 0.0,
                    "totalEarnings" to 0.0,
                    "referralCode" to ownCode,
                    "referralRewardPaid" to false,
                    "referralCount" to 0,
                    "referralEarnings" to 0.0,
                    "hasActivePlan" to false,
                    "faceStatus" to KycStatus.UNVERIFIED.name,
                    "kycStatus" to KycStatus.UNVERIFIED.name,
                    "twoFactorEnabled" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (appliedCode != null && inviterUid != null) {
                    payload["referredByCode"] = appliedCode
                    payload["referredByUserId"] = inviterUid
                }

                val batch = db.batch()
                batch.set(db.collection("users").document(uid), payload)
                batch.set(
                    db.collection("referral_codes").document(ownCode),
                    mapOf(
                        "code" to ownCode,
                        "ownerUserId" to uid,
                        "ownerName" to cleanFullName,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
            } else {
                db.collection("users").document(uid).update(
                    mapOf(
                        "fullName" to cleanFullName,
                        "username" to cleanUsername,
                        "phone" to cleanPhone,
                        "email" to cleanEmail,
                        "passwordHash" to passwordHash,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val updated = db.collection("users").document(uid).get().await()
                .toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalStateException("Profile creation failed")
            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfileDetails(fullName: String, phone: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "fullName" to fullName.trim(),
                    "phone" to phone.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserAvatar(avatarBase64: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "avatarBase64" to avatarBase64,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitFaceVerification(
        userName: String,
        selfieBase64: String,
        faceStatus: KycStatus = KycStatus.PENDING,
        faceAiNote: String? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        val cleanName = userName.ifBlank { "سرمایہ کار" }
        val cleanNote = faceAiNote?.trim()?.take(240)?.takeIf { it.isNotEmpty() }
        return try {
            val batch = db.batch()
            val userUpdates = mutableMapOf<String, Any>(
                "faceSelfieBase64" to selfieBase64,
                "faceStatus" to faceStatus.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanNote != null) {
                userUpdates["faceAiNote"] = cleanNote
            }
            batch.update(
                db.collection("users").document(uid),
                userUpdates
            )
            val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val notifTitle = when (faceStatus) {
                KycStatus.VERIFIED -> "AI فیس ویریفکیشن کامیاب"
                KycStatus.REJECTED -> "AI فیس ویریفکیشن مسترد"
                else -> "فیس ویریفکیشن سیلفی"
            }
            val notifMsg = cleanNote?.let { "$cleanName: $it" }
                ?: "$cleanName نے لائیو فیس ویریفکیشن سیلفی جمع کروائی ہے۔"
            batch.set(
                db.collection("admin_notifications").document(notifId),
                mapOf(
                    "userId" to uid,
                    "userName" to cleanName,
                    "type" to "FACE_VERIFICATION",
                    "title" to notifTitle.take(100),
                    "message" to notifMsg.take(240),
                    "referenceId" to uid,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun configureTwoFactorAuth(enabled: Boolean, pin: String?): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val updates = mutableMapOf<String, Any>(
                "twoFactorEnabled" to enabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (enabled && !pin.isNullOrBlank()) {
                updates["twoFactorPinHash"] = SecurityUtils.sha256(pin.trim())
            }
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- USER DATA STREAMS ---

    fun observeUserDeposits(userId: String): Flow<List<DepositRequest>> = flow {
        val path = "deposits"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DepositRequest::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .withFirestoreErrorHandling(OperationType.LIST, path)
        )
    }

    fun observeUserWithdrawals(userId: String): Flow<List<WithdrawalRequest>> = flow {
        val path = "withdrawals"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(WithdrawalRequest::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .withFirestoreErrorHandling(OperationType.LIST, path)
        )
    }

    fun observeUserInvestments(userId: String): Flow<List<VipInvestment>> = flow {
        val path = "investments"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(VipInvestment::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .withFirestoreErrorHandling(OperationType.LIST, path)
        )
    }

    fun observeUserTransactions(userId: String): Flow<List<WalletTransaction>> = flow {
        val path = "transactions"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(WalletTransaction::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .withFirestoreErrorHandling(OperationType.LIST, path)
        )
    }

    // --- DEPOSITS, WITHDRAWALS, VIP INVESTMENTS & PROMO CODES ---

    suspend fun submitDepositRequest(
        userName: String,
        amount: Double,
        feePercent: Double,
        method: PaymentMethod,
        senderPhone: String,
        transactionRef: String
    ): Result<String> {
        val uid = requireUserId()
        val depositId = "dep_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        val feeAmount = ((amount * feePercent) / 100.0).coerceAtLeast(0.0)
        val netAmount = (amount - feeAmount).coerceAtLeast(1.0)

        return try {
            val batch = db.batch()
            batch.set(
                db.collection("deposits").document(depositId),
                mapOf(
                    "userId" to uid,
                    "userName" to userName,
                    "amount" to amount,
                    "feeAmount" to feeAmount,
                    "netAmount" to netAmount,
                    "method" to method.name,
                    "senderPhone" to senderPhone.trim(),
                    "transactionRef" to transactionRef.trim(),
                    "status" to RequestStatus.PENDING.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("transactions").document(txId),
                mapOf(
                    "userId" to uid,
                    "type" to "DEPOSIT",
                    "amount" to netAmount,
                    "fee" to feeAmount,
                    "method" to method.label,
                    "referenceId" to depositId,
                    "title" to "${method.label} Deposit",
                    "description" to "TID: ${transactionRef.trim()} • Gross Rs. ${amount.toInt()}",
                    "status" to RequestStatus.PENDING.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("admin_notifications").document(notifId),
                mapOf(
                    "userId" to uid,
                    "userName" to userName,
                    "type" to "DEPOSIT_REQUEST",
                    "title" to "New ${method.label} Deposit",
                    "message" to "$userName requested Rs. ${amount.toInt()} deposit (TID: ${transactionRef.trim()})",
                    "referenceId" to depositId,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(depositId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "deposits/$depositId")
            Result.failure(e)
        }
    }

    suspend fun submitWithdrawalRequest(
        userName: String,
        amount: Double,
        method: PaymentMethod,
        accountTitle: String,
        accountNumber: String
    ): Result<String> {
        val uid = requireUserId()
        val withdrawalId = "wd_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        return try {
            db.runTransaction { transaction ->
                val userRef = db.collection("users").document(uid)
                val userSnap = transaction.get(userRef)
                val currentBalance = userSnap.getDouble("balance") ?: 0.0
                if (currentBalance < amount) {
                    throw IllegalStateException("Insufficient wallet balance for withdrawal.")
                }

                transaction.update(
                    userRef,
                    mapOf(
                        "balance" to currentBalance - amount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    db.collection("withdrawals").document(withdrawalId),
                    mapOf(
                        "userId" to uid,
                        "userName" to userName,
                        "amount" to amount,
                        "method" to method.name,
                        "accountTitle" to accountTitle.trim(),
                        "accountNumber" to accountNumber.trim(),
                        "status" to RequestStatus.PENDING.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    db.collection("transactions").document(txId),
                    mapOf(
                        "userId" to uid,
                        "type" to "WITHDRAWAL",
                        "amount" to amount,
                        "fee" to 0.0,
                        "method" to method.label,
                        "referenceId" to withdrawalId,
                        "title" to "${method.label} Withdrawal",
                        "description" to "${accountTitle.trim()} (${accountNumber.trim()})",
                        "status" to RequestStatus.PENDING.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    db.collection("admin_notifications").document(notifId),
                    mapOf(
                        "userId" to uid,
                        "userName" to userName,
                        "type" to "WITHDRAWAL_REQUEST",
                        "title" to "New ${method.label} Withdrawal",
                        "message" to "$userName requested Rs. ${amount.toInt()} payout to ${accountNumber.trim()}",
                        "referenceId" to withdrawalId,
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(withdrawalId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "withdrawals/$withdrawalId")
            Result.failure(e)
        }
    }

    suspend fun investInVipPlan(plan: VipPlan, userName: String): Result<String> {
        val uid = requireUserId()
        val investmentId = "inv_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        return try {
            db.runTransaction { transaction ->
                val userRef = db.collection("users").document(uid)
                val userSnap = transaction.get(userRef)
                val currentBalance = userSnap.getDouble("balance") ?: 0.0
                val currentInvested = userSnap.getDouble("totalInvested") ?: 0.0

                if (currentBalance < plan.investmentAmount) {
                    throw IllegalStateException("Insufficient balance. Deposit funds to activate ${plan.name}.")
                }

                transaction.update(
                    userRef,
                    mapOf(
                        "balance" to currentBalance - plan.investmentAmount,
                        "totalInvested" to currentInvested + plan.investmentAmount,
                        "hasActivePlan" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    db.collection("investments").document(investmentId),
                    mapOf(
                        "userId" to uid,
                        "planId" to plan.id,
                        "planName" to plan.name,
                        "investedAmount" to plan.investmentAmount,
                        "dailyReturn" to plan.dailyReturn,
                        "durationDays" to plan.durationDays,
                        "daysClaimed" to 0,
                        "totalEarned" to 0.0,
                        "status" to "ACTIVE",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    db.collection("transactions").document(txId),
                    mapOf(
                        "userId" to uid,
                        "type" to "INVESTMENT",
                        "amount" to plan.investmentAmount,
                        "fee" to 0.0,
                        "method" to "Wallet",
                        "referenceId" to investmentId,
                        "title" to "Activated ${plan.name}",
                        "description" to "Daily Rs. ${plan.dailyReturn.toInt()} • ${plan.durationDays} Days",
                        "status" to "COMPLETED",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    db.collection("admin_notifications").document(notifId),
                    mapOf(
                        "userId" to uid,
                        "userName" to userName,
                        "type" to "NEW_INVESTMENT",
                        "title" to "VIP Plan Activated",
                        "message" to "$userName invested Rs. ${plan.investmentAmount.toInt()} in ${plan.name}",
                        "referenceId" to investmentId,
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(investmentId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "investments/$investmentId")
            Result.failure(e)
        }
    }

    suspend fun claimDailyProfit(investment: VipInvestment): Result<Double> {
        val uid = requireUserId()
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        return try {
            db.runTransaction { transaction ->
                val invRef = db.collection("investments").document(investment.id)
                val userRef = db.collection("users").document(uid)

                val invSnap = transaction.get(invRef)
                val userSnap = transaction.get(userRef)

                val claimed = (invSnap.getLong("daysClaimed") ?: 0L).toInt()
                val duration = (invSnap.getLong("durationDays") ?: 7L).toInt()
                val status = invSnap.getString("status") ?: "ACTIVE"
                val daily = invSnap.getDouble("dailyReturn") ?: investment.dailyReturn
                val prevEarned = invSnap.getDouble("totalEarned") ?: 0.0

                if (status != "ACTIVE" || claimed >= duration) {
                    throw IllegalStateException("This VIP plan has already completed all payouts.")
                }

                val newClaimed = claimed + 1
                val newStatus = if (newClaimed >= duration) "COMPLETED" else "ACTIVE"
                val curBalance = userSnap.getDouble("balance") ?: 0.0
                val curTotalEarnings = userSnap.getDouble("totalEarnings") ?: 0.0

                transaction.update(
                    invRef,
                    mapOf(
                        "daysClaimed" to newClaimed,
                        "totalEarned" to prevEarned + daily,
                        "status" to newStatus,
                        "lastClaimedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    userRef,
                    mapOf(
                        "balance" to curBalance + daily,
                        "totalEarnings" to curTotalEarnings + daily,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    db.collection("transactions").document(txId),
                    mapOf(
                        "userId" to uid,
                        "type" to "DAILY_PROFIT",
                        "amount" to daily,
                        "fee" to 0.0,
                        "method" to investment.planName,
                        "referenceId" to investment.id,
                        "title" to "${investment.planName} Payout",
                        "description" to "Day $newClaimed of $duration daily profit credited",
                        "status" to "COMPLETED",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                daily
            }.await()
            Result.success(investment.dailyReturn)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "investments/${investment.id}")
            Result.failure(e)
        }
    }

    suspend fun redeemPromoCode(code: String): Result<Double> {
        val uid = requireUserId()
        val cleanCode = code.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "")
        if (cleanCode.length < 3) {
            return Result.failure(IllegalArgumentException("Enter a valid promo code."))
        }
        val redemptionId = "${cleanCode}_${uid}"
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        return try {
            val amount = db.runTransaction { transaction ->
                val userRef = db.collection("users").document(uid)
                val promoRef = db.collection("promo_codes").document(cleanCode)
                val redemptionRef = db.collection("promo_redemptions").document(redemptionId)

                // Perform all reads before writes
                val userSnap = transaction.get(userRef)
                val promoSnap = transaction.get(promoRef)
                val redemptionSnap = transaction.get(redemptionRef)

                val hasActivePlan = userSnap.getBoolean("hasActivePlan") ?: false
                if (!hasActivePlan) {
                    throw IllegalStateException("Promo codes require an active VIP plan. Invest in any VIP plan first.")
                }
                if (!promoSnap.exists()) {
                    throw IllegalStateException("Promo code '$cleanCode' does not exist.")
                }
                val isActive = promoSnap.getBoolean("isActive") ?: false
                if (!isActive) {
                    throw IllegalStateException("Promo code '$cleanCode' is currently disabled.")
                }
                val usedCount = (promoSnap.getLong("usedCount") ?: 0L).toInt()
                val maxUses = (promoSnap.getLong("maxUses") ?: 0L).toInt()
                if (usedCount >= maxUses) {
                    throw IllegalStateException("Promo code '$cleanCode' has reached its maximum uses.")
                }
                if (redemptionSnap.exists()) {
                    throw IllegalStateException("You have already redeemed promo code '$cleanCode'.")
                }

                val bonusAmount = promoSnap.getDouble("bonusAmount") ?: 0.0
                val curBalance = userSnap.getDouble("balance") ?: 0.0
                val curEarnings = userSnap.getDouble("totalEarnings") ?: 0.0

                transaction.update(
                    promoRef,
                    mapOf(
                        "usedCount" to usedCount + 1,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    redemptionRef,
                    mapOf(
                        "promoCode" to cleanCode,
                        "userId" to uid,
                        "bonusAmount" to bonusAmount,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    userRef,
                    mapOf(
                        "balance" to curBalance + bonusAmount,
                        "totalEarnings" to curEarnings + bonusAmount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    db.collection("transactions").document(txId),
                    mapOf(
                        "userId" to uid,
                        "type" to "PROMO_BONUS",
                        "amount" to bonusAmount,
                        "fee" to 0.0,
                        "method" to "Promo Code",
                        "referenceId" to cleanCode,
                        "title" to "Promo Bonus ($cleanCode)",
                        "description" to "Active VIP member promo reward credited",
                        "status" to "COMPLETED",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                bonusAmount
            }.await()
            Result.success(amount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "promo_codes/$cleanCode")
            Result.failure(e)
        }
    }

    // --- LIVE CHAT SUPPORT (USER & ADMIN; ADMIN ALWAYS DISPLAYS "Admin") ---

    fun observeSupportThread(userId: String): Flow<SupportThread?> = flow {
        val path = "support_chats/$userId"
        emitAll(
            db.collection("support_chats").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(SupportThread::class.java, ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .withFirestoreErrorHandling(OperationType.GET, path)
        )
    }

    fun observeSupportMessages(userId: String): Flow<List<SupportMessage>> = flow {
        val path = "support_chats/$userId/messages"
        emitAll(
            db.collection("support_chats").document(userId).collection("messages")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SupportMessage::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(Long.MAX_VALUE / 1000, 0) }
                }
                .withFirestoreErrorHandling(OperationType.LIST, path)
        )
    }

    fun observeAllSupportThreads(): Flow<List<SupportThread>> = flow {
        val path = "support_chats"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SupportThread::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
                .withFirestoreErrorHandling(OperationType.LIST, path)
        )
    }

    suspend fun sendUserChatMessage(userName: String, text: String): Result<Unit> {
        val uid = requireUserId()
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        val cleanName = userName.trim().ifBlank { "Investor" }
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "support_chats/$uid/messages/$msgId"

        return try {
            val threadRef = db.collection("support_chats").document(uid)
            val existingThread = threadRef.get().await()
            val prevUnread = (existingThread.getLong("unreadByAdmin") ?: 0L).toInt()

            val batch = db.batch()
            batch.set(
                threadRef,
                mapOf(
                    "userId" to uid,
                    "userName" to cleanName,
                    "lastMessage" to cleanText.take(200),
                    "unreadByAdmin" to prevUnread + 1,
                    "unreadByUser" to 0,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                threadRef.collection("messages").document(msgId),
                mapOf(
                    "userId" to uid,
                    "senderRole" to "USER",
                    "senderName" to cleanName,
                    "text" to cleanText.take(1000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("admin_notifications").document(notifId),
                mapOf(
                    "userId" to uid,
                    "userName" to cleanName,
                    "type" to "SUPPORT_CHAT",
                    "title" to "Live Chat Message",
                    "message" to "$cleanName: ${cleanText.take(120)}",
                    "referenceId" to uid,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendAdminChatMessage(
        targetUserId: String,
        targetUserName: String,
        text: String
    ): Result<Unit> {
        requireUserId()
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Reply cannot be empty."))
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "support_chats/$targetUserId/messages/$msgId"

        return try {
            val threadRef = db.collection("support_chats").document(targetUserId)
            val existingThread = threadRef.get().await()
            val prevUnreadByUser = (existingThread.getLong("unreadByUser") ?: 0L).toInt()

            val batch = db.batch()
            batch.set(
                threadRef,
                mapOf(
                    "userId" to targetUserId,
                    "userName" to targetUserName.ifBlank { "Investor" },
                    "lastMessage" to "Admin: ${cleanText.take(180)}",
                    "unreadByAdmin" to 0,
                    "unreadByUser" to prevUnreadByUser + 1,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            // Strictly enforce senderName == "Admin" so admin identity stays hidden
            batch.set(
                threadRef.collection("messages").document(msgId),
                mapOf(
                    "userId" to targetUserId,
                    "senderRole" to "ADMIN",
                    "senderName" to "Admin",
                    "text" to cleanText.take(1000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markChatReadByUser(userId: String) {
        try {
            val ref = db.collection("support_chats").document(userId)
            if (ref.get().await().exists()) {
                ref.update(
                    mapOf(
                        "unreadByUser" to 0,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun markChatReadByAdmin(userId: String) {
        try {
            val ref = db.collection("support_chats").document(userId)
            if (ref.get().await().exists()) {
                ref.update(
                    mapOf(
                        "unreadByAdmin" to 0,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (_: Exception) {
        }
    }

    // --- ADMIN REAL-TIME STREAMS & ACTIONS ---

    fun observeAllDeposits(): Flow<List<DepositRequest>> = flow {
        val path = "deposits"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DepositRequest::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .withFirestoreErrorHandling(OperationType.LIST, path)
        )
    }

    fun observeAllWithdrawals(): Flow<List<WithdrawalRequest>> = flow {
        val path = "withdrawals"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(WithdrawalRequest::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .withFirestoreErrorHandling(OperationType.LIST, path)
        )
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .withFirestoreErrorHandling(OperationType.LIST, path)
        )
    }

    fun observeAdminNotifications(): Flow<List<AdminNotification>> = flow {
        val path = "admin_notifications"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(AdminNotification::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .withFirestoreErrorHandling(OperationType.LIST, path)
        )
    }

    fun observeAllPromoCodes(): Flow<List<PromoCode>> = flow {
        val path = "promo_codes"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PromoCode::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .withFirestoreErrorHandling(OperationType.LIST, path)
        )
    }

    suspend fun createPromoCode(
        code: String,
        bonusAmount: Double,
        maxUses: Int,
        description: String
    ): Result<Unit> {
        val cleanCode = code.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "")
        if (cleanCode.length < 3) return Result.failure(IllegalArgumentException("Code must be at least 3 characters."))
        if (bonusAmount <= 0) return Result.failure(IllegalArgumentException("Bonus amount must be positive."))
        val path = "promo_codes/$cleanCode"
        return try {
            requireUserId()
            db.collection("promo_codes").document(cleanCode).set(
                mapOf(
                    "code" to cleanCode,
                    "bonusAmount" to bonusAmount,
                    "maxUses" to maxUses.coerceIn(1, 1_000_000),
                    "usedCount" to 0,
                    "isActive" to true,
                    "description" to description.trim().ifBlank { "VIP Member Bonus" }.take(160),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePromoCodeActive(code: String, active: Boolean): Result<Unit> {
        val path = "promo_codes/$code"
        return try {
            requireUserId()
            db.collection("promo_codes").document(code).update(
                mapOf(
                    "isActive" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePromoCode(code: String): Result<Unit> {
        val path = "promo_codes/$code"
        return try {
            requireUserId()
            db.collection("promo_codes").document(code).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun approveDeposit(deposit: DepositRequest, note: String = "Approved by Admin"): Result<Unit> {
        return try {
            requireUserId()
            val linkedTx = db.collection("transactions")
                .whereEqualTo("referenceId", deposit.id)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
            val referralTxId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"

            db.runTransaction { transaction ->
                val depRef = db.collection("deposits").document(deposit.id)
                val userRef = db.collection("users").document(deposit.userId)
                val settingsRef = db.collection("config").document("app_settings")

                // Perform ALL reads before any writes
                val depSnap = transaction.get(depRef)
                val userSnap = transaction.get(userRef)
                val settingsSnap = transaction.get(settingsRef)

                val referredByUserId = userSnap.getString("referredByUserId")
                val referralRewardPaid = userSnap.getBoolean("referralRewardPaid") ?: false
                val inviterRef = if (!referredByUserId.isNullOrBlank() && !referralRewardPaid && referredByUserId != deposit.userId) {
                    db.collection("users").document(referredByUserId)
                } else null
                val inviterSnap = inviterRef?.let { transaction.get(it) }

                if (depSnap.getString("status") != RequestStatus.PENDING.name) {
                    throw IllegalStateException("Deposit has already been processed.")
                }

                val currentBalance = userSnap.getDouble("balance") ?: 0.0
                val payReferral = inviterSnap != null && inviterSnap.exists()
                val referralBonus = (settingsSnap.getDouble("referralBonusAmount") ?: 250.0).coerceAtLeast(0.0)

                transaction.update(
                    depRef,
                    mapOf(
                        "status" to RequestStatus.APPROVED.name,
                        "adminNote" to note,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val userUpdates = mutableMapOf<String, Any>(
                    "balance" to currentBalance + deposit.netAmount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (payReferral && referralBonus > 0.0) {
                    userUpdates["referralRewardPaid"] = true
                }
                transaction.update(userRef, userUpdates)

                if (payReferral && referralBonus > 0.0) {
                    val invRef = inviterRef!!
                    val invSnap = inviterSnap!!
                    val invBal = invSnap.getDouble("balance") ?: 0.0
                    val invEarn = invSnap.getDouble("referralEarnings") ?: 0.0
                    val invCount = (invSnap.getLong("referralCount") ?: 0L).toInt()
                    transaction.update(
                        invRef,
                        mapOf(
                            "balance" to invBal + referralBonus,
                            "referralEarnings" to invEarn + referralBonus,
                            "referralCount" to invCount + 1,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    transaction.set(
                        db.collection("transactions").document(referralTxId),
                        mapOf(
                            "userId" to referredByUserId!!,
                            "type" to "REFERRAL_BONUS",
                            "amount" to referralBonus,
                            "fee" to 0.0,
                            "method" to "Invite Bonus",
                            "referenceId" to deposit.id,
                            "title" to "Invite Friend Bonus",
                            "description" to "Rs. ${referralBonus.toInt()} reward for ${deposit.userName}'s deposit",
                            "status" to "COMPLETED",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (linkedTx != null) {
                    transaction.update(
                        linkedTx.reference,
                        mapOf(
                            "status" to RequestStatus.APPROVED.name,
                            "description" to "${linkedTx.getString("description") ?: ""} • $note".trim(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "deposits/${deposit.id}")
            Result.failure(e)
        }
    }

    suspend fun rejectDeposit(deposit: DepositRequest, reason: String = "Invalid TID or payment not received"): Result<Unit> {
        return try {
            requireUserId()
            val linkedTx = db.collection("transactions")
                .whereEqualTo("referenceId", deposit.id)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()

            val batch = db.batch()
            batch.update(
                db.collection("deposits").document(deposit.id),
                mapOf(
                    "status" to RequestStatus.REJECTED.name,
                    "adminNote" to reason,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (linkedTx != null) {
                batch.update(
                    linkedTx.reference,
                    mapOf(
                        "status" to RequestStatus.REJECTED.name,
                        "description" to reason,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "deposits/${deposit.id}")
            Result.failure(e)
        }
    }

    suspend fun approveWithdrawal(withdrawal: WithdrawalRequest, note: String = "Disbursed via ${withdrawal.method}"): Result<Unit> {
        return try {
            requireUserId()
            val linkedTx = db.collection("transactions")
                .whereEqualTo("referenceId", withdrawal.id)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()

            val batch = db.batch()
            batch.update(
                db.collection("withdrawals").document(withdrawal.id),
                mapOf(
                    "status" to RequestStatus.APPROVED.name,
                    "adminNote" to note,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (linkedTx != null) {
                batch.update(
                    linkedTx.reference,
                    mapOf(
                        "status" to RequestStatus.APPROVED.name,
                        "description" to "${linkedTx.getString("description") ?: ""} • $note".trim(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "withdrawals/${withdrawal.id}")
            Result.failure(e)
        }
    }

    suspend fun rejectWithdrawal(withdrawal: WithdrawalRequest, reason: String = "Account mismatch — refunded to wallet"): Result<Unit> {
        return try {
            requireUserId()
            val linkedTx = db.collection("transactions")
                .whereEqualTo("referenceId", withdrawal.id)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
            val refundTxId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"

            db.runTransaction { transaction ->
                val wdRef = db.collection("withdrawals").document(withdrawal.id)
                val userRef = db.collection("users").document(withdrawal.userId)
                val wdSnap = transaction.get(wdRef)
                val userSnap = transaction.get(userRef)

                if (wdSnap.getString("status") != RequestStatus.PENDING.name) {
                    throw IllegalStateException("Withdrawal has already been processed.")
                }

                val currentBalance = userSnap.getDouble("balance") ?: 0.0
                transaction.update(
                    wdRef,
                    mapOf(
                        "status" to RequestStatus.REJECTED.name,
                        "adminNote" to reason,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    userRef,
                    mapOf(
                        "balance" to currentBalance + withdrawal.amount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (linkedTx != null) {
                    transaction.update(
                        linkedTx.reference,
                        mapOf(
                            "status" to RequestStatus.REJECTED.name,
                            "description" to reason,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                transaction.set(
                    db.collection("transactions").document(refundTxId),
                    mapOf(
                        "userId" to withdrawal.userId,
                        "type" to "REFUND",
                        "amount" to withdrawal.amount,
                        "fee" to 0.0,
                        "method" to "Wallet Refund",
                        "referenceId" to withdrawal.id,
                        "title" to "Withdrawal Refunded",
                        "description" to reason,
                        "status" to "COMPLETED",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "withdrawals/${withdrawal.id}")
            Result.failure(e)
        }
    }

    suspend fun reviewFaceVerification(userId: String, approve: Boolean): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireUserId()
            val status = if (approve) KycStatus.VERIFIED.name else KycStatus.REJECTED.name
            val note = if (approve) "ایڈمن کی جانب سے فیس ویریفکیشن منظور شدہ" else "ایڈمن کی جانب سے مسترد — براہ کرم واضح سیلفی دوبارہ جمع کریں"
            db.collection("users").document(userId).update(
                mapOf(
                    "faceStatus" to status,
                    "faceAiNote" to note,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val path = "admin_notifications/$notificationId"
        return try {
            requireUserId()
            db.collection("admin_notifications").document(notificationId)
                .update("isRead", true)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
