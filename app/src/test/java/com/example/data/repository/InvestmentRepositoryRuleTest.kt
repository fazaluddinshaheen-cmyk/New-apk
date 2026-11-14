package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.KycStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.VipPlanCatalog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class InvestmentRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun depositApproveInvestClaim_andActivePlanPromoRedemption_endToEnd() = runBlocking {
        val tag = UUID.randomUUID().toString().take(8)
        val investorEmail = "investor_$tag@test.com"
        val adminEmail = "admin_$tag@test.com"

        val investorUid = signInTestUser(investorEmail)
        val investorRepo = InvestmentRepository(firestore, auth)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            investorRepo.registerUserProfile(
                fullName = "Bilal Ahmed",
                username = "bilal_$tag",
                phone = "03001234567",
                email = investorEmail,
                password = "secret"
            )
        }
        assertTrue(profileResult.isSuccess)
        assertEquals(0.0, profileResult.getOrThrow().balance, 0.01)

        // Submit Easypaisa deposit for VIP Plan 1 (Rs. 550)
        val depResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            investorRepo.submitDepositRequest(
                userName = "Bilal Ahmed",
                amount = 550.0,
                feePercent = 0.0,
                method = PaymentMethod.EASYPAISA,
                senderPhone = "03451112233",
                transactionRef = "TID_$tag"
            )
        }
        assertTrue(depResult.isSuccess)
        val depositId = depResult.getOrThrow()

        // Admin approves deposit & creates a promo code
        signInTestUser(adminEmail)
        val adminRepo = InvestmentRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.ensureDefaultSettingsIfMissing()
            adminRepo.grantAdminSession("Fazaluddin", "198500")
            adminRepo.createPromoCode("VIP_$tag", 150.0, 25, "Active VIP Reward")
        }

        val deposits = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllDeposits().first { list -> list.any { it.id == depositId } }
        }
        val approveRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.approveDeposit(deposits.first { it.id == depositId })
        }
        assertTrue(approveRes.isSuccess)

        // Investor tries promo BEFORE activating VIP plan -> must fail
        signInTestUser(investorEmail)
        val blockedPromoRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            investorRepo.redeemPromoCode("VIP_$tag")
        }
        assertTrue("Promo should fail without active VIP plan", blockedPromoRes.isFailure)

        // Investor activates VIP Plan 1 and claims Day 1 profit
        val vip1 = VipPlanCatalog.plans.first()
        val investRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            investorRepo.investInVipPlan(vip1, "Bilal Ahmed")
        }
        assertTrue(investRes.isSuccess)
        val invId = investRes.getOrThrow()

        // Now promo code succeeds because user has an active VIP plan
        val unlockedPromoRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            investorRepo.redeemPromoCode("VIP_$tag")
        }
        assertTrue("Promo should succeed with active VIP plan: ${unlockedPromoRes.exceptionOrNull()}", unlockedPromoRes.isSuccess)
        assertEquals(150.0, unlockedPromoRes.getOrThrow(), 0.01)

        val activeInv = withTimeout(FLOW_TIMEOUT_MS) {
            investorRepo.observeUserInvestments(investorUid).first { list -> list.any { it.id == invId } }
        }.first { it.id == invId }

        val claimRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            investorRepo.claimDailyProfit(activeInv)
        }
        assertTrue(claimRes.isSuccess)
        assertEquals(330.0, claimRes.getOrThrow(), 0.01)

        val finalProfile = withTimeout(FLOW_TIMEOUT_MS) {
            investorRepo.observeUserProfile(investorUid).first { it != null && it.balance >= 480.0 }
        }
        assertEquals(480.0, finalProfile!!.balance, 0.01)
    }

    @Test
    fun referralRs250Bonus_faceVerification_andLiveChatAdminDisplay() = runBlocking {
        val tag = UUID.randomUUID().toString().take(8)
        val inviterEmail = "inviter_$tag@test.com"
        val friendEmail = "friend_$tag@test.com"
        val adminEmail = "admin_ref_$tag@test.com"

        // 1. Inviter registers
        val inviterUid = signInTestUser(inviterEmail)
        val inviterRepo = InvestmentRepository(firestore, auth)
        val inviterProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            inviterRepo.registerUserProfile("Ali Raza", "ali_$tag", "0300111", inviterEmail, "pw123")
        }.getOrThrow()

        // 2. Friend registers using inviter's code, uploads avatar & submits Face Verification + Live Chat
        val friendUid = signInTestUser(friendEmail)
        val friendRepo = InvestmentRepository(firestore, auth)
        val friendProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            friendRepo.registerUserProfile(
                fullName = "Usman Tariq",
                username = "usman_$tag",
                phone = "0300222",
                email = friendEmail,
                password = "pw123",
                inviteCode = inviterProfile.referralCode
            )
        }.getOrThrow()
        assertEquals(inviterUid, friendProfile.referredByUserId)

        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) {
            friendRepo.updateUserAvatar("YW5kcm9pZF9hdmF0YXI=")
        }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) {
            friendRepo.submitFaceVerification(
                userName = "Usman Tariq",
                selfieBase64 = "ZmFjZV9zZWxmaWVfcGF5bG9hZA==",
                faceStatus = KycStatus.VERIFIED,
                faceAiNote = "AI تصدیق کامیاب (95%)"
            )
        }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) {
            friendRepo.sendUserChatMessage("Usman Tariq", "Hello support, please check my deposit.")
        }.isSuccess)

        val depId = withTimeout(DEFAULT_TIMEOUT_MS) {
            friendRepo.submitDepositRequest(
                userName = "Usman Tariq",
                amount = 550.0,
                feePercent = 0.0,
                method = PaymentMethod.JAZZCASH,
                senderPhone = "0300222",
                transactionRef = "REF_TID_$tag"
            )
        }.getOrThrow()

        // 3. Admin approves deposit (triggering Rs. 250 bonus to inviter), verifies Face, and replies in Live Chat as "Admin"
        signInTestUser(adminEmail)
        val adminRepo = InvestmentRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.ensureDefaultSettingsIfMissing()
            adminRepo.grantAdminSession("Fazaluddin", "198500")
        }
        val targetDep = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllDeposits().first { list -> list.any { it.id == depId } }
        }.first { it.id == depId }

        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.approveDeposit(targetDep) }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { adminRepo.reviewFaceVerification(friendUid, true) }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.sendAdminChatMessage(friendUid, "Usman Tariq", "Your deposit & face verification are approved.")
        }.isSuccess)

        // 4. Verify inviter received Rs. 250 bonus
        signInTestUser(inviterEmail)
        val updatedInviter = withTimeout(FLOW_TIMEOUT_MS) {
            inviterRepo.observeUserProfile(inviterUid).first { it != null && it.balance >= 250.0 }
        }
        assertEquals(250.0, updatedInviter!!.balance, 0.01)
        assertEquals(1, updatedInviter.referralCount)

        // 5. Verify friend has Face VERIFIED & sees Admin reply with senderName == "Admin"
        signInTestUser(friendEmail)
        val verifiedFriend = withTimeout(FLOW_TIMEOUT_MS) {
            friendRepo.observeUserProfile(friendUid).first { it?.faceStatus == KycStatus.VERIFIED.name }
        }
        assertEquals(KycStatus.VERIFIED.name, verifiedFriend?.faceStatus)

        val chatMsgs = withTimeout(FLOW_TIMEOUT_MS) {
            friendRepo.observeSupportMessages(friendUid).first { list -> list.any { it.senderRole == "ADMIN" } }
        }
        val adminReply = chatMsgs.first { it.senderRole == "ADMIN" }
        assertEquals("Admin", adminReply.senderName)
    }

    @Test
    fun crossUserRead_isRejectedWithPermissionDenied() = runBlocking {
        val tag = UUID.randomUUID().toString().take(8)
        val aliceUid = signInTestUser("alice_$tag@test.com")
        val aliceRepo = InvestmentRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.registerUserProfile("Alice", "alice_$tag", "0300", "a@test.com", "pw")
        }

        signInTestUser("bob_$tag@test.com")
        val bobRepo = InvestmentRepository(firestore, auth)
        val res = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getUserProfile(aliceUid) }
        assertTrue(res.isFailure)
        val fsEx = res.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    }

    @Test
    fun unauthenticatedObserve_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = InvestmentRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeUserDeposits("someone").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated deposit query")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
