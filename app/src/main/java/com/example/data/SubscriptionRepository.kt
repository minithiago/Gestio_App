package com.example.data

import kotlinx.coroutines.flow.Flow

class SubscriptionRepository(private val subscriptionDao: SubscriptionDao) {
    val allSubscriptions: Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions()

    suspend fun getSubscriptionById(id: Int): Subscription? {
        return subscriptionDao.getSubscriptionById(id)
    }

    suspend fun insert(subscription: Subscription): Long {
        return subscriptionDao.insertSubscription(subscription)
    }

    suspend fun update(subscription: Subscription) {
        subscriptionDao.updateSubscription(subscription)
    }

    suspend fun delete(subscription: Subscription) {
        subscriptionDao.deleteSubscription(subscription)
    }

    suspend fun deleteAll() {
        subscriptionDao.deleteAll()
    }
}
