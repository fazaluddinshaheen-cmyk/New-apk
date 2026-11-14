package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PaymentMethod(val label: String) {
    EASYPAISA("ایزی پیسہ"),
    JAZZCASH("جاز کیش");

    companion object {
        fun fromString(value: String): PaymentMethod =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EASYPAISA
    }
}

enum class KycStatus(val label: String) {
    UNVERIFIED("جمع نہیں ہوئی"),
    PENDING("زیر غور"),
    VERIFIED("تصدیق شدہ"),
    REJECTED("مسترد");

    companion object {
        fun fromString(value: String): KycStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNVERIFIED
    }
}

enum class RequestStatus(val label: String) {
    PENDING("زیر غور"),
    APPROVED("منظور شدہ"),
    REJECTED("مسترد");

    companion object {
        fun fromString(value: String): RequestStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
    }
}

data class VipPlan(
    val id: String,
    val name: String,
    val badge: String,
    val investmentAmount: Double,
    val dailyReturn: Double,
    val durationDays: Int,
    val isMega: Boolean = false
) {
    val totalReturn: Double get() = dailyReturn * durationDays
    val netProfit: Double get() = totalReturn - investmentAmount
}

object VipPlanCatalog {
    val plans: List<VipPlan> = listOf(
        VipPlan(
            id = "vip_1",
            name = "VIP پلان 1",
            badge = "ابتدائی",
            investmentAmount = 550.0,
            dailyReturn = 330.0,
            durationDays = 7
        ),
        VipPlan(
            id = "vip_2",
            name = "VIP پلان 2",
            badge = "ترقی",
            investmentAmount = 1350.0,
            dailyReturn = 690.0,
            durationDays = 7
        ),
        VipPlan(
            id = "vip_3",
            name = "VIP پلان 3",
            badge = "پریمیم",
            investmentAmount = 2750.0,
            dailyReturn = 1330.0,
            durationDays = 7
        ),
        VipPlan(
            id = "vip_4",
            name = "VIP پلان 4",
            badge = "ایگزیکٹو",
            investmentAmount = 5500.0,
            dailyReturn = 2750.0,
            durationDays = 7
        ),
        VipPlan(
            id = "vip_5",
            name = "VIP 5 میگا پلان",
            badge = "میگا",
            investmentAmount = 6500.0,
            dailyReturn = 3300.0,
            durationDays = 10,
            isMega = true
        ),
        VipPlan(
            id = "vip_6",
            name = "VIP 6 میگا 2 پلان",
            badge = "میگا ایلیٹ",
            investmentAmount = 13200.0,
            dailyReturn = 6700.0,
            durationDays = 10,
            isMega = true
        )
    )
}

@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "app_settings",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "پلیٹ فارم کی اپ گریڈیشن جاری ہے۔ ڈپازٹ اور رقم نکالنے کی سروس جلد بحال کر دی جائے گی۔",
    val depositFeePercent: Double = 0.0,
    val referralBonusAmount: Double = 250.0,
    val easypaisaNumber: String = "0345-8812490",
    val easypaisaTitle: String = "Crescent Wealth Merchant",
    val jazzcashNumber: String = "0300-8812490",
    val jazzcashTitle: String = "Crescent Wealth Merchant",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val phone: String = "",
    val email: String = "",
    val passwordHash: String = "",
    val balance: Double = 0.0,
    val totalInvested: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val referralCode: String = "",
    val referredByCode: String? = null,
    val referredByUserId: String? = null,
    val referralRewardPaid: Boolean = false,
    val referralCount: Int = 0,
    val referralEarnings: Double = 0.0,
    val hasActivePlan: Boolean = false,
    val avatarBase64: String? = null,
    val faceSelfieBase64: String? = null,
    val faceStatus: String = KycStatus.UNVERIFIED.name,
    val faceAiNote: String? = null,
    val kycStatus: String = KycStatus.UNVERIFIED.name,
    val kycFullName: String? = null,
    val kycCnic: String? = null,
    val kycDocumentType: String? = null,
    val kycNote: String? = null,
    val twoFactorEnabled: Boolean = false,
    val twoFactorPinHash: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ReferralCodeMapping(
    @DocumentId val id: String = "",
    val code: String = "",
    val ownerUserId: String = "",
    val ownerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PromoCode(
    @DocumentId val id: String = "",
    val code: String = "",
    val bonusAmount: Double = 0.0,
    val maxUses: Int = 100,
    val usedCount: Int = 0,
    val isActive: Boolean = true,
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PromoRedemption(
    @DocumentId val id: String = "",
    val promoCode: String = "",
    val userId: String = "",
    val bonusAmount: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SupportThread(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val lastMessage: String = "",
    val unreadByAdmin: Int = 0,
    val unreadByUser: Int = 0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SupportMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val senderRole: String = "USER",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DepositRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val amount: Double = 0.0,
    val feeAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val method: String = PaymentMethod.EASYPAISA.name,
    val senderPhone: String = "",
    val transactionRef: String = "",
    val status: String = RequestStatus.PENDING.name,
    val adminNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val amount: Double = 0.0,
    val method: String = PaymentMethod.EASYPAISA.name,
    val accountTitle: String = "",
    val accountNumber: String = "",
    val status: String = RequestStatus.PENDING.name,
    val adminNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VipInvestment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val planId: String = "",
    val planName: String = "",
    val investedAmount: Double = 0.0,
    val dailyReturn: Double = 0.0,
    val durationDays: Int = 7,
    val daysClaimed: Int = 0,
    val totalEarned: Double = 0.0,
    val status: String = "ACTIVE",
    val lastClaimedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "DEPOSIT",
    val amount: Double = 0.0,
    val fee: Double = 0.0,
    val method: String = "",
    val referenceId: String = "",
    val title: String = "",
    val description: String = "",
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val type: String = "SYSTEM",
    val title: String = "",
    val message: String = "",
    val referenceId: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
