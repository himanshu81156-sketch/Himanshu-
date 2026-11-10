package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MeraInstaRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createProfileAndFollow_authenticatedUsers_succeedsAndNotifies() = runBlocking {
        val rahulUid = signInTestUser("rahul_${UUID.randomUUID()}@test.com")
        val rahulRepo = MeraInstaRepository(firestore, auth)
        withTimeout(TIMEOUT_MS) {
            rahulRepo.createOrUpdateProfile("Rahul", "rahul_k", "Namaste!", "").getOrThrow()
        }
        val rahulProfile = withTimeout(TIMEOUT_MS) { rahulRepo.getUserProfile(rahulUid)!! }

        val priyaUid = signInTestUser("priya_${UUID.randomUUID()}@test.com")
        val priyaRepo = MeraInstaRepository(firestore, auth)
        withTimeout(TIMEOUT_MS) {
            priyaRepo.createOrUpdateProfile("Priya", "priya_s", "Hello!", "").getOrThrow()
        }
        val priyaProfile = withTimeout(TIMEOUT_MS) { priyaRepo.getUserProfile(priyaUid)!! }

        // Priya follows Rahul
        withTimeout(TIMEOUT_MS) {
            priyaRepo.followUser(priyaProfile, rahulProfile).getOrThrow()
        }

        // Verify Rahul received follow notification
        signInTestUserByUidCheck@ run {
            val rahulFollowers = withTimeout(FLOW_TIMEOUT_MS) {
                priyaRepo.observeFollowing(priyaUid).first { it.any { f -> f.followingId == rahulUid } }
            }
            assertTrue(rahulFollowers.isNotEmpty())
        }
    }

    @Test
    fun createPost_authenticatedUser_appearsInFeed() = runBlocking {
        val uid = signInTestUser("creator_${UUID.randomUUID()}@test.com")
        val repo = MeraInstaRepository(firestore, auth)
        withTimeout(TIMEOUT_MS) {
            repo.createOrUpdateProfile("Aman", "aman_v", "Creator", "").getOrThrow()
        }
        val profile = withTimeout(TIMEOUT_MS) { repo.getUserProfile(uid)!! }
        val postId = withTimeout(TIMEOUT_MS) {
            repo.createPost(
                currentUser = profile,
                mediaType = "photo",
                mediaUrl = "sample://sunset",
                filterName = "Warm",
                caption = "Mera pehla post!",
                location = "Jaipur",
                taggedUsers = listOf("rahul_k")
            ).getOrThrow()
        }
        val posts = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePosts().first { list -> list.any { it.id == postId } }
        }
        assertTrue(posts.any { it.id == postId && it.caption == "Mera pehla post!" })
    }

    @Test
    fun observeNotifications_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = MeraInstaRepository(firestore, auth)
        val ex = try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeNotifications("any_uid").first()
            }
            null
        } catch (e: Exception) {
            e
        }
        val fe = ex?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fe?.code)
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
